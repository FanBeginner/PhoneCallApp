package com.ajiew.phonecallapp.phonecallui;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.telecom.Call;
import android.telecom.VideoProfile;
import android.telephony.TelephonyManager;
import android.widget.Toast;


@RequiresApi(api = Build.VERSION_CODES.M)
public class PhoneCallManager {
    /**
     * 外放模式
     */
    public static final int MODE_SPEAKER = 0;

    /**
     * 耳机模式
     */
    public static final int MODE_HEADSET = 1;

    /**
     * 听筒模式
     */
    public static final int MODE_EARPIECE = 2;
    public static Call call;

    private Context context;
    private AudioManager audioManager;

    public PhoneCallManager(Context context) {
        this.context = context;

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * 接听电话
     */
    public void answer() {
        if (call != null) {
            call.answer(VideoProfile.STATE_AUDIO_ONLY);
            openSpeaker();
        }
    }

    /**
     * 断开电话，包括来电时的拒接以及接听后的挂断
     */
    public void disconnect() {
        if (call != null) {
            call.disconnect();
            audioManager.setMode(AudioManager.MODE_NORMAL);//正常模式
        }
    }

    /**
     * 打开免提
     */
    public void openSpeaker() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_CALL);//接通电话模式
            audioManager.setSpeakerphoneOn(true);
            Toast.makeText(context, "打开免提", Toast.LENGTH_SHORT).show();
        }
    }
    public void closeSpeaker(){
        if(audioManager!= null&&audioManager.isSpeakerphoneOn()){
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(false);
            Toast.makeText(context, "关闭免提", Toast.LENGTH_SHORT).show();
        }
    }
    public void pauseCall(){
        if(call!=null){
            call.hold();
            Toast.makeText(context, "挂起", Toast.LENGTH_SHORT).show();
        }
    }
    public void continueCall(){
        if(call!=null){
            call.unhold();
            Toast.makeText(context, "取消挂起", Toast.LENGTH_SHORT).show();
        }
    }
    public void silence(){
        if(audioManager != null){
            audioManager.setMicrophoneMute(true);//true将麦克风静音

            Toast.makeText(context, "静音"+audioManager.isMicrophoneMute(), Toast.LENGTH_SHORT).show();
        }
    }
    public void cancelSilence(){
        if(audioManager != null&&audioManager.isMicrophoneMute()){
            audioManager.setMicrophoneMute(false);//false关闭静音
            Toast.makeText(context, "取消静音"+audioManager.isMicrophoneMute(), Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * 销毁资源
     * */
    public void destroy() {
        call = null;
        context = null;
        audioManager = null;
    }
}
