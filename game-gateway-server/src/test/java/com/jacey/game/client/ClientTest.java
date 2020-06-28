package com.jacey.game.client;

import com.jacey.game.common.msg.NetMessage;
import com.jacey.game.common.proto3.CommonEnum;
import com.jacey.game.common.proto3.CommonMsg;
import com.jacey.game.common.proto3.Rpc;
import com.jacey.game.common.utils.MD5Util;
import com.jacey.game.gateway.network.netty.tcp.codec.NettyProtocolDecoder;
import com.jacey.game.gateway.network.netty.tcp.codec.NettyProtocolEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * @Description: 测试类
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
public class ClientTest {

    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    private String host = "127.0.0.1";
    private int port = 10001;

    public void test() throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .remoteAddress(new InetSocketAddress(host, port))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new NettyProtocolDecoder())
                                .addLast(new NettyProtocolEncoder())
                                .addLast(new ClientHandle());
                    }
                });
        bootstrap.connect().sync();
    }

    public void sendTest() {
        System.out.println("..................");
        CommonMsg.LoginRequest.Builder builder= CommonMsg.LoginRequest.newBuilder();
        builder.setUsername("张三");
        builder.setPasswordMD5("xxxxxx");
        NetMessage message = new NetMessage(Rpc.RpcNameEnum.Login_VALUE, builder);
        System.out.println("length == " + message.getTotalLength());
        System.out.println("data length == " + message.getDataLength());
        OnlineMannager.getInstance().sendMsg(message);
    }

    public void registTest() {
        System.out.println("========开始注册======");
        CommonMsg.RegistRequest.Builder builder = CommonMsg.RegistRequest.newBuilder();
        builder.setUsername("gds");
        builder.setPassword("12345678");
        NetMessage message = new NetMessage(Rpc.RpcNameEnum.Regist_VALUE, builder);
        OnlineMannager.getInstance().sendMsg(message);
    }

    public void loginTest() {
        System.out.println("========开始登录======");
        CommonMsg.LoginRequest.Builder loginRequest = CommonMsg.LoginRequest.newBuilder();
        loginRequest.setUsername("gds");
        loginRequest.setPasswordMD5(MD5Util.md5("12345678"));
        NetMessage message = new NetMessage(Rpc.RpcNameEnum.Login_VALUE, loginRequest);
        OnlineMannager.getInstance().sendMsg(message);
    }

    public void matchTest() {
        System.out.println("========开始匹配======");
        CommonMsg.MatchRequest.Builder builder = CommonMsg.MatchRequest.newBuilder();
        builder.setBattleType(CommonEnum.BattleTypeEnum.BattleTypeTwoPlayer);
        NetMessage message = new NetMessage(Rpc.RpcNameEnum.Match_VALUE, builder);
        OnlineMannager.getInstance().sendMsg(message);
    }

    public void cancelMatchTest() {
        System.out.println("========取消匹配======");
        CommonMsg.CancelMatchRequest.Builder builder = CommonMsg.CancelMatchRequest.newBuilder();
        NetMessage message = new NetMessage(Rpc.RpcNameEnum.CancelMatch_VALUE, builder);
        OnlineMannager.getInstance().sendMsg(message);
    }

    public static void main(String[] args) throws Exception {
        ClientTest ts = new ClientTest();
        ts.test();
//        ts.registTest();
//        Thread.sleep(15000);
        ts.loginTest();
        Thread.sleep(20000);
        ts.matchTest();
//        Thread.sleep(10000);
//        ts.cancelMatchTest();
    }

}
