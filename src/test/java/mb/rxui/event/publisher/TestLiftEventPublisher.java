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
package mb.rxui.event.publisher;

import org.junit.Test;
import org.mockito.Mockito;

import mb.rxui.event.EventStreamObserver;
import mb.rxui.event.EventStreamSubscriber;
import mb.rxui.event.operator.Operator;

public class TestLiftEventPublisher {
    @Test
    public void testSubscribe() {
        Operator<String, Integer> operator = Mockito.mock(Operator.class);
        EventStreamSubscriber<String> parentSubscriber = Mockito.mock(EventStreamSubscriber.class);

        Mockito.when(operator.apply(Mockito.any())).thenReturn(parentSubscriber);
        
        EventPublisher<String> publisher = Mockito.mock(EventPublisher.class);
        LiftEventPublisher<String, Integer> lifter = new LiftEventPublisher<>(operator, publisher);
        
        lifter.subscribe(Mockito.mock(EventStreamObserver.class));
        
        Mockito.verify(operator).apply(Mockito.any(EventStreamSubscriber.class));
        Mockito.verify(publisher).subscribe(parentSubscriber);
    }
}
