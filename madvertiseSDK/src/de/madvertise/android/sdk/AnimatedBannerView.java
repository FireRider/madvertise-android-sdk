/*
 * Copyright 2011 madvertise Mobile Advertising GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.madvertise.android.sdk;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Handler;
import android.util.Log;
import android.view.View;

/**
 * AnimatedBannerView.java View to show animated ads. This class actually
 * works, but has problems with the different gif formats and should not be used
 * in version 1.0. Error handling is not implemented yet. Both problems will be
 * addressed in the upcoming releases of the sdk. <br>
 */
class AnimatedBannerView extends View {

    private Movie movie;

    // private long movieStart;
    private static final String CACHED_BANNER_FILE = "cachedBanner.gif";

    private Handler handler = new Handler();

    public AnimatedBannerView(Context context) {
        super(context);
        setFocusable(true);

        // try to open downloaded banner
        FileInputStream file = null;
        try {
            file = context.openFileInput(CACHED_BANNER_FILE);
        } catch (FileNotFoundException e) {
            if (MadUtil.PRINT_LOG)
                Log.d(MadUtil.LOG, "Could not load cached banner");
            e.printStackTrace();
        }

        byte[] byteArray = MadUtil.convertStreamToByteArray(file);
        movie = Movie.decodeByteArray(byteArray, 0, byteArray.length);

        if (movie == null) {
            if (MadUtil.PRINT_LOG)
                Log.d(MadUtil.LOG, "Movie is null");
        }

        // start thread to update the view
        new Thread() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    handler.post(new Runnable() {
                        public void run() {
                            invalidate();
                        }
                    });
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (movie != null) {
            long now = android.os.SystemClock.uptimeMillis();
            int duration = Math.max(movie.duration(), 1);
            int pos = (int)(now % duration);
            movie.setTime(pos);
            movie.draw(canvas, getWidth() - movie.width(), getHeight() - movie.height());
        }
    }
}
