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
package mb.rxui.event;

import static mb.rxui.ThreadedTestHelper.createOnEDT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import mb.rxui.Subscription;
import mb.rxui.property.Property;
import mb.rxui.property.PropertyObserver;
import rx.Observable;
import rx.Observer;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class TestEventStream {
    @Test
    public void testOnEvent() {
        String[] events = new String[] { "tacos", "burritos", "fajitas" };
        EventStream<String> stream = createStream("tacos", "burritos", "fajitas");
        
        Consumer<String> eventHandler = Mockito.mock(Consumer.class);
        InOrder inOrder = Mockito.inOrder(eventHandler);

        Subscription subscription = stream.onEvent(eventHandler);
        
        Arrays.asList(events).forEach(event -> inOrder.verify(eventHandler).accept(event));
        inOrder.verifyNoMoreInteractions();
        
        Assert.assertTrue(subscription.isDisposed());
    }
    
    @Test(expected=IllegalStateException.class)
    public void testCallOnEventOnWrongThread() throws Exception {
        EventStream<String> stream = createOnEDT(() -> createStream());
        
        stream.onEvent(Mockito.mock(Consumer.class));
        fail("Trying to call onEvent should have thrown since this is the wrong thread.");
    }

    @Test
    public void testOnCompleted() {
        EventStream<String> stream = createStream();
        
        Runnable onCompleted = Mockito.mock(Runnable.class);
        stream.onCompleted(onCompleted);
        
        Mockito.verify(onCompleted).run();
    }
    
    @Test(expected=IllegalStateException.class)
    public void testCallOnCompletedOnWrongThread() throws Exception {
        EventStream<String> stream = createOnEDT(() -> createStream());
        
        stream.onCompleted(Mockito.mock(Runnable.class));
        fail("Trying to call onCompleted should have thrown since this is the wrong thread.");
    }
    
    @Test
    public void testObserve() {
        String[] events = new String[] { "tacos", "burritos", "fajitas" };
        
        EventStream<String> stream = createStream(events);
        
        EventStreamObserver<String> observer = Mockito.mock(EventStreamObserver.class);
        InOrder inOrder = Mockito.inOrder(observer);

        Subscription subscription = stream.observe(observer);
        
        inOrder.verify(observer).onEvent("tacos");
        inOrder.verify(observer).onEvent("burritos");
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
        
        Assert.assertTrue(subscription.isDisposed());
    }
    
    @Test(expected=IllegalStateException.class)
    public void testCallObserveOnWrongThread() throws Exception {
        EventStream<String> stream = createOnEDT(() -> createStream());
        
        stream.observe(Mockito.mock(EventStreamObserver.class));
        fail("Trying to call observe should have thrown since this is the wrong thread.");
    }
    
    @Test
    public void testToProperty() {
        EventStream<String> stream = createStream("fajitas");
        
        Property<String> property = stream.toProperty("tacos");
        PropertyObserver<String> observer = Mockito.mock(PropertyObserver.class);
        
        InOrder inOrder = Mockito.inOrder(observer);
        
        Subscription subscription = property.observe(observer);
        
        inOrder.verify(observer).onChanged("fajitas");
        inOrder.verifyNoMoreInteractions();
        
        assertFalse(subscription.isDisposed());
    }
    
    @Test(expected=IllegalStateException.class)
    public void testCallToPropertyOnWrongThread() throws Exception {
        EventStream<String> stream = createOnEDT(() -> createStream());
        
        stream.toProperty("burritos");
        fail("Trying to call toProperty should have thrown since this is the wrong thread.");
    }
    
    @Test
    public void testAsObservable() {
        EventStream<String> stream = createStream("tacos", "burritos", "fajitas");
        
        Observable<String> observable = stream.asObservable();
        
        Observer<String> observer = Mockito.mock(Observer.class);
        InOrder inOrder = Mockito.inOrder(observer);
        
        rx.Subscription subscription = observable.subscribe(observer);
        
        inOrder.verify(observer).onNext("tacos");
        inOrder.verify(observer).onNext("burritos");
        inOrder.verify(observer).onNext("fajitas");
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
        
        assertTrue(subscription.isUnsubscribed());
    }
    
    @Test
    public void testCallSubscribeOnAnObservableFromWrongThread() throws Exception {
        EventStream<String> stream = createOnEDT(()-> createStream());

        Throwable[] throwable = new Throwable[1];
        stream.asObservable().subscribe(value->{}, error->throwable[0] = error);
        
        assertEquals(IllegalStateException.class, throwable[0].getClass());
    }
    
    @Test
    public void testFromObservable() {
        EventStream<String> stream = EventStream.from(Observable.just("tacos", "burritos", "fajitas"));
        
        EventStreamObserver<String> observer = Mockito.mock(EventStreamObserver.class);
        InOrder inOrder = Mockito.inOrder(observer);
        
        Subscription subscription = stream.observe(observer);
        
        inOrder.verify(observer).onEvent("tacos");
        inOrder.verify(observer).onEvent("burritos");
        inOrder.verify(observer).onEvent("fajitas");
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
        
        assertTrue(subscription.isDisposed());
    }
    
    @Test
    public void testFromPublishSubject() {
        PublishSubject<String> subject = PublishSubject.create();
        EventStream<String> stream = EventStream.from(subject);
        
        EventStreamObserver<String> observer = Mockito.mock(EventStreamObserver.class);
        InOrder inOrder = Mockito.inOrder(observer);
        
        assertFalse(subject.hasObservers());
        
        stream.observe(observer);
        
        assertTrue(subject.hasObservers());
        
        subject.onNext("tacos");
        inOrder.verify(observer).onEvent("tacos");
        
        subject.onNext("burritos");
        inOrder.verify(observer).onEvent("burritos");
    }
    
    @Test
    public void testFromPublishSubjectUnsubscribe() {
        PublishSubject<String> subject = PublishSubject.create();
        EventStream<String> stream = EventStream.from(subject);
        
        EventStreamObserver<String> observer = Mockito.mock(EventStreamObserver.class);
        
        assertFalse(subject.hasObservers());
        
        Subscription subscription = stream.observe(observer);
        assertTrue(subject.hasObservers());
        
        subscription.dispose();
        assertFalse(subject.hasObservers());
    }
    
    @Test
    public void testFromPublishSubjectCompletes() {
        PublishSubject<String> subject = PublishSubject.create();
        EventStream<String> stream = EventStream.from(subject);
        
        EventStreamObserver<String> observer = Mockito.mock(EventStreamObserver.class);
        
        assertFalse(subject.hasObservers());
        
        Subscription subscription = stream.observe(observer);
        assertTrue(subject.hasObservers());
        
        subject.onCompleted();
        assertFalse(subject.hasObservers());
        Mockito.verify(observer).onCompleted();
        assertTrue(subscription.isDisposed());
    }
    
    @Test
    public void testFromBehaviorSubject() {
        BehaviorSubject<String> subject = BehaviorSubject.create("tacos");
        EventStream<String> stream = EventStream.from(subject);
        
        EventStreamObserver<String> observer = Mockito.mock(EventStreamObserver.class);
        InOrder inOrder = Mockito.inOrder(observer);
        
        assertFalse(subject.hasObservers());
        
        stream.observe(observer);
        
        assertTrue(subject.hasObservers());
        
        inOrder.verify(observer).onEvent("tacos");
        
        subject.onNext("burritos");
        inOrder.verify(observer).onEvent("burritos");
    }
    
    public static <T> EventStream<T> createStream(T... events) {
        return new EventStream<>(observer -> {
            EventStreamSubscriber<T> subscriber = new EventStreamSubscriber<>(observer);
            
            Arrays.asList(events).forEach(subscriber::onEvent);
            subscriber.onCompleted();
            
            return subscriber;
        });
    }
}