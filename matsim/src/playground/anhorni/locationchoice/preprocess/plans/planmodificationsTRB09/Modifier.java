package playground.anhorni.locationchoice.preprocess.plans.planmodificationsTRB09;

import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.network.NetworkLayer;

public abstract class Modifier {

	protected Population plans=null;
	protected NetworkLayer network=null;
	protected ActivityFacilities  facilities=null;


	public Modifier(Population plans, NetworkLayer network, ActivityFacilities  facilities) {
		this.plans=plans;
		this.network=network;
		this.facilities=facilities;
	}

	public abstract void modify();

}
