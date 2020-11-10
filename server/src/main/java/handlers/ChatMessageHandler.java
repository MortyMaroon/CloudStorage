package handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ChatMessageHandler extends SimpleChannelInboundHandler<String>{

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected");

    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected");

    }

    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
        String[] command = s.split(" ");
        if (command[0].equals("/auth")) {
            System.out.println("Client try authorization");
        }
        if (command[0].equals("/reg")) {
            System.out.println("Client try registration");
        }
        if (command[0].equals("exit")){
            ctx.writeAndFlush("exitOk");
            ctx.close();
        }
    }
}
