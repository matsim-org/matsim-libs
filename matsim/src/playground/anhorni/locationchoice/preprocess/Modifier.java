package playground.anhorni.locationchoice.preprocess;

import org.matsim.facilities.Facilities;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Population;

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
