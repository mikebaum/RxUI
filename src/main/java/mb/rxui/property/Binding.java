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

public class Binding<M> implements PropertyObserver<M> {
    
    private final Property<M> boundProperty;
    
    public Binding(Property<M> boundProperty) {
        this.boundProperty = boundProperty;
    }

    @Override
    public void onChanged(M newValue) {
        boundProperty.setValue(newValue);
    }

    @Override
    public void onDisposed() {
        // nothing to do, we don't need to dispose the bound property.
    }

    @Override
    public boolean isBinding() {
        return true;
    }
}
