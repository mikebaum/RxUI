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

import rx.Subscription;

/**
 * A property that can only be observed. This is effectively a read-only version
 * of a {@link Property}.<br>
 * <br>
 * NOTES:<br>
 * 1) A property observable will emit a new value via
 * {@link #onChanged(Consumer)} when it's value changes.<br>
 * 2) Once the property is disposed it will emit an onDiposed event.<br>
 * 3) A property observable is assumed to always contain a value.<br>
 * 
 * @param <M>
 *            the type of the value this property observable emits.
 * @see Property
 */
public interface PropertyObservable<M> extends Supplier<M> {
    /**
     * Gets the current value of this property.
     * 
     * NOTE: Calling get will always return a non-null value. If not the
     * implementation is misbehaved.
     * 
     * @return the current value
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    M get();

    /**
     * Observe this property.
     * 
     * @param observer
     *            some observer to observe this property
     * @return a {@link Subscription} that can be used to cancel the
     *         subscription.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    Subscription observe(PropertyObserver<M> observer);

    /**
     * Observe onChange and onDestroy events.
     * 
     * @param onChanged
     *            some listener of onChanged events.
     * @param onDisposed
     *            some listener of onDisposed events.
     * @return a {@link Subscription} that can be used to cancel the
     *         subscription.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    Subscription observe(Consumer<M> onChanged, Runnable onDisposed);

    /**
     * Adds a listener that will be updated when the value of this property
     * changes.
     * 
     * NOTE: The listener will be called back immediately with the current value
     * when subscribing.
     * 
     * @param onChanged
     *            some listener to update when this property's value changes
     * @return a {@link Subscription} that can be used to cancel the
     *         subscription.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    Subscription onChanged(Consumer<M> onChanged);

    /**
     * Adds some {@link Runnable} to execute when this property is disposed.
     * 
     * @param onDisposedAction
     *            some runnable to run when this property is disposed.
     * @return a {@link Subscription} that can be used to cancel the
     *         subscription.
     * @throws IllegalStateException
     *             if called from a thread other than the one that this property
     *             was created from.
     */
    Subscription onDisposed(Runnable onDisposedAction);
}