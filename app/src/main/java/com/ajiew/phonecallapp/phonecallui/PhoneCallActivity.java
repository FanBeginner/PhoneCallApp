package com.ajiew.phonecallapp.phonecallui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.ajiew.phonecallapp.ActivityStack;
import com.ajiew.phonecallapp.R;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.ajiew.phonecallapp.listenphonecall.CallListenerService.formatPhoneNumber;


/**
 * 提供接打电话的界面，仅支持 Android M (6.0, API 23) 及以上的系统
 *
 * @author aJIEw
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class PhoneCallActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvCallNumberLabel;
    private TextView tvCallNumber;
    private TextView tvPickUp;
    private TextView tvCallingTime;
    private TextView tvHangUp;

    private PhoneCallManager phoneCallManager;
    private PhoneCallService.CallType callType;
    private String phoneNumber;

    private Timer onGoingCallTimer;
    private int callingTime;
    AudioManager audioManager;
    MediaRecorder mMediaRecorder;
    public static void actionStart(Context context, String phoneNumber,
                                   PhoneCallService.CallType callType) {
        Intent intent = new Intent(context, PhoneCallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, callType);
        intent.putExtra(Intent.EXTRA_PHONE_NUMBER, phoneNumber);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_call);

        ActivityStack.getInstance().addActivity(this);

        initData();

        initView();
    }

    private void initData() {
        phoneCallManager = new PhoneCallManager(this);
        onGoingCallTimer = new Timer();
        if (getIntent() != null) {
            phoneNumber = getIntent().getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            callType = (PhoneCallService.CallType) getIntent().getSerializableExtra(Intent.EXTRA_MIME_TYPES);
        }
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    private void initView() {
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION //hide navigationBar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        tvCallNumberLabel = findViewById(R.id.tv_call_number_label);
        tvCallNumber = findViewById(R.id.tv_call_number);
        tvPickUp = findViewById(R.id.tv_phone_pick_up);
        tvCallingTime = findViewById(R.id.tv_phone_calling_time);
        tvHangUp = findViewById(R.id.tv_phone_hang_up);

        tvCallNumber.setText(formatPhoneNumber(phoneNumber));
        tvPickUp.setOnClickListener(this);
        tvHangUp.setOnClickListener(this);


        // 打进的电话
        if (callType == PhoneCallService.CallType.CALL_IN) {
            tvCallNumberLabel.setText("来电号码");
            tvPickUp.setVisibility(View.VISIBLE);
        }
        // 打出的电话
        else if (callType == PhoneCallService.CallType.CALL_OUT) {
            tvCallNumberLabel.setText("呼叫号码");
            tvPickUp.setVisibility(View.GONE);
//            phoneCallManager.openSpeaker();

        }

        showOnLockScreen();
    }


    private void setSpeakerphoneOn(boolean on) {
        if (on) {
            audioManager.setSpeakerphoneOn(true);
        } else {
            audioManager.setSpeakerphoneOn(false);//关闭扬声器
            audioManager.setRouting(AudioManager.MODE_NORMAL, AudioManager.ROUTE_EARPIECE, AudioManager.ROUTE_ALL);
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            //把声音设定成Earpiece（听筒）出来，设定为正在通话中
            audioManager.setMode(AudioManager.MODE_IN_CALL);
        }
    }

    public void showOnLockScreen() {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_phone_pick_up) {
            phoneCallManager.answer();
            tvPickUp.setVisibility(View.GONE);
            tvCallingTime.setVisibility(View.VISIBLE);
            onGoingCallTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            callingTime++;
                            tvCallingTime.setText("通话中：" + getCallingTime());
                        }
                    });
                }
            }, 0, 1000);

        } else if (v.getId() == R.id.tv_phone_hang_up) {
            phoneCallManager.disconnect();
            stopTimer();
            stopRecord();
        }
    }

    private String getCallingTime() {
        int hour = callingTime / 60 / 60;
        int minute = callingTime / 60;
        int second = callingTime % 60;
        return (hour < 10 ? "0" + hour : hour)+
                ":" +
                (minute < 10 ? "0" + minute : minute) +
                ":" +
                (second < 10 ? "0" + second : second);
    }

    private void stopTimer() {
        if (onGoingCallTimer != null) {
            onGoingCallTimer.cancel();
        }

        callingTime = 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        phoneCallManager.destroy();
    }

    public void openSpeaker(View view) {
        phoneCallManager.openSpeaker();

    }

    public void closeSpeaker(View view) {
        phoneCallManager.closeSpeaker();

    }

    public void pauseCall(View view) {
        phoneCallManager.pauseCall();
    }

    public void continueCall(View view) {
        phoneCallManager.continueCall();
    }

    public void silence(View view) {
        phoneCallManager.silence();
    }

    public void cancelSilence(View view) {
        phoneCallManager.cancelSilence();
    }

    public void recording(View view) {

        startRecord();

    }
    private String filePath;
    public void startRecord() {
        String fileName;

        if (mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();
        else{
            Toast.makeText(this, "正在录音！", Toast.LENGTH_SHORT).show();
        }
        try {

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风

            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            fileName = phoneNumber+"@"+DateFormat.format("yyyyMMddHHmmss", Calendar.getInstance(Locale.CHINA)) + ".m4a";
            File destDir = new File(Environment.getExternalStorageDirectory() + "/test/");
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            filePath = Environment.getExternalStorageDirectory() + "/sound_record/" + fileName;

            mMediaRecorder.setOutputFile(filePath);
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (IllegalStateException e) {
            Log.i("failed!", e.getMessage());
        } catch (IOException e) {
            Log.i("failed!", e.getMessage());
        }
    }
    public void stopRecord() {
        if(mMediaRecorder != null) {
            Toast.makeText(this, "录音已保存到："+filePath, Toast.LENGTH_LONG).show();
            try {
                mMediaRecorder.stop();
                mMediaRecorder.release();
                mMediaRecorder = null;
            } catch (RuntimeException e) {
                mMediaRecorder.reset();
                mMediaRecorder.release();
                mMediaRecorder = null;

                File file = new File(filePath);
                if (file.exists())
                    file.delete();
            }
        }
    }
}
