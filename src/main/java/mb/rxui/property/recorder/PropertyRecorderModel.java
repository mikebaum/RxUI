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
package mb.rxui.property.recorder;

import static mb.rxui.property.recorder.RecorderState.PLAYING;
import static mb.rxui.property.recorder.RecorderState.RECORDING;
import static mb.rxui.property.recorder.RecorderState.STOPPED;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import mb.rxui.disposables.Disposable;
import mb.rxui.property.EventSequenceGenerator;
import mb.rxui.property.Property;
import mb.rxui.property.PropertyChangeEvent;
import mb.rxui.property.PropertyObservable;
import rx.Scheduler.Worker;
import rx.schedulers.SwingScheduler;

public class PropertyRecorderModel<M> implements Disposable {
    
    private final Property<M> property;
    private final LinkedList<TimedEvent<M>> timedEvents = new LinkedList<>();
    private final Property<RecorderState> recorderState;

    private final PropertyObservable<Boolean> canStop;
    private final PropertyObservable<Boolean> canPlay;
    private final PropertyObservable<Boolean> canRecord;
    
    private Optional<Disposable> stopper = Optional.empty();
    private long absoluteTime;

    public PropertyRecorderModel(Property<M> property) {
        this.property = Objects.requireNonNull(property);
        this.recorderState = Property.create(STOPPED);
        this.canStop = recorderState.map(state -> state != STOPPED);
        this.canPlay = recorderState.map(state -> state == STOPPED);
        this.canRecord = recorderState.map(state -> state == STOPPED);
        initializeRecorder(recorderState);
    }
    
    @Override
    public void dispose() {
        recorderState.dispose();
    }
    
    private void initializeRecorder(Property<RecorderState> recorderState) {
        recorderState.is(RECORDING).then(this::record);
        recorderState.is(PLAYING).then(this::play);
        recorderState.is(STOPPED).then(this::stop);
    }

    public Property<RecorderState> getRecorderState() {
        return recorderState;
    }

    public void stop() {
        stopper.ifPresent(Disposable::dispose);
        recorderState.setValue(STOPPED);
    }
    
    public PropertyObservable<Boolean> canStop() {
        return canStop;
    }

    public void record() {
        stopper = Optional.of(property.changeEvents().map(this::computeTimedEvent).subscribe(timedEvents::add)::unsubscribe);
        recorderState.setValue(RECORDING);
    }
    
    public PropertyObservable<Boolean> canRecord() {
        return canRecord;
    }

    public void play() {
        setValue(timedEvents, SwingScheduler.getInstance().createWorker());
        recorderState.setValue(PLAYING);
    }
    
    public PropertyObservable<Boolean> canPlay() {
        return canPlay;
    }
    
    private TimedEvent<M> computeTimedEvent(PropertyChangeEvent<M> changeEvent) {
        absoluteTime = EventSequenceGenerator.getInstance().getTimeForSequence(changeEvent.getEventSequence()).get();

        if (timedEvents.isEmpty())
            return new TimedEvent<>(absoluteTime, 0l, changeEvent);
        
        return new TimedEvent<>(absoluteTime, absoluteTime - timedEvents.peekLast().absoluteTime, changeEvent);
    }

    private void setValue(LinkedList<TimedEvent<M>> timedEvents, Worker worker) {
        if (!timedEvents.isEmpty()) {
            TimedEvent<M> timedEvent = timedEvents.pollFirst();
            property.setValue(timedEvent.value);
        }

        if (!timedEvents.isEmpty()) {
            worker.schedule(() -> setValue(timedEvents, worker), timedEvents.peekFirst().timeSinceLast, TimeUnit.MILLISECONDS);
        }
    }

    private static class TimedEvent<M> {
        private final Long absoluteTime;
        private final Long timeSinceLast;
        private final M value;

        public TimedEvent(Long absoluteTime, Long timeSinceLast, PropertyChangeEvent<M> event) {
            this.absoluteTime = absoluteTime;
            this.timeSinceLast = timeSinceLast;
            this.value = event.getNewValue();
        }
    }
}
