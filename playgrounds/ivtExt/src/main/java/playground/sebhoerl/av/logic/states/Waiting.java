package playground.sebhoerl.av.logic.states;

import playground.sebhoerl.agentfsm.instruction.AdvanceInstruction;
import playground.sebhoerl.agentfsm.instruction.Instruction;
import playground.sebhoerl.agentfsm.instruction.WaitForEventInstruction;
import playground.sebhoerl.agentfsm.state.ActivityState;

public class Waiting extends AVState implements ActivityState {
	@Override
	public Instruction enter() {
		service.setPickupArrivalTime(now);
		
		if (service.getPassengerArrivalEvent().hasFired()) {
		    return new AdvanceInstruction("Pickup");
		}
		
        postActivityStartEvent("AVWaitForPassenger");
        return new WaitForEventInstruction(service.getPassengerArrivalEvent());
	}

	@Override
	public AdvanceInstruction leave() {
		postActivityEndEvent("AVWaitForPassenger");
		return new AdvanceInstruction("Pickup");
	}
}
