package client;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {

    public static final String url = "http://127.0.0.1:8080";
    private static final String userName = "Yun";

    private Path currentDirectory = Paths.get("/");
    private boolean running = true;

    public static void main(final String... args) {
        new Client();
    }


    public Client() {
        Scanner scanner = new Scanner(System.in);

        while (running) {
            try {
                System.out.printf("%s@Cloud-Storage %s %% ", userName, currentDirectory);
                String cmd = scanner.nextLine();
                handleCommand(cmd);
            } catch (IOException e) {
                System.out.println("Error: " + e);
            }
        }
    }

    void handleCommand(String cmd) throws IOException {
        try {
            String[] cmdArr = cmd.split(" ");

            switch (cmdArr[0]) {
                case "exit":
                    running = false;
                    break;
                case "ls":
                    Path path = cmdArr.length > 1 ? getAbsolutePath(cmdArr[1]) : currentDirectory;
                    System.out.println(getFilesByPath(path));
                    break;
                case "cd":
                    String directory = cmdArr[1];

                    try {
                        Path newDirectoryPath = getAbsolutePath(directory);
                        getFilesByPath(newDirectoryPath);
                        currentDirectory = newDirectoryPath;
                    } catch (Exception e) {
                        System.out.printf("cd: The directory '%s' does not exist%n", directory);
                    }
                    break;
                case "mkdir":
                    String newDir = cmdArr[1];
                    createDirByPath(getAbsolutePath(newDir));
                    break;
                case "rmdir":
                    String dirToDelete = cmdArr[1];
                    deleteEntryByPath(true, getAbsolutePath(dirToDelete));
                    break;
                case "rm":
                    String fileToDelete = cmdArr[1];
                    deleteEntryByPath(false, getAbsolutePath(fileToDelete));
                    break;
                case "pwd":
                    System.out.println(currentDirectory);
                    break;
                case "curl":
                    downloadFileByPath(getAbsolutePath(cmdArr[1]));
                    break;
                case "scp":
                    uploadFileByPath(Paths.get(cmdArr[1]));
                    break;
                default:
                    System.out.println("Invalid Command");
            }
        } catch (ArrayIndexOutOfBoundsException | URISyntaxException e) {
            System.out.println("Error: Invalid Command" + e.getMessage());
        }

    }

    String getFilesByPath(Path path) throws IOException {
        try {
            URIBuilder builder = new URIBuilder(url + "/dirs");
            builder.addParameter("path", path.toString());

            Content content = Request.Get(builder.build())
                    .execute()
                    .returnContent();
            return content.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    void uploadFileByPath(Path filePath) throws IOException, URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(url + "/files");
        uriBuilder.addParameter("path", currentDirectory.toString());

        File file = new File(filePath.toString());

        HttpClient httpclient = HttpClients.createDefault();

        HttpPost httppost = new HttpPost(uriBuilder.build());
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addPart("file", new FileBody(file));

        HttpEntity entity = builder.build();
        httppost.setEntity(entity);

        ClassicHttpResponse response = (ClassicHttpResponse) httpclient.execute(httppost);

        InputStream inputStream = response.getEntity().getContent();
        String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        System.out.println(result);
    }

    void downloadFileByPath(Path filePath) throws IOException {
        try {
            URIBuilder builder = new URIBuilder(url + "/files");
            builder.addParameter("path", filePath.toString());

            Path downloadFolderPath = Paths.get("").resolve("downloads");
            Files.createDirectories(downloadFolderPath);

            String fileName = filePath.getFileName().toString();
            Path downloadedFilePath = downloadFolderPath.resolve(fileName);
            File file = new File(downloadedFilePath.toString());

            Request.Get(builder.build())
                    .execute()
                    .saveContent(file);

            System.out.println("File downloaded to folder: " + downloadFolderPath.toAbsolutePath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    void createDirByPath(Path path) {
        try {
            URIBuilder builder = new URIBuilder(url + "/dirs");
            builder.addParameter("path", path.toString());

            Request.Post(builder.build())
                    .execute()
                    .returnContent();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    void deleteEntryByPath(boolean isDirectory, Path path) {
        try {
            URIBuilder builder = new URIBuilder(url + (isDirectory ? "/dirs" : "/files"));
            builder.addParameter("path", path.toString());

            Request.Delete(builder.build())
                    .execute()
                    .returnContent();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    Path getAbsolutePath(String pathStr) {
        if (pathStr.startsWith("/")) {
            return Paths.get(pathStr).normalize();
        } else {
            return currentDirectory.resolve(pathStr).normalize();
        }
    }
}