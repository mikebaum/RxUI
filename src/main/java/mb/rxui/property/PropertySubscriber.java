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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;

import mb.rxui.annotations.RequiresTest;
import rx.Subscription;

/**
 * A subscriber of Property events. A property can emit one or many
 * {@link #onChanged(Object)} events followed by one {@link #onDisposed()}
 * event.<br>
 * <br>
 * NOTE: If the underlying observer throws an exception while handling the a
 * callback, the exception will not propagate. For now it is simply printed.
 * Perhaps in the future a global error handler should be added. Alternatively a
 * property could be built with a specific exception handler.Í
 * 
 * @param <M>
 *            the type of value the property manages.
 */
@RequiresTest
public class PropertySubscriber<M> implements PropertyObserver<M>, Subscription {
    private final PropertyObserver<M> observer;
    private final List<Runnable> onUnsubscribedActions;
    private boolean isUnsubscribed = false;

    public PropertySubscriber(PropertyObserver<M> observer) {
        this.observer = requireNonNull(observer);
        onUnsubscribedActions = new ArrayList<>();
    }

    @Override
    public void unsubscribe() {
        isUnsubscribed = true;
        onUnsubscribedActions.stream().map(PropertySubscriber::createSafeCallback).forEach(Runnable::run);
        onUnsubscribedActions.clear();
    }

    @Override
    public boolean isUnsubscribed() {
        return isUnsubscribed;
    }

    /**
     * Adds some action to perform when this property subscription is
     * unsubscribed.
     * 
     * @param onUnsubscribedAction
     *            some action to run when this property subscription is
     *            unsubscribed.
     */
    public void doOnUnsubscribe(Runnable onUnsubscribedAction) {
        onUnsubscribedActions.add(onUnsubscribedAction);
    }

    @Override
    public void onChanged(M newValue) {
        runSafeCallback(() -> observer.onChanged(newValue));
    }

    @Override
    public void onDisposed() {
        runSafeCallback(observer::onDisposed);
        unsubscribe();
    }

    private static void runSafeCallback(Runnable runnable) {
        createSafeCallback(runnable).run();
    }

    private static Runnable createSafeCallback(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                // TODO: clearly not the right solution, perhaps we need to have
                // the exception relayed to some contextual handler. Some
                // component that is
                // capable of displaying an error dialog if need be.
                System.err.println("An exception was caught during a callback");
                throwable.printStackTrace();
            }
        };
    }
}
