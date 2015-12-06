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
package mb.rxui.property.swing;

import static mb.rxui.ThreadChecker.EDT_THREAD_CHECKER;

import java.util.function.Consumer;
import java.util.function.Supplier;

import mb.rxui.annotations.RequiresTest;
import mb.rxui.property.AbstractPropertySource;

/**
 * A property source for Swing components.<br>
 * <br>
 * TODO: consider adding abstract methods addListener, removeListener, createListener 
 * 
 * @param <M> the type of values this source provides
 */
@RequiresTest
public abstract class SwingPropertySource<M> extends AbstractPropertySource<M> {
    
    public SwingPropertySource(Supplier<M> getter, Consumer<M> setter) {
        super(getter, setter);
        EDT_THREAD_CHECKER.checkThread();
    }
    
    @Override
    protected final void dispatchValue(M newValue) {
        // does nothing on purpose, swing takes care of dispatching events for us.
    }
}
