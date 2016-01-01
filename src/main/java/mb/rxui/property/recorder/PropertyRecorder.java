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

import javax.swing.JComponent;

import mb.rxui.disposables.Disposable;
import mb.rxui.property.CompositeSubscription;
import mb.rxui.property.Property;

public class PropertyRecorder<M> implements Disposable {
    private final PropertyRecorderView view;
    private final PropertyRecorderModel<M> model;
    private final Disposable disposer;
    
    public PropertyRecorder(Property<M> property) {
        view = new PropertyRecorderView();
        model = new PropertyRecorderModel<>(property);
        disposer = bind(model, view);
    }
    
    private static <M> Disposable bind(PropertyRecorderModel<M> model, PropertyRecorderView view) {
        
        CompositeSubscription subscriptions = new CompositeSubscription();
        
        subscriptions.add(model.getRecorderState().bind(view.recorderState()));
        subscriptions.add(model.canPlay().onChanged(view::canPlay));
        subscriptions.add(model.canRecord().onChanged(view::canRecord));
        subscriptions.add(model.canStop().onChanged(view::canStop));
        
        return subscriptions;
    }
    
    @Override
    public void dispose() {
        disposer.dispose();
    }

    public PropertyRecorderModel<M> getModel() {
        return model;
    }
    
    public JComponent getView() {
        return view.getComponent();
    }
}
