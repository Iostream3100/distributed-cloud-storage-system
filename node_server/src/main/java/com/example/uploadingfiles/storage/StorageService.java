package com.example.uploadingfiles.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageService {

    /**
     * initialize the storage service by creating the root folder.
     */
    void init();

    void store(MultipartFile file);

    /**
     * store a file by path
     *
     * @param path path
     * @param file file
     */
    void store(String path, MultipartFile file);

    /**
     * create a directory by path.
     *
     * @param dirPath path of the directory.
     */
    void createDirectoryByPath(String dirPath);

    /**
     * delete a directory by its path
     *
     * @param dirPath path of the directory
     */
    void deleteDirectoryByPath(String dirPath);

    /**
     * delete a file by its path.
     *
     * @param filePath path of the file
     */
    void deleteFileByPath(String filePath);

    Stream<Path> loadAllByPath(String rootPath) throws NoSuchFileException;

    /**
     * get the path of an entry on disk,
     * if the entry is not a sub entry of the root location, return root location.
     *
     * @param entryName name of the entry
     * @return path on disk
     */
    Path load(String entryName);

    /**
     * load a file as resource by its path.
     *
     * @param filePath path of the file
     * @return resource of the file
     */
    Resource loadAsResource(String filePath);

    /**
     * delete all files and directories in the storage.
     */
    void deleteAll();

}
