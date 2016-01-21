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
package mb.rxui.event;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import mb.rxui.Subscription;
import mb.rxui.ThreadChecker;
import mb.rxui.event.operator.Operator;
import mb.rxui.event.operator.OperatorFilter;
import mb.rxui.event.operator.OperatorMap;
import mb.rxui.event.publisher.EventPublisher;
import mb.rxui.event.publisher.LiftEventPublisher;
import mb.rxui.property.Property;
import mb.rxui.property.PropertyObservable;
import mb.rxui.property.PropertyObserver;
import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

/**
 * An EventStream represents a stream that emits discrete events. This
 * differs from a {@link Property} since an {@link EventStream} does not have a
 * current value.
 * 
 * <p>
 * Notes:
 * <li>An Event Stream will emit zero, one or many events followed by one
 * completed event
 * <li>An Event Stream can only be interacted with on the same thread that it
 * was created on. Attempting to access any of the methods outside of the thread
 * it was created on will throw an {@link IllegalStateException}
 * <li>An Event Stream does not <i><b>"remember"</b></i> the last event that it
 * published. Therefore new observers will not be called back immediately as
 * with a {@link Property}.
 * 
 * @param <E>
 *            The type of events emitted by this event stream.
 */
public class EventStream<E> {
    
    private final EventPublisher<E> eventPublisher;
    private final ThreadChecker threadChecker;
    
    protected EventStream(EventPublisher<E> eventPublisher, ThreadChecker threadChecker) {
        this.eventPublisher = requireNonNull(eventPublisher);
        this.threadChecker = requireNonNull(threadChecker);
    }
    
    /**
     * Creates an EventStream using the provided {@link EventPublisher}.
     * 
     * @param eventPublisher
     *            some {@link EventPublisher} to back this stream.
     */
    public EventStream(EventPublisher<E> eventPublisher) {        
        this(eventPublisher, ThreadChecker.create());
    }
    
    /**
     * Subscribes to new events emitted by this stream.
     * 
     * @param eventHandler
     *            some event handler that should be fired when a new event is
     *            emitted by this stream.
     * @return a {@link Subscription} that can be used to stop the eventHandler
     *         from consuming events from this stream.
     * @throws IllegalStateException
     *             if called from a thread other than the thread that this event
     *             stream was created on.
     */
    public final Subscription onEvent(Consumer<E> eventHandler) {
        return observe(EventStreamObserver.create(eventHandler));
    }
    
    /**
     * Subscribes to the completed event of this stream.
     * 
     * @param onCompletedAction some Runnable to execute when this stream is completed.
     * @return a {@link Subscription} that can be used to stop the onCompletedAction
     *         from firing when this stream completes.
     * @throws IllegalStateException
     *             if called from a thread other than the thread that this event
     *             stream was created on.
     */
    public final Subscription onCompleted(Runnable onCompletedAction) {
        return observe(EventStreamObserver.create(onCompletedAction));
    }
    
    /**
     * Subscribes to onEvents and onCompleted events, via the provided observer.
     * 
     * @param observer
     *            some {@link EventStreamObserver} to observe this stream.
     * @return a {@link Subscription} that can be used to stop this observer
     *         from responding to events.
     * @throws IllegalStateException
     *             if called from a thread other than the thread that this event
     *             stream was created on.
     */
    public final Subscription observe(EventStreamObserver<E> observer) {
        threadChecker.checkThread();
        return eventPublisher.subscribe(observer);
    }
    
    /**
     * Transforms this stream by the provided operator, creating a new stream.
     * 
     * @param operator
     *            some operator to transform this stream by.
     * @return a new {@link EventStream} transformed by the provided operator.
     */
    public final <R> EventStream<R> lift(Operator<E, R> operator) {
        requireNonNull(operator);
        return new EventStream<>(new LiftEventPublisher<>(operator, eventPublisher), threadChecker);
    }
    
    /**
     * Creates a new stream that transforms events emitted by this stream by the
     * provided mapper.
     * 
     * @param mapper
     *            some function to transform the events emitted by this stream.
     * @return a new {@link EventStream}, that transforms events emitted by this
     *         stream by the provided mapper.
     */
    public final <R> EventStream<R> map(Function<E, R> mapper) {
        return lift(new OperatorMap<>(mapper));
    }
    
    /**
     * Creates a new stream that filters the events emitted by this stream, such
     * that only those that satisfy the provided predicate are emitted.
     * 
     * @param predicate
     *            some predicate to filter this stream by.
     * @return A new {@link EventStream} that only emits values emitted by this
     *         stream that satisfy the provided predicate.
     */
    public final EventStream<E> filter(Predicate<E> predicate) {
        return lift(new OperatorFilter<>(predicate));
    }
    
    /**
     * Creates a property that is bound to this stream.
     * 
     * @param initialValue
     *            some value to initialize the property with.
     * @return a new {@link Property}, that is bound to this stream and is
     *         initialized with the provided intialValue.
     * @throws IllegalStateException
     *             if called from a thread other than the thread that this event
     *             stream was created on. 
     */
    public final Property<E> toProperty(E initialValue) {
        Property<E> property = Property.create(initialValue);
        
        Subscription subscription = onEvent(property::setValue);
        property.onDisposed(subscription::dispose);
        
        return property;
    }
    
    /**
     * Creates a new {@link Observable} backed by this event stream.
     * 
     * <p>
     * <b>Note:</b> The created observable can only be subscribed to from the same
     * thread as this stream was created on. Attempting to subscribe on a
     * different thread will throw an {@link IllegalStateException}.
     * 
     * @return an {@link Observable} backed by this event stream.
     */
    public final Observable<E> asObservable() {
        return Observable.create(subscriber -> {
            observe(EventStreamObserver.create(subscriber::onNext, subscriber::onCompleted)); 
        });
    }
    
    /**
     * Creates an event stream that whose source of events is some observable.
     * 
     * @param observable
     *            an {@link Observable} to provide the source of events to the
     *            created event stream.
     * @return A new {@link EventStream} whose source of events is the provided
     *         observable.
     */
    public static final <E> EventStream<E> from(Observable<E> observable) {
        
        return new EventStream<>(eventObserver -> {

            EventStreamSubscriber<E> subscriber = new EventStreamSubscriber<>(eventObserver);

            rx.Subscriber<E> rxSubscriber = new Subscriber<E>() {
                @Override
                public void onCompleted() {
                    subscriber.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    // TODO: What to do here, perhaps complete and send the error to the global error handler.
                }

                @Override
                public void onNext(E event) {
                    subscriber.onEvent(event);
                }
            };

            observable.subscribe(rxSubscriber);

            Subscription eventStreamSubscription = Subscription.fromDisposable(rxSubscriber::unsubscribe);

            rxSubscriber.add(Subscriptions.create(eventStreamSubscription::dispose));

            return eventStreamSubscription;
        });
    }
}
