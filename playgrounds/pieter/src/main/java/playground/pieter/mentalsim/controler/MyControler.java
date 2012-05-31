package playground.pieter.mentalsim.controler;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class MyControler extends Controler {
	private Population originalPopulation;
	private Population subsetPopulation;
	


	public Population getOriginalPopulation() {
		return originalPopulation;
	}



	public void setOriginalPopulation(Population originalPopulation) {
		this.originalPopulation = originalPopulation;
	}



	public Population getSubsetPopulation() {
		return subsetPopulation;
	}



	public void setSubsetPopulation(Population subsetPopulation) {
		this.subsetPopulation = subsetPopulation;
	}



	public MyControler(Config config) {
		super(config);
		// TODO Auto-generated constructor stub
		
	}

}
