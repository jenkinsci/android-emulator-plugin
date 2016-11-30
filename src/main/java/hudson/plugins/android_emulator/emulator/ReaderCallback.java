package hudson.plugins.android_emulator.emulator;

import javax.annotation.Nonnull;

/**
 * Created by Adam Kobus on 01.12.2016.
 * Copyright (c) 2016, inFullMobile
 * License: MIT, file: /LICENSE
 */
interface ReaderCallback {
    void onLineRead(@Nonnull String line);

    ReaderCallback STUB = new ReaderCallback() {
        @Override
        public void onLineRead(@Nonnull String line) {

        }
    };
}
