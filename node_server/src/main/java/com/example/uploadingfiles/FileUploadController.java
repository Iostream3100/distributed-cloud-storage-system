package com.example.uploadingfiles;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.uploadingfiles.storage.StorageException;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import com.example.uploadingfiles.storage.StorageFileNotFoundException;
import com.example.uploadingfiles.storage.StorageService;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class FileUploadController {

    private final StorageService storageService;

    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * get all the files and directories under of a directory
     *
     * @param path path of the directory
     * @return A List of String with the name of paths and directories
     */
    @GetMapping("/dirs")
    @ResponseBody
    public List<String> getDirectoriesByPath(@RequestParam(value = "path", defaultValue = "/") String path) {
        try {
            Stream<Path> pathStream = storageService.loadAllByPath(path);
            return pathStream.map(Path::toString).collect(Collectors.toList());
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
    public ResponseEntity deleteDirectoryByPath(@RequestParam(value = "path") String path) {
        storageService.deleteDirectoryByPath(path);

        return ResponseEntity.status(HttpStatus.CREATED).body("Directory Deleted");
    }


    @GetMapping("/")
    public String listUploadedFiles(Model model) {

        Stream<Path> pathStream = storageService.loadAll();
        pathStream.forEach(path -> {
            System.out.println(path.toString());

        });
        model.addAttribute("filestest", storageService.loadAll().map(
                        path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                                "serveFile", path.getFileName().toString()).build().toUri().toString())
                .collect(Collectors.toList()));

        return "uploadForm";
    }


    @GetMapping("/filestest/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }


    @GetMapping("/files")
    @ResponseBody
    public ResponseEntity<Resource> getFileByPath(@RequestParam(value = "path") String path) {
        Resource file = storageService.loadAsResource(path);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @RequestMapping(path = "/files", method = POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})

    public ResponseEntity handleFileUpload(
            @RequestBody MultipartFile file,
            @RequestParam("path") String path) {

        System.out.println(file.getName() + "--" + file.getOriginalFilename() + " <> " + file.getContentType() + " <> " + file.getSize());
        storageService.store(path, file);
//        redirectAttributes.addFlashAttribute("message",
//                "You successfully uploaded " + file.getOriginalFilename() + "!");

        return ResponseEntity.status(HttpStatus.CREATED).body("File uploaded");
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
