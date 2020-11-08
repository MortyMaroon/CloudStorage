package nio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class NioTelnetServer {

    private final ByteBuffer BUFFER = ByteBuffer.allocate(1024);
    private final String ROOT_PATH = "server";
    private final int PORT = 8189;
    private final HashMap<SocketAddress, Path> usersDirectory;

    public NioTelnetServer() throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(PORT));
        server.configureBlocking(false);
        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        usersDirectory = new HashMap<>();
        System.out.println("Server started!");
        while (server.isOpen()) {
            selector.select();
            var selectionKeys = selector.selectedKeys();
            var iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                var key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    // TODO: 04.11.2020  
    //  ls - список файлов (сделано на уроке),
    //  cd (name) - перейти в папку
    //  touch (name) создать текстовый файл с именем
    //  mkdir (name) создать директорию
    //  rm (name) удалить файл по имени
    //  copy (src, target) скопировать файл из одного пути в другой
    //  cat (name) - вывести в консоль содержимое файла

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        int read = channel.read(BUFFER);
        if (read == -1) {
            channel.close();
            return;
        }
        if (read == 0) {
            return;
        }
        BUFFER.flip();
        byte[] buf = new byte[read];
        int pos = 0;
        while (BUFFER.hasRemaining()) {
            buf[pos++] = BUFFER.get();
        }
        BUFFER.clear();
        String command = new String(buf, StandardCharsets.UTF_8)
                .replace("\n", "")
                .replace("\r", "");
        if (command.equals("--help")) {
            channel.write(ByteBuffer.wrap(("input command:\n\r" +
                    "ls for show file list\n\r" +
                    "cd (name) for change current directory\n\r" +
                    "touch (name) for create .txt file\n\r" +
                    "mkdir (name) for create directory\n\r" +
                    "rm (name) for delete file\n\r" +
                    "copy (src, target) for copy file from src in target\n\r" +
                    "cat (name) for output file contents\n\r").getBytes()));
        } else if (command.equals("ls")) {
            channel.write(ByteBuffer.wrap(getFilesList(channel.getRemoteAddress()).getBytes()));
        } else if (command.startsWith("cd ")) {
            channel.write(ByteBuffer.wrap(changeDirectory(channel.getRemoteAddress(), command.substring(3)).getBytes()));
        } else if (command.startsWith("touch ")) {
            channel.write(ByteBuffer.wrap(createFile(channel.getRemoteAddress(), command.substring(6)).getBytes()));
        } else if (command.startsWith("mkdir ")) {
            channel.write(ByteBuffer.wrap(createDirectory(channel.getRemoteAddress(), command.substring(6)).getBytes()));
        } else if (command.startsWith("rm ")) {
            channel.write(ByteBuffer.wrap(deleteFile(channel.getRemoteAddress(), command.substring(3)).getBytes()));
        } else if (command.startsWith("copy ")) {
            channel.write(ByteBuffer.wrap(copyFile(channel.getRemoteAddress(), command.substring(5)).getBytes()));
        } else if (command.startsWith("cat ")) {
            readFile(channel.getRemoteAddress(), command.substring(4));
        }
    }

    private String  getFilesList(SocketAddress address) {
        return String.join("/", new File(usersDirectory.get(address).toString()).list()) + "\n\r";
    }

    private String changeDirectory (SocketAddress address, String directory) {
        Path path = Path.of(ROOT_PATH, directory);
        if (directory.equals(ROOT_PATH)) {
            usersDirectory.put(address,Path.of(ROOT_PATH));
            return path.toString() + "\n\r";
        }
        if (!Files.exists(path)) {
            return "path not found\n\r";
        } else {
            usersDirectory.put(address,path);
            return directory + "\n\r";
        }
    }

    private String createFile(SocketAddress address, String fileName) {
        Path path = Path.of(usersDirectory.get(address).toString(), fileName);
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
                return "file was created\n\r";
            } catch (IOException e) {
                e.printStackTrace();
                return "error: " + e.getMessage() +"\n\r";
            }
        } else {
           return "file already exist\n\r";
        }
    }

    private String createDirectory(SocketAddress address, String directory) {
        Path path = Path.of(usersDirectory.get(address).toString(), directory);
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
                return "file is created\n\r";
            } catch (IOException e) {
                e.printStackTrace();
                return "error: " + e.getMessage() +"\n\r";
            }
        } else {
            return "file already exist\n\r";
        }
    }

    private String deleteFile(SocketAddress address, String fileName) {
        Path path = Path.of(usersDirectory.get(address).toString(), fileName);
        if (Files.exists(path)) {
            try {
                Files.delete(path);
                return "file deleted\n\r";
            } catch (IOException e) {
                e.printStackTrace();
                return "error: " + e.getMessage() + "\n\r";
            }
        } else {
            return "file not exist\n\r";
        }
    }

    private String copyFile(SocketAddress address, String command) {
        String[] cmd = command.split(" ");
        Path oldPath = Path.of(usersDirectory.get(address).toString(), cmd[0]);
        Path newPath = Path.of(usersDirectory.get(address).toString(), cmd[1]);
        if (Files.exists(oldPath) && Files.exists(newPath)) {
            try {
                Files.copy(oldPath, newPath);
                return "file copied\n\r";
            } catch (IOException e) {
                e.printStackTrace();
                return "error: " + e.getMessage() + "\n\r";
            }
        } else {
            return "invalid file path\n\r";
        }
    }

    private void readFile(SocketAddress address, String fileName) {
        try {
            FileChannel channel = new RandomAccessFile(usersDirectory.get(address).toString() + "/" + fileName, "rw").getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = channel.read(buffer);
            while (bytesRead != -1) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    System.out.print((char) buffer.get());
                }
                buffer.clear();
                bytesRead = channel.read(buffer);
            }
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        SocketAddress ipUser = channel.getRemoteAddress();
        if (!usersDirectory.containsKey(ipUser)) {
            usersDirectory.put(ipUser, Path.of(ROOT_PATH));
        }
        System.out.println("Client accepted. IP: " + ipUser.toString());
        channel.register(selector, SelectionKey.OP_READ);
        channel.write(ByteBuffer.wrap(("enter command: ").getBytes()));
    }
}
