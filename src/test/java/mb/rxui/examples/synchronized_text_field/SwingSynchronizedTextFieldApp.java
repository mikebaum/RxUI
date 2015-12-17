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
package mb.rxui.examples.synchronized_text_field;

import static mb.rxui.examples.synchronized_text_field.TextView.defaultSwingTextView;

import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Simple Swing application to demonstrate how to use properties.
 */
public class SwingSynchronizedTextFieldApp {

    private static void buildAndShowSwingView() {
        TextComponent<JComponent> textComponent1 = new TextComponent<>(defaultSwingTextView(), "tacos");
        TextComponent<JComponent> textComponent2 = new TextComponent<>(defaultSwingTextView(), "");
        textComponent2.getModel().getTextProperty().synchronize(textComponent1.getModel().getTextProperty());
        
        initUI(textComponent1, textComponent2);
    }

    private static void initUI(TextComponent<JComponent> textComponent1, 
                               TextComponent<JComponent> textComponent2) {
        
        JFrame frame = new JFrame("Synchronized Text Field Test App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new GridLayout(2, 1));
        
        panel.add(textComponent1.getView());
        panel.add(textComponent2.getView());
        
        frame.setContentPane(panel);
        
        frame.pack();
        frame.setVisible(true);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(SwingSynchronizedTextFieldApp::buildAndShowSwingView );
    }
}
