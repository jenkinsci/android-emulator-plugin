package hudson.plugins.android_emulator.emulator;

import javax.annotation.Nonnull;

/**
 * Created by Adam Kobus on 01.12.2016.
 * Copyright (c) 2016, inFullMobile
 * License: MIT, file: /LICENSE
 */
interface ReadFinishedListener {
    void onReadFinished(@Nonnull EmulatorOutputReader reader);

    ReadFinishedListener STUB = new ReadFinishedListener() {
        @Override
        public void onReadFinished(@Nonnull EmulatorOutputReader reader) {

        }
    };
}
