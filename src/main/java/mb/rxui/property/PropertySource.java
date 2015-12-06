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

import java.util.function.Consumer;
import java.util.function.Supplier;

import mb.rxui.disposables.Disposable;

/**
 * A source of property values.
 * 
 * @param <M> The type of the values this property source provides.
 */
public interface PropertySource<M> extends Consumer<M>, Supplier<M>, Disposable {
    /**
     * Sets the value on this property source.
     * @param newValue some value to set.
     */
    void setValue(M newValue);
    
    default public void accept(M newValue)
    {
        setValue(newValue);
    }
    
    M get();

    void setDispatcher(PropertyDispatcher<M> dispatcher);
}
