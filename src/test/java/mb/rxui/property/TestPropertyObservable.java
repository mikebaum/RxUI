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

import org.junit.Test;

public class TestPropertyObservable {
    
    @Test
    public void testUnsubscribe() throws Exception {
        Property<String> property = Property.create("tacos");
        assertFalse(property.hasObservers());
        
        PropertyObservable<Integer> observable = property.map(String::length);
        assertFalse(property.hasObservers());
        
        Subscription subscription = observable.onChanged(val -> {});
        assertTrue(property.hasObservers());
        
        subscription.dispose();
        assertFalse(property.hasObservers());
    }
    
    @Test
    public void testEquals() throws Exception {
        Property<String> stringProperty = Property.create("tacos");
        Property<String> stringProperty2 = Property.create("burritos");
        Property<Integer> integerProperty = Property.create(10);
        
        assertFalse(stringProperty.equals(stringProperty2));
        assertFalse(stringProperty.equals(integerProperty));
        assertFalse(stringProperty.equals(new Double(20)));
        
        stringProperty2.setValue("tacos");
        assertTrue(stringProperty.equals(stringProperty2));
    }
}
