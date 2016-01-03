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
package mb.rxui.property.operator;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import mb.rxui.property.PropertyObserver;
import mb.rxui.property.PropertySubscriber;
import mb.rxui.property.publisher.PropertyPublisher;

public class OperatorMap<S, R> implements PropertyOperator<S, R>{

    private final Function<S, R> mapper;
    
    public OperatorMap(Function<S, R> mapper) {
        this.mapper = mapper;
    }
    
    @Override
    public PropertyPublisher<R> apply(PropertyPublisher<S> source) {
        
        return new PropertyPublisher<R>() {
            
            private R lastValue = get();
            
            @Override
            public R get() {
                return mapper.apply(source.get());
            }

            @Override
            public PropertySubscriber<R> subscribe(PropertyObserver<R> observer) {
                
                PropertySubscriber<R> subscriber = new PropertySubscriber<>(observer);
                
                AtomicBoolean hasEmittedFirstValue = new AtomicBoolean(false);
                
                PropertySubscriber<S> sourceSubscriber = 
                        source.subscribe(PropertyObserver.create(newValue -> fireOnChangedIfNecessary(subscriber, hasEmittedFirstValue),
                                                                 subscriber::onDisposed));
                
                subscriber.doOnDispose(sourceSubscriber::dispose);
                
                return subscriber;
            }

            private void fireOnChangedIfNecessary(PropertySubscriber<R> subscriber, AtomicBoolean hasEmitted) {
                if(get().equals(lastValue) && !hasEmitted.compareAndSet(false, true))
                    return;
                
                lastValue = get(); 
                subscriber.onChanged(get());
            }
        };
    }
}
