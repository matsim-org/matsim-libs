package playground.sebhoerl.av.logic.states;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;

import playground.sebhoerl.agentfsm.instruction.AdvanceInstruction;
import playground.sebhoerl.agentfsm.instruction.Instruction;
import playground.sebhoerl.agentfsm.instruction.WaitBlockingInstruction;
import playground.sebhoerl.agentfsm.state.LegState;
import playground.sebhoerl.av.router.GenericLeg;

public class DropoffDrive extends AVState implements LegState {
	@Override
	public Instruction enter() {
		service.setPickupDepartureTime(now);
        postActivityStartEvent("AVDropoffDrive");
        return new WaitBlockingInstruction();
	}
	
	@Override
	public AdvanceInstruction leave() {
		postActivityEndEvent("AVDropoffDrive");
		return new AdvanceInstruction("Dropoff");
	}
	
	@Override
	public Leg createLeg() {
		Leg leg = new GenericLeg(TransportMode.car);
		leg.setDepartureTime(now);
		leg.setRoute(service.getDropoffRoute());
		leg.setTravelTime(service.getDropoffRoute().getTravelTime());
		return leg;
	}
}
