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

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import mb.rxui.event.EventSequenceGenerator;

public class TestPropertyChangeEvent {
    
    @Before
    public void setup() {
        EventSequenceGenerator.getInstance().reset();
    }
    
    @Test
    public void testCreate() {
        PropertyChangeEvent<Integer> event = new PropertyChangeEvent<>(10, 15);
        assertEquals(0, event.getEventSequence());
        assertEquals(new Integer(10), event.getOldValue());
        assertEquals(new Integer(15), event.getNewValue());
    }
    
    @Test
    public void testNext() throws Exception {
        Optional<PropertyChangeEvent<Integer>> event0 = PropertyChangeEvent.next(Optional.empty(), 15);
        assertTrue(event0.isPresent());
        assertEquals(0, event0.get().getEventSequence());
        assertEquals(null, event0.get().getOldValue());
        assertEquals(new Integer(15), event0.get().getNewValue());
        
        Optional<PropertyChangeEvent<Integer>> event1 = event0.get().next(20);
        assertTrue(event1.isPresent());
        assertEquals(1, event1.get().getEventSequence());
        assertEquals(new Integer(15), event1.get().getOldValue());
        assertEquals(new Integer(20), event1.get().getNewValue());
        
        Optional<PropertyChangeEvent<Integer>> event2 = PropertyChangeEvent.next(event1, 30);
        assertTrue(event2.isPresent());
        assertEquals(2, event2.get().getEventSequence());
        assertEquals(new Integer(20), event2.get().getOldValue());
        assertEquals(new Integer(30), event2.get().getNewValue());
        
        Optional<PropertyChangeEvent<Integer>> event3 = PropertyChangeEvent.next(event2, 30);
        assertFalse(event3.isPresent());
    }
    
    @Test
    public void testEquals() throws Exception {
        PropertyChangeEvent<Integer> event0 = new PropertyChangeEvent<>(0, 15, 0);
        assertTrue(event0.equals(event0));
        assertFalse(event0.equals(null));
        assertFalse(event0.equals(15));

        PropertyChangeEvent<Integer> event1 = new PropertyChangeEvent<>(0, 15, 0);
        assertTrue(event0.equals(event1));
        
        // events should not be equal if the sequence number differs
        PropertyChangeEvent<Integer> event2 = new PropertyChangeEvent<>(0, 15, 1);
        assertFalse(event0.equals(event2));
        
        // events should not be equal if the old value is different
        PropertyChangeEvent<Integer> event3 = new PropertyChangeEvent<>(1, 15, 0);
        assertFalse(event0.equals(event3));
        
        // events should not be equal if the new value is different
        PropertyChangeEvent<Integer> event4 = new PropertyChangeEvent<>(0, 16, 0);
        assertFalse(event0.equals(event4));
        
        // events should be equal if the old values are null
        PropertyChangeEvent<Integer> event5 = new PropertyChangeEvent<>(null, 15, 0);
        PropertyChangeEvent<Integer> event6 = new PropertyChangeEvent<>(null, 15, 0);
        assertTrue(event5.equals(event6));
        
        // events should not be equal if one of the old values is null
        PropertyChangeEvent<Integer> event7 = new PropertyChangeEvent<>(null, 15, 0);
        assertFalse(event0.equals(event7));
        assertFalse(event7.equals(event0));
    }
}
