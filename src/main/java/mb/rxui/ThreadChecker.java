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
import static mb.rxui.Preconditions.checkState;

import javax.swing.SwingUtilities;

import javafx.application.Platform;

/**
 * Validates the current thread against some criteria.
 */
@FunctionalInterface
public interface ThreadChecker extends Runnable {
    /**
     * Checks if the current thread against some criteria.
     * @throws IllegalStateException if the current thread is invalid.
     */
    void checkThread();

    @Override
    default void run() {
        checkThread();
    }

    static ThreadChecker create() {
        if (isEventDispatchThread())
            return EDT_THREAD_CHECKER;
        
        if(isFxApplicationThread())
            return PLATFORM_THREAD_CHECKER;

        long threadId = Thread.currentThread().getId();

        return () -> checkState(Thread.currentThread().getId() == threadId,
                                "Method should have been called from Thread with id, [" + threadId + "]" + 
                                " but instead it was called from: " + Thread.currentThread());
    }

    static final ThreadChecker EDT_THREAD_CHECKER = () -> {
        checkState(SwingUtilities.isEventDispatchThread(),
                   "Method should have been called on the EDT, but instead it was called from: " + 
                   Thread.currentThread());
    };
    
    static final ThreadChecker PLATFORM_THREAD_CHECKER = () -> {
        checkState(Platform.isFxApplicationThread(),
                   "Method should have been called on the JavaFx Platform Thread, but instead it was called from: " + 
                   Thread.currentThread());
    };
}
