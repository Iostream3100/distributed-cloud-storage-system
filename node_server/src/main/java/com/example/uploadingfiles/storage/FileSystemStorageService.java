package com.example.uploadingfiles.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation;

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public void store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file.");
            }
            Path destinationFile = this.rootLocation.resolve(
                            Paths.get(Objects.requireNonNull(file.getOriginalFilename())))
                    .normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                // This is a security check
                throw new StorageException(
                        "Cannot store file outside current directory.");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @Override
    public void store(String path, MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file.");
            }


            Path folderPath = load(path)
                    .normalize()
                    .toAbsolutePath();

            if (!Files.exists(folderPath)) {
                throw new StorageException("Folder doesn't exist");
            }

            Path destinationFile = folderPath.resolve(Paths.get(Objects.requireNonNull(file.getOriginalFilename())));

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file.", e);
        }
    }

    @Override
    public void createDirectoryByPath(String dirPath) {
        try {
            Path path = load(dirPath);
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new StorageException("Failed to create directory.", e);
        }

    }

    @Override
    public void deleteDirectoryByPath(String dirPath) {
        try {
            Path path = load(dirPath);

            if (!Files.exists(path)) {
                throw new StorageException("Directory doesn't exist.");
            }
            if (!Files.isDirectory(path)) {
                throw new StorageException(dirPath + " is not a directory");
            }
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new StorageException("Failed to delete directory.", e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(this.rootLocation::relativize);
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
    }

    @Override
    public Stream<Path> loadAllByPath(String rootPath) throws NoSuchFileException {
        try {
            if (!rootPath.startsWith("/")) {
                throw new StorageException("Path should start with /");
            }

            Path dirPath = load(rootPath);

            if (!Files.isDirectory(dirPath)) {
                throw new StorageException("This is not a directory");
            }
            return Files.walk(dirPath, 1)
                    .filter(path -> !path.equals(dirPath))
                    .map(dirPath::relativize);
        } catch (NoSuchFileException nfe) {
            throw nfe;
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
    }

    /**
     * get the path of an entry on disk,
     * if the entry is not a sub entry of the root location, return root location.
     *
     * @param entryName name of the entry
     * @return path on disk
     */
    @Override
    public Path load(String entryName) {
        if (entryName.startsWith("/")) {
            entryName = entryName.substring(1);
        }

        Path entryPath = rootLocation.resolve(entryName).normalize();

        // check if the path is outside the root folder
        if (!entryPath.startsWith(rootLocation)) {
            entryPath = rootLocation;
        }
        return entryPath;
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);

            if (Files.isDirectory(file)) {
                throw new StorageException("Cannot download a folder");
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);

            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteFileByPath(String filePath) {
        try {
            Path path = load(filePath);
            if (!Files.exists(path)) {
                throw new StorageException("File doesn't exist.");
            }
            if (Files.isDirectory(path)) {
                throw new StorageException(filePath + " is not a file");
            }
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new StorageException("Failed to delete directory.", e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }
}
