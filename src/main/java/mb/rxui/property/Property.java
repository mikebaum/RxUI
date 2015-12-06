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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.security.auth.Subject;

import mb.rxui.disposables.Disposable;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * A Property is some value that can change over time. It is similar to a
 * {@link Subject} with the exception that the onNext method has been called
 * onChanged and there is no onError or onCompleted methods. To 'complete' the
 * property it is necessary to call {@link #dispose()}.<br>
 * <br>
 * A property is intended to be used as a replacement for traditional mutable
 * fields and listeners. The canonical use case would be as a field in some
 * model.<br>
 * <br>
 * For a property to be well behaved it must guarantee the following contract:<br>
 * 1) only emit a values if the new value differs than the previous<br>
 * 2) always offer a non-null value from the method {@link #get()}<br>
 * 3) always emit a value when subscribed to. If the property is already
 * disposed when subscribed to the subscriber will receive the last value and
 * then be unsubscribed<br>
 * 4) never accept a new value while it is currently dispatching a value.
 * Re-entrancy is strictly blocked<br>
 * 5) cleanup all subscriptions after being destroyed<br>
 * 6) not leak any subscriptions after being destroyed<br>
 * <br>
 * ADDITIONAL NOTES:<br>
 * 1) Attempting to set the current value to null, will result
 * in a {@link NullPointerException}.<br>
 * 2) If your property can be null at any point, make sure to create an optional
 * property, using {@link #createOptional()} or {@link #createOptional(Object)}
 * <br>
 * 3) A property is not thread safe, therefore it can only be accessed from the
 * same thread that it was created on. Attempting to access any of a properties
 * methods a from a thread other than the one it was created on will result in
 * an {@link IllegalStateException}.<br>
 * 
 * 
 * @param <T>
 *            the type of object that this property emits.
 */
public interface Property<M> extends Consumer<M>, Supplier<M>, Disposable, PropertyObservable<M> {
    /**
     * Sets the current value of this property.
     * 
     * @param value
     *            a new value to set to this property.
     * @throws NullPointerException
     *             if an attempt is made to set the value to null.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    void setValue(M value);

    /**
     * Implementation of Consumer, redirects to {@link #setValue(Object)}
     * 
     * @throws NullPointerException
     *             if an attempt is made to set the value to null.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    default void accept(M value) {
        setValue(value);
    }
    
    /**
     * Resets this property to it's initial value.
     * 
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    void reset();

    /**
     * @return returns true, if this property currently has observers, false
     *         otherwise.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    boolean hasObservers();

    /**
     * Binds this property to the provided property. Any value changes from the
     * bound property will be propagated to this property.<br>
     * <br>
     * @param propertyToBindTo
     *            some property to bind to
     * @return a Subscription that can be used to cancel this binding.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     *             
     * TODO: what should we do if we try to bind to ourselves. Possible ideas: throw, ignore the 
     * request and return a already cancelled subscription (perhaps log a warning) or allow it 
     * since it will not cause any harm (I think).
     */
    default Subscription bind(PropertyObservable<M> propertyToBindTo) {
        return propertyToBindTo.onChanged(this);
    }

    /**
     * Synchronizes two properties, such that whenever either of the properties'
     * value changes the value of the other property will be updated with the
     * new value.<br>
     * <br>
     * NOTE:<br>
     * When initializing the synchronization the properties will take the value
     * of the propertyToSynchronize with. For example, consider propA = "taco"
     * and propB = "burrito", if propA.synchronize( propB ) is called both
     * properties will have the value "burrito" after establishing
     * synchronization.<br>
     * <br>
     * 
     * @param propertyToSynchornizeWith
     *            some property to synchronize this property with.
     * @return a subscription which can be used to cancel the synchronization.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from. TODO: what should we do if we try to
     *             synchronize to ourselves. Possible ideas: throw, ignore the
     *             request and return a already cancelled subscription (perhaps
     *             log a warning) or allow it since it will not cause any harm
     *             (I think).
     */
    default Subscription synchronize(Property<M> propertyToSynchornizeWith) {
        CompositeSubscription subscriptions = new CompositeSubscription();

        subscriptions.add(bind(propertyToSynchornizeWith));
        subscriptions.add(propertyToSynchornizeWith.bind(this));

        // If either of the properties is destroyed, cancel the subscription.
        onDisposed(subscriptions::unsubscribe);
        propertyToSynchornizeWith.onDisposed(subscriptions::unsubscribe);

        return subscriptions;
    }

    /**
     * Observe onChange and onDestroy events.
     * 
     * @param onChanged
     *            some listener of onChanged events.
     * @param onDisposed
     *            some listener of onDestroyed events.
     * @return a {@link Subscription} that can be used to cancel the
     *         subscription.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    @Override
    default Subscription observe(Consumer<M> onChanged, Runnable onDisposed) {
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
    @Override
    default Subscription onChanged(Consumer<M> onChanged) {
        return observe(PropertyObserver.create(onChanged));
    }

    /**
     * Adds some {@link Runnable} to execute when this property is destroyed.
     * 
     * @param onDisposedAction
     *            some runnable to run when this property is destroyed.
     * @return a {@link Subscription} that can be used to cancel the
     *         subscription.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    @Override
    default Subscription onDisposed(Runnable onDisposedAction) {
        return observe(PropertyObserver.create(onDisposedAction));
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
    default Observable<M> asObservable() {
        return Observable.create(subscriber -> {
            Subscription subscription = observe(subscriber::onNext, subscriber::onCompleted);
            subscriber.add(subscription);
        });
    }

    // Factory methods

    /**
     * Creates a property that is initialized with the provided value.
     * 
     * @param initialValue
     *            some initial value for this property
     * @return a new Property
     */
    static <M> Property<M> create(M initialValue) {
        return create(ModelPropertySource.create(initialValue));
    }

    /**
     * Creates a new property that is backed by the provided
     * {@link PropertySource}.
     * 
     * @param setter
     *            some consumer of set calls.
     * @param getter
     *            some supplier of values for this property.
     * @return a new property.
     */
    static <M> Property<M> create(PropertySource<M> propertySource) {
        return new PropertyImpl<>(propertySource);
    }

    /**
     * Creates an optional property
     * 
     * @return a new property initialized with empty.
     */
    static <M> Property<Optional<M>> createOptional() {
        return create(Optional.empty());
    }

    /**
     * Creates an optional property.
     * 
     * @param initialValue
     *            some value to initialize the optional property with.
     * @return a new property initialize with the provided initial value.
     */
    static <M> Property<Optional<M>> createOptional(M initialValue) {
        return create(Optional.of(initialValue));
    }

    /**
     * Creates a property this is bound to a {@link BehaviorSubject}.<br>
     * <br>
     * NOTE:<br>
     * The property will not manage the lifetime of the {@link BehaviorSubject}.
     * In other words, the property when destroyed will not complete the
     * {@link BehaviorSubject}. <br>
     * 
     * @param subject
     *            some subject to create a new property from.
     * @return a property that is backed by the provided subject.
     */
    public static <T> Property<T> fromSubject(BehaviorSubject<T> subject) {
        Property<T> property = Property.create(subject.getValue());

        Subscription subscription = subject.subscribe(property::setValue);
        property.onDisposed(subscription::unsubscribe);

        return property;
    }
}
