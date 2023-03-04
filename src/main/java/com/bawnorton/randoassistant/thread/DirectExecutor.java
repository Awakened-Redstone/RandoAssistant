package com.bawnorton.randoassistant.thread;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class DirectExecutor implements Executor {
    @Override
    public void execute(@NotNull Runnable command) {
        command.run();
    }
}
