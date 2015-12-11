# RxUI
Tools for creating reactive based UI applications

### Motivation
#### Minimize Boilerplate
Creating UIs generally means wiring together components using listeners. Although writing listeners and callbacks is exciting in it's own way, writing all the boilerplate to achive this is error prone and highly repetative. Never mind the ***callback hell*** that goes along with the traditional apporach. In an ideal world we as developers should be focused on the look and feel, and behavior of the application. So in a nutshell, let's consider ourselves as designers/architects rather than plumbers and capenters. Using a reactive (streaming/event) based approach allows us to escape callback hell.
#### Ensure thread correctness
Most UI toolkits are not thread safe, so if you're not careful you can introduce subtle bugs. Part of the goal of this library is to make it easier to ensure that we only ever interact with the underlying UI toolkit on it's event loop.
#### Improve Consistency and Re-Usability
In order to increase code re-use and agility there is a need for a minimal set of abstractions for the typical elements of a UI. Of course there are 5,001 different UI architectural patterns, this library will be based on the [MVVM architecture](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel) with [Data Binding](https://en.wikipedia.org/wiki/Data_binding) at it's core. The MVVM pattern and data binding have been chosen since using this approach makes it easy to have a clean separation between the view and business logic of an application.
#### Portability
Since you may, at some time in the future, want to change the UI toolkit you are using this library attempts to contain the UI toolkit as much as possible. This may not be the case if the application you work on is a web app, but if you're lucky enough to be using Swing (half joking) this is definitely a concern. Therefore a goal of the library is to minimize the UI toolkit specific aspects. Clearly constructing and assembling the actual interface is toolkit specific, but the models, business logic and binding between the models and views should not be. This is where data binding comes to the rescue. The first targeted toolkit will be **Swing**, followed by **JavaFx** and possibly Html/Javascript (pipe dream).
#### Easy Client/Server Connectivity
Establishing a connection between your client application and the server should not involve space flight. It should be simple, flexible and quick to setup. Therefore the approach will be to use reactive microservices. The initial thought is to explore using [Vert.x](http://vertx.io/), although I'm open to other technologies, so long as they're not blocking, RPC based. If you have a good idea convince me.

### Inspiration
This library is heavily inspired by [ReactiveUI](http://reactiveui.net/) and [RxJava](https://github.com/ReactiveX/RxJava). Although at the core the library does not use RxJava (*still on the fence about this*), the implementation of the Property API can be considered RxJava light specifically designed for UI development. Of course there is interop between RxJava and Properties.
#### Hold on a second, what, why aren't you just using RxJava
Well, as peviously mentioned, most UI toolkits are single threaded and you will have problems if you access the view from more than one thread. Considing this, it becomes clear that the implementation of the Property API must protect against wrong thread access. Attempting to bolt this protection on top of RxJava was not impossible but more of an annoyance. I did it and when I was done I was not happy with the result. Keep in mind also, the RxJava library was developed from the perspective of the server and although it can be used for UI, it inherits a lot of things not need in the UI. The philosophy of this library is that all the code you write that interacts with the View should be in a happy single threaded bubble. Additionally, the API should be tuned for UI and not have to make any compromises. Of course you need concurrency and async but that probably represents a much smaller portion of the code than does the single threaded portion. Additionally RxJava is not the only way to achieve async, you could use actors, futures (sad face), promises / completable futures, etc... This library will not be married to one approach. You can use whatever form of concurrency you're confortable with, so long as when you call into the property api, it's on the right thread. While I'm at it, here's a note about using a `BehaviorSubjects` to back model properties.
##### What's wrong with the BehaviorSubject
There are two main problems with the `BehaviorSubject`

1. **Early Completion** - Care must be taken when binding Subjects, since in my opinion you are only really interested in propagating onNext events. onError events and onComplete events can complete bound properties if you're not careful. Bound model properties could have different life times, so the automatic completion that you get from BehaviorSubjects works against you in this case. Of course you could be careful to only subscribe onNext everwhere but why tip toe around like this if you don't have to. Also considering a Property is meant to replace a mutable field in a model, you should be able to get the value even after the property is no longer streaming values. A `BehaviorSubject` does not do this, it returns null. In contrast, an `AsyncSubject` does provide the value after completion, so in effect a Property takes the best parts of the `BehaviorSubject` and the `AsyncSubject` and leaves the rest.
2. **Reentrancy** - ~~If~~ When you create binding loops, you can end up with reentrancy which ~~could~~ will cause problems. Again, I found a solution to this and in the end I felt it could be easier.

### Where are we now
This library is in it's infancy. There is a lot of work to be done, so if you want to get in and participate in the design and implementation now's the time. The first thing I've worked on is the Property, Data Binding API. Since this is such a core element, I've spent considerable time developing it and it is thuroughly tested. It's not 100% nailed down (~~90~~70% maybe), so I'm open to suggestions. Here's an example that demonstrates how the Properties API can be used. I have yet to commit any type of model or view object, so please ignore that fact for the moment. The application presents a form with two text fields that are synchronized, so that edits in one field are mirrored in the other.
```
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
```
