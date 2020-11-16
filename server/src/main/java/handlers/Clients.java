package handlers;

import io.netty.channel.ChannelHandlerContext;

public class Clients {

    public static void authorization(ChannelHandlerContext ctx, String str) {
        String[] parts = str.split(" ");
        String path = AuthService.checkAuthorization(parts[1],parts[2]);
        System.out.println(path);
        if (path != null) {
            ctx.writeAndFlush("/auth\nok");
        } else {
            ctx.writeAndFlush("/auth\nnoSuch");
        }
    }
}
