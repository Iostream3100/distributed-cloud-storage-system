package client;

import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.HttpEntity;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Scanner;

public class Client {

    public static String url = "http://127.0.0.1:8080";
    private String userName = "Yun";

    private Path currentDirectory = Paths.get("/");
    private boolean running = true;

    public static void main(final String... args) throws Exception {
        // The fluent API relieves the user from having to deal with manual
        // deallocation of system resources at the cost of having to buffer
        // response content in memory in some cases.

        new Client();
//        System.out.println(Request.Get(url + "/dirs").execute().returnContent());
//        Request.Post("http://targethost/login")
//                .bodyForm(Form.form().add("username",  "vip").add("password",  "secret").build())
//                .execute().returnContent();
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
                    deleteDirByPath(getAbsolutePath(dirToDelete));
                    break;
                case "pwd":
                    System.out.println(currentDirectory);
                    break;
                case "curl":
                    downloadFileByPath(getAbsolutePath(cmdArr[1]));
                    break;
                case "scp":
                    uploadFileByPath(Paths.get(cmdArr[1]));
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
//        URIBuilder uriBuilder = new URIBuilder(url + "/files");
//
//        File file = new File(filePath.toString());
//
//        CloseableHttpClient httpClient = HttpClients.createDefault();
//        HttpPost uploadFile = new HttpPost(uriBuilder.build());
//        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//        builder.addTextBody("field1", "yes", ContentType.TEXT_PLAIN);
//
//        // This attaches the file to the POST:
//        builder.addBinaryBody(
//                "file",
//                new FileInputStream(file),
//                ContentType.APPLICATION_OCTET_STREAM,
//                file.getName()
//        );
//
//        HttpEntity multipart = builder.build();
//        uploadFile.setEntity(multipart);
//        CloseableHttpResponse response = httpClient.execute(uploadFile);
//        HttpEntity responseEntity = response.getEntity();
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

    void deleteDirByPath(Path path) {
        try {
            URIBuilder builder = new URIBuilder(url + "/dirs");
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