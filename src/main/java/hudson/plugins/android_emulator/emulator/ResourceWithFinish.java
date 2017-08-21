package hudson.plugins.android_emulator.emulator;

import javax.annotation.Nonnull;

/**
 * Created by Adam Kobus on 30.11.2016.
 * Copyright (c) 2016, inFullMobile
 * License: MIT, file: /LICENSE
 */
public interface ResourceWithFinish {
    boolean isFinished();
    void finish();
    @Nonnull String getResourceName();
}
