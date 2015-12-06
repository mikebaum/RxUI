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
package mb.rxui.property;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import mb.rxui.annotations.RequiresTest;
import mb.rxui.disposables.Disposable;

/**
 * A dispatcher for property change events.
 * 
 * @param <M> the type of value this dispatcher dispatches.
 */
@RequiresTest
public final class PropertyDispatcher<M> implements Disposable {

    private final List<PropertySubscriber<M>> subscribers = new ArrayList<>();
    
    private boolean isDispatching = false;
    private boolean isDisposed = false;
    
    public void dispatchValue(M newValue) {
        subscribers.forEach(subscriber -> dispatchOnChanged(subscriber, newValue));
    }

    @Override
    public void dispose() {
        isDisposed = true;
        new ArrayList<>(subscribers).forEach(PropertyObserver::onDisposed);
        subscribers.clear();
    }
    
    public void onDisposed(Disposable toDispose)
    {
        Runnable runnable = toDispose::dispose;
        subscribers.add(new PropertySubscriber<>(PropertyObserver.create(runnable)));
    }
    
    public boolean isDispatching() {
        return isDispatching;
    }
    
    public boolean isDisposed() {
        return isDisposed;
    }
    
    public boolean hasObservers() {
        return ! subscribers.isEmpty();
    }
    
    /**
     * Adds a subscriber to this dispatcher.
     * 
     * @param subscriber
     *            some property subscriber.
     * @return a consumer that could be used to dispatch the first value to the
     *         subscriber.
     */
    public Consumer<M> addSubscriber(PropertySubscriber<M> subscriber) {
        subscriber.doOnUnsubscribe(() -> subscribers.remove(subscriber));
        subscribers.add(subscriber);
        return value -> dispatchOnChanged(subscriber, value);
    }
    
    private void dispatchOnChanged(PropertySubscriber<M> subscriber, M valueToDispatch) {
        isDispatching = true;
        subscriber.onChanged(valueToDispatch);
        isDispatching = false;
    }
}
