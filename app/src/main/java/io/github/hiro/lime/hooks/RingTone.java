package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class RingTone implements IHook {
    private android.media.Ringtone ringtone = null;
    private boolean isPlaying = false;

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.DeviceCallTone.checked && !limeOptions.MuteCallTone.checked && !limeOptions.CustomDialTone.checked) {
            return;
        }

        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application appContext = (Application) param.thisObject;
                if (appContext == null) {
                    return;
                }

                if (limeOptions.DeviceCallTone.checked) {
                    XposedBridge.hookAllMethods(
                            loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
                            Constants.RESPONSE_HOOK.methodName,
                            new XC_MethodHook() {

                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    String paramValue = param.args[1].toString();
                                    Context context = AndroidAppHelper.currentApplication().getApplicationContext();

                                    if (paramValue.contains("type:NOTIFIED_RECEIVED_CALL,") && !isPlaying) {
                                        if (context != null) {
                                            if (ringtone != null && ringtone.isPlaying()) {
                                                //Log.d("Xposed", "Ringtone is already playing. Skipping playback.");
                                                return; // 再生中の場合は何もしない
                                            }
                                            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                                            ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                                            ringtone.play();
                                            isPlaying = true;
                                        }
                                    }

                                    if (paramValue.contains("RESULT=REJECTED,") || paramValue.contains("RESULT=REJECTED,")) {
                                        if (ringtone != null && ringtone.isPlaying()) {
                                            ringtone.stop();
                                            isPlaying = false;
                                        }
                                    }

                                }
                            });
                }

                Class<?> targetClass = loadPackageParam.classLoader.loadClass("com.linecorp.andromeda.audio.AudioManager");
                Method[] methods = targetClass.getDeclaredMethods();

                for (Method method : methods) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                            if (method.getName().equals("setServerConfig")) {
                                if (ringtone != null && ringtone.isPlaying()) {
                                    ringtone.stop();
                                    isPlaying = false;
                                }
                            }


                            if (method.getName().equals("stop")) {
                                if (ringtone != null && ringtone.isPlaying()) {
                                    ringtone.stop();
                                    isPlaying = false;
                                }
                            }


                            if (method.getName().equals("processToneEvent")) {
                                Object arg0 = param.args[0];
                                String type = arg0.toString().contains("RING_BACK") ? "dialtone" : arg0.toString().contains("RING") && !arg0.toString().contains("RING_BACK") ? "calltone" : "";

                                if ((limeOptions.CustomDialTone.checked && type.equals("dialtone")) || (limeOptions.MuteCallTone.checked && type.equals("calltone"))) {
                                    //Log.d("Xposed", "MuteTone is enabled. Suppressing tone event.");
                                    param.setResult(null);
                                }

                                if (arg0.toString().contains("START") && ((limeOptions.CustomDialTone.checked && type.equals("dialtone")) || (limeOptions.DeviceCallTone.checked && type.equals("calltone")))) {
                                    if (appContext != null) {
                                        // ringtone が初期化されており、再生中の場合はスキップ
                                        if (ringtone != null && ringtone.isPlaying()) {
                                            //Log.d("Xposed", "Ringtone is already playing. Skipping playback.");
                                            return; // 再生中の場合は何もしない
                                        }

                                        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                                        if (arg0.toString().contains("RING_BACK")) {
                                            Uri customRingtoneUri = Uri.parse("file://" + Environment.getExternalStorageDirectory().getPath() + "/Notifications/LIME_Dialtone.mp3");
                                            if (isFileExists(customRingtoneUri)) {
                                                ringtoneUri = customRingtoneUri;
                                            }
                                        }
                                        ringtone = RingtoneManager.getRingtone(appContext, ringtoneUri);

                                        if (ringtone != null) {
                                            //Log.d("Xposed", "Playing ringtone.");
                                            ringtone.play();
                                            isPlaying = true;
                                        } else {
                                            //Log.d("Xposed", "Ringtone is null. Cannot play ringtone.");
                                            return;
                                        }
                                    } else {
                                        //Log.d("Xposed", "appContext is null. Cannot play ringtone.");
                                        return;
                                    }
                                } else {
                                    //Log.d("Xposed", "Argument is not 'START'. Actual value: " + arg0);
                                }

                            }

                            if (method.getName().equals("ACTIVATED") && param.args != null && param.args.length > 0) {
                                Object arg0 = param.args[0];
                                if ("ACTIVATED".equals(arg0)) {
                                    if (ringtone != null && ringtone.isPlaying()) {
                                        ringtone.stop();
                                        isPlaying = false;
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    public boolean isFileExists(Uri uri) {
        if (uri == null) {
            return false;
        }
        String filePath = uri.getPath();
        if (filePath == null) {
            return false;
        }
        File file = new File(filePath);
        return file.exists();
    }
}