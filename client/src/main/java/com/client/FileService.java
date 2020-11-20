package com.client;

import com.utils.FileInfo;
import com.utils.FileType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            fileInfo.setType(data[0].equals(FileType.FILE.toString()) ? FileType.FILE : FileType.DIRECTORY);
            fileInfo.setFileName(data[1]);
            fileInfo.setSize(Long.parseLong(data[2]));
            fileInfo.setLastModified(LocalDateTime.parse(data[3]));
            newList.add(fileInfo);
        }
        return  newList;
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
