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
package mb.rxui.dispatcher;

import static java.util.Objects.requireNonNull;
import static mb.rxui.dispatcher.Dispatcher.Type.EVENT;

import java.util.ArrayList;
import java.util.List;

import mb.rxui.event.EventObserver;
import mb.rxui.event.EventStream;
import mb.rxui.event.EventSubscriber;

/**
 * An {@link EventDispatcher} is a {@link Dispatcher} suitable to be used with
 * an {@link EventStream}.
 * 
 * @param <V> the type of events this dispatcher dispatches.
 */
public class EventDispatcher<V> extends AbstractDispatcher<V, EventSubscriber<V>, EventObserver<V>> {

    private final List<EventSubscriber<V>> subscribers;

    private EventDispatcher(List<EventSubscriber<V>> subscribers) {
        super(subscribers, subscriber -> subscriber::onEvent, subscriber -> subscriber::onCompleted, EVENT);
        this.subscribers = requireNonNull(subscribers);
    }
    
    static <E> EventDispatcher<E> create() {
        return new EventDispatcher<>(new ArrayList<>());
    }

    @Override
    public EventSubscriber<V> subscribe(EventObserver<V> observer) {
        
        EventSubscriber<V> subscriber = new EventSubscriber<>(wrapObserver(observer));
        
        if(isDisposed()) {
            subscriber.onCompleted();
            return subscriber;
        }
        
        subscriber.doOnDispose(() -> subscribers.remove(subscriber));
        subscribers.add(subscriber);
        
        return subscriber;
    }

    private EventObserver<V> wrapObserver(EventObserver<V> observer) {
        return new EventObserver<V>() {
            @Override
            public void onEvent(V event) {
                invoke(() -> observer.onEvent(event));
            }

            @Override
            public void onCompleted() {
                invoke(observer::onCompleted);
            }
        };
    }
}
