package handlers;

import io.netty.channel.ChannelHandlerContext;

import java.util.Vector;

public class Clients {
    private static Vector<String> onlineUsers;

    public static void authorization(ChannelHandlerContext ctx, String str) {
        String[] parts = str.split(" ");
        String login = AuthService.checkAuthorization(parts[1],parts[2]);
        if (login != null) {
            if (checkOnline(login)) {
                ctx.writeAndFlush("/auth\nok");
                addOnlineUsers(login);
            } else {
                ctx.writeAndFlush("/auth\nbusy");
            }
        } else {
            ctx.writeAndFlush("/auth\nnoSuch");
        }
    }

    private static boolean checkOnline(String client) {
        if (!onlineUsers.isEmpty()) {
            for (String user: onlineUsers) {
                if (user.equals(client)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void addOnlineUsers(String user) {
        onlineUsers.add(user);
    }
}
