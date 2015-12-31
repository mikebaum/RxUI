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
package mb.rxui.property.dispatcher;

import static mb.rxui.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import mb.rxui.annotations.RequiresTest;
import mb.rxui.disposables.Disposable;
import mb.rxui.property.PropertyObserver;
import mb.rxui.property.PropertySubscriber;

/**
 * A dispatcher for property change events.
 * 
 * @param <M> the type of value this dispatcher dispatches.
 */
@RequiresTest
public final class PropertyDispatcher<M> implements Dispatcher<M> {

    private final List<PropertySubscriber<M>> subscribers = new ArrayList<>();
    private final IsDispatching isDispatching = new IsDispatching();
    private static Comparator<? super PropertySubscriber<?>> SUBSCRIBER_COMPARATOR = createComparator();
    
    private boolean isDisposed = false;
    
    PropertyDispatcher() {}
    
    @Override
    public void dispatchValue(M newValue) {
        checkState(! isDisposed, "Dispatcher has been dipsoed, cannot dispatch: " + newValue);
        new ArrayList<>(subscribers).forEach(subscriber -> subscriber.onChanged(newValue));
    }

    @Override
    public void dispose() {
        isDisposed = true;
        new ArrayList<>(subscribers).forEach(PropertyObserver::onDisposed);
        subscribers.clear();
    }
    
    @Override
    public void onDisposed(Disposable toDispose)
    {
        if (isDisposed) {            
            toDispose.dispose();
            return;
        }
        
        Runnable runnable = toDispose::dispose;
        subscribers.add(new PropertySubscriber<>(PropertyObserver.create(runnable)));
    }
    
    @Override
    public boolean isDispatching() {
        return isDispatching.isDispatching();
    }
    
    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
    
    @Override
    public int getSubscriberCount() {
        return subscribers.size();
    }
    
    @Override
    public PropertySubscriber<M> subscribe(PropertyObserver<M> observer) {
        
        PropertySubscriber<M> subscriber = new PropertySubscriber<>(wrapObserver(observer, isDispatching));
        
        subscriber.doOnUnsubscribe(() -> subscribers.remove(subscriber));
        subscribers.add(subscriber);
        subscribers.sort(SUBSCRIBER_COMPARATOR);
        
        return subscriber;
    }
    
    private static Comparator<? super PropertySubscriber<?>> createComparator() {
        return (subscriber1, subscriber2) -> {
            if (subscriber1.isBinding() && !subscriber2.isBinding())
                return -1;
            
            if (!subscriber1.isBinding() && subscriber2.isBinding())
                return 1;
            
            return 0;
        };
    }

    private static <M> PropertyObserver<M> wrapObserver(PropertyObserver<M> observer, IsDispatching isDispatching) {
        return new PropertyObserver<M>() {

            @Override
            public void onChanged(M newValue) {
                try {
                    isDispatching.setDispatching(true);
                    observer.onChanged(newValue);
                } finally {
                    isDispatching.setDispatching(false);
                }
            }

            @Override
            public void onDisposed() {
                observer.onDisposed();
            }

            @Override
            public boolean isBinding() {
                return observer.isBinding();
            }
        };
    }
    
    /**
     * Small helper to share the dispatching flag between a Subscription and the dispatcher.
     */
    static class IsDispatching
    {
        private boolean isDispatching = false;

        public void setDispatching(boolean isDispatching) {
            this.isDispatching = isDispatching;
        }

        public boolean isDispatching() {
            return isDispatching;
        }
    }
}
