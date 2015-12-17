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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import mb.rxui.property.Property;
import mb.rxui.property.PropertyObservable;

public class PropertyRecorderView {

    private final JComponent view;
    private final Action recordAction;
    private final Action playAction;
    private final Action stopAction;
    private final Property<RecorderState> recorderState;

    public PropertyRecorderView() {
        this.recorderState = Property.create(STOPPED);
        playAction = createAction("Play", () -> recorderState.setValue(PLAYING));
        recordAction = createAction("Record", () -> recorderState.setValue(RECORDING));
        stopAction = createAction("Stop", () -> recorderState.setValue(STOPPED));
        view = buildView(recorderState, playAction, recordAction, stopAction);
    }

    private static JComponent buildView(Property<RecorderState> recorderState, Action playAction,
            Action recordAction, Action stopAction) {

        JPanel playerPanel = new JPanel();

        playerPanel.add(new JButton(playAction));
        playerPanel.add(new JButton(recordAction));
        playerPanel.add(new JButton(stopAction));
        
        return playerPanel;
    }

    private static AbstractAction createAction(String label, Runnable action) {
        return new AbstractAction(label) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        };
    }

    public JComponent getComponent() {
        return view;
    }
    
    public void canRecord(boolean canRecord) {
        recordAction.setEnabled(canRecord);
    }
    
    public void canPlay(boolean canPlay) {
        playAction.setEnabled(canPlay);
    }
    
    public void canStop(boolean canStop) {
        stopAction.setEnabled(canStop);
    }
    
    public PropertyObservable<RecorderState> recorderState() {
        return recorderState;
    }
}
