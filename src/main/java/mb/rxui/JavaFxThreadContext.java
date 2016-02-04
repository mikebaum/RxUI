/**
 * Copyright 2015 Mike Baum
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package mb.rxui;

import static mb.rxui.Preconditions.checkArgument;
import static mb.rxui.Preconditions.checkState;

import java.util.concurrent.TimeUnit;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import mb.rxui.disposables.Disposable;
import mb.rxui.disposables.DisposableRunnable;

/**
 * A thread context that can be used for JavaFx applications.
 */
public class JavaFxThreadContext implements ThreadContext {

    @Override
    public void checkThread() {
        checkState(Platform.isFxApplicationThread(),
                   "Method should have been called on the JavaFx Platform Thread, but instead it was called from: " + 
                   Thread.currentThread());
    }

    @Override
    public Disposable schedule(Runnable runnable, long time, TimeUnit timeUnit) {
        checkArgument(time >= 0, "Cannot schedule a runnable with a negative time [" + time + "]");
        
        DisposableRunnable disposableRunnable = new DisposableRunnable(runnable);
        
        Timeline timeLine = new Timeline(new KeyFrame(Duration.millis(timeUnit.toMillis(time)), 
                                                      event -> disposableRunnable.run()));
        timeLine.play();
        
        return disposableRunnable;
    }
}
