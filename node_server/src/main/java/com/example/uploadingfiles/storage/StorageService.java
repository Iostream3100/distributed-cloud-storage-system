package com.example.uploadingfiles.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageService {

    void init();

    void store(MultipartFile file);

    void store(String path, MultipartFile file);

    void createDirectoryByPath(String dirPath);

    void deleteDirectoryByPath(String dirPath);


    Stream<Path> loadAll();

    Stream<Path> loadAllByPath(String rootPath) throws NoSuchFileException;

    Path load(String filename);

    Resource loadAsResource(String filename);

    void deleteAll();

}
