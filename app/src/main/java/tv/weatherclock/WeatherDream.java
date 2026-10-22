package tv.weatherclock;

import android.service.dreams.DreamService;

/** Screensaver: the clock, date and weather (WeatherScene). Any key wakes the TV back to where it was. */
public class WeatherDream extends DreamService {

    private WeatherScene scene;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.dream_weather);
        scene = new WeatherScene(this, getWindow().getDecorView());
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        scene.start();
    }

    @Override
    public void onDreamingStopped() {
        scene.stop();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (scene != null) scene.destroy();
        super.onDetachedFromWindow();
    }
}
