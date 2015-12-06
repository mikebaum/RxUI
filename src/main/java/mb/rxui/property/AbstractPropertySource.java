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
import java.util.function.Consumer;
import java.util.function.Supplier;

import mb.rxui.annotations.RequiresTest;
import mb.rxui.disposables.Disposable;

/**
 * An abstract property source, implementations will need to implement
 * {@link #dispatchValue(Object)} and
 * {@link #bindDispatcher(PropertyDispatcher)}.<br>
 * <br>
 * TODO: consider bundling the two abstract methods below as a separate
 * interface and collapse the hierarchy, composition over inheritence and all
 * that :).
 * 
 * @param <M>
 *            the type of the property this source provides.
 */
@RequiresTest
public abstract class AbstractPropertySource<M> implements PropertySource<M> {

    private final Supplier<M> getter;
    private final Consumer<M> setter;

    private Optional<Disposable> disposable = Optional.empty();

    public AbstractPropertySource(Supplier<M> getter, Consumer<M> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void dispose() {
        disposable.ifPresent(Disposable::dispose);
        disposable = Optional.empty();
    }

    @Override
    public void setDispatcher(PropertyDispatcher<M> dispatcher) {
        dispose();
        dispatcher.dispatchValue(get());
        disposable = Optional.of(bindDispatcher(dispatcher));
    }

    @Override
    public void setValue(M newValue) {
        if (get().equals(newValue))
            return;

        setter.accept(newValue);
        dispatchValue(newValue); // TODO: remove this, let the actual setter decide how to dispatch
    }

    @Override
    public M get() {
        return getter.get();
    }

    /**
     * Dispatches the new value using the bound dispatcher. Does nothing if a
     * dispatcher has not been bound yet.
     * 
     * @param newValue
     *            the value to dispatch.
     * <br>
     * TODO: remove this abstract method since the actual implementation should 
     * handle this internally
     */
    protected abstract void dispatchValue(M newValue);

    /**
     * Connects the dispatcher to the actual property source.
     * 
     * @param dispatcher
     *            the dispatcher that the property source should use to dispatch
     *            updates
     * @return A {@link Disposable} that can be used to detach the dispatcher
     *         from the property source
     */
    protected abstract Disposable bindDispatcher(PropertyDispatcher<M> dispatcher);
}