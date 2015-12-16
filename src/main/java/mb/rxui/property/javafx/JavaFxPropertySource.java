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
package mb.rxui.property.javafx;

import static java.util.Objects.requireNonNull;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import mb.rxui.disposables.Disposable;
import mb.rxui.property.Dispatcher;
import mb.rxui.property.PropertyDispatcher;
import mb.rxui.property.PropertySource;

/**
 * A property source for JavaFx properties
 * 
 * @param <M>
 *            the type of data the property represents.
 */
public class JavaFxPropertySource<M> implements PropertySource<M> {
    
    private final Property<M> fxProperty;
    
    JavaFxPropertySource(Property<M> fxProperty, PropertyDispatcher<M> dispatcher) {
        this.fxProperty = requireNonNull(fxProperty);
        dispatcher.onDisposed(addPropertyListener(dispatcher, fxProperty));
    }
    
    @Override
    public void setValue(M newValue) {
        fxProperty.setValue(newValue);
    }

    @Override
    public M get() {
        return fxProperty.getValue();
    }
    
    private static <M> Disposable addPropertyListener(Dispatcher<M> dispatcher, Property<M> property) {
        ChangeListener<M> listener = (observable, oldValue, newValue) -> dispatcher.dispatchValue(newValue);
        property.addListener(listener);
        return () -> property.removeListener(listener);
    }
}
