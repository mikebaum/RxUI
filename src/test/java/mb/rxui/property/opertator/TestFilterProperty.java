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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;

import mb.rxui.Subscription;
import mb.rxui.event.EventStream;
import mb.rxui.event.EventStreamObserver;
import mb.rxui.property.Property;

public class TestFilterProperty {
    @Test
    public void testFilter() throws Exception {
        Property<String> property = Property.create("tacos");
        
        assertFalse(property.hasObservers());
        
        EventStream<String> stream = property.filter(value -> value.equals("tacos"));
        
        EventStreamObserver<String> observer = Mockito.mock(EventStreamObserver.class);
        Subscription subscription = stream.observe(observer);
        
        assertTrue(property.hasObservers());
        
        Mockito.verify(observer).onEvent("tacos");
        
        property.setValue("burritos");
        Mockito.verifyNoMoreInteractions(observer);
        
        property.setValue("tacos");
        Mockito.verify(observer, Mockito.times(2)).onEvent("tacos");
        Mockito.verifyNoMoreInteractions(observer);
        
        subscription.dispose();
        
        assertFalse(property.hasObservers());
    }
    
    @Test
    public void testDisposeProperty() throws Exception {
        Property<String> property = Property.create("tacos");
        
        assertFalse(property.hasObservers());
        
        EventStream<String> stream = property.filter(value -> value.equals("tacos"));
        
        EventStreamObserver<String> observer = Mockito.mock(EventStreamObserver.class);
        Subscription subscription = stream.observe(observer);
        
        assertTrue(property.hasObservers());
        
        Mockito.verify(observer).onEvent("tacos");
        
        property.dispose();
        
        assertTrue(subscription.isDisposed());
    }
    
    @Test
    public void testSubscribeAfterDispose() throws Exception {
        Property<String> property = Property.create("tacos");
        
        assertFalse(property.hasObservers());
        
        EventStream<String> stream = property.filter(value -> value.equals("tacos"));
        
        property.dispose();
        
        EventStreamObserver<String> observer = Mockito.mock(EventStreamObserver.class);
        Subscription subscription = stream.observe(observer);
        
        assertFalse(property.hasObservers());
        
        Mockito.verify(observer).onEvent("tacos");
        Mockito.verify(observer).onCompleted();
        assertTrue(subscription.isDisposed());
    }
}
