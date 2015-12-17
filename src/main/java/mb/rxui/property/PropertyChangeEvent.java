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

import java.util.Optional;

import mb.rxui.annotations.VisibleForTesting;

/**
 * A property change event captures the change of a property value.
 * 
 * @param <M>
 *            the type of the value the change event tracks
 */
public class PropertyChangeEvent<M> {
    private final M oldValue;
    private final M newValue;
    private final long eventSequence;

    @VisibleForTesting
    PropertyChangeEvent(M oldValue, M newValue, long eventSequence) {
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.eventSequence = eventSequence;
    }
    
    public PropertyChangeEvent(M oldValue, M newValue) {
        this(oldValue, newValue, EventSequenceGenerator.getInstance().next());
    }
    
    /**
     * NOTE: this value can be null
     * @return the value before the change.
     */
    public M getOldValue() {
        return oldValue;
    }
    
    /**
     * @return the new value that resulted from the change.
     */
    public M getNewValue() {
        return newValue;
    }
    
    /**
     * @return a globally consistent sequence number. This can be used to order
     *         all property change events, regardless of from which property
     *         they originate from.
     */
    public long getEventSequence() {
        return eventSequence;
    }
    
    public static <M> Optional<PropertyChangeEvent<M>> next(Optional<PropertyChangeEvent<M>> last, M newValue) {
        if (!last.isPresent())
            return Optional.of(new PropertyChangeEvent<M>(null, newValue));
        
        return last.get().next(newValue);
    }
    
    public Optional<PropertyChangeEvent<M>> next(M newValue) {
        if (newValue.equals(this.newValue))
            Optional.empty();
        
        return Optional.of(new PropertyChangeEvent<M>(this.newValue, newValue));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
        result = prime * result + ((oldValue == null) ? 0 : oldValue.hashCode());
        result = prime * result + (int) (eventSequence ^ (eventSequence >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PropertyChangeEvent<?> other = (PropertyChangeEvent<?>) obj;
        if (newValue == null) {
            if (other.newValue != null)
                return false;
        } else if (!newValue.equals(other.newValue))
            return false;
        if (oldValue == null) {
            if (other.oldValue != null)
                return false;
        } else if (!oldValue.equals(other.oldValue))
            return false;
        if (eventSequence != other.eventSequence)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PropertyChangeEvent [oldValue=" + oldValue + ", newValue=" + newValue + ", eventSequence=" + eventSequence + "]";
    }
}
