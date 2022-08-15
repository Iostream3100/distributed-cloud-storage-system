package com.example.uploadingfiles.transaction;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public class Transaction {
    private File file;
    private String path;

    public Transaction(File file, String path) {
        this.file = file;
        this.path = path;
    }

    public File getFile() {
        return file;
    }

    public String getPath() {
        return path;
    }
}
