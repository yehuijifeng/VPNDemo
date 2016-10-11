package com.lh.vpn;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.lh.vpn.service.VPNService;

/**
 * Created by LuHao on 2016/10/11.
 */
public class VpnServiceActivity extends AppCompatActivity implements View.OnClickListener {

    private Dialog mShowingDialog;
    private Button start_btn, stop_btn, status_btn;
    private EditText ip_text, port_text, name_text, pwd_text;
    private final int goService = 1001;

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
        initData();
    }

    private void initData() {
        /**VpnService.prepare函数的目的，
         * 主要是用来检查当前系统中是不是已经存在一个VPN连接了
         * 如果有了的话，是不是就是本程序创建的。
         * 这个intent就是用来触发确认对话框的，
         * 程序会接着调用startActivityForResult将对话框弹出来等用户确认。
         **/
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, goService);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == goService && resultCode == RESULT_OK) {
            /**
             * 如果用户确认了，则会关闭前面已经建立的VPN连接，并重置虚拟端口。
             * 该对话框返回的时候，会调用onActivityResult函数，并告之用户的选择。
             * */
            Intent intent = new Intent(this, VPNService.class);
            startService(intent);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_btn://开始连接vpn
                break;
            case R.id.stop_btn://停止连接vpn
                break;
            case R.id.status_btn://连接vpn状态
                break;
        }
    }

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

}
