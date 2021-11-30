package mg.eight.mplayer.task;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import mg.eight.mplayer.R;
import mg.eight.mplayer.model.Command;
import mg.eight.mplayer.model.Message;
import mg.eight.mplayer.model.Setting;
import mg.eight.mplayer.model.Song;
import mg.eight.mplayer.presenter.LauncherActivity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class AppService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String ACTION = "m.player.commander.task.COMMAND";
    private final int close = -1, playPause = 0, previous = 1, next = 2,
            random = 3, repeat = 4, cancel = 5;
    private final int notificationId = 9;
    private final String MESSAGE_RECEIVED = "m.player.commander.action.RECEIVE",
            MESSAGE_SENT = "m.player.commander.action.SEND";
    private boolean stoppable = false, connected = false;
    private SocketManager socketManager;
    private MessageSender messageSender;
    private SocketDownloadManager downloadManager;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "m.player.commander.action.SEND".equals(intent.getAction())) {
                sendMessage((Message<?>) intent.getSerializableExtra("MESSAGE"));
            }
        }
    };
    private Setting setting = Setting.getInstance();
    private SharedPreferences preferences;
    private Notification notification;
    private RemoteViews controlRemoteViews;
    private LocalBroadcastManager manager;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(receiver,
                new IntentFilter(MESSAGE_SENT));
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        preferences.registerOnSharedPreferenceChangeListener(this);
        controlRemoteViews = new RemoteViews(getPackageName(),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN?
                R.layout.control_notification_layout : R.layout.control_notfication_old_layout);
        Intent intent = new Intent(getApplicationContext(), AppService.class);
        intent.setAction(ACTION);
        intent.putExtra("code", close);
        controlRemoteViews.setOnClickPendingIntent(R.id.closeBtn,
                PendingIntent.getService(getApplicationContext(), close, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        intent.putExtra("code", playPause);
        controlRemoteViews.setOnClickPendingIntent(R.id.playpause,
                PendingIntent.getService(getApplicationContext(), playPause, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        intent.putExtra("code", previous);
        controlRemoteViews.setOnClickPendingIntent(R.id.previous,
                PendingIntent.getService(getApplicationContext(), previous, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        intent.putExtra("code", next);
        controlRemoteViews.setOnClickPendingIntent(R.id.next,
                PendingIntent.getService(getApplicationContext(), next, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        intent.putExtra("code", random);
        controlRemoteViews.setOnClickPendingIntent(R.id.random,
                PendingIntent.getService(getApplicationContext(), random, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        intent.putExtra("code", repeat);
        controlRemoteViews.setOnClickPendingIntent(R.id.repeat,
                PendingIntent.getService(getApplicationContext(), repeat, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        Intent i = new Intent(getApplicationContext(), LauncherActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel("M-PLAYER") == null) {
                NotificationChannel channel = new NotificationChannel("M-PLAYER", "M-PLAYER Control",
                        NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                "M-PLAYER");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            builder.setCustomBigContentView(controlRemoteViews);
        else builder.setCustomContentView(controlRemoteViews);
        builder.setSmallIcon(R.mipmap.app_icon)
                .setOngoing(true)
                .setTicker(getString(R.string.app_name))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent);
        notification = builder.build();
        startHandler();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return  null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION.equals(intent.getAction()) && intent.hasExtra("code")) {
            switch (intent.getIntExtra("code", 10)) {
                case playPause:
                    sendMessage(new Message<>(Command.PLAYPAUSE, null));
                    break;
                case previous:
                    sendMessage(new Message<>(Command.PREVIOUS, null));
                    break;
                case next:
                    sendMessage(new Message<>(Command.NEXT, null));
                    break;
                case close:
                    notificationManager.cancel(notificationId);
                    pushMessage(new Message<>(Command.CLOSE, null));
                    manager.unregisterReceiver(receiver);
                    preferences.unregisterOnSharedPreferenceChangeListener(this);
                    stoppable = true;
                    socketManager.stopHandler();
                    stopSelf();
                    break;
                case random:
                    sendMessage(new Message<>(Command.RANDOM, !setting.isRandom()));
                    break;
                case cancel:
                    downloadManager.setCanceled(true);
                    notificationManager.cancel(intent.getStringExtra("name"), 10);
                    break;
                case repeat:
                    if (setting.getRepeat().equals("No repeat"))
                        sendMessage(new Message<>(Command.REPEAT, "Repeat one"));
                    else if (setting.getRepeat().equals("Repeat one"))
                        sendMessage(new Message<>(Command.REPEAT, "Repeat all"));
                    else sendMessage(new Message<>(Command.REPEAT, "No repeat"));
                    break;
            }
        } else {
            pushMessage(new Message<>(Command.STATUS, connected));
            sendMessage(new Message<>(Command.REFRESH, null));
        }
        return START_STICKY;
    }



    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (messageSender == null)
            startHandler();
        else socketManager.stopHandler();
    }

    private String getName() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter == null)
            return android.os.Build.DEVICE;
        return adapter.getName();
    }

    private void pushMessage(Message<?> message) {
        Intent intent = new Intent(MESSAGE_RECEIVED);
        intent.putExtra("MESSAGE", message);
        manager.sendBroadcast(intent);
    }

    private void sendMessage(final Message<?> message) {
        if (messageSender != null)
            messageSender.addMessage(message);
    }

    public void updateDownload(String name, String message, boolean completed) {
        Intent intent = new Intent(getApplicationContext(), AppService.class);
        intent.setAction(ACTION);
        intent.putExtra("code", cancel);
        intent.putExtra("name", name);
        RemoteViews downloadRemoteViews = new RemoteViews(getPackageName(), R.layout.download_notification_layout);
        downloadRemoteViews.setOnClickPendingIntent(R.id.closeBtn,
                PendingIntent.getService(getApplicationContext(), cancel, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        downloadRemoteViews.setTextViewText(R.id.fileNameTxt, name);
        downloadRemoteViews.setTextViewText(R.id.statusTxt, message == null ? "" : message);
        downloadRemoteViews.setViewVisibility(R.id.statusTxt, completed ? VISIBLE : GONE);
        downloadRemoteViews.setViewVisibility(R.id.statusProgress, completed ? GONE : VISIBLE);
        downloadRemoteViews.setViewVisibility(R.id.closeBtn, completed ? GONE : VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(name) == null) {
                NotificationChannel channel = new NotificationChannel(name, "M-PLAYER download",
                        NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
        }

        Notification downloadNotification = new NotificationCompat.Builder(getApplicationContext()
                , name)
                .setSmallIcon(R.mipmap.get_app_white_24dp)
                .setTicker(getString(R.string.app_name))
                .setOngoing(!completed)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContent(downloadRemoteViews)
                .build();
        notificationManager.notify(name, 10, downloadNotification);
    }

    public void startHandler() {
        if (socketManager != null)
            socketManager.stopHandler();
        if (!stoppable) {
            connected = false;
            controlRemoteViews.setTextViewText(R.id.songTxt, "Server unreachable");
            notificationManager.notify(notificationId, notification);
            pushMessage(new Message<>(Command.STATUS, connected));
            messageSender = null;
            socketManager = new SocketManager(this);
            socketManager.startHandler();
        }
    }

    public void setRequestManager(MessageSender sender, SocketDownloadManager downloadManager) {
        connected = true;
        controlRemoteViews.setTextViewText(R.id.songTxt, "");
        notificationManager.notify(notificationId, notification);
        pushMessage(new Message<>(Command.STATUS, connected));
        this.messageSender = sender;
        this.downloadManager = downloadManager;
    }

    public void handleMessage(Message<?> message) {
        switch (message.getCommand()) {
            case SECRET: {
                sendMessage(new Message<>(Command.SECRET, Command.SECRET.toString()));
                return;
            }
            case DEVICE: {
                sendMessage(new Message<>(Command.DEVICE, getName()));
                return;
            }
            case MUTE: {
                setting.setMute((Boolean) message.getData());
                break;
            }
            case RANDOM: {
                setting.setRandom((Boolean) message.getData());
                controlRemoteViews.setImageViewResource(R.id.random,
                        setting.isRandom() ? R.mipmap.shuffle_blue_24dp
                                : R.mipmap.shuffle_white_24dp);
                notificationManager.notify(notificationId, notification);
                break;
            }
            case REPEAT: {
                setting.setRepeat((String) message.getData());
                controlRemoteViews.setImageViewResource(R.id.repeat, setting.getRepeat().equals("Repeat one")
                        ? R.mipmap.repeat_one_blue_24dp
                        : setting.getRepeat().equals("Repeat all") ?
                        R.mipmap.repeat_blue_24dp
                        : R.mipmap.repeat_white_24dp);
                notificationManager.notify(notificationId, notification);
                break;
            }
            case PLAY: {
                controlRemoteViews.setImageViewResource(R.id.playpause, R.mipmap.pause);
                controlRemoteViews.setTextViewText(R.id.songTxt, ((Song) message.getData())
                        .getName());
                notificationManager.notify(notificationId, notification);
                break;
            }
            case PAUSE: {
                controlRemoteViews.setImageViewResource(R.id.playpause, R.mipmap.play);
                notificationManager.notify(notificationId, notification);
                break;
            }
            case STOP: {
                controlRemoteViews.setImageViewResource(R.id.playpause, R.mipmap.play);
                controlRemoteViews.setTextViewText(R.id.songTxt, "");
                notificationManager.notify(notificationId, notification);
                break;
            }
        }
        pushMessage(message);
    }
}
