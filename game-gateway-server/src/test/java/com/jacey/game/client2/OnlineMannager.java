package com.jacey.game.client2;

import com.jacey.game.common.msg.IMessage;
import io.netty.channel.Channel;

/**
 * @Description:
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class OnlineMannager {

    private static OnlineMannager instance = new OnlineMannager();

    public static OnlineMannager getInstance() {
        return instance;
    }

    private Channel channel;

    public void addSession(Channel channel) {
        this.channel = channel;
    }

    public void sendMsg(IMessage message) { if (channel != null) {
            System.out.println("client2 send >>> ");
            channel.writeAndFlush(message);
        }
    }

}
