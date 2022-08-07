package com.example.uploadingfiles;

import com.example.uploadingfiles.storage.StorageException;
import com.example.uploadingfiles.storage.StorageFileNotFoundException;
import com.example.uploadingfiles.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class FileUploadController {

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
     */
    @PostMapping("/dirs")
    @ResponseBody
    public ResponseEntity createDirectoryByPath(@RequestParam(value = "path") String path) {
        storageService.createDirectoryByPath(path);

        return ResponseEntity.status(HttpStatus.CREATED).body("Directory Created");
    }


    /**
     * delete a directory by path
     *
     * @param path path of the directory
     * @return response
     */
    @DeleteMapping("/dirs")
    @ResponseBody
    public ResponseEntity<?> deleteDirectoryByPath(@RequestParam(value = "path") String path) {
        storageService.deleteDirectoryByPath(path);

        return ResponseEntity.status(HttpStatus.CREATED).body("Directory Deleted");
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
     */
    @RequestMapping(path = "/files", method = POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> handleFileUpload(@RequestBody MultipartFile file, @RequestParam("path") String path) {
        storageService.store(path, file);

        return ResponseEntity.status(HttpStatus.CREATED).body("File uploaded");
    }


    /**
     * delete a file by path
     *
     * @param path path of the file
     * @return response
     */
    @DeleteMapping("/files")
    @ResponseBody
    public ResponseEntity<?> deleteFileByPath(@RequestParam(value = "path") String path) {
        storageService.deleteFileByPath(path);

        return ResponseEntity.status(HttpStatus.CREATED).body("Directory Deleted");
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
}
