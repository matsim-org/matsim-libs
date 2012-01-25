package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.model.SignalGroupState;

import playground.gregor.sim2d_v2.controller.PedestrianSignal;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.forces.ForceModule;

public class SignalsPerception implements ForceModule {

	
	private final Map<Id, PedestrianSignal> signals;

	public SignalsPerception(Map<Id, PedestrianSignal> signals) {
		this.signals = signals;
	}
	
	@Override
	public void run(Agent2D agent, double time) {
		PedestrianSignal sig = this.signals.get(agent.getCurrentLinkId());
		if (sig != null) {
			if (!sig.hasGreenForToLink(agent.chooseNextLinkId())) {
				agent.informAboutSignalState(SignalGroupState.RED, time);
			} else {
				agent.informAboutSignalState(SignalGroupState.GREEN, time);
			}	
		}
		
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

}
