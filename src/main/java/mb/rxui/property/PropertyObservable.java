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

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import mb.rxui.Preconditions;
import mb.rxui.ThreadChecker;
import mb.rxui.property.operator.OperatorFilter;
import mb.rxui.property.operator.OperatorIsDirty;
import mb.rxui.property.operator.OperatorMap;
import mb.rxui.property.operator.OperatorTake;
import mb.rxui.property.operator.PropertyOperator;
import rx.Observable;
import rx.subscriptions.Subscriptions;

/**
 * A property that can only be observed. This is effectively a read-only version
 * of a {@link Property}.<br>
 * <br>
 * NOTES:<br>
 * 1) A property observable will emit a new value via
 * {@link #onChanged(Consumer)} when it's value changes.<br>
 * 2) Once the property is disposed it will emit an onDiposed event.<br>
 * 3) A property observable is assumed to always contain a value.<br>
 * 
 * @param <M>
 *            the type of the value this property observable emits.
 * @see Property
 */
public class PropertyObservable<M> implements Supplier<M> {
    
    private final PropertyPublisher<M> propertyPublisher;
    private final ThreadChecker threadChecker;
    private final M initialValue;
    
    protected PropertyObservable(PropertyPublisher<M> propertyPublisher, ThreadChecker threadChecker) {
        this.propertyPublisher = requireNonNull(propertyPublisher);
        this.threadChecker = requireNonNull(threadChecker);
        initialValue = requireNonNull(propertyPublisher.get());
    }

    /**
     * Gets the current value of this property.
     * 
     * NOTE: Calling get will always return a non-null value. If not the
     * implementation is misbehaved.
     * 
     * @return the current value
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    @Override
    public final M get() {
        threadChecker.checkThread();
        return propertyPublisher.get();
    }

    public final Subscription observe(PropertyObserver<M> observer) {
        threadChecker.checkThread();
        return propertyPublisher.subscribe(observer);
    }

    /**
     * Observe onChange and onDestroy events.
     * 
     * @param onChanged
     *            some listener of onChanged events.
     * @param onDisposed
     *            some listener of onDisposed events.
     * @return a {@link Subscription} that can be used to cancel the
     *         subscription.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    public final Subscription observe(Consumer<M> onChanged, Runnable onDisposed) {
        return observe(PropertyObserver.create(onChanged, onDisposed));
    }

    /**
     * Adds a listener that will be updated when the value of this property
     * changes.
     * 
     * NOTE: The listener will be called back immediately with the current value
     * when subscribing.
     * 
     * @param onChanged
     *            some listener to update when this property's value changes
     * @return a {@link Subscription} that can be used to cancel the
     *         subscription.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    public final Subscription onChanged(Consumer<M> onChanged) {
        return observe(PropertyObserver.create(onChanged));
    }

    /**
     * Adds some {@link Runnable} to execute when this property is disposed.
     * 
     * @param onDisposedAction
     *            some runnable to run when this property is disposed.
     * @return a {@link Subscription} that can be used to cancel the
     *         subscription.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    public final Subscription onDisposed(Runnable onDisposedAction) {
        return observe(PropertyObserver.create(onDisposedAction));
    }
    
    /**
     * Transforms this Property Observable by the provided mapper function.
     * 
     * @param mapper some function the emitted values of this property observable.
     * @return a new PropertyObservable with the values transformed by the provided mapper.
     */
    public final <R> PropertyObservable<R> map(Function<M, R> mapper) {
        return lift(new OperatorMap<>(mapper));
    }
    
    /**
     * Filters out the values emitted by this property that do not satisfy the
     * provided predicate. If the current value of this property observable do
     * not satisfy the predicate the current value of the filtered property will
     * become {@link Optional#empty()}.
     * 
     * @param predicate
     *            some predicate to use to filter this property observable.
     * @return a new {@link PropertyObservable} that optionally emits the
     *         current value or empty if the current value does not satisfy the
     *         predicate.
     */
    public final PropertyObservable<Optional<M>> filter(Predicate<M> predicate) {
        return lift(new OperatorFilter<>(predicate));
    }
    
    /**
     * Converts this property observable into boolean property observable
     * representing whether or not the current value differs from the initial.
     * 
     * @return a new {@link PropertyObservable} that emits true if the current
     *         value is different than the initial value, false otherwise.
     */
    public final PropertyObservable<Boolean> isDirty() {
        return lift(new OperatorIsDirty<M>(initialValue));
    }
    
    /**
     * Creates a new {@link PropertyObservable} that when subscribed to will
     * only every emit as many items as specified by the provided amount
     * parameter.<br>
     * <br>
     * NOTE: The returned {@link PropertyObservable} will always return the
     * latest value of the property observable that it was derived from.
     * 
     * @param amount
     *            the amount of values to emit
     * @return a new {@link PropertyObservable} that only emits as many items as
     *         specified in the amount parameter
     * @throws IllegalStateException if zero elements is requested
     */
    public final PropertyObservable<M> take(int amount) {
        Preconditions.checkState(amount > 0, "Cannot take zero elements");
        return lift(new OperatorTake<>(amount));
    }
    
    /**
     * Using the provided operator creates a new, converted property observable.
     * 
     * @param operator
     *            some operator that converts the value stream.
     * @return a new {@link PropertyObservable} which results from applying the
     *         provided operator to this property observable.
     */
    public final <R> PropertyObservable<R> lift(PropertyOperator<M, R> operator) {
        Objects.requireNonNull(operator);
        return new PropertyObservable<>(operator.apply(propertyPublisher), threadChecker);
    }
    
    /**
     * Creates an Observable that is backed by this property.<br>
     * <br>
     * NOTES:<br>
     * 1) If this property is destroyed, the observable will be completed.<br>
     * 2) Subscribers to this observable will never have their onError method
     * called, since properties do not propagate errors.
     * 
     * @return an Observable that is backed by this property.
     */
    public final Observable<M> asObservable() {
        return Observable.create(subscriber -> {
            Subscription subscription = observe(subscriber::onNext, subscriber::onCompleted);
            subscriber.add(Subscriptions.create(subscription::dispose));
        });
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((get() == null) ? 0 : get().hashCode());
        return result;
    }

    /**
     * Compares the current value of this property observable to the current
     * value of the provided property observable.
     * 
     * @param obj
     *            some other property to compare for equality
     * @return true if the provided object is a property and if the current
     *         value of this property and the provided object are equal, false
     *         otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
            
        return get().equals(((Supplier<?>) obj).get());
    }

    /**
     * Combines the values of two property observables and produces a new result
     * using the provided function any time either of the values changes.
     * 
     * @param observable1
     *            the first observable to combine
     * @param observable2
     *            the second observable to combine
     * @param combiner
     *            some function that will be called any time either of the
     *            provided observables changes
     * @return a new Property Observable that will emit the result of combining
     *         the values of the provided observables using the provided
     *         function any time either observables' value changes.
     */
    public static <T1, T2, R> PropertyObservable<R> combine(PropertyObservable<T1> observable1, PropertyObservable<T2> observable2, BiFunction<T1, T2, R> combiner) {
        List<PropertyObservable<?>> observables =Arrays.asList(observable1, observable2);
        Supplier<R> combineSupplier = () -> combiner.apply(observable1.get(), observable2.get());
        
        return new PropertyObservable<R>(new CombinePropertyPublisher<R>(combineSupplier, observables), ThreadChecker.create());
    }
}