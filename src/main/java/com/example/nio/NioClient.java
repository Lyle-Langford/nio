package com.example.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * nio客户端
 */
public class NioClient {

    public void start() throws IOException {
        //链接服务器端
        SocketChannel socketChannel = SocketChannel.open(
                new InetSocketAddress("127.0.0.1", 8000));

        //接收服务器响应(异步)
        Selector selector = Selector.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        new Thread(new NioClientHandler(selector)).start();

        //向服务器端发送数据
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()){
            String request = scanner.nextLine();
            if (request != null && request.length() > 0){
                socketChannel.write(StandardCharsets.UTF_8.encode(request));
            }
        }
    }

    public static void main(String[] args) {
        try {
            new NioClient().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
