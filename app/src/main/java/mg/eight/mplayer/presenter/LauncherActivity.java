package mg.eight.mplayer.presenter;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import mg.eight.mplayer.R;
import mg.eight.mplayer.task.AppService;
import mg.eight.mplayer.model.Command;
import mg.eight.mplayer.model.Message;
import mg.eight.mplayer.model.Setting;
import mg.eight.mplayer.model.Song;
import mg.eight.mplayer.model.VerticalSeekBar;

public class LauncherActivity extends AppCompatActivity implements View.OnKeyListener {

    private SeekBar timeSeekBar;
    private VerticalSeekBar volumeSeekBar;
    private ListView songList;
    private TextView songTxt, timeTxt;
    private ImageButton playPauseBtn, randomBtn, repeatBtn, volumeBtn;
    private RelativeLayout statusLayout, controlLayout, menuLayout;
    private SearchView searchView;
    private ImageButton playNextBtn, removeBtn, downloadBtn;

    private ArrayList<Song> originalList = new ArrayList<>();
    private SongListAdapter adapter;
    private Song songPlayed = null;
    private Setting setting = Setting.getInstance();
    private String[] playLists;
    private int plId;

    private LocalBroadcastManager manager;

    private final String MESSAGE_RECEIVED = "m.player.commander.action.RECEIVE",
            MESSAGE_SENT = "m.player.commander.action.SEND";
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "m.player.commander.action.RECEIVE".equals(intent.getAction())){
                handleMessage((Message<?>) intent.getSerializableExtra("MESSAGE"));
            }
        }
    };

    private Song[] filesToDownload;
    private final int PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        controlLayout = findViewById(R.id.controlLayout);
        menuLayout = findViewById(R.id.menuLayout);
        findViewById(R.id.closeBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.setCheck(false);
                adapter.notifyDataSetChanged();
            }
        });
        randomBtn = findViewById(R.id.randomBtn);
        downloadBtn = findViewById(R.id.downloadBtn);
        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filesToDownload = new Song[adapter.getSelected().size()];
                adapter.getSelected().toArray(filesToDownload);
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest
                        .permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    sendMessage(new Message<>(Command.DOWNLOAD, filesToDownload));
                    adapter.setCheck(false);
                    adapter.notifyDataSetChanged();
                }
                else {
                    ActivityCompat.requestPermissions(LauncherActivity.this, new String[]
                            {Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CODE);
                }
            }
        });
        playNextBtn = findViewById(R.id.playNextBtn);
        playNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.getSelected().remove(songPlayed);
                if(!adapter.getSelected().isEmpty()){
                    Song[] songs = new Song[adapter.getSelected().size()];
                    adapter.getSelected().toArray(songs);
                    sendMessage(new Message<>(Command.PLAYNEXT, songs));
                }
                adapter.setCheck(false);
                adapter.notifyDataSetChanged();
            }
        });
        removeBtn = findViewById(R.id.removeBtn);
        removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Song[] songs = new Song[adapter.getSelected().size()];
                adapter.getSelected().toArray(songs);
                sendMessage(new Message<>(Command.REMOVE, songs));
                adapter.setCheck(false);
                adapter.notifyDataSetChanged();
            }
        });
        randomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(new Message<>(Command.RANDOM, !setting.isRandom()));
            }
        });
        repeatBtn = findViewById(R.id.repeatBtn);
        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (setting.getRepeat().equals("No repeat"))
                    sendMessage(new Message<>(Command.REPEAT, "Repeat one"));
                else if (setting.getRepeat().equals("Repeat one"))
                    sendMessage(new Message<>(Command.REPEAT, "Repeat all"));
                else sendMessage(new Message<>(Command.REPEAT, "No repeat"));
            }
        });
        statusLayout = findViewById(R.id.statusLayout);
        volumeSeekBar = findViewById(R.id.volumeSbr);
        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (volumeSeekBar.isFromUser())
                    sendMessage(new Message<>(Command.VOLUME, volumeSeekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        volumeBtn = findViewById(R.id.volumeBtn);
        volumeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(new Message<>(Command.MUTE, !setting.isMute()));
            }
        });

        timeSeekBar = findViewById(R.id.timeSbr);
        timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) sendMessage(new Message<>(Command.DURATION, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        songList = findViewById(R.id.songList);
        songList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(adapter.isCheck()){
                    Song s = adapter.getItem(position);
                    if(adapter.getSelected().contains(s))
                        adapter.getSelected().remove(s);
                    else adapter.getSelected().add(s);
                    adapter.notifyDataSetChanged();
                    playNextBtn.setVisibility((adapter.getSelected().isEmpty() || songPlayed == null)?
                            View.GONE:View.VISIBLE);
                    removeBtn.setVisibility(adapter.getSelected().isEmpty()?View.GONE:View.VISIBLE);
                    downloadBtn.setVisibility(adapter.getSelected().isEmpty()?View.GONE:View.VISIBLE);
                }else sendMessage(new Message<>(Command.PLAY, adapter.getItem(position)));
            }
        });
        songList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if(!adapter.isCheck()) {
                    adapter.setCheck(true);
                    Song s = adapter.getItem(position);
                    adapter.getSelected().add(s);
                    adapter.notifyDataSetChanged();
                }
                return true;
            }
        });
        adapter = new SongListAdapter(this);
        songList.setAdapter(adapter);
        songTxt = findViewById(R.id.songTxt);
        timeTxt = findViewById(R.id.timeTxt);
        findViewById(R.id.previousBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(new Message<>(Command.PREVIOUS, null));
            }
        });
        findViewById(R.id.stopBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(new Message<>(Command.STOP, null));
            }
        });
        playPauseBtn = findViewById(R.id.playpauseBtn);
        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(new Message<>(Command.PLAYPAUSE, null));
            }
        });
        findViewById(R.id.nextBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(new Message<>(Command.NEXT, null));
            }
        });
        findViewById(R.id.playlistBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog().show();
            }
        });
        songList.setOnKeyListener(this);
        controlLayout.setOnKeyListener(this);
        manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(receiver,
                new IntentFilter(MESSAGE_RECEIVED));
        startService(new Intent(this, AppService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_launcher, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                refreshList(query);
                return false;
            }

        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                controlLayout.setVisibility(View.GONE);
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                if(menuLayout.getVisibility() == View.GONE)
                    controlLayout.setVisibility(View.VISIBLE);
                return false;
            }
        });
        return true;
    }

    @Override
    public void onBackPressed() {
        if(searchView != null && !searchView.isIconified()){
            searchView.onActionViewCollapsed();
            if(menuLayout.getVisibility() == View.GONE){
                controlLayout.setVisibility(View.VISIBLE);
            }
            return;
        }
        if(menuLayout.getVisibility() == View.VISIBLE){
            adapter.setCheck(false);
            adapter.notifyDataSetChanged();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        switch (id){
            case R.id.menu_setting:
                intent = new Intent(LauncherActivity.this, SettingActivity.class);
                startActivity(intent);
                return true;
            case  R.id.menu_about:
                intent = new Intent(LauncherActivity.this, AboutActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKey(View view, int i, KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeSeekBar.setProgress(volumeSeekBar.getProgress() + 1);
            sendMessage(new Message<>(Command.VOLUME, volumeSeekBar.getProgress()));
        }
        if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeSeekBar.setProgress(volumeSeekBar.getProgress() - 1);
            sendMessage(new Message<>(Command.VOLUME, volumeSeekBar.getProgress()));
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(filesToDownload != null){
                    sendMessage(new Message<>(Command.DOWNLOAD, filesToDownload));
                    adapter.setCheck(false);
                    adapter.notifyDataSetChanged();
                }
            }
            else {
                Toast.makeText(this, "Permission denied, download canceled!",
                        Toast.LENGTH_LONG);
            }
        }
    }

    @Override
    protected void onDestroy() {
        manager.unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void updateStatus(boolean connected) {
        statusLayout.setVisibility(connected ? View.GONE : View.VISIBLE);
        if (!connected) {
            adapter.setCheck(false);
            adapter.getSongs().clear();
            originalList.clear();
            timeSeekBar.setProgress(0);
            playPauseBtn.setImageResource(R.mipmap.play);
            volumeBtn.setImageResource(R.mipmap.volume_up_blue_200_24dp);
            volumeSeekBar.setProgress(0);
            timeTxt.setText("");
            adapter.notifyDataSetChanged();
            songTxt.setText("");
            timeTxt.setText("");
        }
    }

    private void refreshList(String string) {
        adapter.getSongs().clear();
        for (Song s : originalList){
            if (s.getName().substring(0, s.getName().lastIndexOf(".")).toLowerCase()
                    .contains(string.toLowerCase()))
                adapter.getSongs().add(s);
        }
        adapter.notifyDataSetChanged();
    }

    private int getPlID(String pl) {
        for (int i = 0; i < playLists.length; i++) {
            if (playLists[i].equals(pl))
                return i;
        }
        return 0;
    }

    private Dialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Playlists");
        builder.setSingleChoiceItems(playLists, plId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendMessage(new Message<>(Command.SETLIST, which));
                adapter.getSongs().clear();
                dialog.dismiss();
            }
        });
        return builder.create();
    }

    private void sendMessage(Message<?> message) {
        Intent intent = new Intent(MESSAGE_SENT);
        intent.putExtra("MESSAGE", message);
        manager.sendBroadcast(intent);
    }

    private void handleMessage(Message message) {
        if(statusLayout.getVisibility() == View.VISIBLE)
            statusLayout.setVisibility(View.GONE);
        switch (message.getCommand()) {
            case STATUS: {
                updateStatus((Boolean) message.getData());
                break;
            }
            case CLOSE: {
                finish();
                break;
            }
            case PLAY: {
                songPlayed = (Song) message.getData();
                playNextBtn.setVisibility((adapter.isCheck() && !adapter.getSelected().isEmpty())?
                        View.VISIBLE: View.GONE);
                songTxt.setText(songPlayed.getName());
                playPauseBtn.setImageResource(R.mipmap.pause);
                adapter.notifyDataSetChanged();
                break;
            }
            case PAUSE: {
                playPauseBtn.setImageResource(R.mipmap.play);
                songTxt.setText(((Song) message.getData()).getName());
                break;
            }

            case STOP: {
                songPlayed = null;
                playNextBtn.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
                songTxt.setText("");
                timeTxt.setText("");
                timeSeekBar.setProgress(0);
                playPauseBtn.setImageResource(R.mipmap.play);
                break;
            }

            case TIME: {
                timeTxt.setText((String) message.getData());
                break;
            }
            case VOLUME: {
                int vol = (Integer) message.getData();
                if (volumeSeekBar.getProgress() != vol) {
                    volumeSeekBar.setFromUser(false);
                    volumeSeekBar.setProgress(vol);
                }
                break;
            }

            case MUTE: {
                setting.setMute((Boolean) message.getData());
                volumeBtn.setImageResource(setting.isMute()
                        ? R.mipmap.ic_volume_off_blue_24dp
                        : R.mipmap.volume_up_blue_200_24dp);
                break;
            }

            case RANDOM: {
                setting.setRandom((Boolean) message.getData());
                randomBtn.setBackgroundResource(setting.isRandom() ? R.drawable.random_selected
                        : R.drawable.random_unselected);
                break;
            }
            case REPEAT: {
                setting.setRepeat((String) message.getData());
                repeatBtn.setBackgroundResource(setting.getRepeat().equals("No repeat") ? R.drawable.random_unselected
                        : R.drawable.random_selected);
                repeatBtn.setImageResource(setting.getRepeat().equals("Repeat one")
                        ? R.mipmap.repeat_one_white_24dp
                        : R.mipmap.repeat_white_24dp);
                break;
            }

            case PLAYLIST: {
                originalList.clear();
                originalList.addAll(Arrays.asList((Song[]) message.getData()));
                adapter.setCheck(false);
                refreshList(searchView == null?"": searchView.getQuery().toString());
                break;
            }

            case DURATION: {
                timeSeekBar.setProgress((Integer) message.getData());
                break;
            }

            case SETLIST: {
                plId = getPlID((String) message.getData());
                break;
            }

            case LIST: {
                playLists = (String[]) message.getData();
                break;
            }
        }
    }

    public void showMenu(boolean flag){
        controlLayout.setVisibility(!flag?View.VISIBLE:View.GONE);
        menuLayout.setVisibility(flag?View.VISIBLE:View.GONE);
        playNextBtn.setVisibility((flag && songPlayed != null)?
                View.VISIBLE:View.GONE);
        removeBtn.setVisibility(flag?View.VISIBLE:View.GONE);
        downloadBtn.setVisibility(flag?View.VISIBLE:View.GONE);
    }

    public Song getSongPlayed() {
        return songPlayed;
    }
}
