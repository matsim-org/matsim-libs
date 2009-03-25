package playground.anhorni.locationchoice.preprocess.planmodifications;

import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.population.Population;
import org.matsim.core.network.NetworkLayer;

public abstract class Modifier {

	protected Population plans=null;
	protected NetworkLayer network=null;
	protected Facilities  facilities=null;


	public Modifier(Population plans, NetworkLayer network, Facilities  facilities) {
		this.plans=plans;
		this.network=network;
		this.facilities=facilities;
	}

	public abstract void modify();

}
