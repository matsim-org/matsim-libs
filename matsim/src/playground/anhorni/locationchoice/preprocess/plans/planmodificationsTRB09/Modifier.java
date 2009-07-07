package playground.anhorni.locationchoice.preprocess.plans.planmodificationsTRB09;

import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;

public abstract class Modifier {

	protected PopulationImpl plans=null;
	protected NetworkLayer network=null;
	protected ActivityFacilities  facilities=null;


	public Modifier(PopulationImpl plans, NetworkLayer network, ActivityFacilities  facilities) {
		this.plans=plans;
		this.network=network;
		this.facilities=facilities;
	}

	public abstract void modify();

}
