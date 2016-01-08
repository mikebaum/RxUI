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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.Assert;

import javafx.application.Platform;

public class ThreadedTestHelper {
    
    public static final ThreadedTestHelper EDT_TEST_HELPER = new ThreadedTestHelper(SwingUtilities::invokeLater);
    
    private final Executor testExecutor;
    
    public ThreadedTestHelper(Executor testExecutor) {
        this.testExecutor = testExecutor;
    }
    
    public void invokeAndWait(Runnable runnable) {
        CountDownLatch latch = new CountDownLatch(1);
        
        testExecutor.execute(runnable);
        testExecutor.execute(latch::countDown);
        
        awaitLatch(latch);
    }

    public static boolean awaitLatch(CountDownLatch latch) {
        try {
            return latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Runs the provided runnable test on this test helpers thread. Any
     * failures will be properly notified on the JUnit test runner thread.
     * 
     * @param someTest
     *            some {@link Runnable} test that should be performed on
     *            this test helpers thread.
     */
    public void runTest(Runnable someTest) {
        AtomicReference<Throwable> error = runTestAndCaptureExceptionIfAny(someTest);
        
        if (error.get() != null)
            Assert.fail(error.get().getMessage());
    }

    private AtomicReference<Throwable> runTestAndCaptureExceptionIfAny(Runnable someTest) {
        AtomicReference<Throwable> error = new AtomicReference<>();
        
        Runnable errorCapturingRunnable = () -> {
            try {
                someTest.run();
            } catch (Throwable throwable) {
                error.set(throwable);
            }
        };
        
        invokeAndWait(errorCapturingRunnable);
        
        return error;
    }

    /**
     * Runs the provided test on this test helpers thread and re-throws any
     * exceptions that were thrown.
     * 
     * @param someTestThatShouldThrow
     *            some test to execute on this test helpers thread.
     * @throws Throwable
     *             some throwable if the test fails exceptionally.
     */
    public void runTestReThrowException(Runnable someTestThatShouldThrow) throws Throwable {
        AtomicReference<Throwable> error = runTestAndCaptureExceptionIfAny(someTestThatShouldThrow);
        
        if (error.get() != null) {
            if ( error.get() instanceof AssertionError) {
                throw error.get().getCause();
            } else {
                throw error.get();
            }
        }
    }
}