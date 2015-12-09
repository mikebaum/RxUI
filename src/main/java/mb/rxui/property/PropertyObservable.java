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

import java.util.function.Consumer;
import java.util.function.Supplier;

import mb.rxui.ThreadChecker;
import mb.rxui.property.PropertySource.PropertySourceFactory;
import rx.Observable;
import rx.Subscription;

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
    
    private final PropertySource<M> propertySource;
    private final ThreadChecker threadChecker;
    private final PropertyDispatcher<M> dispatcher = new PropertyDispatcher<>();
    
    protected PropertyObservable(PropertySourceFactory<M> propertySourceFactory, ThreadChecker threadChecker) {
        this.propertySource = requireNonNull(propertySourceFactory).apply(dispatcher);
        this.threadChecker = threadChecker;
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
        checkThread();
        return propertySource.get();
    }

    public final boolean hasObservers() {
        threadChecker.checkThread();
        return dispatcher.hasObservers();
    }

    public final Subscription observe(PropertyObserver<M> observer) {
        threadChecker.checkThread();

        PropertySubscriber<M> subscriber = new PropertySubscriber<>(observer);

        // adds and pushes the latest value to the subscriber
        dispatcher.addSubscriber(subscriber).accept(get());

        if (dispatcher.isDisposed())
            subscriber.onDisposed();

        return subscriber;
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
//    public final <R> PropertyObservable<M> map(Function<M, R> mapper) {
//        
//    }
    
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
            subscriber.add(subscription);
        });
    }
    
    protected void checkThread()
    {
        threadChecker.checkThread();
    }
    
    protected PropertyDispatcher<M> getDispatcher()
    {
        return dispatcher;
    }
    
    protected PropertySource<M> getPropertySource() {
        return propertySource;
    }
}