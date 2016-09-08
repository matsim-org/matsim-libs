package playground.sebhoerl.av.logic.states;

import playground.sebhoerl.agentfsm.instruction.AdvanceInstruction;
import playground.sebhoerl.agentfsm.instruction.Instruction;
import playground.sebhoerl.agentfsm.instruction.WaitUntilInstruction;
import playground.sebhoerl.agentfsm.state.ActivityState;

public class Pickup extends AVState implements ActivityState {
	final double pickupTime;
	
	public Pickup(double pickupTime) {
		this.pickupTime = pickupTime;
	}
	
	@Override
	public Instruction enter() {
        postActivityStartEvent("AVPickup");
        
        service.setPickupTime(now);
        service.getPickupEvent().fire();
        
        return new WaitUntilInstruction(now + pickupTime);
	}

	@Override
	public AdvanceInstruction leave() {
		postActivityEndEvent("AVPickup");
		return new AdvanceInstruction("DropoffDrive");
	}
}
