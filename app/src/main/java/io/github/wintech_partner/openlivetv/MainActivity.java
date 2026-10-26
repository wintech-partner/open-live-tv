package io.github.wintech_partner.openlivetv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One screen: the player on top (or left), the channel list below (or right). Channels come from assets/channels.json. */
public class MainActivity extends Activity {
    private static final int BG = 0xFF0B0D10, PANEL = 0xFF14181D, FG = 0xFFECEFF3, DIM = 0xFF8B95A1, ACC = 0xFFE5243B;

    private static final class Channel {
        final String id, name, group, url;
        Channel(String id, String name, String group, String url) { this.id = id; this.name = name; this.group = group; this.url = url; }
    }

    private static final class Row {
        final Channel channel; final String header;
        Row(Channel c, String h) { channel = c; header = h; }
    }

    private final List<Channel> channels = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final Set<String> favs = new HashSet<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private ExoPlayer player;
    private PlayerView playerView;
    private ListView listView;
    private LinearLayout root;
    private BaseAdapter adapter;
    private String currentId = "";
    private boolean fullscreen;
    private final Runnable retry = new Runnable() { @Override public void run() { play(find(currentId)); } };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("openlivetv", MODE_PRIVATE);
        favs.addAll(prefs.getStringSet("favs", new HashSet<String>()));
        currentId = prefs.getString("last", "");
        loadChannels();
        buildRows();

        root = new LinearLayout(this);
        root.setBackgroundColor(BG);
        playerView = new PlayerView(this);
        playerView.setKeepScreenOn(true);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        playerView.setFullscreenButtonClickListener(new PlayerView.FullscreenButtonClickListener() {
            @Override public void onFullscreenButtonClick(boolean isFullScreen) { setFullscreen(isFullScreen); }
        });
        listView = new ListView(this);
        listView.setBackgroundColor(PANEL);
        listView.setDivider(null);
        adapter = new Adapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override public void onItemClick(android.widget.AdapterView<?> p, View v, int pos, long id) {
                Row r = rows.get(pos);
                if (r.channel != null) play(r.channel); else if (r.header == null) about();
            }
        });
        listView.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(android.widget.AdapterView<?> p, View v, int pos, long id) {
                Channel c = rows.get(pos).channel;
                if (c == null) return false;
                toggleFav(c);
                return true;
            }
        });
        layoutForOrientation();
        setContentView(root);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public WindowInsets onApplyWindowInsets(View v, WindowInsets in) {
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets s = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    v.setPadding(s.left, s.top, s.right, s.bottom);
                } else {
                    v.setPadding(in.getSystemWindowInsetLeft(), in.getSystemWindowInsetTop(), in.getSystemWindowInsetRight(), in.getSystemWindowInsetBottom());
                }
                return in;
            }
        });

        player = new ExoPlayer.Builder(this).build();
        // one media item at a time, so the controller would hide next/previous: report them as available and zap the channel list instead
        playerView.setPlayer(new ForwardingPlayer(player) {
            @Override public Player.Commands getAvailableCommands() {
                return super.getAvailableCommands().buildUpon().addAll(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM).build();
            }
            @Override public boolean isCommandAvailable(int command) {
                return command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS
                        || command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM || command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
                        || super.isCommandAvailable(command);
            }
            @Override public boolean hasNextMediaItem() { return true; }
            @Override public boolean hasPreviousMediaItem() { return true; }
            @Override public void seekToNext() { zap(1); }
            @Override public void seekToPrevious() { zap(-1); }
            @Override public void seekToNextMediaItem() { zap(1); }
            @Override public void seekToPreviousMediaItem() { zap(-1); }
        });
        player.addListener(new Player.Listener() {
            @Override public void onPlayerError(PlaybackException e) {
                Toast.makeText(MainActivity.this, R.string.error_retry, Toast.LENGTH_SHORT).show();
                handler.removeCallbacks(retry);
                handler.postDelayed(retry, 5000);
            }
        });
        Channel start = find(currentId);
        if (start == null && !channels.isEmpty()) start = channels.get(0);
        if (start != null) play(start);
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        layoutForOrientation();
    }

    private void layoutForOrientation() {
        boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        root.removeAllViews();
        root.setOrientation(land ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        float d = getResources().getDisplayMetrics().density;
        if (land) {
            root.addView(playerView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            root.addView(listView, new LinearLayout.LayoutParams((int) (300 * d), ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            int h = getResources().getDisplayMetrics().widthPixels * 9 / 16;
            root.addView(playerView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));
            root.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }
        listView.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
    }

    private void setFullscreen(boolean on) {
        fullscreen = on;
        listView.setVisibility(on ? View.GONE : View.VISIBLE);
        if (on && getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else if (!on) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                if (on) { c.hide(WindowInsets.Type.systemBars()); c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE); }
                else c.show(WindowInsets.Type.systemBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(on
                    ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    : View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    @Override public void onBackPressed() {
        if (fullscreen) setFullscreen(false); else super.onBackPressed();
    }

    @Override public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_CHANNEL_UP || code == KeyEvent.KEYCODE_PAGE_UP) { zap(-1); return true; }
        if (code == KeyEvent.KEYCODE_CHANNEL_DOWN || code == KeyEvent.KEYCODE_PAGE_DOWN) { zap(1); return true; }
        return super.onKeyDown(code, e);
    }

    private void zap(int step) {
        int i = 0;
        for (int k = 0; k < channels.size(); k++) if (channels.get(k).id.equals(currentId)) i = k;
        play(channels.get((i + step + channels.size()) % channels.size()));
    }

    @Override protected void onStop() { super.onStop(); if (player != null) player.pause(); }
    @Override protected void onStart() { super.onStart(); if (player != null) player.play(); }
    @Override protected void onDestroy() {
        handler.removeCallbacks(retry);
        if (player != null) { player.release(); player = null; }
        super.onDestroy();
    }

    private void play(Channel c) {
        if (c == null || player == null) return;
        handler.removeCallbacks(retry);
        currentId = c.id;
        prefs.edit().putString("last", c.id).apply();
        player.setMediaItem(MediaItem.fromUri(c.url));
        player.prepare();
        player.setPlayWhenReady(true);
        setTitle(c.name);
        adapter.notifyDataSetChanged();
    }

    private Channel find(String id) {
        for (Channel c : channels) if (c.id.equals(id)) return c;
        return null;
    }

    private void toggleFav(Channel c) {
        boolean added = favs.add(c.id);
        if (!added) favs.remove(c.id);
        prefs.edit().putStringSet("favs", new HashSet<>(favs)).apply();
        Toast.makeText(this, added ? R.string.fav_added : R.string.fav_removed, Toast.LENGTH_SHORT).show();
        buildRows();
        adapter.notifyDataSetChanged();
    }

    private void about() {
        new AlertDialog.Builder(this).setTitle(R.string.about).setMessage(R.string.about_text).setPositiveButton(R.string.close, null).show();
    }

    private void loadChannels() {
        try {
            InputStream in = getAssets().open("channels.json");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            JSONArray a = new JSONArray(out.toString("UTF-8"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                channels.add(new Channel(o.getString("id"), o.getString("name"), o.getString("group"), o.getString("url")));
            }
        } catch (Exception e) {
            Toast.makeText(this, "channels.json: " + e, Toast.LENGTH_LONG).show();
        }
    }

    private void buildRows() {
        rows.clear();
        Map<String, List<Channel>> groups = new LinkedHashMap<>();
        List<Channel> fav = new ArrayList<>();
        for (Channel c : channels) {
            if (favs.contains(c.id)) fav.add(c);
            List<Channel> g = groups.get(c.group);
            if (g == null) { g = new ArrayList<>(); groups.put(c.group, g); }
            g.add(c);
        }
        if (!fav.isEmpty()) {
            rows.add(new Row(null, getString(R.string.favorites)));
            for (Channel c : fav) rows.add(new Row(c, null));
        }
        for (Map.Entry<String, List<Channel>> e : groups.entrySet()) {
            rows.add(new Row(null, e.getKey()));
            for (Channel c : e.getValue()) rows.add(new Row(c, null));
        }
        rows.add(new Row(null, null));   // "About" row
    }

    private final class Adapter extends BaseAdapter {
        @Override public int getCount() { return rows.size(); }
        @Override public Object getItem(int i) { return rows.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public boolean areAllItemsEnabled() { return false; }
        @Override public boolean isEnabled(int i) { Row r = rows.get(i); return r.channel != null || r.header == null; }
        @Override public View getView(int i, View v, ViewGroup parent) {
            TextView t = new TextView(MainActivity.this);
            float d = getResources().getDisplayMetrics().density;
            t.setSingleLine(true);
            Row r = rows.get(i);
            if (r.channel != null) {
                boolean cur = r.channel.id.equals(currentId);
                t.setText((favs.contains(r.channel.id) ? "★  " : "") + r.channel.name);
                t.setTextSize(16);
                t.setTextColor(FG);
                t.setBackgroundColor(cur ? ACC : PANEL);
                t.setPadding((int) (20 * d), (int) (12 * d), (int) (12 * d), (int) (12 * d));
            } else if (r.header != null) {
                t.setText(r.header);
                t.setTextSize(13);
                t.setTextColor(DIM);
                t.setBackgroundColor(0xFF1B2128);
                t.setPadding((int) (12 * d), (int) (10 * d), (int) (12 * d), (int) (10 * d));
            } else {
                t.setText(R.string.about);
                t.setTextSize(14);
                t.setTextColor(DIM);
                t.setGravity(Gravity.CENTER);
                t.setBackgroundColor(PANEL);
                t.setPadding(0, (int) (18 * d), 0, (int) (18 * d));
            }
            return t;
        }
    }
}
