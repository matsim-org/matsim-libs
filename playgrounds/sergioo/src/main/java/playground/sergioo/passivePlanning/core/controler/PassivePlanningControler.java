package playground.sergioo.passivePlanning.core.controler;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.households.PersonHouseholdMapping;

import playground.sergioo.passivePlanning.core.mobsim.passivePlanning.PassivePlanningSocialFactory;
import playground.sergioo.passivePlanning.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlanningControler extends Controler{
		
	//Constants
	private final static Logger log = Logger.getLogger(PassivePlanningControler.class);
	
	//Attributes
	private PassivePlannerManager passivePlannerManager;
	
	//Constructors
	/**
	 * @param config
	 */
	public PassivePlanningControler(Config config) {
		super(config);
	}
	/**
	 * @param scenario
	 */
	public PassivePlanningControler(Scenario scenario) {
		super(scenario);
	}
	/**
	 * @param configFileName
	 */
	public PassivePlanningControler(String configFileName) {
		super(configFileName);
	}
	/**
	 * @param args
	 */
	public PassivePlanningControler(String[] args) {
		super(args);
	}
	
	//Methods
	public void setPassivePlaningSocial(boolean b) {
		if(b==true)
			if(this.config.scenario().isUseHouseholds())
				this.setMobsimFactory(new PassivePlanningSocialFactory(passivePlannerManager, new PersonHouseholdMapping(this.getScenario().getHouseholds()), (IntermodalLeastCostPathCalculator) this.getLeastCostPathCalculatorFactory().createPathCalculator(network, this.createTravelCostCalculator(), this.getLinkTravelTimes())));
			else
				log.error("Households information is neccesary for passive planning with social");
	}

}
