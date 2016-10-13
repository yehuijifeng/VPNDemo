package com.lh.vpn.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import com.lh.vpn.R;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Created by LuHao on 2016/10/11.
 * VpnService类封装了建立VPN连接所必须的所有函数
 * <p/>
 * 在Apache许可下的,2.0版本(“许可证”);
 * 你可能不使用这个文件除了遵守许可证。
 * 你可以获得许可证的副本
 * <p/>
 * http://www.apache.org/licenses/license - 2.0
 * <p/>
 * 除非适用法律要求或书面同意,软件
 * 在许可证下发布的分布在一个“目前的”基础上,
 * 没有任何形式的保证或条件,明示或默示。
 * 查看许可证的管理权限和特定的语言
 * 限制下的许可。
 */
public class VPNService extends VpnService implements Handler.Callback, Runnable {

    private static final String TAG = "VPNService";

    private String mServerAddress;
    private String mServerPort;
    private byte[] mSharedSecret;
    private PendingIntent mConfigureIntent;

    private Handler mHandler;
    private Thread mThread;

    private ParcelFileDescriptor mInterface;
    private String mParameters;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //这个程序只是用来显示信息的
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // 在连接新的线程之前，先停止旧的线程工作
        if (mThread != null) {
            mThread.interrupt();
        }

        //提取意图中的信息
        String prefix = getPackageName();
        mServerAddress = intent.getStringExtra(prefix + ".ADDRESS");
        mServerPort = intent.getStringExtra(prefix + ".PORT");
        mSharedSecret = intent.getStringExtra(prefix + ".SECRET").getBytes();

        // 通过创建一个新线程启动一个新的会话。
        mThread = new Thread(this, "VpnThread");
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public synchronized void run() {
        try {
            Log.i(TAG, "Starting");

            //如果有任何需要使用网络,让它现在。
            //这大大减少了复杂性的无缝交接
            //试图重现隧道在不关闭一切。
            //在这个演示中,所有我们需要知道的是服务器地址。
            InetSocketAddress server = new InetSocketAddress(mServerAddress, Integer.parseInt(mServerPort));

            // 我们试图创建隧道几次。更好的方法
            // 与ConnectivityManager合作,比如在只有当吗
            // 网络、。在这里,我们只使用一个计数器
            // 简单的事情,连接十次
            for (int attempt = 0; attempt < 10; ++attempt) {
                mHandler.sendEmptyMessage(R.string.connecting);
                // 如果我们连接的话，重置计数器。
                if (runStartVPN(server)) {
                    attempt = 0;
                }
                //睡3s，让我们得到检查
                Thread.sleep(3000);
            }
            Log.i(TAG, "Giving up");
        } catch (Exception e) {
            mHandler.sendEmptyMessage(R.string.error);
            Log.e(TAG, "Got " + e.toString());
        } finally {
            try {
                mInterface.close();
            } catch (Exception e) {
                // ignore
            }
            mInterface = null;
            mParameters = null;
            mHandler.sendEmptyMessage(R.string.disconnected);
            Log.i(TAG, "Exiting");
        }
    }

    /**
     * 连接vpn的具体方法
     *
     * @param server
     * @return
     * @throws Exception
     */
    private boolean runStartVPN(InetSocketAddress server) throws Exception {
        DatagramChannel tunnel = null;
        boolean connected = false;
        try {
            // 创建一个DatagramChannel VPN隧道。
            tunnel = DatagramChannel.open();

            //保护隧道连接以避免回环之前。
            if (!protect(tunnel.socket())) {
                mHandler.sendEmptyMessage(R.string.error);
                throw new IllegalStateException("Cannot protect the tunnel");
            }

            // 链接到服务器
            tunnel.connect(server);

            // 为简单起见,我们使用相同的阅读和线程
            // 写作。在这里我们把隧道进入非阻塞模式。
            tunnel.configureBlocking(false);

            // 验证和配置虚拟网络接口。
            handshake(tunnel);

            // 现在我们联系。设置标志和显示信息。
            connected = true;
            mHandler.sendEmptyMessage(R.string.connected);

            // 发送数据包排队在这个输入流。
            FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());

            // 接收到的数据包需要写入输出流。
            FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());

            //为一个数据包分配缓冲区。
            ByteBuffer packet = ByteBuffer.allocate(32767);

            // 我们使用一个计时器来确定隧道的状态。它双方的工作。正数意味着发送,和任何其他方式接收。我们从收到开始。
            int timer = 0;

            //我们继续转发数据包到出现问题。
            while (true) {
                //假设我们没有取得任何进展在这个迭代。
                boolean idle = true;

                //从输入流中读取即将离任的包。
                int length = in.read(packet.array());
                if (length > 0) {
                    //即将离任的数据包写入隧道。
                    packet.limit(length);
                    tunnel.write(packet);
                    packet.clear();

                    //可能会有更外向包。
                    idle = false;

                    //如果我们收到,切换到发送。
                    if (timer < 1) {
                        timer = 1;
                    }
                }

                //从隧道读取传入的数据包。
                length = tunnel.read(packet);
                if (length > 0) {
                    //忽略控制消息,从0开始。
                    if (packet.get(0) != 0) {
                        //传入的数据包写入输出流。
                        out.write(packet.array(), 0, length);
                    }
                    packet.clear();

                    // 可能有更多的传入的数据包。
                    idle = false;

                    // 如果我们发送、接收开关
                    if (timer > 0) {
                        timer = 0;
                    }
                }

                // 如果我们空闲或等待网络,睡了一小部分的时间来避免繁忙的循环
                if (idle) {
                    Thread.sleep(100);

                    // 增加计时器。这是不准确的,但足够好,因为一切都是在非阻塞模式。;
                    timer += (timer > 0) ? 100 : -100;

                    // 我们收到了很长一段时间但不发送
                    if (timer < -15000) {
                        // 发送空的控制消息。
                        packet.put((byte) 0).limit(1);
                        for (int i = 0; i < 3; ++i) {
                            packet.position(0);
                            tunnel.write(packet);
                        }
                        packet.clear();

                        // 切换到发送
                        timer = 1;
                    }

                    // 我们寄了很长一段时间但不接收。
                    if (timer > 20000) {
                        throw new IllegalStateException("Timed out");
                    }
                }
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
        } finally {
            try {
                tunnel.close();
            } catch (Exception e) {
                // ignore
            }
        }
        return connected;
    }

    /**
     * 验证和配置虚拟网络接口。
     *
     * @param tunnel
     * @throws Exception
     */
    private void handshake(DatagramChannel tunnel) throws Exception {
        //建立一个安全的隧道,我们应该进行相互身份验证和交流会话密钥进行加密。
        // 在这个演示为简单起见,我们只发送明文的共享密钥和等待服务器发送的参数。;
        // 分配的缓冲区握手。
        ByteBuffer packet = ByteBuffer.allocate(1024);

        // 控制消息总是从0开始。
        packet.put((byte) 0).put(mSharedSecret).flip();

        // 发送数据包的秘密几次,以防损失。
        for (int i = 0; i < 3; ++i) {
            packet.position(0);
            tunnel.write(packet);
        }
        packet.clear();

        // 在有限的时间等参数。
        for (int i = 0; i < 50; ++i) {
            Thread.sleep(100);

            // 通常我们不应该接受随机数据包。;
            int length = tunnel.read(packet);
            if (length > 0 && packet.get(0) == 0) {
                configure(new String(packet.array(), 1, length - 1).trim());
                return;
            }
        }
        throw new IllegalStateException("Timed out");
    }

    private void configure(String parameters) throws Exception {
        // 如果旧的接口有相同的参数,使用它!;
        if (mInterface != null && parameters.equals(mParameters)) {
            Log.i(TAG, "Using the previous interface");
            return;
        }

        // 配置一个构建器在解析参数。;
        Builder builder = new Builder();
        for (String parameter : parameters.split(" ")) {
            String[] fields = parameter.split(",");
            try {
                switch (fields[0].charAt(0)) {
                    case 'm':
                        builder.setMtu(Short.parseShort(fields[1]));
                        break;
                    case 'a':
                        builder.addAddress(fields[1], Integer.parseInt(fields[2]));
                        break;
                    case 'r':
                        builder.addRoute(fields[1], Integer.parseInt(fields[2]));
                        break;
                    case 'd':
                        builder.addDnsServer(fields[1]);
                        break;
                    case 's':
                        builder.addSearchDomain(fields[1]);
                        break;
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Bad parameter: " + parameter);
            }
        }

        // 关闭旧接口参数以来发生了变化。;
        try {
            mInterface.close();
        } catch (Exception e) {
            // ignore
        }

        //创建一个新接口使用builder和保存参数。
        mInterface = builder
                .setSession(mServerAddress)
                .setConfigureIntent(mConfigureIntent)
                .establish();
        mParameters = parameters;
        Log.i(TAG, "New interface: " + parameters);
    }

}
