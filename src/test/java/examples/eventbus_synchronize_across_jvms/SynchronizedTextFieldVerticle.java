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
package examples.eventbus_synchronize_across_jvms;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import io.vertx.core.json.Json;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.EventBus;
import mb.rxui.examples.synchronized_text_field.SwingSynchronizedTextFieldApp;
import mb.rxui.property.Property;
import rx.subjects.AsyncSubject;

/**
 * Simple Swing application to demonstrate how to use properties.
 */
public class SynchronizedTextFieldVerticle extends AbstractVerticle {
    
    private final UUID appInstanceId = UUID.randomUUID();
    private final AsyncSubject<Property<String>> textProperty = AsyncSubject.create();
    
    public SynchronizedTextFieldVerticle() {
        SwingUtilities.invokeLater(() -> {
            SwingSynchronizedTextFieldApp app = new SwingSynchronizedTextFieldApp("event-bus-sync");
            app.show();
            textProperty.onNext(app.getTextProperty());
            textProperty.onCompleted();
        });
    }
    
    @Override
    public void start() throws Exception {
        textProperty.subscribe(property -> {            
            startSync(vertx.eventBus(), property, appInstanceId);
        });
    }

    private static void startSync(EventBus eventBus, Property<String> textProperty, UUID appInstanceId) {
        AtomicBoolean isReceiving = new AtomicBoolean(false);

        SwingUtilities.invokeLater(() -> textProperty.getChangeEvents().subscribe(changeEvent -> { 
            if (isReceiving.get())
                return;

            PropertyMessage message = new PropertyMessage(changeEvent.getNewValue(), appInstanceId.toString());
            eventBus.publish("property", message.toJason());
        }));

        eventBus.consumer("property", message -> {
            PropertyMessage propertyMessage = PropertyMessage.fromJson(message.body());

            if (appInstanceId.equals(propertyMessage.appInstanceUUID))
                return;

            SwingUtilities.invokeLater(() -> {
                isReceiving.set(true);
                textProperty.setValue((String)propertyMessage.propertyValue); 
                isReceiving.set(false);
            });
        });
    }

    private static class PropertyMessage implements Serializable{
        private static final long serialVersionUID = 1L;
        private Object propertyValue;
        private String appInstanceUUID;
        
        public PropertyMessage() {}
        
        public PropertyMessage(Object propertyValue, String uuid) {
            this.propertyValue = propertyValue;
            this.appInstanceUUID = uuid;
        }
        
        public Object getPropertyValue() {
            return propertyValue;
        }
        
        public void setPropertyValue(Object propertyValue) {
            this.propertyValue = propertyValue;
        }
        
        public String getAppInstanceUUID() {
            return appInstanceUUID;
        }
        
        public void setAppInstanceUUID(String appInstanceUUID) {
            this.appInstanceUUID = appInstanceUUID;
        }
        
        public String toJason() {
            return Json.encode(this);
        }
        
        public static PropertyMessage fromJson(Object jsonString) {
            return Json.decodeValue((String)jsonString, PropertyMessage.class);
        }
    }
    
    public static void main(String[] args) {
        Runner.runClusteredExample(SynchronizedTextFieldVerticle.class);
    }
}
