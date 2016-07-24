package com.example.ruofei.bus_locator.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.example.ruofei.bus_locator.R;
import com.example.ruofei.bus_locator.routes.RouteListActivity;
import com.example.ruofei.bus_locator.api.BusLocatorApi;
import com.example.ruofei.bus_locator.util.Constants;
import com.example.ruofei.bus_locator.util.Server;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ruofei on 5/31/2016.
 */
public class BusStatusUpdateService extends Service {
    private String TAG = this.getClass().getName();
    private MyThread mythread;

    public boolean isRunning = false;

    private int lastUpdateTime, alarmTime;
    private volatile int currentCountingTime;
    private CountDownTimer mTimer;

    private BroadcastReceiver mReceiver;
    private volatile boolean timeUpdateFlag;
    private Context context;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mythread = new MyThread();

        lastUpdateTime = -1;
        currentCountingTime = -1;
        timeUpdateFlag = false;

//        IntentFilter intentFilter = new IntentFilter(
//                "android.intent.action.MAIN");

        IntentFilter mStatusIntentFilter = new IntentFilter(
                Constants.BROADCAST_NEW_BUS_REMAINING_TIME);

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, "receive new time from fb message");
                Integer newRemaingTime = intent.getIntExtra(Constants.BUS_REMAINING_TIME, -1);

                //
                if (newRemaingTime != -1) {
                    currentCountingTime = newRemaingTime;
                    timeUpdateFlag = true;
                }
            }
        };
        //registering our receiver
        getApplicationContext().registerReceiver(mReceiver, mStatusIntentFilter);

    }


    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (isRunning) {
            mythread = null;
            isRunning = false;
        }
    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStart");
        if (!isRunning) {
            mythread.start();
            isRunning = true;
        }
        return START_NOT_STICKY;
    }


    public void readWebPage() {
//        HttpClient client = new DefaultHttpClient();
//        HttpGet request = new HttpGet("http://google.com");
//        // Get the response
//        ResponseHandler<String> responseHandler = new BasicResponseHandler();
//        String response_str = null;
//Toast.makeText(this, "Service Update:" ,
//                Toast.LENGTH_SHORT).show();
        Server server = Server.getInstance(this.getApplicationContext());
        server.buildRetrofit(Constants.BUS_LOCATOR_URL);
        server.setApi(BusLocatorApi.class);
        BusLocatorApi service = (BusLocatorApi) server.getService();
        Call<String> call = service.getBusLocationIndicator();

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response != null) {

                    SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(Constants.DISIRED_BUS_PREFFERNCE, Context.MODE_PRIVATE);
                    String defaultValue = getResources().getString(R.string.disired_bus_default);
                    String desiredBusLocation = sharedPref.getString(getString(R.string.disired_bus_key), defaultValue);

                    String updateLocation = response.body();
                    Log.e(TAG, "response " + updateLocation + ", preference:" + desiredBusLocation);
                    if (updateLocation.equals(desiredBusLocation)) {
                        showNotification("Bus is coming at " + updateLocation, "Bus is comming", 3);
                    }
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });

        //showNotification("service:" + testCounter,"service", testCounter++);
        try {
//            response_str = client.execute(request, responseHandler);
//            if(!response_str.equalsIgnoreCase("")){
//                Log.d(TAG, "Got Response");
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showNotification(String title, String detail, int id) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.common_full_open_on_phone);
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(detail);
//        mBuilder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});
        mBuilder.setVibrate(new long[]{1000});
        //LED
        mBuilder.setLights(Color.RED, 3000, 3000);
//        //Ton
//        mBuilder.setSound(Uri.parse("uri://sadfasdfasdf.mp3"));
        Intent resultIntent = new Intent(this, RouteListActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(RouteListActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

// notificationID allows you to update the notification later on.
        mNotificationManager.notify(id, mBuilder.build());
        Log.e(TAG, "Notification");
    }

    public void checkingAlarmStatus() throws Exception {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.alarm_preference_key), Context.MODE_PRIVATE);
        // check if user set an alarm
        String flag = sharedPref.getString(getString(R.string.alarm_flag_key), "true");
        if (!flag.equals("true")) {
            Log.e(TAG, "stop alarm service");
            currentCountingTime = -1;
            this.stopSelf();
        }
//        if(currentCountingTime == -1)
//            return;

//        Integer alarmTimeInSec = sharedPref.getInt(getString(R.string.alarm_remaining_time_key), currentCountingTime);
//        if (alarmTimeInMin != null && alarmTimeInMin >= 0) {
//
//        }
        Integer alarmSettingTime = sharedPref.getInt(getString(R.string.alarm_setting_time_key), -1);

        Log.e(TAG, "alarmSettingTime:" + alarmSettingTime + ",currentCountingTime:" + currentCountingTime);

        if (alarmSettingTime == -1) {
            // exception
            Log.e(TAG, "Error: alarm Time is not setting");
            throw new Exception();
        } else {

            Log.e(TAG, "alarmCheckingEnd1");
            if (currentCountingTime <= alarmSettingTime && currentCountingTime != -1) {
                //send notification
                Log.e(TAG, "alarmCheckingEnd2");
                showNotification("bus is approaching", "remaning time:" + currentCountingTime, 100);

                // unsubscribe alarm
                Server server = Server.getInstance(context);
//                Call<Void> call = server.unsubscribeBusstop(routeID, busstopID, token);
//                Log.d(TAG, "send token to subscribe alarm");
//                call.enqueue(new Callback<Void>() {
//                    @Override
//                    public void onResponse(Call<Void> call, Response<Void> response) {
//
//                    }
//
//                    @Override
//                    public void onFailure(Call<Void> call, Throwable t) {
//                        Log.e(TAG, "Fail to setup alarm:" + t.getMessage());
//                        t.printStackTrace();
//                    }
//                });
            } else {
                Log.e(TAG, "alarmCheckingEnd3");
                if (mTimer != null)
                    mTimer.cancel();

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mTimer = new CountDownTimer(currentCountingTime, 1000) {
                            public void onTick(long millisUntilFinished) {
//                mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                                Log.e(TAG, "alarmCheckingEnd:" + currentCountingTime);
                                if (currentCountingTime > 0)
                                    currentCountingTime -= 1;
                            }

                            public void onFinish() {
//                mTextField.setText("done!");
                                Log.e(TAG, "alarmCheckingEndFinish:" + currentCountingTime);

                            }
                        }.start();
                    }


                });


                Log.e(TAG, "alarmCheckingEnd4");
            }
        }
        Log.e(TAG, "alarmCheckingEnd");
    }

    class MyThread extends Thread {
        static final long DELAY = 3000;

        @Override
        public void run() {

            Thread tmpThread = Thread.currentThread();
            Log.d(TAG, "Running1");
            while (isRunning && mythread == tmpThread) {
                Log.d(TAG, "Running");
                try {
                    Log.e(TAG, "alarm checking running");
                    checkingAlarmStatus();
                    Log.e(TAG, "alarm checking running2");
                    this.sleep(DELAY);
                } catch (Exception e) {
                    isRunning = false;
                    e.printStackTrace();
                }
            }
        }

    }
}
