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
package mb.rxui.property.publisher;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import mb.rxui.property.CompositeSubscription;
import mb.rxui.property.PropertyObservable;
import mb.rxui.property.PropertyObserver;
import mb.rxui.property.PropertySubscriber;
import mb.rxui.property.Subscription;

public class MergePropertyPublisher<R> implements PropertyPublisher<R> {

    private final List<PropertyObservable<R>> observables;
    private int disposeCount = 0;
    private R currentValue;
    
    public MergePropertyPublisher(List<PropertyObservable<R>> observables) {
        this.observables = Objects.requireNonNull(observables);
        this.currentValue = observables.get(0).get();
    }
    
    /**
     * @return true if the disposeCount equals the number of observables that have been subscribed to.
     */
    private boolean incrementDisposeCount() {
        disposeCount++;
        return disposeCount == observables.size();
    }
    
    @Override
    public R get() {
        return currentValue;
    }

    @Override
    public PropertySubscriber<R> subscribe(PropertyObserver<R> observer) {
        
        PropertySubscriber<R> mergeSubscriber = new PropertySubscriber<>(observer);
        
        AtomicBoolean hasEmittedFirstValue = new AtomicBoolean(false);
        
        List<Subscription> subscriptions = 
            observables.stream()
                       .map(subscribe(mergeSubscriber, hasEmittedFirstValue))
                       .collect(Collectors.toList());
        
        CompositeSubscription subscription = new CompositeSubscription(subscriptions);
        
        mergeSubscriber.doOnDispose(subscription::dispose);
        
        return mergeSubscriber;
    }

    private Function<? super PropertyObservable<R>, ? extends Subscription> 
        subscribe(PropertySubscriber<R> mergeSubscriber, AtomicBoolean hasEmittedFirstValue) {
        
        return observable -> observable.observe(value -> {
            if (!value.equals(get()) || hasEmittedFirstValue.compareAndSet(false, true)) {
                MergePropertyPublisher.this.currentValue = value;
                mergeSubscriber.onChanged(get());
            }
        } , () -> {
            if (incrementDisposeCount())
                mergeSubscriber.onDisposed();
        });
    }
}
