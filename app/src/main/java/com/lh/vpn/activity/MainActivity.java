package com.lh.vpn.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.lh.vpn.R;

import topsec.sslvpn.svsdklib.SVSDKLib;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Dialog mShowingDialog;
    private Button start_btn, stop_btn, status_btn;
    private EditText ip_text, port_text, name_text, pwd_text;
    private SVSSDKTest svssdkTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start_btn = (Button) findViewById(R.id.start_btn);
        stop_btn = (Button) findViewById(R.id.stop_btn);
        status_btn = (Button) findViewById(R.id.status_btn);
        ip_text = (EditText) findViewById(R.id.ip_text);
        port_text = (EditText) findViewById(R.id.port_text);
        name_text = (EditText) findViewById(R.id.name_text);
        pwd_text = (EditText) findViewById(R.id.pwd_text);
        start_btn.setOnClickListener(this);
        stop_btn.setOnClickListener(this);
        status_btn.setOnClickListener(this);
        svssdkTest = new SVSSDKTest(this, handler);
    }

    private Handler handler = new Handler() {
        // 处理具体的message,该方法由父类中进行继承.
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = (Bundle) msg.obj;
            switch (msg.what) {
                case SVSSDKTest.VPN_MSG_STATUS_UPDATE: // VPN库发送消息处理
                    if (null != bundle) {
                        String vpnStatus = bundle.getString("vpnstatus");
                        String vpnErr = bundle.getString("vpnerror");

                        if (vpnStatus.equalsIgnoreCase("6")) {
                            // VPN隧道建立成功
                            Log.i(SVSSDKTest.TAG, "VPN库消息通知：VPN隧道建立成功");
                           showErrorDialog("VPN库消息通知：VPN隧道建立成功");
                        }
                        if (vpnStatus.equalsIgnoreCase("200")) {
                           showErrorDialog("VPN库消息通知：VPN隧道超时");
                        }
                        if (!vpnErr.equalsIgnoreCase("0")) {
                            if (vpnErr.equalsIgnoreCase("10")) {
                                Log.i(SVSSDKTest.TAG, "VPN库消息通知：VPN需要重新登陆，可提示用户进行选择是否踢出上一个用户，现在是强行踢出上一个用户");
                                SVSDKLib vpnlib = SVSDKLib.getInstance();
                                vpnlib.reLoginVPN();

                            } else {
                                // VPN隧道建立出错
                               showErrorDialog("VPN库消息通知：当前VPN错误为：" + vpnErr);
                            }
                        }
                    }
                    break;
                case SVSSDKTest.QUERY_VPN_MSG_STATUS_UPDATE: // 查询线程消息处理
                    if (null != bundle) {
                        String vpnStatus = bundle.getString("vpnstatus");
                        String vpnErr = bundle.getString("vpnerror");
                        if (vpnStatus.equalsIgnoreCase("6")) {
                            // VPN隧道建立成功
                           showErrorDialog("查询线程通知：VPN隧道建立成功");
                        }

                        if (!vpnErr.equalsIgnoreCase("0")) {
                            // VPN隧道建立出错
                           showErrorDialog("查询线程通知：当前VPN错误为：" + vpnErr);
                        }
                    }
                    break;
            }
        }
    };


    private void showErrorDialog(String sErrInfo) {

        mShowingDialog = new AlertDialog
                .Builder(this)
                .setTitle("通知")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(sErrInfo)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                })
                .create();
        mShowingDialog.show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_btn://开始连接vpn
                String ip = ip_text.getText().toString().trim();
                int port = Integer.parseInt(port_text.getText().toString().trim());
                String name = name_text.getText().toString().trim();
                String pwd = pwd_text.getText().toString().trim();
                svssdkTest.startVPN(ip, port, name, pwd);
                Toast.makeText(MainActivity.this, svssdkTest.startVPN(ip, port, name, pwd), Toast.LENGTH_SHORT).show();
                break;
            case R.id.stop_btn://停止连接vpn
                svssdkTest.stopVPN();
                break;
            case R.id.status_btn://连接vpn状态
                svssdkTest.getVPNStatus(MainActivity.this);
                break;
        }
    }
}
