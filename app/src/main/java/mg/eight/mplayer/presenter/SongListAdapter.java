package mg.eight.mplayer.presenter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import mg.eight.mplayer.R;
import mg.eight.mplayer.model.Song;

public class SongListAdapter extends BaseAdapter {
    private final LauncherActivity launcher;
    private final LayoutInflater mInflater;
    private final ArrayList<Song> songs = new ArrayList<>();
    private final ArrayList<Song> selected = new ArrayList<>();
    private boolean check = false;

    public SongListAdapter(Context context) {
        super();
        mInflater = LayoutInflater.from(context);
        launcher = (LauncherActivity) context;
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Song getItem(int position) {
        return songs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.song_list_screen, null);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.icon);
            holder.text = convertView.findViewById(R.id.text);
            holder.select = convertView.findViewById(R.id.select_check);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final Song s = getItem(position);
        holder.select.setVisibility(isCheck() ? View.VISIBLE : View.GONE);
        if (selected.contains(s))
            holder.select.setImageResource(R.mipmap.check_box_blue_36dp);
        else holder.select.setImageResource(R.mipmap.check_box_outline_blank_blue_36dp);
        convertView.setBackgroundResource(R.drawable.unselected_selector);
        holder.text.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        if (s.equals(launcher.getSongPlayed())) {
            convertView.setBackgroundResource(R.drawable.selected_selector);
            holder.text.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);
        }
        holder.text.setText(s.getName());
        return convertView;
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public ArrayList<Song> getSelected() {
        return selected;
    }

    public boolean isCheck() {
        return check;
    }

    public void setCheck(boolean check) {
        this.check = check;
        selected.clear();
        launcher.showMenu(check);
    }

    class ViewHolder {
        ImageView select;
        ImageView icon;
        TextView text;
    }
}
