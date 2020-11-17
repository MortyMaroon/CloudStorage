package com.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import com.service.Clients;


public class ChatMessageHandler extends SimpleChannelInboundHandler<String>{

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected");

    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected");

    }

    protected void channelRead0(ChannelHandlerContext ctx, String s) {
        String[] command = s.split(" ");
        if (command[0].equals("/auth")) {
            System.out.println("Client try authorization");
            Clients.authorization(ctx, command[1], command[2]);

        }
        if (command[0].equals("/reg")) {
            System.out.println("Client try registration");
            System.out.println(command[1] + " " + command[2]);
            Clients.registration(ctx, command[1], command[2]);
        }
        if (command[0].equals("exit")){
            ctx.writeAndFlush("exitOk");
            ctx.close();
        }
    }
}
