package tv.weatherclock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pick the city for the forecast (or go back to automatic location) and preview the screensaver. Also takes a
 * city from adb, picking the first match: am start -n tv.weatherclock/.SettingsActivity --es city "Budapest",
 * or --ez auto true for automatic.
 */
public class SettingsActivity extends Activity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView current;
    private TextView status;
    private EditText query;
    private LinearLayout results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Typeface light = getResources().getFont(R.font.outfit_light);
        Typeface regular = getResources().getFont(R.font.outfit_regular);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(96), dp(64), dp(96), dp(48));

        TextView title = text(getString(R.string.app_name), 40, 0xE6FFFFFF, light);
        root.addView(title);
        current = text("", 18, 0x99FFFFFF, regular);
        current.setPadding(0, dp(6), 0, dp(28));
        root.addView(current);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        query = new EditText(this);
        query.setHint(R.string.settings_hint);
        query.setSingleLine(true);
        query.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        query.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_SEARCH || enter) {
                search(query.getText().toString(), false);
                return true;
            }
            return false;
        });
        row.addView(query, new LinearLayout.LayoutParams(dp(420), ViewGroup.LayoutParams.WRAP_CONTENT));
        Button go = new Button(this);
        go.setText(R.string.search);
        go.setOnClickListener(v -> search(query.getText().toString(), false));
        LinearLayout.LayoutParams goLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        goLp.setMarginStart(dp(16));
        row.addView(go, goLp);
        Button auto = new Button(this);
        auto.setText(R.string.automatic);
        auto.setOnClickListener(v -> useAutomatic());
        row.addView(auto, goLp);
        Button preview = new Button(this);
        preview.setText(R.string.preview);
        preview.setOnClickListener(v -> startActivity(new Intent(this, PreviewActivity.class)));
        row.addView(preview, goLp);
        root.addView(row);

        status = text("", 16, 0x99FFFFFF, regular);
        status.setPadding(0, dp(12), 0, dp(8));
        root.addView(status);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        root.addView(results);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF090C14);
        scroll.addView(root);
        setContentView(scroll);
        showCurrent();

        if (!handleExtras(getIntent()) && Weather.isAuto(this) && Weather.placeName(this) == null) useAutomatic();
    }

    /** The page is already open: an adb start brings it to the front with the new extras. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleExtras(intent);
    }

    /** The adb extras: --es city NAME or --ez auto true. False when there were none. */
    private boolean handleExtras(Intent intent) {
        String city = intent.getStringExtra("city");
        if (city != null && !city.trim().isEmpty()) {
            search(city, true);
            return true;
        }
        if (intent.getBooleanExtra("auto", false)) {
            useAutomatic();
            return true;
        }
        return false;
    }

    /** Switch to the IP-based location and look it up now, so the page can say where it landed. */
    private void useAutomatic() {
        Weather.setAuto(this);
        results.removeAllViews();
        status.setText(R.string.locating);
        showCurrent();
        io.execute(() -> {
            boolean ok;
            try {
                Weather.locateIfNeeded(this);
                ok = true;
            } catch (Exception e) {
                ok = false;
            }
            boolean found = ok;
            main.post(() -> {
                if (isFinishing()) return;
                status.setText(found ? "" : getString(R.string.locate_failed));
                showCurrent();
            });
        });
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void showCurrent() {
        String place = Weather.placeName(this);
        String text = place == null ? getString(R.string.no_city) : "Forecast for " + place;
        if (Weather.isAuto(this)) text += "  ·  " + getString(R.string.automatic_note);
        current.setText(text);
    }

    /** pickFirst: from the adb extra, choose the first match without asking. */
    private void search(String name, boolean pickFirst) {
        if (name.trim().isEmpty()) return;
        status.setText(R.string.searching);
        results.removeAllViews();
        io.execute(() -> {
            List<Weather.Place> found;
            try {
                found = Weather.search(name);
            } catch (Exception e) {
                main.post(() -> status.setText(R.string.search_failed));
                return;
            }
            main.post(() -> {
                if (isFinishing()) return;
                if (found.isEmpty()) {
                    status.setText(R.string.no_results);
                } else if (pickFirst) {
                    choose(found.get(0));
                } else {
                    status.setText("");
                    for (Weather.Place p : found) {
                        Button b = new Button(this);
                        b.setText(p.label);
                        b.setAllCaps(false);
                        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                        b.setOnClickListener(v -> choose(p));
                        results.addView(b, new LinearLayout.LayoutParams(dp(640), ViewGroup.LayoutParams.WRAP_CONTENT));
                    }
                    results.getChildAt(0).requestFocus();
                }
            });
        });
    }

    private void choose(Weather.Place p) {
        Weather.setPlace(this, p);
        results.removeAllViews();
        status.setText(p.label);
        showCurrent();
    }

    private TextView text(String s, int sp, int color, Typeface face) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(face);
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
