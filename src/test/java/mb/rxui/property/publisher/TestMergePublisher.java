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
package mb.rxui.property.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import mb.rxui.property.Property;
import mb.rxui.property.PropertyObservable;
import mb.rxui.property.PropertyObserver;
import mb.rxui.property.Subscription;

public class TestMergePublisher {
    @Test
    public void testMergePropertyObservables() throws Exception {
        Property<String> property1 = Property.create("tacos");
        Property<String> property2 = Property.create("burritos");
        Property<String> property3 = Property.create("fajitas");
        
        PropertyObservable<String> mergedProperty = PropertyObservable.merge(property1, property2, property3);
        
        PropertyObserver<String> observer = mock(PropertyObserver.class);
        InOrder inOrder = Mockito.inOrder(observer);
        
        // assert the dispatched values when subscribing
        mergedProperty.observe(observer);
        inOrder.verify(observer).onChanged("tacos");
        inOrder.verify(observer).onChanged("burritos");
        inOrder.verify(observer).onChanged("fajitas");
        inOrder.verifyNoMoreInteractions();
        
        // assert that the merged property reflects the last value changed
        property1.setValue("burritos");
        assertEquals("burritos", mergedProperty.get());
        inOrder.verify(observer).onChanged("burritos");
        inOrder.verifyNoMoreInteractions();
        
        property2.setValue("fajitas");
        assertEquals("fajitas", mergedProperty.get());
        inOrder.verify(observer).onChanged("fajitas");
        inOrder.verifyNoMoreInteractions();
        
        property3.setValue("tacos");
        assertEquals("tacos", mergedProperty.get());
        inOrder.verify(observer).onChanged("tacos");
        inOrder.verifyNoMoreInteractions();
        
        // assert changing the value of one of the properties to the current value of the merged property does not dispatch.
        property1.setValue("tacos");
        property2.setValue("tacos");
        assertEquals("tacos", mergedProperty.get());
        inOrder.verifyNoMoreInteractions();
    }
    
    @Test
    public void testDisposeMergedProperties() throws Exception {
        Property<String> property1 = Property.create("tacos");
        Property<String> property2 = Property.create("burritos");
        Property<String> property3 = Property.create("fajitas");
        
        PropertyObservable<String> mergedProperty = PropertyObservable.merge(property1, property2, property3);
        
        PropertyObserver<String> observer = mock(PropertyObserver.class);
        InOrder inOrder = Mockito.inOrder(observer);
        
        // assert the dispatched values when subscribing
        Subscription subscription = mergedProperty.observe(observer);
        inOrder.verify(observer).onChanged("tacos");
        inOrder.verify(observer).onChanged("burritos");
        inOrder.verify(observer).onChanged("fajitas");
        inOrder.verifyNoMoreInteractions();
        
        property1.dispose();
        inOrder.verifyNoMoreInteractions();
        
        property2.dispose();
        inOrder.verifyNoMoreInteractions();
        
        // disposing the last property should dispose the merged property
        assertFalse(subscription.isDisposed());
        property3.dispose();
        inOrder.verify(observer).onDisposed();
        assertTrue(subscription.isDisposed());
    }
    
    @Test
    public void testUnsubscribeDisposesAllMergeSubscriptions() throws Exception {
        Property<String> property1 = Property.create("tacos");
        Property<String> property2 = Property.create("burritos");
        Property<String> property3 = Property.create("fajitas");
        
        PropertyObservable<String> mergedProperty = PropertyObservable.merge(property1, property2, property3);
        
        PropertyObserver<String> observer = mock(PropertyObserver.class);
        
        assertFalse(property1.hasObservers());
        assertFalse(property2.hasObservers());
        assertFalse(property3.hasObservers());
        
        Subscription subscription = mergedProperty.observe(observer);
        
        assertTrue(property1.hasObservers());
        assertTrue(property2.hasObservers());
        assertTrue(property3.hasObservers());
        
        // dispose the subscription and assert that the properties no longer have any observers
        subscription.dispose();
        assertFalse(property1.hasObservers());
        assertFalse(property2.hasObservers());
        assertFalse(property3.hasObservers());
    }
    
    @Test
    public void testMergeWith() throws Exception {
        Property<Integer> property1 = Property.create(10);
        Property<Integer> property2 = Property.create(20);
        
        PropertyObservable<Integer> mergedProperties = property1.mergeWith(property2);
        Assert.assertEquals(new Integer(10), mergedProperties.get());
        
        Consumer<Integer> onChanged = Mockito.mock(Consumer.class);
        Runnable onDisposed = Mockito.mock(Runnable.class);
        InOrder inOrder = Mockito.inOrder(onChanged, onDisposed);

        Subscription subscription = mergedProperties.observe(PropertyObserver.create(onChanged, onDisposed));
        
        inOrder.verify(onChanged).accept(10);
        inOrder.verify(onChanged).accept(20);
        inOrder.verifyNoMoreInteractions();
        
        property1.setValue(30);
        Assert.assertEquals(new Integer(30), mergedProperties.get());
        inOrder.verify(onChanged).accept(30);
        inOrder.verifyNoMoreInteractions();
        
        property2.setValue(40);
        Assert.assertEquals(new Integer(40), mergedProperties.get());
        inOrder.verify(onChanged).accept(40);
        inOrder.verifyNoMoreInteractions();
        
        // this should not trigger an onChangedEvent.
        property1.setValue(40);
        inOrder.verifyNoMoreInteractions();
        
        property2.dispose();
        assertFalse(subscription.isDisposed());
        
        property2.setValue(50);
        Assert.assertEquals(new Integer(40), mergedProperties.get());
        inOrder.verifyNoMoreInteractions();
        
        property1.dispose();
        assertTrue(subscription.isDisposed());
    }
}
