package zlq.com.onlinecrash.data;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * author: ZlqPC
 * created on: 2018/12/15 23:44
 * description:数据层 使用NIO实现tcp通信，意义在于让通信变成非阻塞模式
 */
public class TcpClient {

    //单例模式有七种模式下面是最常用的模式优化了双重检查模式
    private static final TcpClient instance = new TcpClient();
    private static final String TAG = "TcpClient";
    //定义检测SocketChannel的Selector对象
    private Selector selector = null;
    //客户端SocketChannel
    private SocketChannel sc = null;
    private NioResponse nioResponse;
    private Charset charset = Charset.forName("UTF-8");
    private Handler clientHandler;

    //私有化构造方法（单例模式）
    private TcpClient() {
        HandlerThread clientThread = new HandlerThread("TcpClient");
        clientThread.start();
        clientHandler = new Handler(clientThread.getLooper());
    }

    //创建一个静态方法获取单例
    public static TcpClient getInstance() {
        return instance;
    }

    /**
     * 开始连接
     * 通信基本三元素（ip,端口和协议）
     */
    public void startClient(String hostName, int port) {
        clientHandler.post(() -> {
            try {
                selector = Selector.open();
                InetSocketAddress isa = new InetSocketAddress(hostName, port);
                //调用open静态方法创建连接到指定主机的SocketChannel
                sc = SocketChannel.open(isa);
                //设置该sc以非阻塞方式工作
                sc.configureBlocking(false);
                //将SocketChannel对象注册到指定Selector
                sc.register(selector, SelectionKey.OP_READ);
                new ResponseThread(nioResponse).start();
            } catch (IOException e) {
                if (nioResponse != null) {
                    nioResponse.clientError();
                }
                Log.e(TAG, "startClient", e);
            }
        });
    }

    /**
     * 开始接收
     */
    public void startResponse(NioResponse nioResponse) {
        this.nioResponse = nioResponse;
    }

    /**
     * 结束接收
     */
    public void stopResponse() {
        nioResponse = null;
    }

    /**
     * 发送信息
     */
    public void sendMessage(String res) {
        clientHandler.post(() -> {
            if (sc != null && sc.isConnected()) {
                try {
                    sc.write(charset.encode(res));
                } catch (IOException e) {
                    Log.e(TAG, "sendMessage", e);
                }
            } else {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (nioResponse != null)
                        nioResponse.sendNoConnect();
                });
            }
        });
    }


    /**
     * 接口在主线程中运行主要返回未连接状态和接收信息
     */
    public interface NioResponse {
        void sendNoConnect();//未连接

        void clientError();

        void receive(String res);//接收消息
    }

    /**
     * 处理响应信息的子线程（响应的方式类似于camera2的管道工作的方式，
     * 如：camera2会通过管道把请求的信息给底层，而这里的接受的信息也一样通过
     * 这种类似的机制来接收信息）
     */
    private class ResponseThread extends Thread {
        private NioResponse nioResponse;

        ResponseThread(NioResponse nioResponse) {
            super("ResponseThread");
            this.nioResponse = nioResponse;
        }

        @Override
        public void run() {
            try {
                //io操作的读，管道（Channel）至少有1个
                while (selector.select() > 0) {
                    //返回的是select的key集合并遍历（如果一个key所指的对象为空会报异常）
                    for (SelectionKey key : selector.selectedKeys()) {
                        if (sc == null || !sc.isConnected()) {
                            return;
                        }
                        //如果该SelectionKey对应的Channel中有可读的数据
                        if (key.isReadable()) {
                            ByteBuffer buff = ByteBuffer.allocate(1024);
                            StringBuilder content = new StringBuilder();
                            //使用NIO读取Channel中的数据
                            while (((SocketChannel) key.channel()).read(buff) > 0) {
                                sc.read(buff);
                                buff.flip();
                                content.append(charset.decode(buff));
                            }
                            //输出数据
                            if (nioResponse != null) {
                                nioResponse.receive(content.toString());
                            }
                            //为下一次读取作准备
                            key.interestOps(SelectionKey.OP_READ);
                        }
                        //删除正在处理的SelectionKey
                        selector.selectedKeys().remove(key);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "startClient", e);
            }

        }
    }

    public void release() {
        try {
            nioResponse = null;
            selector.close();
            sc.close();
            selector = null;
            sc = null;
        } catch (IOException e) {
            Log.e(TAG, "release", e);
        }
    }

}
