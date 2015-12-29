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

import java.util.function.Function;

import mb.rxui.property.PropertyObserver;
import mb.rxui.property.PropertySubscriber;
import mb.rxui.property.publisher.PropertyPublisher;

public class OperatorMap<Downstream, Upstream> implements PropertyOperator<Downstream, Upstream>{

    private final Function<Downstream, Upstream> mapper;
    
    public OperatorMap(Function<Downstream, Upstream> mapper) {
        this.mapper = mapper;
    }
    
    @Override
    public PropertyPublisher<Upstream> apply(PropertyPublisher<Downstream> source) {
        
        return new PropertyPublisher<Upstream>() {
            @Override
            public Upstream get() {
                return mapper.apply(source.get());
            }

            @Override
            public PropertySubscriber<Upstream> subscribe(PropertyObserver<Upstream> observer) {
                
                PropertySubscriber<Upstream> subscriber = new PropertySubscriber<>(observer);
                
                PropertySubscriber<Downstream> sourceSubscriber = 
                        source.subscribe(new PropertyObserver<Downstream>() {
                            @Override
                            public void onChanged(Downstream newValue) {
                                subscriber.onChanged(mapper.apply(newValue));
                            }

                            @Override
                            public void onDisposed() {
                                subscriber.onDisposed();
                            }
                        });
                
                subscriber.doOnUnsubscribe(sourceSubscriber::dispose);
                
                return subscriber;
            }
        };
    }
}
