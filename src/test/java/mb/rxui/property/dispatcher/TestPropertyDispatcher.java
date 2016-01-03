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
package mb.rxui.property.dispatcher;

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import mb.rxui.disposables.Disposable;
import mb.rxui.property.Binding;
import mb.rxui.property.Property;
import mb.rxui.property.PropertyObserver;

public class TestPropertyDispatcher {
    
    private Dispatcher<String> dispatcher;
    private Consumer<String> onChanged;
    private Runnable onDisposed;

    @Before
    public void setup() {
        dispatcher = Dispatcher.createPropertyDispatcher();
        
        onChanged = Mockito.mock(Consumer.class);
        onDisposed = Mockito.mock(Runnable.class);
        
        dispatcher.subscribe(PropertyObserver.create(onChanged, onDisposed));
        dispatcher.subscribe(PropertyObserver.create(value -> assertTrue(dispatcher.isDispatching())));
        assertEquals(2, dispatcher.getSubscriberCount());
    }
    
    @Test
    public void testDispatch() {
        assertFalse(dispatcher.isDispatching());
        dispatcher.dispatchValue("tacos");
        verify(onChanged).accept("tacos");
        
        dispatcher.dispatchValue("tacos");
        verify(onChanged, times(2)).accept("tacos");
        
        dispatcher.dispatchValue("burritos");
        verify(onChanged).accept("burritos");
    }
    
    @Test
    public void testDispose() throws Exception {
        assertFalse(dispatcher.isDisposed());
        
        dispatcher.dispose();
        assertTrue(dispatcher.isDisposed());
        Mockito.verify(onDisposed).run();
        Mockito.verifyNoMoreInteractions(onDisposed, onChanged);
        
        dispatcher.dispose();
        assertEquals(0, dispatcher.getSubscriberCount());
        Mockito.verifyNoMoreInteractions(onDisposed, onChanged);
    }
    
    @Test
    public void testOnDisposed() throws Exception {
        Disposable disposable = Mockito.mock(Disposable.class);
        dispatcher.onDisposed(disposable);
        
        Mockito.verifyNoMoreInteractions(disposable);
        
        dispatcher.dispose();
        Mockito.verify(disposable).dispose();
    }
    
    @Test
    public void testOnDisposedAfterDisposed() throws Exception {
        dispatcher.dispose();

        Disposable disposable = Mockito.mock(Disposable.class);
        dispatcher.onDisposed(disposable);
        Mockito.verify(disposable).dispose();
        Mockito.verifyNoMoreInteractions(disposable);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testDispatchAfterDisposedThrows() throws Exception {
        dispatcher.dispose();
        dispatcher.dispatchValue("tacos");
    }
    
    @Test
    public void testOnChangedThrowsDoesNotAffectIsDispatching() throws Exception {
        dispatcher.subscribe(PropertyObserver.create(value -> { throw new RuntimeException(); }));
        
        dispatcher.dispatchValue("tacos");
        assertFalse(dispatcher.isDispatching());
    }
    
    @Test
    public void testBindingsDispatchedFirst() throws Exception {
        Binding<String> binding = Mockito.spy(new Binding<>(Property.create("burritos")));
        dispatcher.subscribe(binding);
        
        PropertyObserver<String> observer = Mockito.mock(PropertyObserver.class);
        
        dispatcher.subscribe(observer);
        
        InOrder inOrder = Mockito.inOrder(onChanged, onDisposed, binding, observer);
        
        dispatcher.dispatchValue("fajitas");
        
        inOrder.verify(binding).onChanged("fajitas");
        inOrder.verify(onChanged).accept("fajitas");
        inOrder.verify(observer).onChanged("fajitas");
        inOrder.verifyNoMoreInteractions();
        
        dispatcher.dispose();
        
        inOrder.verify(binding).onDisposed();
        inOrder.verify(onDisposed).run();
        inOrder.verify(observer).onDisposed();
        inOrder.verifyNoMoreInteractions();
    }
    
    @Test
    public void testOrderOfBindingsSameAsAdded() throws Exception {
        Binding<String> binding1 = Mockito.spy(new Binding<>(Property.create("burritos")));
        dispatcher.subscribe(binding1);
        
        PropertyObserver<String> observer = Mockito.mock(PropertyObserver.class);
        dispatcher.subscribe(observer);
        
        Binding<String> binding2 = Mockito.spy(new Binding<>(Property.create("burritos")));
        dispatcher.subscribe(binding2);
        
        InOrder inOrder = Mockito.inOrder(onChanged, observer, binding1, binding2);
        
        dispatcher.dispatchValue("tacos");
        
        inOrder.verify(binding1).onChanged("tacos");
        inOrder.verify(binding2).onChanged("tacos");
        inOrder.verify(onChanged).accept("tacos");
        inOrder.verify(observer).onChanged("tacos");
        inOrder.verifyNoMoreInteractions();
    }
}
