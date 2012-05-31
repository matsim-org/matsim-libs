package playground.pieter.mentalsim.controler;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class MyControler extends Controler {
	private Population originalPlans;
	
	public Population getOriginalPlans() {
		return originalPlans;
	}

	public void setOriginalPlans(Population originalPlans) {
		this.originalPlans = originalPlans;
	}

	public MyControler(Config config) {
		super(config);
		// TODO Auto-generated constructor stub
	}

}
