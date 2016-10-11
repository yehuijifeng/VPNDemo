package com.lh.vpn.service;

import android.content.Context;

/**
 * Created by LuHao on 2016/10/11.
 */
public class OWLPptpProfileEditor extends OWLVpnProfileEditor {
    public OWLPptpProfileEditor(Context context) {
        super(context);
    }

    @Override
    protected VpnProfile createProfile() {
        return new PptpProfile(mContext);
    }


    @Override
    protected void doPopulateProfile() {
        PptpProfile profile = getProfile();
        profile.setName("OWLPPTP");
        profile.setServerName("0.0.0.0");
        profile.setDomainSuffices("8.8.8.8");
        profile.setUsername("whyonly");
        profile.setPassword(".....");
        profile.setEncryptionEnabled(true);
    }
}
