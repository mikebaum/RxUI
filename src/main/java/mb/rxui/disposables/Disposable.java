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
package mb.rxui.disposables;

import java.util.Arrays;
import java.util.List;

import mb.rxui.annotations.RequiresTest;

/**
 * Represents a disposable resource.
 */
@FunctionalInterface
public interface Disposable {
    /**
     * Disposes the resource.
     */
    void dispose();
    
    @RequiresTest
    static Disposable create(Disposable... disposables) {
        List<Disposable> toDispose = Arrays.asList(disposables);
        
        return () -> {
            toDispose.forEach(Disposable::dispose);
            toDispose.clear();
        };
    }
}
