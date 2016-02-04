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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import mb.rxui.Subscription;
import mb.rxui.SwingTestRunner;
import mb.rxui.event.EventSequenceGenerator;
import rx.functions.Action0;
import rx.functions.Action1;

@RunWith(SwingTestRunner.class)
public class TestPropertyStream {
    
    @Before
    public void setup() {
        EventSequenceGenerator.getInstance().reset();
    }
    
    @Test
    public void testUnsubscribe() throws Exception {
        Property<String> property = Property.create("tacos");
        assertFalse(property.hasObservers());
        
        PropertyStream<Integer> stream = property.map(String::length);
        assertFalse(property.hasObservers());
        
        Subscription subscription = stream.onChanged(val -> {});
        assertTrue(property.hasObservers());
        
        subscription.dispose();
        assertFalse(property.hasObservers());
    }
    
    @Test
    public void testEquals() throws Exception {
        Property<String> stringProperty = Property.create("tacos");
        Property<String> stringProperty2 = Property.create("burritos");
        Property<Integer> integerProperty = Property.create(10);
        
        assertTrue(stringProperty.equals(stringProperty));
        assertFalse(stringProperty.equals(null));
        
        assertFalse(stringProperty.equals(stringProperty2));
        assertFalse(stringProperty.equals(integerProperty));
        assertFalse(stringProperty.equals(new Double(20)));
        
        stringProperty2.setValue("tacos");
        assertTrue(stringProperty.equals(stringProperty2));
    }
    
    @Test
    public void testAsObservable() throws Exception {
        Property<String> property = Property.create("tacos");
        
        Assert.assertFalse(property.hasObservers());
        
        Action1<String> onNext = Mockito.mock(Action1.class);
        Action1<Throwable> error = Mockito.mock(Action1.class);
        Action0 onComplete = Mockito.mock(Action0.class);
        property.asObservable().subscribe(onNext, error, onComplete);
        
        Assert.assertTrue(property.hasObservers());
        
        verify(onNext).call("tacos");
        
        property.setValue("burritos");
        verify(onNext).call("burritos");
        Mockito.verifyNoMoreInteractions(onNext, error, onComplete);
        
        property.dispose();
        verify(onComplete).call();
        Assert.assertFalse(property.hasObservers());
    }
    
    @Test
    public void testHashCode() {
        Property<String> stringProperty = Property.create("tacos");
        Property<String> stringProperty2 = Property.create("burritos");
        
        assertFalse(stringProperty.hashCode() == stringProperty2.hashCode());
        
        stringProperty2.setValue("tacos");
        assertTrue(stringProperty.hashCode() == stringProperty2.hashCode());
    }
    
    @Test
    public void testUnsubscribeObservable() throws Exception {
        Property<String> property = Property.create("tacos");
        
        Assert.assertFalse(property.hasObservers());
        
        Action1<String> onNext = Mockito.mock(Action1.class);
        Action1<Throwable> error = Mockito.mock(Action1.class);
        Action0 onComplete = Mockito.mock(Action0.class);
        rx.Subscription subscription = property.asObservable().subscribe(onNext, error, onComplete);
        
        Assert.assertTrue(property.hasObservers());
        
        verify(onNext).call("tacos");
        
        subscription.unsubscribe();
        
        Assert.assertFalse(property.hasObservers());
    }
    
    @Test
    public void testNoReentryThroughObservable() throws Exception {
        Property<String> property = Property.create("tacos");
        
        Action1<String> onNext = value -> property.setValue("changed");
        rx.Subscription subscription = property.asObservable().subscribe(onNext);
        
        assertEquals("tacos", property.get());
    }
}
