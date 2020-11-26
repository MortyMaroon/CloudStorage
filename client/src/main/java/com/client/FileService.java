package com.client;

import com.utils.FileInfo;
import com.utils.FileType;
import com.utils.Signal;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    private ByteBuffer buffer;


    public List<FileInfo> makeFileList(String line) {
        List<FileInfo> newList = new ArrayList<>();
        String[] files = line.split("\n");
        for (String file: files) {
            String[] data = file.split(",");
            FileInfo fileInfo = new FileInfo();
            if (data[0].equals(FileType.FILE)) {
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

    public void sendCommand(DataOutputStream out, String command) throws IOException {
        buffer = ByteBuffer.allocate(1+4+command.length());
        buffer.put(Signal.COMMAND);
        buffer.putInt(command.length());
        buffer.put(command.getBytes());
        out.write(buffer.array());
        buffer.clear();
    }

    public void sendFile(DataOutputStream out, Path path) throws IOException {
        BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(new File(String.valueOf(path))));
        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        buffer = ByteBuffer.allocate(1 + 4 + filenameBytes.length + 8);
        buffer.put(Signal.FILE);
        buffer.putInt(path.getFileName().toString().length());
        buffer.put(filenameBytes);
        buffer.putLong(Files.size(path));
        out.write(buffer.array());
        buffer.clear();
        int read;
        byte[] buf = new byte[256];
        while ((read = fileIn.read(buf)) != -1) {
            buffer.put(buf, 0, read);
        }
        out.write(buffer.array());
        buffer.clear();
        fileIn.close();
    }

    public void delete(Path path) throws IOException {
        Files.delete(path);
    }

    public void createDirectory(Path path, String directory) throws Exception {
        File newDirectory = new File(path + File.separator + directory);
        if (newDirectory.exists()) {
            throw new Exception("The directory is already exists");
        } else {
            newDirectory.mkdir();
        }
    }
}
