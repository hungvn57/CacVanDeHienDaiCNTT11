package com.ssaurel.simplelauncher;

import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.IBinder;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import android.os.Handler;

public class GIFWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        try{
            Movie movie = Movie.decodeStream(getResources().getAssets().open("a1.gif"));
            return new GIFWallpaperEngine(movie);
        }
            catch(IOException e){
                Log.d("GIF", "Could not load assets");
                return null;
            }
    }

    private class GIFWallpaperEngine extends Engine{
        private final int frameDuration = 20;

        private SurfaceHolder holder;
        private Movie movie;
        private boolean visible;
        private Handler handler ;

        public GIFWallpaperEngine(Movie movie) {
            this.movie = movie;
            handler = new Handler();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            this.holder = surfaceHolder;
        }

        private Runnable drawGIF = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };

        private void draw(){
            if(visible){
                Canvas canvas = holder.lockCanvas();
                canvas.save();
                canvas.scale(6f, 6f);
                movie.draw(canvas, -100, 0);
                canvas.restore();
                holder.unlockCanvasAndPost(canvas);
                movie.setTime((int) (System.currentTimeMillis() % movie.duration()));

                handler.removeCallbacks(drawGIF);
                handler.postDelayed(drawGIF, frameDuration);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if(visible){
                handler.post(drawGIF);
            }else{
                handler.removeCallbacks(drawGIF);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            handler.removeCallbacks(drawGIF);
        }
    }
}
