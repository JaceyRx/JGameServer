package com.jacey.game.gateway.network.netty;

import com.jacey.game.gateway.manager.ConfigManager;
import com.jacey.game.gateway.network.ServerNode;
import com.jacey.game.gateway.network.netty.tcp.codec.NettyProtocolDecoder;
import com.jacey.game.gateway.network.netty.tcp.codec.NettyProtocolEncoder;
import com.jacey.game.gateway.network.netty.tcp.handle.NettySocketHandle;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @Description: Netty服务器
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class NettySocketServer implements ServerNode {

    private NettySocketServer() {}

    private static NettySocketServer instance = new NettySocketServer();

    public static NettySocketServer getInstance() {
        return instance;
    }

    // 避免使用默认线程数参数
    private EventLoopGroup bossGroup = new NioEventLoopGroup(4);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    @Override
    public void start() throws Exception {
        int serverPort = ConfigManager.CLIENT_PORT;
        // Netty 服务端启动引导
        ServerBootstrap bootstrap = new ServerBootstrap();
        try {
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChildChannelHandler());
            bootstrap.bind(new InetSocketAddress(serverPort)).sync();
            log.info("netty socket服务已启动，正在监听用户的请求@port:" + serverPort + "......");
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            throw e;
        }
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            ChannelPipeline pipeline = socketChannel.pipeline();
            pipeline.addLast(new NettyProtocolDecoder()); // 自定义解码器
            pipeline.addLast(new NettyProtocolEncoder()); // 自定义编码器
            // 心跳处理：客户端300秒没收发包，便会触发IdleStateEvent事件。到UserEventTriggered方法处理
            pipeline.addLast(new IdleStateHandler(ConfigManager.SOCKET_READER_IDLE_TIME,
                    ConfigManager.SOCKET_WRITER_IDLE_TIME,
                    ConfigManager.SOCKET_ALL_IDLE_TIME));
            pipeline.addLast(new NettySocketHandle());   // 消息处理
        }

    }

    @Override
    public void shutdown() {

    }


}
