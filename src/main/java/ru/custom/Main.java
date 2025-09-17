package ru.custom;

import java.io.*;
import java.util.concurrent.*;

public class Main {
    private static Process serverProcess;
    private static volatile boolean running = false;

    public static void main(String[] args) {
        startServer();
        startCommandHandler();
    }

    private static void startServer() {
        Thread serverThread = new Thread(() -> {
            try {
                String currentDir = System.getProperty("user.dir");

                serverProcess = new ProcessBuilder(
                        "java",
                        "-Xms8G", "-Xmx18G",
                        "-XX:+UseStringDeduplication",
                        "-XX:+UseCompressedOops",
                        "-XX:+UseCodeCacheFlushing",
                        "-Dfml.readTimeout=180",
                        "-jar",
                        "forge-1.7.10-10.13.4.1614-1.7.10-universal.jar",
                        "nogui"
                )
                        .directory(new File(currentDir))
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start();

                running = true;

                int exitCode = serverProcess.waitFor();
                running = false;
                System.out.println("Server end code: " + exitCode);

            } catch (IOException | InterruptedException e) {
                running = false;
                System.err.println("Server error: " + e.getMessage());
            }
        });

        serverThread.start();
    }

    private static void startCommandHandler() {
        Thread commandThread = new Thread(() -> {
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            try {
                Thread.sleep(60);

                while (true) {
                    System.out.print("> ");
                    String command = consoleReader.readLine();

                    if (command == null || "exit".equalsIgnoreCase(command)) {
                        break;
                    }

                    processCommand(command);
                }
            } catch (IOException e) {
                System.err.println("Read error: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        commandThread.start();
    }

    private static void processCommand(String command) {
        if (!running) {
            System.out.println("Server is not running!");
            return;
        }

        try {
            OutputStream output = serverProcess.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output));

            writer.println(command);
            writer.flush();
            System.out.println("Command send: " + command);

        } catch (Exception e) {
            System.err.println("Error send command: " + e.getMessage());
        }
    }

    public static void sendCommand(String command) {
        processCommand(command);
    }
}