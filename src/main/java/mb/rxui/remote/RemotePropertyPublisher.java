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
package mb.rxui.remote;

import static java.util.Objects.requireNonNull;
import static mb.rxui.Preconditions.checkState;

import mb.rxui.EventLoop;
import mb.rxui.dispatcher.Dispatcher;
import mb.rxui.dispatcher.PropertyDispatcher;
import mb.rxui.disposables.Disposable;
import mb.rxui.property.PropertyId;
import mb.rxui.property.PropertyObserver;
import mb.rxui.property.publisher.PropertyPublisher;
import mb.rxui.subscription.Subscription;

/**
 * A remote property publisher is connected to a remote property using the
 * provided {@link PropertyService}. This property will receive updates via
 * callbacks from a remote source.
 * 
 * @param <T> the type of data of the remote property 
 */
public class RemotePropertyPublisher<T> implements PropertyPublisher<T>, Disposable {
    
    private final PropertyId<T> id;
    private final PropertyDispatcher<T> dispatcher;
    private final Subscription subscription;
    private final EventLoop eventLoop;
    private T value; // always accessed on the EDT does not need to be volatile
    
    public RemotePropertyPublisher(PropertyService propertyService, PropertyId<T> id) {
        this.id = requireNonNull(id);
        this.dispatcher = Dispatcher.createPropertyDispatcher();
        this.eventLoop = EventLoop.createEventLoop();
        this.value = requireNonNull(propertyService.getValue(id));
        this.subscription = propertyService.registerListener(id, this::updateValue);
    }

    @Override
    public T get() {
        return value;
    }
    
    @Override
    public Subscription subscribe(PropertyObserver<T> observer) {
        return dispatcher.subscribe(observer);
    }

    @Override
    public void dispose() {
        subscription.dispose();
        dispatcher.dispose();
    }
    
    public PropertyId<T> getId() {
        return id;
    }
    
    private void updateValue(T newValue) {
        checkState(!eventLoop.isInEventLoop(), "Update should not be called from the event loop");
        
        eventLoop.invokeLater(() -> {
            value = newValue;
            dispatcher.dispatch(newValue);
        });
    }
}
