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

import mb.rxui.ThreadChecker;
import rx.Subscription;

/**
 * Default implementation of a {@link Property}
 * 
 * @param <M>
 *            the type this property holds
 */
public final class PropertyImpl<M> implements Property<M> {
    private final PropertySource<M> propertySource;
    private final M initialValue;
    private final ThreadChecker threadChecker;
    private final PropertyDispatcher<M> dispatcher = new PropertyDispatcher<>();

    PropertyImpl(PropertySource<M> propertySource) {
        this.propertySource = requireNonNull(propertySource);
        this.initialValue = requireNonNull(propertySource.get(), "A Property must be initialized with a value");
        threadChecker = ThreadChecker.create();
        
        propertySource.setDispatcher(dispatcher);
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

        propertySource.setValue(requireNonNull(value));
    }

    @Override
    public M get() {
        threadChecker.checkThread();
        return propertySource.get();
    }
    
    @Override
    public void reset() {
        setValue(initialValue);
    }

    @Override
    public boolean hasObservers() {
        threadChecker.checkThread();
        return dispatcher.hasObservers();
    }

    @Override
    public Subscription observe(PropertyObserver<M> observer) {
        threadChecker.checkThread();

        PropertySubscriber<M> subscriber = new PropertySubscriber<>(observer);

        // adds and pushes the latest value to the subscriber
        dispatcher.addSubscriber(subscriber).accept(get());

        if (dispatcher.isDisposed())
            subscriber.onDisposed();

        return subscriber;
    }
}
