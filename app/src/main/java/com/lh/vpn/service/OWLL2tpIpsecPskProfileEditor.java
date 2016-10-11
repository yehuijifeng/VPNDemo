package com.lh.vpn.service;

import android.content.Context;

/**
 * Created by LuHao on 2016/10/11.
 */
public class OWLL2tpIpsecPskProfileEditor extends OWLVpnProfileEditor {

    public OWLL2tpIpsecPskProfileEditor(final Context context) {
        super(context);
    }

    @Override
    protected VpnProfile createProfile() {
        return new L2tpIpsecPskProfile(mContext);
    }


    @Override
    protected void doPopulateProfile() {
        L2tpIpsecPskProfile p = getProfile();

        p.setName("OWLL2tpIpsecPsk");
        p.setServerName("0.0.0.0");
        p.setDomainSuffices("8.8.8.8");
        p.setUsername("xxxxxxx");
        p.setPassword("xxxx");

        p.setPresharedKey("xxxxx");
        boolean secretEnabled = false;
        p.setSecretEnabled(secretEnabled);
        p.setSecretString(secretEnabled ? "xxxx" : "");
    }


}
