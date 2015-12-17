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

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observer;

public class TestPropertyChangeEvents {
    @Test
    public void testPropertyChangeEvents() throws Exception {
        Property<String> property = Property.create("tacos");
        Property<Integer> property2 = Property.create(10);
        
        Observer<PropertyChangeEvent<String>> changeEventsObserver = Mockito.mock(Observer.class);
        property.getChangeEvents().subscribe(changeEventsObserver);
        verify(changeEventsObserver).onNext(new PropertyChangeEvent<>(null, "tacos", 0));

        Observer<PropertyChangeEvent<Integer>> changeEventsObserver2 = Mockito.mock(Observer.class);
        property2.getChangeEvents().subscribe(changeEventsObserver2);
        verify(changeEventsObserver2).onNext(new PropertyChangeEvent<>(null, 10, 1));
        
        property.setValue("burritos");
        verify(changeEventsObserver).onNext(new PropertyChangeEvent<>("tacos", "burritos", 2));
        Mockito.verifyNoMoreInteractions(changeEventsObserver);
        
        property2.setValue(20);
        verify(changeEventsObserver2).onNext(new PropertyChangeEvent<>(10, 20, 3));
        Mockito.verifyNoMoreInteractions(changeEventsObserver2);
        
        property.dispose();
        property2.dispose();
        verify(changeEventsObserver).onCompleted();
        verify(changeEventsObserver2).onCompleted();
    }
}
