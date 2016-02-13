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

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import mb.rxui.disposables.Disposable;

/**
 * A event loop captures a thread and provides a mechanism to assert that the
 * current thread matches this context. Additionally a event loop can be used to
 * schedule a runnable to run at some fixed time in the future on this context's
 * thread.
 * <p>
 * NOTE: An event loop guarantees that only one runnable is executing at a time.
 * Scheduling Runnables through an event loop serializes their execution. This
 * differs than an arbitrary {@link Executor} in the sense that an executor
 * cannot guarantee serialized execution unless it's concurrency is 1.
 * <p>
 * TODO: Consider adding invokeNow, invokeLater and invokeAndWait.
 */
public interface EventLoop {
    /**
     * Checks if the current thread matches the thread for this context.
     * 
     * @throws IllegalStateException
     *             if the current thread does not match the valid thread for
     *             this context.
     */
    void checkInEventLoop();
    
    /**
     * @return true if the current thread matches the thread of this event loop, false otherwise.
     */
    boolean isInEventLoop();
    
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
    

    static EventLoop create() {
        if (isEventDispatchThread())
            return SWING_EVENT_LOOP;

        if (isFxApplicationThread())
            return JAVAFX_EVENT_LOOP;

        throw new IllegalStateException(
                "Thread: [" + Thread.currentThread() + "] cannot be used to back a event loop.");
    }

    static final EventLoop SWING_EVENT_LOOP = new SwingEventLoop();
    static final EventLoop JAVAFX_EVENT_LOOP = new JavaFxEventLoop();
}
