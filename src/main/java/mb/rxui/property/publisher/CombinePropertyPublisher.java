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

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import mb.rxui.Subscription;
import mb.rxui.property.CompositeSubscription;
import mb.rxui.property.PropertyObservable;
import mb.rxui.property.PropertyObserver;
import mb.rxui.property.PropertySubscriber;

/**
 * A {@link PropertyPublisher} that will combine the values of the provided
 * property observables any time the value of any of them change.
 *
 * @param <R>
 *            the type of the combined result
 */
public class CombinePropertyPublisher<R> implements PropertyPublisher<R> {

    private final List<PropertyObservable<?>> observables;
    private Supplier<R> combineSupplier;
    private R lastValue;
    private int disposeCount = 0;

    public CombinePropertyPublisher(Supplier<R> combineSupplier, List<PropertyObservable<?>> observables) {
        this.combineSupplier = requireNonNull(combineSupplier);
        this.observables = requireNonNull(observables);
        lastValue = combineSupplier.get();
    }

    /**
     * @return true if the disposeCount equals the number of observables that
     *         have been subscribed to.
     */
    private boolean incrementDisposeCount() {
        disposeCount++;
        return disposeCount == observables.size();
    }

    @Override
    public R get() {
        return combineSupplier.get();
    }

    @Override
    public Subscription subscribe(PropertyObserver<R> observer) {

        PropertySubscriber<R> combineSubscriber = new PropertySubscriber<>(observer);

        AtomicBoolean hasEmittedFirstValue = new AtomicBoolean(false);

        List<Subscription> subscriptions =
            observables.stream()
                       .map(subscribe(combineSubscriber, hasEmittedFirstValue))
                       .collect(Collectors.toList());

        CompositeSubscription subscription = new CompositeSubscription(subscriptions);

        combineSubscriber.doOnDispose(subscription::dispose);

        return combineSubscriber;
    }

    private Function<? super PropertyObservable<?>, ? extends Subscription> subscribe(
            PropertySubscriber<R> combineSubscriber, AtomicBoolean hasEmittedFirstValue) {

        return observable -> observable.observe(value -> {
            if (!get().equals(lastValue) || hasEmittedFirstValue.compareAndSet(false, true)) {
                CombinePropertyPublisher.this.lastValue = get();
                combineSubscriber.onChanged(get());
            }
        } , () -> {
            if (incrementDisposeCount())
                combineSubscriber.onDisposed();
        });
    }
}
