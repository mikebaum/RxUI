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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rx.Subscription;

public class TestPropertyObservable {
    
    @Test
    public void testUnsubscribe() throws Exception {
        Property<String> property = Property.create("tacos");
        assertFalse(property.hasObservers());
        
        PropertyObservable<Integer> observable = property.map(String::length);
        assertFalse(property.hasObservers());
        
        Subscription subscription = observable.onChanged(val -> {});
        assertTrue(property.hasObservers());
        
        subscription.unsubscribe();
        assertFalse(property.hasObservers());
    }
}