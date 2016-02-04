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

import static javafx.application.Platform.isFxApplicationThread;
import static javax.swing.SwingUtilities.isEventDispatchThread;

import java.util.concurrent.TimeUnit;

import mb.rxui.disposables.Disposable;

/**
 * A thread context captures a thread and provides a mechanism to assert that
 * the current thread matches this context. Additionally a thread context can be
 * used to schedule a runnable to run at some fixed time in the future on this
 * context's thread.
 * <p>
 * TODO: Consider renaming to EventLoop and adding invokeNow, invokeLater and
 * invokeAndWait.
 */
public interface ThreadContext {
    /**
     * Checks if the current thread matches the thread for this context.
     * 
     * @throws IllegalStateException
     *             if the current thread does not match the valid thread for
     *             this context.
     */
    void checkThread();
    
    /**
     * Schedules some runnable to execute at some later time.
     * 
     * @param runnable
     *            some runnable to execute
     * @param delay
     *            some delay from now.
     * @param timeUnit
     *            the time unit of the delay
     * @return a {@link Disposable} that can be used to cancel the scheduled
     *         runnable.
     */
    Disposable schedule(Runnable runnable, long time, TimeUnit timeUnit);
    

    static ThreadContext create() {
        if (isEventDispatchThread())
            return SWING_THREAD_CONTEXT;

        if (isFxApplicationThread())
            return JAVAFX_THREAD_CONTEXT;

        throw new IllegalStateException(
                "Thread: [" + Thread.currentThread() + "] cannot be used to back a thread context.");
    }

    static final ThreadContext SWING_THREAD_CONTEXT = new SwingThreadContext();
    static final ThreadContext JAVAFX_THREAD_CONTEXT = new JavaFxThreadContext();
}
