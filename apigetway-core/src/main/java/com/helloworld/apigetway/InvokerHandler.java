package com.helloworld.apigetway;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ListenableFuture;
import com.helloworld.apigetway.client.NettyHttpClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * netty处理调最后一环，请求其它系统；
 * 需要：协议转换组件、具体协议的请求转发组件
 *
 * @author tbc on 2017/3/3 13:10:53.
 */
public class InvokerHandler extends ChannelInboundHandlerAdapter {

    private NettyHttpClient nettyHttpClient = new NettyHttpClient();

    private Executor executor = Executors.newFixedThreadPool(10);


    @Override
    public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
        if(msg instanceof DefaultHttpRequest){
            DefaultHttpRequest request = (DefaultHttpRequest) msg;
            handleHttp(context, request);
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        context.close();
        context.fireChannelRead(cause);
    }


    private void handleHttp(ChannelHandlerContext context, DefaultHttpRequest request1) throws Exception{
        String host = "www.baidu.com";
        int port = 80;

        URI uri = new URI("http://www.baidu.com");
        String msg = "Are you ok?";
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                uri.toASCIIString(), Unpooled.wrappedBuffer(msg.getBytes()));

        // 构建http请求
        request.headers().set(HttpHeaders.Names.HOST, host);
        request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, request.content().readableBytes());
        request.headers().set("messageType", "normal");
        request.headers().set("businessType", "testServerState");
        //路由策略  选一个后台服务器

        ListenableFuture<FullHttpResponse> listenableFuture = nettyHttpClient.execute(host, port, request);

        listenableFuture.addListener(() -> {
            try {
                context.writeAndFlush(listenableFuture.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executor);
    }


}
