package tv.weatherclock;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

/** The screensaver as an ordinary screen, from Settings > Preview: the shell can't start a dream without root. */
public class PreviewActivity extends Activity {

    private WeatherScene scene;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.dream_weather);
        scene = new WeatherScene(this, getWindow().getDecorView());
    }

    @Override
    protected void onStart() {
        super.onStart();
        scene.start();
    }

    @Override
    protected void onStop() {
        scene.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        scene.destroy();
        super.onDestroy();
    }
}
