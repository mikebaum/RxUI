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
package mb.rxui.examples;

import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import mb.rxui.property.Property;
import mb.rxui.property.swing.TextPropertySource;

/**
 * Simple application to demonstrate how to use properties.
 */
public class SynchronizedTextFieldApp {

    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Synchronized Text Field Test App");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            JTextComponent textComponent1 = new JTextField();
            TextView textView1 = new TextView(textComponent1);
            TextModel textModel1 = new TextModel("tacos");
            textView1.getTextProperty().synchronize(textModel1.getTextProperty());
            
            JTextComponent textComponent2 = new JTextField();
            TextView textView2 = new TextView(textComponent2);
            TextModel textModel2 = new TextModel("");
            textView2.getTextProperty().synchronize(textModel2.getTextProperty());
            
            textModel2.getTextProperty().synchronize(textModel1.getTextProperty());
            
            JPanel panel = new JPanel(new GridLayout(2, 1));
            
            panel.add(textView1.getText());
            panel.add(textView2.getText());
            
            frame.setContentPane(panel);
            
            frame.pack();
            frame.setVisible(true);
        });
    }
    
    private static class TextView {
        private final Property<String> textProperty;
        private final JComponent textComponent;
        
        public TextView(JTextComponent textComponent) {
            textProperty = TextPropertySource.createTextProperty(textComponent);
            this.textComponent = textComponent;
        }
        
        public JComponent getText() {
            return textComponent;
        }
        
        public Property<String> getTextProperty() {
            return textProperty;
        }
    }
    
    private static class TextModel {
        private final Property<String> textProperty;
        
        public TextModel(String initialValue) {
            textProperty = Property.create(initialValue);
        }
        
        public Property<String> getTextProperty() {
            return textProperty;
        }
    }
}
