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
package mb.rxui.subscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mb.rxui.disposables.Disposable;

public class CompositeSubscription implements Subscription {

    private final List<Subscription> subscriptions = new ArrayList<>();
    private boolean isDisposed = false;
    
    public CompositeSubscription(List<Subscription> subscriptions) {
        this.subscriptions.addAll(subscriptions);
    }
    
    public CompositeSubscription(Subscription... subscriptions) {
        this(Arrays.asList(subscriptions));
    }
    
    public CompositeSubscription() {}
    
    @Override
    public void dispose() {
        if(isDisposed)
            return;
        
        isDisposed = true;
        new ArrayList<>(subscriptions).forEach(Disposable::dispose);
        subscriptions.clear();
    }

    @Override
    public boolean isDisposed() {
        return isDisposed;
    }
    
    public void add(Subscription subscription) {
        if (isDisposed)
            subscription.dispose();
        else
            subscriptions.add(subscription);
    }
    
    public void remove(Subscription subscription) {
        subscriptions.remove(subscription);
    }
}
