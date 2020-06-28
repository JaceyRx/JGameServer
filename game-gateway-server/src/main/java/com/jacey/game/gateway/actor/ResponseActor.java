package com.jacey.game.gateway.actor;

import akka.actor.UntypedAbstractActor;
import com.jacey.game.common.msg.IMessage;
import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.msg.NetResponseMessage;
import com.jacey.game.common.proto3.Rpc;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 专门用于接收远程服务器的响应
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class ResponseActor extends UntypedAbstractActor {

    private io.netty.channel.Channel channel;

    public ResponseActor(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void onReceive(Object o) throws Throwable {
        if (o instanceof NetMessage) {
            NetMessage msg = (NetMessage) o;
            switch (msg.getRpcNum()) {
                case Rpc.RpcNameEnum.Login_VALUE: {
                    // 远程逻辑服务器返回玩家登录成功后，不仅需要返回给玩家登录成功
                    // 也需要通知自己的父Actor即ChannelActor为这个玩家的ChannelActor绑定userId
                    context().parent().tell(new NetResponseMessage(msg), self());
                    break;
                }
                case Rpc.RpcNameEnum.ForceOfflinePush_VALUE: {
                    // 强制离线响应
                    write(msg);
                    channel.close();
                }
                default: {
                    // 转发给客户端
                    write(msg);
                    break;
                }
            }
        } else {
            log.error("【消息解析失败】, not support msg type = {}", o.getClass().getName());
        }
    }

    private void write(IMessage msg) {
        if (this.channel != null && this.channel.isActive() && this.channel.isWritable()) {
            channel.writeAndFlush(msg);
        }
    }
}
