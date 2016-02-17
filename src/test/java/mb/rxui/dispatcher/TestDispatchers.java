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
package mb.rxui.dispatcher;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import mb.rxui.SwingTestRunner;
import mb.rxui.dispatcher.Dispatchers.EventDispatcherFactory;
import mb.rxui.dispatcher.Dispatchers.PropertyDispatcherFactory;

@RunWith(SwingTestRunner.class)
public class TestDispatchers {
    
    @Before
    public void setup() {
        PropertyDispatcherFactory propertyDispatcherFactory = new PropertyDispatcherFactory() {
            @Override
            public <M> PropertyDispatcher<M> create() {
                return Mockito.spy(PropertyDispatcher.create());
            }
        };
        
        EventDispatcherFactory eventDispatcherFactory = new EventDispatcherFactory() {
            @Override
            public <M> EventDispatcher<M> create() {
                return Mockito.spy(EventDispatcher.create());
            }
        };
        
        Dispatchers.getInstance().setPropertyDispatcherFactory(propertyDispatcherFactory);
        Dispatchers.getInstance().setEventDispatcherFactory(eventDispatcherFactory);
    }
    
    @Test
    public void testCaptureDispatchState() {
        PropertyDispatcher<String> propertyDispatcher1 = Dispatcher.createPropertyDispatcher();
        PropertyDispatcher<String> propertyDispatcher2 = Dispatcher.createPropertyDispatcher();
        PropertyDispatcher<String> propertyDispatcher3 = Dispatcher.createPropertyDispatcher();
        PropertyDispatcher<String> propertyDispatcher4 = Dispatcher.createPropertyDispatcher();
        
        Runnable runnable = Mockito.mock(Runnable.class);
        
        propertyDispatcher1.setDispatching(true);
        propertyDispatcher3.setDispatching(true);
        
        Mockito.verify(propertyDispatcher1).setDispatching(true);
        Mockito.verify(propertyDispatcher3).setDispatching(true);
        
        Runnable wrappedRunnable = Dispatchers.getInstance().wrapRunnableWithCurrentDispatchState(runnable);
        
        propertyDispatcher1.setDispatching(false);
        propertyDispatcher3.setDispatching(false);
        
        wrappedRunnable.run();

        Mockito.verify(runnable).run();
        Mockito.verify(propertyDispatcher1, Mockito.times(2)).setDispatching(true);
        Mockito.verify(propertyDispatcher3, Mockito.times(2)).setDispatching(true);
        Mockito.verify(propertyDispatcher1, Mockito.times(2)).setDispatching(false);
        Mockito.verify(propertyDispatcher3, Mockito.times(2)).setDispatching(false);
        
        Assert.assertFalse(propertyDispatcher1.isDispatching());
        Assert.assertFalse(propertyDispatcher2.isDispatching());
        Assert.assertFalse(propertyDispatcher3.isDispatching());
        Assert.assertFalse(propertyDispatcher4.isDispatching());
    }
}
