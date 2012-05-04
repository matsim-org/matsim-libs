package playground.gregor.sim2d_v3.simulation.floor.forces.deliberative;

import org.matsim.api.core.v01.Id;

import playground.gregor.sim2d_v3.simulation.floor.Agent2D;

public interface LinkSwitcher {

	public abstract void checkForMentalLinkSwitch(Id curr, Id next,
			Agent2D agent);

}