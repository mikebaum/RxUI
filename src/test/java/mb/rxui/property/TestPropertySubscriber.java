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
import static org.mockito.Mockito.never;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class TestPropertySubscriber {
    private PropertySubscriber<String> subscriber;
    private PropertyObserver<String> observer;
    
    @Before
    public void setup() {
        observer = Mockito.mock(PropertyObserver.class);
        subscriber = new PropertySubscriber<>(observer);
    }

    @Test
    public void testOnChanged() throws Exception {
        subscriber.onChanged("tacos");
        Mockito.verify(observer).onChanged("tacos");
    }
    
    @Test
    public void testOnDispose() throws Exception {
        
        Runnable onUnsubscribedAction = Mockito.mock(Runnable.class);
        InOrder inOrder = Mockito.inOrder(onUnsubscribedAction, observer);
        
        subscriber.doOnDispose(onUnsubscribedAction);
        
        assertFalse(subscriber.isDisposed());
        
        subscriber.onDisposed();
        inOrder.verify(observer).onDisposed();
        inOrder.verify(onUnsubscribedAction).run();
        inOrder.verifyNoMoreInteractions();
        
        assertTrue(subscriber.isDisposed());
    }
    
    @Test
    public void testDispose() throws Exception {
        Runnable onUnsubscribedAction = Mockito.mock(Runnable.class);
        InOrder inOrder = Mockito.inOrder(onUnsubscribedAction, observer);
        
        subscriber.doOnDispose(onUnsubscribedAction);
        
        assertFalse(subscriber.isDisposed());
        
        subscriber.dispose();
        inOrder.verify(observer, never()).onDisposed();
        inOrder.verify(onUnsubscribedAction).run();
        inOrder.verifyNoMoreInteractions();
        
        assertTrue(subscriber.isDisposed());
    }
    
    @Test
    public void testReceiveNoEventsAfterDispose() throws Exception {
        subscriber.dispose();
        Mockito.verify(observer, never()).onDisposed();
        
        subscriber.onChanged("tacos");
        Mockito.verify(observer, never()).onChanged(Mockito.anyString());
    }
    
    @Test
    public void testReceiveNoEventsAfterOnDisposed() throws Exception {
        subscriber.onDisposed();
        Mockito.verify(observer).onDisposed();
        
        subscriber.onChanged("tacos");
        Mockito.verify(observer, never()).onChanged(Mockito.anyString());
        
        subscriber.onDisposed();
        Mockito.verifyNoMoreInteractions(observer);
    }
    
    @Test
    public void testBindingSubscriber() throws Exception {
        Binding<String> binding = new Binding<>(Property.create("tacos"));
        PropertySubscriber<String> propertySubscriber = new PropertySubscriber<>(binding);
        assertTrue(propertySubscriber.isBinding());
    }
    
    @Test
    public void testNotBindingSubscriber() throws Exception {
        PropertySubscriber<String> propertySubscriber = new PropertySubscriber<>(Mockito.mock(PropertyObserver.class));
        assertFalse(propertySubscriber.isBinding());
    }
    
    @Test
    public void testOnChangedThrows() throws Exception {
        PropertyObserver<String> propertyObserver = Mockito.mock(PropertyObserver.class);
        Mockito.doThrow(new RuntimeException()).when(propertyObserver).onChanged(Mockito.anyString());
        PropertySubscriber<String> propertySubscriber = new PropertySubscriber<>(propertyObserver);
        
        try {
            propertySubscriber.onChanged("tacos");
        } catch(RuntimeException exception) {
            fail("should not have thrown an exception");
        }
    }
    
    @Test
    public void testOnDisposedThrows() throws Exception {
        PropertyObserver<String> propertyObserver = Mockito.mock(PropertyObserver.class);
        Mockito.doThrow(new RuntimeException()).when(propertyObserver).onDisposed();
        PropertySubscriber<String> propertySubscriber = new PropertySubscriber<>(propertyObserver);
        
        try {
            propertySubscriber.onDisposed();
        } catch(RuntimeException exception) {
            fail("should not have thrown an exception");
        }
    }
    
    @Test
    public void testDoOnDisposedThrows() throws Exception {
        PropertyObserver<String> propertyObserver = Mockito.mock(PropertyObserver.class);
        PropertySubscriber<String> propertySubscriber = new PropertySubscriber<>(propertyObserver);
        
        Runnable onDisposedActionThatThrows = Mockito.mock(Runnable.class);
        Mockito.doThrow(new RuntimeException()).when(onDisposedActionThatThrows).run();
        propertySubscriber.doOnDispose(onDisposedActionThatThrows);

        Runnable onDisposedAction = Mockito.mock(Runnable.class);
        propertySubscriber.doOnDispose(onDisposedAction);
        
        InOrder inOrder = Mockito.inOrder(onDisposedActionThatThrows, onDisposedAction);
        
        try {
            propertySubscriber.dispose();
        } catch(RuntimeException exception) {
            fail("should not have thrown an exception");
        }
        
        inOrder.verify(onDisposedActionThatThrows).run();
        inOrder.verify(onDisposedAction).run();
        inOrder.verifyNoMoreInteractions();
    }
}
