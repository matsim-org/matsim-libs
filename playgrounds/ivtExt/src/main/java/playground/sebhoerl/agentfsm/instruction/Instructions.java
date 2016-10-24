package playground.sebhoerl.agentfsm.instruction;

import playground.sebhoerl.agentlock.events.Event;

public class Instructions {
    static public WaitBlockingInstruction waitBlocking() {
        return new WaitBlockingInstruction();
    }
    
    static public WaitUntilInstruction waitUntil(double time) {
        return new WaitUntilInstruction(time);
    }
    
    static public WaitForEventInstruction waitFor(Event event) {
        return new WaitForEventInstruction(event);
    }
    
    static public AdvanceInstruction advance(String destinationStateId) {
        return new AdvanceInstruction(destinationStateId);
    }
}
