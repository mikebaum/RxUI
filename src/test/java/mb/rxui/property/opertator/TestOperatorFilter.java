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
package mb.rxui.property.opertator;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mockito.Mockito;

import mb.rxui.Subscription;
import mb.rxui.property.Property;
import mb.rxui.property.PropertyObservable;
import mb.rxui.property.PropertyObserver;

public class TestOperatorFilter {
    @Test
    public void testFilter() throws Exception {
        Property<String> property = Property.create("tacos");
        
        assertFalse(property.hasObservers());
        
        PropertyObservable<String> observable = property.filter(value -> value.equals("tacos"));
        
        PropertyObserver<String> observer = Mockito.mock(PropertyObserver.class);
        Subscription subscription = observable.observe(observer);
        
        assertTrue(property.hasObservers());
        
        Mockito.verify(observer).onChanged("tacos");
        
        property.setValue("burritos");
        Mockito.verifyNoMoreInteractions(observer);
        
        property.setValue("tacos");
        Mockito.verifyNoMoreInteractions(observer);
        
        subscription.dispose();
        
        assertFalse(property.hasObservers());
    }
    
    @Test
    public void testDisposeProperty() throws Exception {
        Property<String> property = Property.create("tacos");
        
        assertFalse(property.hasObservers());
        
        PropertyObservable<String> observable = property.filter(value -> value.equals("tacos"));
        
        PropertyObserver<String> observer = Mockito.mock(PropertyObserver.class);
        Subscription subscription = observable.observe(observer);
        
        assertTrue(property.hasObservers());
        
        Mockito.verify(observer).onChanged("tacos");
        
        property.dispose();
        
        assertTrue(subscription.isDisposed());
    }
    
    @Test
    public void testSubscribeAfterDispose() throws Exception {
        Property<String> property = Property.create("tacos");
        
        assertFalse(property.hasObservers());
        
        PropertyObservable<String> observable = property.filter(value -> value.equals("tacos"));
        
        property.dispose();
        
        PropertyObserver<String> observer = Mockito.mock(PropertyObserver.class);
        Subscription subscription = observable.observe(observer);
        
        assertFalse(property.hasObservers());
        
        Mockito.verify(observer).onChanged("tacos");
        Mockito.verify(observer).onDisposed();
        assertTrue(subscription.isDisposed());
    }
}
