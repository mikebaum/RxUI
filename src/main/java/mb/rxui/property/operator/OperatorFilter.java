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

import java.util.Optional;
import java.util.function.Predicate;

import mb.rxui.Subscription;
import mb.rxui.property.PropertyObserver;
import mb.rxui.property.PropertySubscriber;
import mb.rxui.property.publisher.PropertyPublisher;

/**
 * Operator that filters the values emitted by the source property observable
 * such that only those values that satisfy the predicate are emitted.<br>
 * <br>
 * NOTES:<br>
 * 1) This operator differs from {@link OperatorFilterToOptional} since it will
 * only emit values that satisfy the predicate.<br>
 * 2) The get method of the new property observable this operator creates will
 * always return the current value of the source property observable.
 * 
 * @param <M>
 *            the type of values this operator filters
 */
public class OperatorFilter<M> implements PropertyOperator<M, M> {

    private final Predicate<M> predicate;
    
    public OperatorFilter(Predicate<M> predicate) {
        this.predicate = predicate;
    }

    @Override
    public PropertyPublisher<M> apply(PropertyPublisher<M> source) {
        return new PropertyPublisher<M>() {
            
            private Optional<M> lastValue = Optional.empty();
            
            @Override
            public M get() {
                return source.get();
            }

            @Override
            public Subscription subscribe(PropertyObserver<M> observer) {
                PropertySubscriber<M> subscriber = new PropertySubscriber<>(observer);
                
                Subscription sourceSubscriber = 
                        source.subscribe(PropertyObserver.<M>create(value -> fireOnChangedIfNecessary(subscriber),
                                                                    subscriber::onDisposed));
                
                subscriber.doOnDispose(sourceSubscriber::dispose);
                
                return subscriber;
            }
            
            private void fireOnChangedIfNecessary(PropertySubscriber<M> subscriber) {
                if (!predicate.test(get()))
                    return;
                    
                if (lastValue.isPresent() && get().equals(lastValue.get()))
                    return;
                
                lastValue = Optional.of(get());
                subscriber.onChanged(lastValue.get());
            }
        };
    }
}
