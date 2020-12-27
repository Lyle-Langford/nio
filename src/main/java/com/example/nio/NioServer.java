package com.example.nio;

import javax.swing.text.html.HTMLDocument;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * nio服务器端
 */
public class NioServer {

    /**
     * 启动服务器
     */
    public void start() throws IOException {
        //1.创建Selector
        Selector selector = Selector.open();

        //2.通过serverSocketChannel创建Channel通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        //3.为serverSocketChannel通道绑定监听端口
        serverSocketChannel.bind(new InetSocketAddress(8000));

        //4.设置serverSocketChannel为非阻塞模式
        serverSocketChannel.configureBlocking(false);

        //5.将serverSocketChannel注册到selector上，监听连接事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务器启动成功!");

        //6.循环等待新接入的连接
        for (; ; ) { //while(true)
            //获取可用channel数量
            int readyChannels = selector.select();
            //防止返回0
            if (readyChannels == 0) {
                continue;
            }

            //获取可用channel的集合，然后遍历
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while(iterator.hasNext()){
                SelectionKey selectionKey = iterator.next();

                //移除selector中的对应selectionKey，不然selector中的selectionKey会越来越多
                iterator.remove();

                //7.根据就绪状态，调用对应方法处理业务逻辑
                {
                    //如果是接入事件
                    if (selectionKey.isAcceptable()){
                        acceptHandler(serverSocketChannel, selector);
                    }

                    //如果是可读事件
                    if (selectionKey.isReadable()){
                        readHandler(selectionKey, selector);
                    }
                }

            }

        }


    }

    /**
     * 接入事件处理器
     */
    private void acceptHandler(ServerSocketChannel serverSocketChannel,
                               Selector selector) throws IOException {
        //创建一个socketChannel，与服务器端建立连接
        SocketChannel socketChannel = serverSocketChannel.accept();

        //将socketChannel设置为非阻塞模式
        socketChannel.configureBlocking(false);

        //将socketChannel注册到selector上，监听可读事件
        socketChannel.register(selector, SelectionKey.OP_READ);

        //回复客户端，提示信息
        socketChannel.write(StandardCharsets.UTF_8.encode("你与聊天室建立连接"));
    }

    /**
     * 可读事件处理器
     */
    private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException {
        //从selectionKey中获取到已经就绪的channel
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        //创建buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        //循环读取客户端请求
        String request = "";
        while (socketChannel.read(byteBuffer) > 0){
            byteBuffer.flip();
            //读取buffer中的内容
            request += StandardCharsets.UTF_8.decode(byteBuffer);
        }

        //将channel再次注册到selector上，监听他的可读事件
        socketChannel.register(selector, SelectionKey.OP_READ);

        //将客户端发送的请求信息 广播给其他客户端
        if (request.length() > 0){
            broadcast(selector, socketChannel, request);
        }
    }

    /**
     * 广播给其他客户端
     */
    private void broadcast(Selector selector, SocketChannel sourceChannel, String request){
        //获取到所有已接入的客户端channel
        Set<SelectionKey> selectionKeySet = selector.keys();

        //循环向所有channel广播信息
        selectionKeySet.forEach(selectionKey -> {
            Channel targetChannel = selectionKey.channel();
            //剔除发消息的客户端
            if (targetChannel instanceof SocketChannel && targetChannel != sourceChannel){
                try {
                    ((SocketChannel)targetChannel).write(StandardCharsets.UTF_8.encode(request));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    public static void main(String[] args) {
        NioServer nioServer = new NioServer();
        try {
            nioServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
