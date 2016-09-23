package playground.sebhoerl.av.logic.states;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;

import playground.sebhoerl.agentfsm.instruction.AdvanceInstruction;
import playground.sebhoerl.agentfsm.instruction.Instruction;
import playground.sebhoerl.agentfsm.instruction.WaitBlockingInstruction;
import playground.sebhoerl.agentfsm.state.LegState;
import playground.sebhoerl.av.router.GenericLeg;

public class PickupDrive extends AVState implements LegState {
	@Override
	public Instruction enter() {
		service.setStartTime(now);
		service.setStartLinkId(agent.getCurrentLinkId());
        
        if (service.getRequest().getPickupLinkId().equals(agent.getCurrentLinkId())) {
            return new AdvanceInstruction("Waiting");
        }
        
        postActivityStartEvent("AVPickupDrive");
        return new WaitBlockingInstruction();
	}

	@Override
	public AdvanceInstruction leave() {
		postActivityEndEvent("AVPickupDrive");
		return new AdvanceInstruction("Waiting");
	}

	@Override
	public Leg createLeg() {
		Leg leg = new GenericLeg(TransportMode.car);
		leg.setDepartureTime(now);
		leg.setRoute(service.getPickupRoute());
		leg.setTravelTime(service.getPickupRoute().getTravelTime());
		return leg;
	}
}
