package com.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    public List<FileInfo> makeFileList(String line) {
        List<FileInfo> newList = new ArrayList<>();
        String[] files = line.split("\n");
        for (String file: files) {
            String[] data = file.split(",");
            FileInfo fileInfo = new FileInfo();
            if (data[0].equals("FILE")) {
                fileInfo.setType(FileType.FILE);
            } else {
                fileInfo.setType(FileType.DIRECTORY);
            }
            fileInfo.setFileName(data[1]);
            fileInfo.setSize(Long.parseLong(data[2]));
            fileInfo.setLastModified(LocalDateTime.parse(data[3]));
            newList.add(fileInfo);
        }
        return newList;
    }

    public void sendFile(DataOutputStream out, Path path) throws IOException {
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(new File(String.valueOf(path))));
        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + filenameBytes.length + 8);
        buffer.put(Signal.FILE);
        buffer.putInt(path.getFileName().toString().length());
        buffer.put(filenameBytes);
        buffer.putLong(Files.size(path));
        out.write(buffer.array());
        int read;
        byte[] tempBuf = new byte[1024];
        while ((read = fileIn.read(tempBuf)) != -1) {
            out.write(tempBuf, 0, read);
        }
        fileIn.close();
    }

    public void getFile(DataInputStream inputStream) throws IOException {
        System.out.println("Get file path length.");
        int filePathLength = inputStream.readInt();

        byte[] filePathBytes = new byte[filePathLength];
        inputStream.read(filePathBytes, 0, filePathLength);
        String userPath = new String(filePathBytes, StandardCharsets.UTF_8);
        System.out.println("File path received: " + userPath);

        System.out.println("Get filename length.");
        int filenameLength = inputStream.readInt();

        byte[] filenameBytes = new byte[filenameLength];
        inputStream.read(filenameBytes, 0, filenameLength);
        String filename = new String(filenameBytes, StandardCharsets.UTF_8);
        System.out.println("Filename received: " + filename);

        File downloadFile = new File(userPath + File.separator + filename);
        BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(downloadFile));

        System.out.println("Get file length.");
        long fileSize = inputStream.readLong();

        byte[] buf = new byte[256];
        for (int i = 0; i < (fileSize + 255) / 256; i++) {
            int read = inputStream.read(buf);
            fileOut.write(buf, 0, read);
        }
        fileOut.flush();
        fileOut.close();
        System.out.println("File received.");
    }

    public void delete(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void createDirectory(Path path, String folderName) throws Exception {
        File newDirectory = new File(path + File.separator + folderName);
        if (newDirectory.exists()) {
            throw new Exception("The directory is already exists");
        } else {
            if (!newDirectory.mkdir()){
                throw new Exception("Failed to create directory");
            }
        }
    }

    public void createFile(Path path, String filename) throws Exception {
        File newFile = new File(path + File.separator + filename);
        if (newFile.exists()) {
            throw new Exception("The file is already exists");
        } else {
            if (!newFile.createNewFile()) {
                throw new Exception("Failed to create file");
            }

        }
    }

    public void renameFile(Path path, String oldName, String newName) throws Exception {
        File file = new File(path + File.separator + oldName);
        File renameFile = new File(path + File.separator + newName);
        if (!file.renameTo(renameFile)) {
            throw new Exception("Failed to rename file");
        }
    }
}
