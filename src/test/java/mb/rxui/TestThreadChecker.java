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

import static mb.rxui.ThreadedTestHelper.EDT_TEST_HELPER;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import mb.rxui.property.javafx.JavaFxTestHelper;

public class TestThreadChecker {
    
    @Test(expected=IllegalStateException.class)
    public void testEDTThreadChecker() throws Exception {
        ThreadChecker.EDT_THREAD_CHECKER.checkThread();
    }
    
    @Test(expected=IllegalStateException.class)
    public void testPlatfromThreadChecker() throws Exception {
        ThreadChecker.PLATFORM_THREAD_CHECKER.checkThread();
    }
    
    @Test
    public void testCreateEDTThreadChecker() throws Exception {
        AtomicReference<ThreadChecker> threadChecker = new AtomicReference<>();
        
        EDT_TEST_HELPER.runTest(() -> {
            threadChecker.set(ThreadChecker.create());
        });
        
        EDT_TEST_HELPER.runTest(() -> {
            threadChecker.get().checkThread();
        }); 
    }
    
    @Test(expected=IllegalStateException.class)
    public void testCreateEDTThreadCheckerThrows() throws Exception {
        AtomicReference<ThreadChecker> threadChecker = new AtomicReference<>();
        
        EDT_TEST_HELPER.runTest(() -> {
            threadChecker.set(ThreadChecker.create());
        });
        
        threadChecker.get().checkThread();
    }
    
    @Test
    public void testCreatePlatformThreadChecker() throws Exception {
        AtomicReference<ThreadChecker> threadChecker = new AtomicReference<>();
        
        JavaFxTestHelper.instance().runTest(() -> {
            threadChecker.set(ThreadChecker.create());
        });
        
        JavaFxTestHelper.instance().runTest(() -> {
            threadChecker.get().checkThread();
        }); 
    }
    
    @Test(expected=IllegalStateException.class)
    public void testCreatePlatformThreadCheckerThrows() throws Exception {
        AtomicReference<ThreadChecker> threadChecker = new AtomicReference<>();
        
        JavaFxTestHelper.instance().runTest(() -> {
            threadChecker.set(ThreadChecker.create());
        });
        
        threadChecker.get().run(); // run should call checkThread.
    }
    
    @Test
    public void testArbitraryThreadChecker() throws Exception {
        ThreadChecker threadChecker = ThreadChecker.create();
        
        assertNotEquals(ThreadChecker.EDT_THREAD_CHECKER, threadChecker);
        assertNotEquals(ThreadChecker.PLATFORM_THREAD_CHECKER, threadChecker);
        
        threadChecker.checkThread();
    }
    
    @Test(expected=IllegalStateException.class)
    public void testArbitraryThreadCheckerThrows() throws Throwable {
        ThreadChecker threadChecker = ThreadChecker.create();
        
        EDT_TEST_HELPER.runTestReThrowException(threadChecker::checkThread);
    }
}
