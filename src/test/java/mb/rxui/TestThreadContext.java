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

public class TestThreadContext {
    
    @Test(expected=IllegalStateException.class)
    public void testEDTThreadContext() throws Exception {
        ThreadContext.SWING_THREAD_CONTEXT.checkThread();
    }
    
    @Test(expected=IllegalStateException.class)
    public void testPlatfromThreadContext() throws Exception {
        ThreadContext.JAVAFX_THREAD_CONTEXT.checkThread();
    }
    
    @Test
    public void testCreateEDTThreadContext() throws Exception {
        AtomicReference<ThreadContext> threadContext = new AtomicReference<>();
        
        EDT_TEST_HELPER.runTest(() -> {
            threadContext.set(ThreadContext.create());
        });
        
        EDT_TEST_HELPER.runTest(() -> {
            threadContext.get().checkThread();
        }); 
    }
    
    @Test(expected=IllegalStateException.class)
    public void testCreateEDTThreadContextThrows() throws Exception {
        AtomicReference<ThreadContext> threadContext = new AtomicReference<>();
        
        EDT_TEST_HELPER.runTest(() -> {
            threadContext.set(ThreadContext.create());
        });
        
        threadContext.get().checkThread();
    }
    
    @Test
    public void testCreatePlatformThreadContext() throws Exception {
        AtomicReference<ThreadContext> threadContext = new AtomicReference<>();
        
        JavaFxTestHelper.instance().runTest(() -> {
            threadContext.set(ThreadContext.create());
        });
        
        JavaFxTestHelper.instance().runTest(() -> {
            threadContext.get().checkThread();
        }); 
    }
    
    @Test(expected=IllegalStateException.class)
    public void testCreatePlatformThreadContextThrows() throws Exception {
        AtomicReference<ThreadContext> threadContext = new AtomicReference<>();
        
        JavaFxTestHelper.instance().runTest(() -> {
            threadContext.set(ThreadContext.create());
        });
        
        threadContext.get().checkThread();
    }
    
    @Test(expected=IllegalStateException.class)
    public void testCreateOnUnknownThread() throws Exception {
        ThreadContext.create();
    }
    
    @Test(expected=IllegalStateException.class)
    public void testArbitraryThreadContextThrows() throws Throwable {
        ThreadContext threadContext = ThreadedTestHelper.createOnEDT(ThreadContext::create);
        
        JavaFxTestHelper.instance().runTestReThrowException(threadContext::checkThread);
    }
}
