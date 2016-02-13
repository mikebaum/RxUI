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
package mb.rxui.dispatcher;

import static java.util.Objects.requireNonNull;
import static mb.rxui.Callbacks.runSafeCallback;
import static mb.rxui.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import mb.rxui.Observer;
import mb.rxui.Subscriber;
import mb.rxui.disposables.Disposable;

public abstract class AbstractDispatcher<V, S extends Subscriber & Observer<V>, O extends Observer<V>> implements Dispatcher<V, S, O> {

    private final List<S> subscribers;
    private final List<Disposable> disposables;
    private final Function<S, Consumer<V>> dispatchFunction;
    private final Function<S, Runnable> disposeFunction;

    private boolean isDispatching = false;
    private boolean isDisposed = false;
    
    protected AbstractDispatcher(List<S> subscribers, 
                                 Function<S, Consumer<V>> dispatchFunction, 
                                 Function<S, Runnable> disposeFunction) {
        this.subscribers = requireNonNull(subscribers);
        this.disposables = new ArrayList<>();
        this.dispatchFunction = requireNonNull(dispatchFunction);
        this.disposeFunction = requireNonNull(disposeFunction);
    }
    
    @Override
    public void dispose() {
        if(isDisposed)
            return;
        
        isDisposed = true;
        
        new ArrayList<>(subscribers).stream().map(disposeFunction).forEach(Runnable::run);
        subscribers.clear();
        
        disposables.forEach(disposable -> runSafeCallback(disposable::dispose));
        disposables.clear();
    }
    
    @Override
    public void onDisposed(Disposable toDispose) {
        if (isDisposed) {            
            toDispose.dispose();
            return;
        }
        
        disposables.add(toDispose);
    }

    @Override
    public void dispatch(V newValue) {
        checkState(!isDisposed, "Dispatcher has been disposed, cannot dispatch: " + newValue);
        new ArrayList<>(subscribers).stream().map(dispatchFunction).forEach(consumer -> consumer.accept(newValue));
    }

    @Override
    public void schedule(Runnable runnable) {
        wrapRunnable(runnable).run();
    }

    @Override
    public boolean isDispatching() {
        return isDispatching;
    }
    
    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
    
    @Override
    public int getSubscriberCount() {
        return subscribers.size();
    }

    /**
     * Ensures that when the runnable is running that the dispatch flag is
     * turned on and then off when execution is finished.
     * 
     * @param runnable
     *            some runnable to wrap
     * @return a new runnable that ensures to toggle on/off the dispatch flag
     *         before and after execution.
     */
    private Runnable wrapRunnable(Runnable runnable) {
        return () -> {            
            isDispatching = true;
            try {
                runnable.run();
            } finally {            
                isDispatching = false;
            }
        };
    }
}
