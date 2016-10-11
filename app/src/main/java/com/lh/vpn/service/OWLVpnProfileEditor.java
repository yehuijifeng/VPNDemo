package com.lh.vpn.service;

import android.content.Context;

import java.security.KeyStore;

/**
 * Created by LuHao on 2016/10/11.
 * 保存VPN Profile的抽象类，供PPTP，L2tp,L2tpIpsecPsk等扩展
 */
public abstract class OWLVpnProfileEditor {
    private VpnProfile profile;
    private VpnProfileRepository repository;
    private KeyStore keyStore;
    private Runnable resumeAction;

    protected Context mContext;

    public OWLVpnProfileEditor(final Context context) {
        mContext = context;
        repository = VpnProfileRepository.getInstance(context);
        keyStore = new KeyStore(context);
    }

    public void onSave() {
        try {
            profile = createProfile();
            populateProfile();
            saveProfile();
        } catch (InvalidProfileException e) {
            throw e;
        }
    }

    private void populateProfile() {
        profile.setState(VpnState.IDLE);
        doPopulateProfile();
        repository.checkProfile(profile);
    }

    private void saveProfile() {
        repository.addVpnProfile(profile);
    }

    @SuppressWarnings("unchecked")
    protected <T extends VpnProfile> T getProfile() {
        return (T) profile;
    }

    protected abstract VpnProfile createProfile();

    protected abstract void doPopulateProfile();
}
