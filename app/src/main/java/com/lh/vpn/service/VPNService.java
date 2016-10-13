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

            // We keep forwarding packets till something goes wrong.
            while (true) {
                // Assume that we did not make any progress in this iteration.
                boolean idle = true;

                // Read the outgoing packet from the input stream.
                int length = in.read(packet.array());
                if (length > 0) {
                    // Write the outgoing packet to the tunnel.
                    packet.limit(length);
                    tunnel.write(packet);
                    packet.clear();

                    // There might be more outgoing packets.
                    idle = false;

                    // If we were receiving, switch to sending.
                    if (timer < 1) {
                        timer = 1;
                    }
                }

                // Read the incoming packet from the tunnel.
                length = tunnel.read(packet);
                if (length > 0) {
                    // Ignore control messages, which start with zero.
                    if (packet.get(0) != 0) {
                        // Write the incoming packet to the output stream.
                        out.write(packet.array(), 0, length);
                    }
                    packet.clear();

                    // There might be more incoming packets.
                    idle = false;

                    // If we were sending, switch to receiving.
                    if (timer > 0) {
                        timer = 0;
                    }
                }

                // If we are idle or waiting for the network, sleep for a
                // fraction of time to avoid busy looping.
                if (idle) {
                    Thread.sleep(100);

                    // Increase the timer. This is inaccurate but good enough,
                    // since everything is operated in non-blocking mode.
                    timer += (timer > 0) ? 100 : -100;

                    // We are receiving for a long time but not sending.
                    if (timer < -15000) {
                        // Send empty control messages.
                        packet.put((byte) 0).limit(1);
                        for (int i = 0; i < 3; ++i) {
                            packet.position(0);
                            tunnel.write(packet);
                        }
                        packet.clear();

                        // Switch to sending.
                        timer = 1;
                    }

                    // We are sending for a long time but not receiving.
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
        // To build a secured tunnel, we should perform mutual authentication
        // and exchange session keys for encryption. To keep things simple in
        // this demo, we just send the shared secret in plaintext and wait
        // for the server to send the parameters.

        // Allocate the buffer for handshaking.
        ByteBuffer packet = ByteBuffer.allocate(1024);

        // Control messages always start with zero.
        packet.put((byte) 0).put(mSharedSecret).flip();

        // Send the secret several times in case of packet loss.
        for (int i = 0; i < 3; ++i) {
            packet.position(0);
            tunnel.write(packet);
        }
        packet.clear();

        // Wait for the parameters within a limited time.
        for (int i = 0; i < 50; ++i) {
            Thread.sleep(100);

            // Normally we should not receive random packets.
            int length = tunnel.read(packet);
            if (length > 0 && packet.get(0) == 0) {
                configure(new String(packet.array(), 1, length - 1).trim());
                return;
            }
        }
        throw new IllegalStateException("Timed out");
    }

    private void configure(String parameters) throws Exception {
        // If the old interface has exactly the same parameters, use it!
        if (mInterface != null && parameters.equals(mParameters)) {
            Log.i(TAG, "Using the previous interface");
            return;
        }

        // Configure a builder while parsing the parameters.
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

        // Close the old interface since the parameters have been changed.
        try {
            mInterface.close();
        } catch (Exception e) {
            // ignore
        }

        // Create a new interface using the builder and save the parameters.
        mInterface = builder
                .setSession(mServerAddress)
                .setConfigureIntent(mConfigureIntent)
                .establish();
        mParameters = parameters;
        Log.i(TAG, "New interface: " + parameters);
    }


}
