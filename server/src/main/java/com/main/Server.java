package com.main;

import com.service.AuthService;
import com.handlers.CloudStorageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Server {

    public Server() {
        AuthService.connect();
        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            AuthService.connect();
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(auth,worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast(
                                    new CloudStorageHandler()
                            );
                        }
                    });
            ChannelFuture future = bootstrap.bind(8189).sync();
            System.out.println("Server started");
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            auth.shutdownGracefully();
            worker.shutdownGracefully();
            AuthService.disconnect();
        }
    }
}
