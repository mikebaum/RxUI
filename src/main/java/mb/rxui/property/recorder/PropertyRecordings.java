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
package mb.rxui.property.recorder;

import java.util.Optional;
import java.util.WeakHashMap;

import mb.rxui.property.Property;

public class PropertyRecordings {

    private static final PropertyRecordings recorder = new PropertyRecordings();
    
    private WeakHashMap<Property<?>, PropertyRecorderModel<?>> recordings = new WeakHashMap<>();
    
    private PropertyRecordings() {}
    
    public static PropertyRecordings getInstance() {
        return recorder;
    }
    
    public <M> PropertyRecorderModel<M> createRecorder(Property<M> property) {
        PropertyRecorderModel<M> propertyRecording = new PropertyRecorderModel<>(property);
        recordings.put(property, propertyRecording);
        return propertyRecording;
    }
    
    public <M> Optional<PropertyRecorderModel<M>> getRecording(Property<M> property) {
        @SuppressWarnings("unchecked")
        PropertyRecorderModel<M> propertyRecording = (PropertyRecorderModel<M>) recordings.get(property);
        
        return Optional.ofNullable(propertyRecording);
    }
}
