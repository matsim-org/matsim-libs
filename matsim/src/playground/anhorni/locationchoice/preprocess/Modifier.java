package playground.anhorni.locationchoice.preprocess;

import org.matsim.facilities.Facilities;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;

public abstract class Modifier {

	protected Plans plans=null;
	protected NetworkLayer network=null;
	protected Facilities  facilities=null;


	public Modifier(Plans plans, NetworkLayer network, Facilities  facilities) {
		this.plans=plans;
		this.network=network;
		this.facilities=facilities;
	}

	public abstract void modify();

}
