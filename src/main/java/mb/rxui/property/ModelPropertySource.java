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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import mb.rxui.annotations.RequiresTest;
import mb.rxui.disposables.Disposable;

/**
 * A property source for model properties.
 * 
 * @param <M>
 *            the type of value this property supplies
 */
@RequiresTest
public class ModelPropertySource<M> extends AbstractPropertySource<M> implements PropertySource<M> {
    
    private Optional<PropertyDispatcher<M>> dispatcher = Optional.empty();
    
    private ModelPropertySource(Supplier<M> getter, Consumer<M> setter) {
        super(getter, setter);
    }
    
    public static <M> PropertySource<M> create(M initialValue)
    {
        Object[] value = new Object[1];
        value[0] = initialValue;
        
        @SuppressWarnings("unchecked")
        Supplier<M> getter = () -> (M) value[0];
        Consumer<M> setter = newValue -> value[0] = newValue;
        
        return new ModelPropertySource<>(getter, setter);
    }
    
    @Override
    protected void dispatchValue(M newValue) {
        dispatcher.ifPresent(dispatcher -> dispatcher.dispatchValue(newValue));
    }
    
    @Override
    protected Disposable bindDispatcher(PropertyDispatcher<M> dispatcher) {
        this.dispatcher = Optional.of(dispatcher);
        return () -> this.dispatcher = Optional.empty();
    }
}
