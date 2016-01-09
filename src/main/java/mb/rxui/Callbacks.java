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
package mb.rxui;

public enum Callbacks {
    ; // no instances, helper class

    public static Runnable createSafeCallback(Runnable runnable) {
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

    public static void runSafeCallback(Runnable runnable) {
        createSafeCallback(runnable).run();
    }
}
