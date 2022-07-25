package client;

//import org.apache.hc.client5.http.fluent.Form;
//import org.apache.hc.client5.http.fluent.Request;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Client {

    public static String url = "http://127.0.0.1:8080";
    private String userName = "Yun";
    private String currentDirectory = "/";
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
                System.out.println("Error: " + e.getMessage());
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
                    String path = cmdArr.length > 1 ? cmdArr[1] : currentDirectory;
                    System.out.println(getFilesByPath(path));
                    break;
                case "cd":
                    String newPath = cmdArr[1];

                    try {
                        String absolutePath = "";
                        if (cmdArr[1].startsWith("/")) {
                            absolutePath = newPath;
                        } else {
                            if (currentDirectory.equals("/")) {
                                absolutePath = currentDirectory + newPath;
                            } else {
                                absolutePath += currentDirectory + "/" + newPath;
                            }
                        }
                        getFilesByPath(absolutePath);
                        currentDirectory = absolutePath;
                    } catch (Exception e) {
                        System.out.println(String.format("cd: The directory '%s' does not exist", newPath));
                    }

                    break;
                case "pwd":
                    System.out.println(currentDirectory);
                    break;
                default:
                    System.out.println("Invalid Command");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Invalid Command");
        }

    }

    String getFilesByPath(String path) throws IOException {
        try {
            URIBuilder builder = new URIBuilder(url + "/dirs");
            builder.addParameter("path", path);
            Content content = Request.Get(builder.build())
                    .execute()
                    .returnContent();
            List<String> files = new ArrayList<>(Arrays.asList(content.toString()));
            return content.toString();

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}