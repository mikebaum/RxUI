package mb.rxui.property;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * A helper that should be used to run unit tests that must run on the JavaFx
 * Platform thread.
 */
public class JavaFxTestHelper
{
    private static volatile JavaFxTestHelper helper;
    private static CountDownLatch initLatch = new CountDownLatch(1);
    
    private JavaFxTestHelper() {
        new Thread(JavaFxTestHelper::initJavaFx).start();
        awaitLatch(initLatch);
    }

    public static synchronized JavaFxTestHelper instance() {
        if (helper == null) {
            helper = new JavaFxTestHelper();
        }
        return helper;
    }
    
    private static void initJavaFx() {
        Platform.setImplicitExit(false);
        Application.launch(JavaFxTestApp.class);
    }
    
    /**
     * Runs the provided runnable test on the JavaFx Platform thread. Any
     * failures will be properly notified on the JUnit test runner thread.
     * 
     * @param someTest
     *            some {@link Runnable} test that should be performed on the
     *            JavaFx Platform thread.
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

    /**
     * Runs the provided runnable on the JavaFx thread and blocks the current
     * thread until complete. This should not be used to run a test, use
     * {@link #runTest(Runnable)} for tests.
     * 
     * @param runnable
     *            some runnable to run on the JavaFx Platform thread.
     */
    public void invokeAndWait(Runnable runnable) {
        awaitFxInitialization();
        
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(runnable);
        Platform.runLater(latch::countDown);
        
        awaitLatch(latch);
    }

    private void awaitFxInitialization() {
        awaitLatch(initLatch);
    }

    private boolean awaitLatch(CountDownLatch latch) {
        try {
            return latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    
    public static class JavaFxTestApp extends Application 
    {
        @Override
        public void start(Stage paramStage) throws Exception 
        {
            initLatch.countDown();
        }
    }
}