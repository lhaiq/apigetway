package com.helloworld.apigetway.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * @author tbc on 2017/3/8.
 */
public class NettyHttpClient {

    private final Bootstrap bootstrap;

    public NettyHttpClient() {
        this.bootstrap = getBootstrap();
    }

    private Bootstrap getBootstrap() {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new HttpClientCodec());
                        pipeline.addLast(new ReadTimeoutHandler(10000,
                                TimeUnit.MILLISECONDS));

                    }
                });
        return bootstrap;
    }

    public ListenableFuture execute(String host, int port, HttpRequest httpRequest) {

        final SettableFuture settableFuture = SettableFuture.create();

        ChannelFutureListener connectionListener = future -> {
            if (future.isSuccess()) {
                Channel channel = future.channel();
                channel.pipeline().addLast(new RequestExecuteHandler(settableFuture));
                channel.writeAndFlush(httpRequest);
            } else {
                settableFuture.setException(future.cause());
            }
        };

        this.bootstrap.connect(host, port).addListener(connectionListener);

        return settableFuture;
    }


    private static class RequestExecuteHandler extends ChannelInboundHandlerAdapter {

        private SettableFuture<FullHttpResponse> settableFuture;


        public RequestExecuteHandler(SettableFuture<FullHttpResponse> settableFuture) {
            this.settableFuture = settableFuture;
        }


        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {


//            settableFuture.set(msg);
        }


        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
            settableFuture.setException(cause);
        }
    }

}
