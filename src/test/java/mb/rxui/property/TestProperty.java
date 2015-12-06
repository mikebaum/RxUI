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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;

import mb.rxui.property.Property;
import mb.rxui.property.PropertyObserver;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;

public class TestProperty {
    @Test
    public void testStartWith() throws Exception {
        Property<Integer> property = Property.create(10);
        assertEquals(new Integer(10), property.get());
    }

    @Test
    public void testStartWithId() throws Exception {
        Property<Integer> property = Property.create(10);

        assertEquals(new Integer(10), property.get());
    }

    @Test(expected = NullPointerException.class)
    public void testCannotStartWithNullValue() throws Exception {
        Property.create(null);
    }

    @Test
    public void testOptionalProperty() throws Exception {
        Property<Optional<Integer>> optionalProperty = Property.createOptional();
        assertEquals(Optional.empty(), optionalProperty.get());

        optionalProperty = Property.createOptional(10);
        assertEquals(Optional.of(10), optionalProperty.get());
    }

    @Test
    public void testSetValue() throws Exception {
        Property<Integer> property = Property.create(10);
        assertEquals(new Integer(10), property.get());

        property.setValue(20);
        assertEquals(new Integer(20), property.get());
    }

    @Test
    public void testReset() throws Exception {
        Property<String> property = Property.create("tacos");
        Consumer<String> listener = mock(Consumer.class);

        property.onChanged(listener);
        verify(listener).accept("tacos");

        property.setValue("burritos");
        verify(listener).accept("burritos");

        property.reset();
        assertEquals("tacos", property.get());
        verify(listener, times(2)).accept("tacos");

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testAccept() throws Exception {
        Property<Integer> property = Property.create(10);
        assertEquals(new Integer(10), property.get());

        property.accept(30);
        assertEquals(new Integer(30), property.get());
    }

    @Test
    public void testCannotSetNullValue() throws Exception {
        Property<Integer> property = Property.create(10);

        try {
            property.setValue(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(new Integer(10), property.get());
    }

    @Test
    public void testCannotAcceptNullValue() throws Exception {
        Property<Integer> property = Property.create(10);

        try {
            property.accept(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(new Integer(10), property.get());
    }

    @Test
    public void testOnChange() throws Exception {
        Property<String> property = Property.create("tacos");

        Consumer<String> consumer = mock(Consumer.class);

        Subscription subscription = property.onChanged(consumer);
        verify(consumer).accept("tacos");

        property.setValue("burritos");
        verify(consumer).accept("burritos");

        subscription.unsubscribe();

        property.setValue("fajitas");
        Mockito.verifyNoMoreInteractions(consumer);
    }

    @Test
    public void testSubscribeOnChangeAfterDisposed() throws Exception {
        Property<String> property = Property.create("tacos");

        Consumer<String> consumer = mock(Consumer.class);
        Subscription subscription = property.onChanged(consumer);
        verify(consumer).accept("tacos");

        assertFalse(subscription.isUnsubscribed());

        property.dispose();
        assertTrue(subscription.isUnsubscribed());
        assertEquals("tacos", property.get());

        // This setValue should have been ignored and not forwarded to the
        // consumer.
        property.setValue("burritos");
        Mockito.verifyNoMoreInteractions(consumer);
        assertEquals("tacos", property.get());

        // Subscribe after the property has been destroyed and assert the latest
        // value is emitted.
        Consumer<String> consumer2 = mock(Consumer.class);
        Subscription subscription2 = property.onChanged(consumer2);

        verify(consumer2).accept("tacos");
        assertTrue(subscription2.isUnsubscribed());
    }

    @Test
    public void testDestroy() throws Exception {
        Property<String> property = Property.create("tacos");

        PropertyObserver<String> observer = Mockito.mock(PropertyObserver.class);
        PropertyObserver<String> observer2 = Mockito.mock(PropertyObserver.class);
        PropertyObserver<String> observer3 = Mockito.mock(PropertyObserver.class);

        // Throw some exceptions during callbacks.
        Mockito.doThrow(new RuntimeException()).when(observer).onDisposed();
        Mockito.doThrow(new RuntimeException()).when(observer2).onChanged(Matchers.any(String.class));

        // subscribe the mocks
        Subscription subscription = property.observe(observer);
        Subscription subscription2 = property.observe(observer2);
        Subscription subscription3 = property.observe(observer3);

        Assert.assertFalse(subscription.isUnsubscribed());
        Assert.assertFalse(subscription2.isUnsubscribed());
        Assert.assertFalse(subscription3.isUnsubscribed());

        InOrder inOrder = Mockito.inOrder(observer, observer2, observer3);

        inOrder.verify(observer).onChanged("tacos");
        inOrder.verify(observer2).onChanged("tacos");
        inOrder.verify(observer3).onChanged("tacos");

        property.dispose();

        inOrder.verify(observer).onDisposed();
        inOrder.verify(observer2).onDisposed();
        inOrder.verify(observer3).onDisposed();

        inOrder.verifyNoMoreInteractions();

        Assert.assertTrue(subscription.isUnsubscribed());
        Assert.assertTrue(subscription2.isUnsubscribed());
        Assert.assertTrue(subscription3.isUnsubscribed());
    }

    @Test(expected = IllegalStateException.class)
    public void testCallGetValueOnWrongThread() throws Exception {
        createPropertyOnEDT("tacos").get();
    }

    @Test(expected = IllegalStateException.class)
    public void testCallSetValueOnWrongThread() throws Exception {
        createPropertyOnEDT("tacos").setValue("burritos");
    }

    @Test(expected = IllegalStateException.class)
    public void testCallAcceptValueOnWrongThread() throws Exception {
        createPropertyOnEDT("tacos").accept("burritos");
    }

    @Test(expected = IllegalStateException.class)
    public void testCallOnChangeOnWrongThread() throws Exception {
        createPropertyOnEDT("tacos").onChanged(value -> {
        });
    }

    @Test(expected = IllegalStateException.class)
    public void testCallOnDisposedOnWrongThread() throws Exception {
        createPropertyOnEDT("tacos").onDisposed(() -> {
        });
    }

    @Test(expected = IllegalStateException.class)
    public void testCallObserveOnWrongThread() throws Exception {
        PropertyObserver<String> propertyObserver = Mockito.mock(PropertyObserver.class);
        createPropertyOnEDT("tacos").observe(propertyObserver);
    }

    @Test(expected = IllegalStateException.class)
    public void testCallObserveWithCallbacksOnWrongThread() throws Exception {
        createPropertyOnEDT("tacos").observe(value -> {
        } , () -> {
        });
    }

    @Test(expected = IllegalStateException.class)
    public void testCallHasObserversOnWrongThread() throws Exception {
        createPropertyOnEDT("tacos").hasObservers();
    }

    @Test(expected = IllegalStateException.class)
    public void testCallResetOnWrongThread() throws Exception {
        createPropertyOnEDT("tacos").reset();
    }

    @Test
    public void testExceptionInOnChangedDoesntStopOtherCallbacks() throws Exception {
        Property<String> property = Property.create("tacos");

        property.onChanged(value -> {
            throw new RuntimeException();
        });

        Consumer<String> listner = mock(Consumer.class);
        property.onChanged(listner);

        verify(listner).accept("tacos");

        property.setValue("burritos");
        verify(listner).accept("burritos");
    }

    @Test
    public void testExceptionInOnDisposedDoesntStopOtherCallbacks() throws Exception {
        Property<String> property = Property.create("tacos");

        property.onDisposed(() -> {
            throw new RuntimeException();
        });

        Runnable onDestroy = mock(Runnable.class);
        property.onDisposed(onDestroy);

        property.dispose();
        verify(onDestroy).run();
    }

    @Test
    public void testReentrancyIsBlocked() throws Exception {
        Property<String> property = Property.create("tacos");

        property.onChanged(newValue -> property.setValue("burritos"));
        Assert.assertEquals("tacos", property.get());

        property.setValue("fajitas");
        Assert.assertEquals("fajitas", property.get());
    }

    @Test
    public void testPropertyOnlyEmitValuesWhenValuesDifferentThanLast() throws Exception {
        Property<String> property = Property.create("tacos");

        Consumer<String> onChanged = Mockito.mock(Consumer.class);
        property.onChanged(onChanged);

        verify(onChanged).accept("tacos");

        property.setValue("tacos");

        verifyNoMoreInteractions(onChanged);
    }

    @Test
    public void testBind() throws Exception {
        Property<String> property1 = Property.create("tacos");
        Property<String> property2 = Property.create("burritos");

        Consumer<String> consumer1 = Mockito.mock(Consumer.class);
        property1.onChanged(consumer1);

        Consumer<String> consumer2 = Mockito.mock(Consumer.class);
        property2.onChanged(consumer2);

        verify(consumer1).accept("tacos");
        verify(consumer2).accept("burritos");

        // bind and assert that property1 takes the value of property2
        Subscription subscription = property1.bind(property2);

        verify(consumer1).accept("burritos");
        assertEquals("burritos", property1.get());
        assertEquals("burritos", property2.get());

        // assert that changes in property1 are not propagated to property2
        property1.setValue("fajitas");

        verify(consumer1).accept("fajitas");
        verifyNoMoreInteractions(consumer1, consumer2);

        // unsubscribe and asser that the properties are no longer bound and
        // that they still emit value updates
        subscription.unsubscribe();

        property2.setValue("enchiladas");
        verify(consumer2).accept("enchiladas");
        verifyNoMoreInteractions(consumer1, consumer2);

        property1.setValue("enchiladas");
        verify(consumer1).accept("enchiladas");
        verifyNoMoreInteractions(consumer1, consumer2);
    }

    @Test
    public void testDestroyABoundProperty() throws Exception {
        Property<String> property1 = Property.create("tacos");
        Property<String> property2 = Property.create("burritos");
        Consumer<String> consumer1 = Mockito.mock(Consumer.class);

        Subscription consumerSubscription = property1.onChanged(consumer1);
        verify(consumer1).accept("tacos");

        // bind properties
        Subscription bindSubscription = property1.bind(property2);
        verify(consumer1).accept("burritos");

        // destroy the binding source property and assert the binding
        // subscription is unsubscribed.
        property2.dispose();
        assertTrue(bindSubscription.isUnsubscribed());
        // the consumer of property1 should not have been unsubscribed.
        assertFalse(consumerSubscription.isUnsubscribed());
    }

    @Test
    public void testBindManyInLoop() throws Exception {
        Property<String> property1 = Property.create("tacos");
        Property<String> property2 = Property.create("burritos");
        Property<String> property3 = Property.create("fajitas");

        Consumer<String> consumer1 = Mockito.mock(Consumer.class);
        Consumer<String> consumer2 = Mockito.mock(Consumer.class);
        Consumer<String> consumer3 = Mockito.mock(Consumer.class);

        InOrder inOrder = Mockito.inOrder(consumer1, consumer2, consumer3);

        Subscription consumerSubscription1 = property1.onChanged(consumer1);
        Subscription consumerSubscription2 = property2.onChanged(consumer2);
        Subscription consumerSubscription3 = property3.onChanged(consumer3);

        Subscription bindSubscription1 = property1.bind(property2);
        Subscription bindSubscription2 = property2.bind(property3);
        Subscription bindSubscription3 = property3.bind(property1);

        inOrder.verify(consumer1).accept("burritos");
        inOrder.verify(consumer2).accept("fajitas");
        inOrder.verify(consumer1).accept("fajitas");
        inOrder.verifyNoMoreInteractions();

        property2.setValue("nachos");
        inOrder.verify(consumer1).accept("nachos"); // accept the nachos, or
                                                    // accept death
        inOrder.verify(consumer3).accept("nachos");
        inOrder.verifyNoMoreInteractions();

        property3.setValue("tacos");
        inOrder.verify(consumer2).accept("tacos");
        inOrder.verify(consumer1).accept("tacos");
        inOrder.verifyNoMoreInteractions();

        property1.setValue("burritos");
        inOrder.verify(consumer3).accept("burritos");
        inOrder.verify(consumer2).accept("burritos");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSynchronize() throws Exception {
        Property<String> property1 = Property.create("tacos");
        Property<String> property2 = Property.create("burritos");
        Property<String> property3 = Property.create("fajitas");

        Consumer<String> consumer1 = Mockito.mock(Consumer.class);
        Consumer<String> consumer2 = Mockito.mock(Consumer.class);
        Consumer<String> consumer3 = Mockito.mock(Consumer.class);

        InOrder inOrder = Mockito.inOrder(consumer1, consumer2, consumer3);

        Subscription consumerSubscription1 = property1.onChanged(consumer1);
        Subscription consumerSubscription2 = property2.onChanged(consumer2);
        Subscription consumerSubscription3 = property3.onChanged(consumer3);

        Subscription synchronize1Sub = property1.synchronize(property2);
        inOrder.verify(consumer1).accept("burritos");
        inOrder.verifyNoMoreInteractions();

        Subscription synchronize2Sub = property2.synchronize(property3);
        inOrder.verify(consumer2).accept("fajitas");
        inOrder.verify(consumer1).accept("fajitas");
        inOrder.verifyNoMoreInteractions();

        Subscription synchronize3Sub = property3.synchronize(property1);
        inOrder.verifyNoMoreInteractions();

        property2.setValue("burritos");
        inOrder.verify(consumer1).accept("burritos");
        inOrder.verify(consumer3).accept("burritos");
        inOrder.verifyNoMoreInteractions();

        property3.setValue("tacos");
        inOrder.verify(consumer2).accept("tacos");
        inOrder.verify(consumer1).accept("tacos");
        inOrder.verifyNoMoreInteractions();

        property1.setValue("nachos");
        inOrder.verify(consumer2).accept("nachos");
        inOrder.verify(consumer3).accept("nachos");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testDestroySynchronizedProperty() throws Exception {
        Property<String> property1 = Property.create("tacos");
        Property<String> property2 = Property.create("burritos");

        Consumer<String> consumer1 = Mockito.mock(Consumer.class);
        Consumer<String> consumer2 = Mockito.mock(Consumer.class);

        InOrder inOrder = Mockito.inOrder(consumer1, consumer2);

        Subscription consumerSubscription1 = property1.onChanged(consumer1);
        Subscription consumerSubscription2 = property2.onChanged(consumer2);

        Subscription synchronize1Sub = property1.synchronize(property2);

        inOrder.verify(consumer1).accept("burritos");
        inOrder.verifyNoMoreInteractions();

        property1.dispose();

        assertTrue(synchronize1Sub.isUnsubscribed());
    }

    @Test
    public void testFromSubject() throws Exception {
        BehaviorSubject<String> subject = BehaviorSubject.create("tacos");
        Property<String> property = Property.fromSubject(subject);

        Consumer<String> consumer = Mockito.mock(Consumer.class);
        property.onChanged(consumer);
        Mockito.verify(consumer).accept("tacos");

        subject.onNext("burritos");
        Mockito.verify(consumer).accept("burritos");

        subject.onCompleted();

        property.setValue("fajitas");
        Mockito.verify(consumer).accept("fajitas");
    }

    @Test
    public void testDestroyPropertyFromSubject() throws Exception {
        BehaviorSubject<String> subject = BehaviorSubject.create("tacos");
        Property<String> property = Property.fromSubject(subject);

        Consumer<String> consumer = Mockito.mock(Consumer.class);
        property.onChanged(consumer);
        Mockito.verify(consumer).accept("tacos");

        subject.onNext("burritos");
        Mockito.verify(consumer).accept("burritos");

        property.dispose();

        subject.onNext("fajitas");
        Mockito.verifyNoMoreInteractions(consumer);
        assertEquals("fajitas", subject.getValue());

        Action1<String> action = Mockito.mock(Action1.class);
        subject.subscribe(action);
        verify(action).call("fajitas");
    }

    @Test
    public void testHasObservers() throws Exception {
        Property<Optional<String>> property = Property.createOptional();
        assertFalse(property.hasObservers());

        property.onChanged(value -> {
        });
        property.onChanged(value -> {
        });
        property.onChanged(value -> {
        });
        assertTrue(property.hasObservers());

        property.dispose();
        assertFalse(property.hasObservers());
    }

    @Test
    public void testAsObservable() throws Exception {
        Property<String> property = Property.create("tacos");
        Observable<String> observable = property.asObservable();

        Action1<String> onNext = Mockito.mock(Action1.class);
        Action1<Throwable> onError = Mockito.mock(Action1.class);
        Action0 onComplete = Mockito.mock(Action0.class);

        observable.subscribe(onNext, onError, onComplete);
        verify(onNext).call("tacos");
        verifyNoMoreInteractions(onNext, onError, onComplete);

        property.setValue("burritos");
        verify(onNext).call("burritos");
        verifyNoMoreInteractions(onNext, onError, onComplete);

        property.dispose();
        verify(onComplete).call();
        verifyNoMoreInteractions(onNext, onError, onComplete);
    }

    @Test
    public void testUnsubscribingAnObservableRemovesPropertyObserver() throws Exception {
        Property<String> property = Property.create("tacos");
        Observable<String> observable = property.asObservable();

        assertFalse(property.hasObservers());

        Subscription subscription = observable.subscribe();
        assertTrue(property.hasObservers());

        subscription.unsubscribe();
        assertFalse(property.hasObservers());
    }

    private Property<String> createPropertyOnEDT(String initialValue) throws Exception {
        Object[] property = new Object[1];

        SwingUtilities.invokeAndWait(() -> {
            property[0] = Property.create(initialValue);
        });

        return (Property<String>) property[0];
    }
}
