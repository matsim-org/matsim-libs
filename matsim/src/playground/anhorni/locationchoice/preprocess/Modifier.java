package playground.anhorni.locationchoice.preprocess;

import org.matsim.facilities.Facilities;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.NetworkLayer;

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
