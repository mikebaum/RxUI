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

import java.util.ArrayList;
import java.util.List;

import mb.rxui.dispatcher.AbstractDispatcher;
import mb.rxui.dispatcher.Dispatcher;

/**
 * An {@link EventDispatcher} is a {@link Dispatcher} suitable to be used with
 * an {@link EventStream}.
 * 
 * @param <V> the type of events this dispatcher dispatches.
 */
public class EventDispatcher<V> extends AbstractDispatcher<V, EventStreamSubscriber<V>, EventStreamObserver<V>> {

    private final List<EventStreamSubscriber<V>> subscribers;

    private EventDispatcher(List<EventStreamSubscriber<V>> subscribers) {
        super(subscribers, subscriber -> subscriber::onEvent, subscriber -> subscriber::onCompleted);
        this.subscribers = requireNonNull(subscribers);
    }
    
    public static <E> EventDispatcher<E> create() {
        return new EventDispatcher<>(new ArrayList<>());
    }

    @Override
    public EventStreamSubscriber<V> subscribe(EventStreamObserver<V> observer) {
        
        EventStreamSubscriber<V> subscriber = new EventStreamSubscriber<>(wrapObserver(observer));
        
        if(isDisposed()) {
            subscriber.onCompleted();
            return subscriber;
        }
        
        subscriber.doOnDispose(() -> subscribers.remove(subscriber));
        subscribers.add(subscriber);
        
        return subscriber;
    }

    private EventStreamObserver<V> wrapObserver(EventStreamObserver<V> observer) {
        return new EventStreamObserver<V>() {
            @Override
            public void onEvent(V event) {
                schedule(() -> observer.onEvent(event));
            }

            @Override
            public void onCompleted() {
                schedule(observer::onCompleted);
            }
        };
    }
}
