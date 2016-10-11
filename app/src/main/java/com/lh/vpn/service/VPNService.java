package com.lh.vpn.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by LuHao on 2016/10/11.
 * VpnService类封装了建立VPN连接所必须的所有函数
 */
public class VPNService extends VpnService {


    public void initService(Context context) {
        /**
         * 建立链接的第一步是要用合适的参数，
         * 创建并初始化好tun0虚拟网络端口，
         * 这可以通过在VpnService类中的一个内部类Builder来做到：
         * */
        /**
         *在正式建立（establish）虚拟网络接口之前，需要设置好几个参数，分别是：

         1）MTU（Maximun Transmission Unit），即表示虚拟网络端口的最大传输单元，
         如果发送的包长度超过这个数字，则会被分包；

         2）Address，即这个虚拟网络端口的IP地址；

         3）Route，只有匹配上的IP包，才会被路由到虚拟端口上去。
         如果是0.0.0.0/0的话，则会将所有的IP包都路由到虚拟端口上去；

         4）DNS Server，就是该端口的DNS服务器地址；

         5）Search Domain，就是添加DNS域名的自动补齐。
         DNS服务器必须通过全域名进行搜索，但每次查找都输入全域名太麻烦了，可以通过配置域名的自动补齐规则予以简化；

         6）Session，就是你要建立的VPN连接的名字，它将会在系统管理的与VPN连接相关的通知栏和对话框中显示出来；

         7）Configure Intent，这个intent指向一个配置页面，用来配置VPN链接。
         它不是必须的，如果没设置的话，则系统弹出的VPN相关对话框中不会出现配置按钮。

         **/
        Builder builder = new Builder();
        builder.setMtu(...);
        builder.addAddress(...);
        builder.addRoute(...);
        builder.addDnsServer(...);
        builder.addSearchDomain(...);
        builder.setSession(...);
        builder.setConfigureIntent(...);
        //最后调用Builder.establish函数，如果一切正常的话，tun0虚拟网络接口就建立完成了。
        // 并且，同时还会通过iptables命令，修改NAT表，将所有数据转发到tun0接口上。
        builder.establish();//函数

        ParcelFileDescriptor interfaces = builder.establish();

        /**
         * 这之后，就可以通过读写VpnService.Builder返回的ParcelFileDescriptor实例
         * 来获得设备上所有向外发送的IP数据包和返回处理过后的IP数据包到TCP/IP协议栈：
         * */

        // Packets to be sent are queued in this input stream.
        FileInputStream in = new FileInputStream(interfaces.getFileDescriptor());
        // Packets received need to be written to this output stream.
        FileOutputStream out = new FileOutputStream(interfaces.getFileDescriptor());
        // Allocate the buffer for a single packet.
        ByteBuffer packet = ByteBuffer.allocate(32767);
        try {
            // Read packets sending to this interface
            int length = in.read(packet.array());
            // Write response packets back
            out.write(packet.array(), 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        OWLVpnProfileEditor pptp = new OWLPptpProfileEditor(this);
        pptp.onSave();
        OWLVpnProfileEditor lwtpIpsec = new OWLL2tpIpsecPskProfileEditor(this);
        lwtpIpsec.onSave();

        VpnActor actor = new VpnActor(getApplicationContext());
        actor.connect(profile);

    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_VPN_CONNECTIVITY);
        stateBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                String action = intent.getAction();

                if (ACTION_VPN_CONNECTIVITY.equals(action)) {
                    onStateChanged(intent);
                } else {
                    Log.d(TAG, "VPNSettings receiver ignores intent:" + intent); //$NON-NLS-1$
                }
            }
        };
        registerReceiver(stateBroadcastReceiver, filter);
    }

    private void onStateChanged(final Intent intent) {
        //Log.d(TAG, "onStateChanged: " + intent); //$NON-NLS-1$

        final String profileName = intent.getStringExtra(BROADCAST_PROFILE_NAME);
        final VpnState state = Utils.extractVpnState(intent);
        final int err = intent.getIntExtra(BROADCAST_ERROR_CODE, VPN_ERROR_NO_ERROR);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stateChanged(profileName, state, err);
            }
        });
    }

    private void stateChanged(final String profileName, final VpnState state, final int errCode) {
        processing change
    }


}
