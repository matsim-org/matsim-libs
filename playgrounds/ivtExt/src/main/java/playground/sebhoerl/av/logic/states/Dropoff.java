package playground.sebhoerl.av.logic.states;

import playground.sebhoerl.agentfsm.instruction.AdvanceInstruction;
import playground.sebhoerl.agentfsm.instruction.Instruction;
import playground.sebhoerl.agentfsm.instruction.WaitUntilInstruction;
import playground.sebhoerl.agentfsm.state.ActivityState;

public class Dropoff extends AVState implements ActivityState {
	final double dropoffTime;
	
	public Dropoff(double dropoffTime) {
		this.dropoffTime = dropoffTime;
	}
	
	@Override
	public Instruction enter() {
		service.setDropoffArrivalTime(now);
        postActivityStartEvent("AVDropoff");
        
        return new WaitUntilInstruction(now + dropoffTime);
	}

	@Override
	public AdvanceInstruction leave() {
	    service.setDropoffTime(now);
	    service.getDropoffEvent().fire();
	    
		postActivityEndEvent("AVDropoff");
		return new AdvanceInstruction("Idle");
	}
}
