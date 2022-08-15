package com.example.uploadingfiles;

import com.example.uploadingfiles.mode.Mode;
import com.example.uploadingfiles.storage.StorageException;
import com.example.uploadingfiles.storage.StorageFileNotFoundException;
import com.example.uploadingfiles.storage.StorageService;
import com.example.uploadingfiles.transaction.Transaction;
import org.apache.http.client.fluent.Request;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.example.uploadingfiles.mode.Mode.COMMIT;
import static com.example.uploadingfiles.mode.Mode.PROPOSE;
import static com.example.uploadingfiles.transaction.TransactionResponse.*;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class FileUploadController {
    @Value("${nodeServerUrls}")
    private String[] nodeServerUrls;
    private final StorageService storageService;

    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * get all the files and directories under a directory
     *
     * @param path path of the directory
     * @return A List of String with the name of paths and directories
     */
    @GetMapping("/dirs")
    @ResponseBody
    public ResponseEntity<?> getDirectoriesByPath(@RequestParam(value = "path", defaultValue = "/") String path) {
        try {
            Stream<Path> pathStream = storageService.loadAllByPath(path);
            String dirs = String.join(" ", pathStream.map(Path::toString).collect(Collectors.toList()));

            return ResponseEntity.ok(dirs);
        } catch (NoSuchFileException exc) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Invalid path, directory not found", exc);
        }
    }

    /**
     * create a directory by path
     *
     * @param path path of the new directory
     * @return response
     * TODO Add Paxos
     */
    @PostMapping("/dirs")
    @ResponseBody
    public ResponseEntity createDirectoryByPath(@RequestParam(value = "path") String path, @RequestParam(required = false) Mode mode) {
        if (mode == null) {
            mode = Mode.PREPARE;
        }
        switch (mode) {
            case COMMIT:
                storageService.createDirectoryByPath(path);
                return ResponseEntity.status(HttpStatus.CREATED).body("Directory Created");
            case PREPARE:
                // START PAXOS
                Boolean proposeResult = this.sendProposeMessage(new Transaction(null, path), "/dirs");
                if (!proposeResult) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_PROPOSE_FAILED);
                }

                Boolean commitResult = this.sendCommitMessage(new Transaction(null, path), "/dirs");
                if (!commitResult) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_FAILED);
                }
                return ResponseEntity.status(HttpStatus.CREATED).body("Directory Created");
            case PROPOSE:
                return ResponseEntity.status(HttpStatus.OK).body(TXN_PROPOSE_SUCCESS);
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_UNKNOWN_MODE);
        }
    }


    /**
     * delete a directory by path
     *
     * @param path path of the directory
     * @return response
     * TODO Add Paxos
     */
    @DeleteMapping("/dirs")
    @ResponseBody
    public ResponseEntity<?> deleteDirectoryByPath(@RequestParam(value = "path") String path, @RequestParam(required = false) Mode mode) {
        if (mode == null) {
            mode = Mode.PREPARE;
        }
        switch (mode) {
            case COMMIT:
                storageService.deleteDirectoryByPath(path);
                return ResponseEntity.status(HttpStatus.CREATED).body("Directory Deleted");
            case PREPARE:
                // START PAXOS
                Boolean proposeResult = this.sendProposeDeleteMessaage(new Transaction(null, path), "/dirs");
                if (!proposeResult) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_PROPOSE_FAILED);
                }

                Boolean commitResult = this.sendCommitDeleteMessaage(new Transaction(null, path), "/dirs");
                if (!commitResult) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_FAILED);
                }
                return ResponseEntity.status(HttpStatus.CREATED).body("Directory Deleted");
            case PROPOSE:
                return ResponseEntity.status(HttpStatus.OK).body(TXN_PROPOSE_SUCCESS);
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_UNKNOWN_MODE);
        }
    }

    /**
     * get a file by path
     *
     * @param path path of the file
     * @return file
     */
    @GetMapping("/files")
    @ResponseBody
    public ResponseEntity<Resource> getFileByPath(@RequestParam(value = "path") String path) {
        Resource file = storageService.loadAsResource(path);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    /**
     * upload a file in a multipart form
     *
     * @param file file to upload
     * @param path path of the file
     * @return response
     *
     * TODO Add Paxos
     */
    @RequestMapping(path = "/files", method = POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> handleFileUpload(@RequestBody MultipartFile file, @RequestParam("path") String path, @RequestParam(required = false) Mode mode) {
        if (mode == null) {
            mode = Mode.PREPARE;
        }
        switch (mode) {
            case COMMIT:
                storageService.store(path, file);
                return ResponseEntity.status(HttpStatus.CREATED).body("File uploaded");
            case PREPARE:
                // START PAXOS
                File tmpFile = null;
                if (file != null) {
                    tmpFile = new File(System.getProperty("java.io.tmpdir")+"/" + file.getOriginalFilename());
                    try {
                        file.transferTo(tmpFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                Boolean proposeResult = this.sendProposeMessage(new Transaction(tmpFile, path), "/files");
                if (!proposeResult) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_PROPOSE_FAILED);
                }

                Boolean commitResult = this.sendCommitMessage(new Transaction(tmpFile, path), "/files");
                if (!commitResult) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_FAILED);
                }
                return ResponseEntity.status(HttpStatus.CREATED).body("File uploaded");
            case PROPOSE:
                return ResponseEntity.status(HttpStatus.OK).body(TXN_PROPOSE_SUCCESS);
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_UNKNOWN_MODE);
        }
    }


    /**
     * delete a file by path
     *
     * @param path path of the file
     * @return response
     *
     * TODO Add Paxos
     */
    @DeleteMapping("/files")
    @ResponseBody
    public ResponseEntity<?> deleteFileByPath(@RequestParam(value = "path") String path, @RequestParam(required = false) Mode mode) {
        if (mode == null) {
            mode = Mode.PREPARE;
        }
        switch (mode) {
            case COMMIT:
                storageService.deleteFileByPath(path);
                return ResponseEntity.status(HttpStatus.CREATED).body("Directory Deleted");
            case PREPARE:
                // START PAXOS
                Boolean proposeResult = this.sendProposeDeleteMessaage(new Transaction(null, path), "/files");
                if (!proposeResult) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_PROPOSE_FAILED);
                }

                Boolean commitResult = this.sendCommitDeleteMessaage(new Transaction(null, path), "/files");
                if (!commitResult) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_FAILED);
                }
                return ResponseEntity.status(HttpStatus.CREATED).body("Directory Deleted");
            case PROPOSE:
                return ResponseEntity.status(HttpStatus.OK).body(TXN_PROPOSE_SUCCESS);
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TXN_UNKNOWN_MODE);
        }
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageException exc) {
        return new ResponseEntity<Object>(
                exc.getMessage(), new HttpHeaders(), HttpStatus.FORBIDDEN);

    }

    private Boolean sendProposeMessage(Transaction txn, String url) {

        AtomicInteger successCounter = new AtomicInteger();

        for (String i : this.nodeServerUrls) {
            Thread t = new Thread(() -> {
                URIBuilder uriBuilder = null;
                try {
                    uriBuilder = new URIBuilder(i + url);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                uriBuilder.addParameter("path", txn.getPath());
                uriBuilder.addParameter("mode", String.valueOf(PROPOSE));

                HttpClient httpclient = HttpClients.createDefault();

                HttpPost httppost = null;
                try {
                    httppost = new HttpPost(uriBuilder.build());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                if (txn.getFile() != null) {
                    builder.addPart("file", new FileBody(txn.getFile()));
                    HttpEntity entity = builder.build();
                    httppost.setEntity(entity);
                }

                ClassicHttpResponse response = null;
                try {
                    response = (ClassicHttpResponse) httpclient.execute(httppost);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                InputStream inputStream = null;
                try {
                    inputStream = response.getEntity().getContent();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String result;
                try {
                    result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (response.getCode() == 200) {
                    successCounter.addAndGet(1);
                }
            });
            t.start();
            try {
                t.join();
            } catch (Exception ignored) {

            }
        }
        return successCounter.get() >= this.nodeServerUrls.length / 2 + 1;
    }

    private Boolean sendProposeDeleteMessaage(Transaction txn, String  url) {
        AtomicInteger successCounter = new AtomicInteger();
        for (String i : this.nodeServerUrls) {
            Thread t = new Thread(() -> {
                try {
                    URIBuilder uriBuilder = new URIBuilder(i + url);
                    uriBuilder.addParameter("path", txn.getPath());
                    uriBuilder.addParameter("mode", String.valueOf(PROPOSE));
                    Request.Delete(uriBuilder.build()).execute().returnContent();
                } catch (URISyntaxException | IOException var4) {
                    throw new RuntimeException(var4);
                }
                successCounter.addAndGet(1);
            });
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        return successCounter.get() >= this.nodeServerUrls.length / 2 + 1;
    }

        private Boolean sendCommitDeleteMessaage(Transaction txn, String  url) {
        AtomicInteger successCounter = new AtomicInteger();
        for (String i : this.nodeServerUrls) {
            Thread t = new Thread(() -> {
                try {
                    URIBuilder uriBuilder = new URIBuilder(i + url);
                    uriBuilder.addParameter("path", txn.getPath());
                    uriBuilder.addParameter("mode", String.valueOf(COMMIT));
                    Request.Delete(uriBuilder.build()).execute().returnContent();
                } catch (URISyntaxException | IOException var4) {
                    throw new RuntimeException(var4);
                }
                successCounter.addAndGet(1);
            });
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };

        return successCounter.get() >= this.nodeServerUrls.length / 2 + 1;
    }

    private Boolean sendCommitMessage(Transaction txn, String url) {

        AtomicInteger successCounter = new AtomicInteger();

        for (String i : this.nodeServerUrls) {
            Thread t = new Thread(() -> {
                URIBuilder uriBuilder = null;
                try {
                    uriBuilder = new URIBuilder(i + url);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                uriBuilder.addParameter("path", txn.getPath());
                uriBuilder.addParameter("mode", String.valueOf(COMMIT));

                HttpClient httpclient = HttpClients.createDefault();

                HttpPost httppost = null;
                try {
                    httppost = new HttpPost(uriBuilder.build());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                if (txn.getFile() != null) {
                    builder.addPart("file", new FileBody(txn.getFile()));
                    HttpEntity entity = builder.build();
                    httppost.setEntity(entity);
                }

                ClassicHttpResponse response = null;
                try {
                    response = (ClassicHttpResponse) httpclient.execute(httppost);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                InputStream inputStream = null;
                try {
                    inputStream = response.getEntity().getContent();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String result;
                try {
                    result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (response.getCode() == 201) {
                    successCounter.addAndGet(1);
                }
            });
            t.start();
            try {
                t.join();
            } catch (Exception ignored) {

            }
        }

        return successCounter.get() >= this.nodeServerUrls.length / 2 + 1;
    }
}
