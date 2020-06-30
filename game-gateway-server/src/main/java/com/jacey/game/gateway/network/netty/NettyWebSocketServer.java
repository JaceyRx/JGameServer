package com.jacey.game.gateway.network.netty;

import com.jacey.game.gateway.manager.ConfigManager;
import com.jacey.game.gateway.network.ServerNode;
import com.jacey.game.gateway.network.netty.tcp.codec.NettyProtocolDecoder;
import com.jacey.game.gateway.network.netty.tcp.codec.NettyProtocolEncoder;
import com.jacey.game.gateway.network.netty.tcp.handle.NettySocketHandle;
import com.jacey.game.gateway.network.netty.websocket.handle.NettyWebSocketHandle;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @Description: TODO
 * @Author: JaceyRuan
 * @Email: jacey.ruan@outlook.com
 */
@Slf4j
public class NettyWebSocketServer implements ServerNode {

    private NettyWebSocketServer() {}

    private static NettyWebSocketServer instance = new NettyWebSocketServer();

    public static NettyWebSocketServer getInstance() {
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
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChildChannelHandler());
            bootstrap.bind(new InetSocketAddress(serverPort)).sync();
            log.info("netty WebSocket服务已启动，正在监听用户的请求@port:" + serverPort + "......");
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            throw e;
        }
    }

    @Override
    public void shutdown() {

    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            ChannelPipeline pipeline = socketChannel.pipeline();


            // 心跳处理：客户端300秒没收发包，便会触发IdleStateEvent事件。到UserEventTriggered方法处理
            pipeline.addLast(new IdleStateHandler(ConfigManager.SOCKET_READER_IDLE_TIME,
                    ConfigManager.SOCKET_WRITER_IDLE_TIME,
                    ConfigManager.SOCKET_ALL_IDLE_TIME));
            // 将请求和应答消息编码或解码为HTTP消息
            pipeline.addLast(new HttpServerCodec());  // 编解码器
            // 将HTTP消息的多个部分组合成一条完整的HTTP消息
            pipeline.addLast(new HttpObjectAggregator(65536));
            pipeline.addLast(new ChunkedWriteHandler());
            pipeline.addLast(new WebSocketServerCompressionHandler());
            pipeline.addLast(new WebSocketServerProtocolHandler("/websocket", null, true));  // 连接前缀
            pipeline.addLast(new NettyWebSocketHandle());   // 消息处理
        }

    }

}
