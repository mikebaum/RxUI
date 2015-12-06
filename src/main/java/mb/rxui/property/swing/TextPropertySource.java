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
package mb.rxui.property.swing;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import mb.rxui.annotations.RequiresTest;
import mb.rxui.disposables.Disposable;
import mb.rxui.property.Property;
import mb.rxui.property.PropertyDispatcher;

/**
 * A Swing text property source.
 */
@RequiresTest
public class TextPropertySource extends SwingPropertySource<String> {

    private final Document document;

    private TextPropertySource(JTextComponent textComponent) {
        super(textComponent::getText, textComponent::setText);
        document = textComponent.getDocument();
    }
    
    public static Property<String> createTextProperty(JTextComponent textComponent) {
        return Property.create(new TextPropertySource(textComponent));
    }

    @Override
    protected Disposable bindDispatcher(PropertyDispatcher<String> dispatcher) {
        DocumentListener documentListener = createDocumentListener(dispatcher);

        document.addDocumentListener(documentListener);

        return () -> document.removeDocumentListener(documentListener);
    }

    private DocumentListener createDocumentListener(PropertyDispatcher<String> dispatcher) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                dispatcher.dispatchValue(get());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                dispatcher.dispatchValue(get());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // nothing to do. Change updates relate to text attributes.
            }
        };
    }
}
