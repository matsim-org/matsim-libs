package playground.sebhoerl.av.logic.states;

import playground.sebhoerl.agentfsm.instruction.AdvanceInstruction;
import playground.sebhoerl.agentfsm.instruction.Instruction;
import playground.sebhoerl.agentfsm.instruction.WaitBlockingInstruction;
import playground.sebhoerl.agentfsm.state.ActivityState;

public class Idle extends AVState implements ActivityState {
	@Override
	public Instruction enter() {
		if (service != null) {
			service.setEndTime(now);
			service.getFinishTaskEvent().fire();
		}
		
        postActivityStartEvent("AVIdle");
        return new WaitBlockingInstruction();
	}

	@Override
	public AdvanceInstruction leave() {
		postActivityEndEvent("AVIdle");
		return new AdvanceInstruction("PickupDrive");
	}
}
