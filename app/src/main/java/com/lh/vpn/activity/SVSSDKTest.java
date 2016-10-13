package com.lh.vpn.activity;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import topsec.sslvpn.svsdklib.SVSDKLib;

/**
 * Created by LuHao on 2016/10/11.
 */
public class SVSSDKTest {

    public static final String TAG = "TestSVSDKLib";

    public static final int VPN_MSG_STATUS_UPDATE = 100; // VPN状态通知消息号
    public static final int QUERY_VPN_MSG_STATUS_UPDATE = 101; // VPN状态通知消息号

    // VPN服务器地址、端口号、用户名、密码
    private final String VPN_SERVER = "59.49.15.130";
    private final int VPN_PORT = 443;
    private final String VPN_USERNAME = "oa";
    private final String VPN_PASSWORD = "123456";

    public SVSSDKTest(Context context, Handler handler) {
        initSVSDKLib(context, handler);
    }

    // 初始化VPN库
    private void initSVSDKLib(Context context, Handler handler) {
        // 获取VPN库实例
        SVSDKLib vpnlib = SVSDKLib.getInstance();

        // 设置VPN客户端的释放目录
        Context appContext = context.getApplicationContext();
        vpnlib.setSVClientPath(appContext.getFilesDir().getPath());

        // 设置应用程序的资产管理器
        vpnlib.setAppam(context.getAssets());

        // 设置VPNSDK库的消息处理器
        vpnlib.setMsgHandler(handler);

        // 设置VPNSDK库的VPN状态变更消息号
        vpnlib.setVPNMsgID(VPN_MSG_STATUS_UPDATE);

        // 设置VPN连接信息
        vpnlib.setVPNInfo(VPN_SERVER, VPN_PORT, VPN_USERNAME, VPN_PASSWORD);

        // VPN客户端连接前的准备
        vpnlib.prepareVPNSettings();

        Log.i(TAG, "InitSVSDKLib done");
    }

    // 启动VPN连接
    public String startVPN(String ip, int port, String name, String pwd) {
        Log.i(TAG, "start vpn");
        SVSDKLib vpnlib = SVSDKLib.getInstance();
        if (TextUtils.isEmpty(ip)) {
            return "VPN地址不能为空";
        }
        if ((port <= 0) || (port > 65535)) {
            return "VPN端口有效范围是1-65535";
        }
        if (TextUtils.isEmpty(name)) {
            return "用户名不能为空";
        }
        if (TextUtils.isEmpty(pwd)) {
            return "用户密码不能为空";
        }
        // vpnlib.setVPNInfo("192.168.95.84", 443, "1", "111111");
        vpnlib.setVPNInfo(ip, port, name, pwd);
        Log.i("ttt", "ip= " + ip + " port= " + port + " uname= " + name);
        vpnlib.prepareVPNSettings();
        // 获取VPN库实例
        vpnlib.stopVPN();
        // 启动VPN连接
        vpnlib.startVPN();
        return "正在打开VPN";
        // 启动一个查询线程，主动查询VPN状态，VPN成功后
        // 查询线程会给UI主线程发送消息
        // 若VPNSDK库初始化时设置了MsgHandler和MSGID
        // 可由VPNSDK库来发送通知，此线程可不必开启
        // new GetVPNStatusThread().start();

    }

    // 关闭VPN连接
    public void stopVPN() {
        // 停止VPN连接
        SVSDKLib vpnlib = SVSDKLib.getInstance();
        vpnlib.stopVPN();
    }

    // 获取VPN状态
    public void getVPNStatus(Context context) {
        // 获取VPN状态
        String sVPNStatus;
        SVSDKLib vpnlib = SVSDKLib.getInstance();
        sVPNStatus = vpnlib.getVPNStatus();

        Toast.makeText(context, "当前VPN状态为：" + sVPNStatus, Toast.LENGTH_SHORT).show();

        ArrayList<HashMap<String, String>> reslist = vpnlib.getResList();
        Log.i("test", "port1 :" + vpnlib.getResLocalPort("59.49.15.130", 443));
        Log.i("test", "port2 :" + vpnlib.getResLocalPort("59.49.15.130", 443));

    }

}
