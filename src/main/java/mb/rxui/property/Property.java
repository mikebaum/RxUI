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

import java.util.Optional;

import javax.security.auth.Subject;

import mb.rxui.ThreadChecker;
import mb.rxui.disposables.Disposable;
import mb.rxui.property.operator.OperatorIsDirty;
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
public class Property<M> extends PropertyObservable<M> implements PropertySource<M>, Disposable {

    private final PropertySource<M> propertySource;
    private final PropertyDispatcher<M> dispatcher;
    private final M initialValue;
    private final ThreadChecker threadChecker;

    private Property(PropertySource<M> propertySource, PropertyDispatcher<M> dispatcher, ThreadChecker threadChecker) {
        super(new PropertyPublisherImpl<>(propertySource, dispatcher), threadChecker);
        this.propertySource = requireNonNull(propertySource);
        this.dispatcher = requireNonNull(dispatcher);
        this.initialValue = requireNonNull(get(), "A Property must be initialized with a value");
        this.threadChecker = requireNonNull(threadChecker);
    }
    
    @Override
    public void dispose() {
        threadChecker.checkThread();
        dispatcher.dispose();
    }

    @Override
    public void setValue(M value) {
        threadChecker.checkThread();
    
        // blocks reentrant calls
        if (dispatcher.isDispatching())
            return;
    
        // once a property is disposed it is frozen
        if (dispatcher.isDisposed())
            return;
        
        // don't update the value if it's the same as the current value
        if (get().equals(value))
            return;
    
        propertySource.setValue(requireNonNull(value));
    }

    /**
     * Resets this property to it's initial value.
     * 
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    public final void reset() {
        setValue(initialValue);
    }
    
    /**
     * Binds this property to the provided property observable. Any value
     * changes from the bound property observable will be propagated to this
     * property.<br>
     * <br>
     * 
     * @param propertyToBindTo
     *            some property to bind to
     * @return a Subscription that can be used to cancel this binding.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     * 
     *             TODO: what should we do if we try to bind to ourselves.
     *             Possible ideas: throw, ignore the request and return a
     *             already cancelled subscription (perhaps log a warning) or
     *             allow it since it will not cause any harm (I think).
     */
    public final Subscription bind(PropertyObservable<M> propertyToBindTo) {
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
    public final Subscription synchronize(Property<M> propertyToSynchornizeWith) {
        CompositeSubscription subscriptions = new CompositeSubscription();

        subscriptions.add(bind(propertyToSynchornizeWith));
        subscriptions.add(propertyToSynchornizeWith.bind(this));

        // If either of the properties is destroyed, cancel the subscription.
        onDisposed(subscriptions::unsubscribe);
        propertyToSynchornizeWith.onDisposed(subscriptions::unsubscribe);

        return subscriptions;
    }

    public final boolean hasObservers() {
        threadChecker.checkThread();
        return dispatcher.hasSubscribers();
    }
    
    // Factory methods
    
    /**
     * Creates a property using the provided property source factory and thread
     * checker.
     * 
     * @param propertySourceFactory
     *            some factory that can be used to create a property source.
     * @param threadChecker
     *            the thread checker to use to verify thread contract when using
     *            the created property
     * @return a new {@link Property}
     */
    static final <M> Property<M> create(PropertySourceFactory<M> propertySourceFactory, 
                                        ThreadChecker threadChecker) {
        PropertyDispatcher<M> dispatcher = new PropertyDispatcher<>();
        return new Property<>(propertySourceFactory.apply(dispatcher), dispatcher, threadChecker);
    }
    
    /**
     * Creates a property that is initialized with the provided value.
     * 
     * @param initialValue
     *            some initial value for this property
     * @return a new Property
     */
    public static <M> Property<M> create(M initialValue) {
        return create(ModelPropertySource.createFactory(initialValue));
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
    public static <M> Property<M> create(PropertySourceFactory<M> propertySourceFactory) {
        return Property.create(propertySourceFactory, ThreadChecker.create());
    }

    /**
     * Creates an optional property
     * 
     * @return a new property initialized with empty.
     */
    public static <M> Property<Optional<M>> createOptional() {
        return create(Optional.empty());
    }

    /**
     * Creates an optional property.
     * 
     * @param initialValue
     *            some value to initialize the optional property with.
     * @return a new property initialize with the provided initial value.
     */
    public static <M> Property<Optional<M>> createOptional(M initialValue) {
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
