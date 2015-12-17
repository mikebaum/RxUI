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

import static mb.rxui.ThreadChecker.PLATFORM_THREAD_CHECKER;

import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import mb.rxui.property.Property;
import mb.rxui.property.PropertyObservable;

public enum JavaFxProperties {
    ;  // no instances
    
    /**
     * Creates a Property from a JavaFx Property.
     * 
     * @param property
     *            some JavaFx property to create a {@link Property} for.
     * @return a new {@link Property} that is backed by a JavaFx Property.
     * @throws IllegalStateException
     *             if this method is called from a thread other that the JavaFx
     *             Platform Thread
     */
    public static <M> Property<M> fromFxProperty(javafx.beans.property.Property<M> property) {
        PLATFORM_THREAD_CHECKER.checkThread();
        return Property.create(dispatcher -> new JavaFxPropertySource<>(property, dispatcher));
    }
    
    /**
     * Creates a property observable from an observable value.
     * 
     * @param observableValue
     *            some observable value to create a property observable for
     * @return a new {@link PropertyObservable} that is backed by the provided
     *         observable value.
     * @throws IllegalStateException
     *             if the provided {@link ObservableValue} is actually an
     *             instance of a {@link WritableValue}, or if this method is
     *             called from a thread other that the JavaFx Platform Thread
     */
    public static <M> PropertyObservable<M> fromObservableValue(ObservableValue<M> observableValue) {
        PLATFORM_THREAD_CHECKER.checkThread();
        // TODO: need to figure out a way to prevent creating a PropertyObservable from a WritableValue
//        checkArgument(! WritableValue.class.isAssignableFrom(observableValue.getClass()), 
//                     "Use [" + JavaFxPropertySource.class.getSimpleName() + "] instead, " + 
//                     "to ensure to be protected against re-entrancy." );
        return PropertyObservable.create(new ObservableValuePropertyPublisher<>(observableValue));
    }
}
