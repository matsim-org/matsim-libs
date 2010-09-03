package plans;

import java.util.Iterator;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.ArgumentParser;

public class DeleteRoutes {

	/**
	 * @param args
	 */
	
	private Config config;
	private String configfile = null;	
	
	private void parseArguments(final String[] args) {
		if (args.length == 0) {
			System.out.println("Too few arguments.");
			System.exit(1);
		}
		Iterator<String> argIter = new ArgumentParser(args).iterator();
		String arg = argIter.next();
		if (arg.equals("-h") || arg.equals("--help")) {
			System.exit(0);
		} else {
			this.configfile = arg;
			if (argIter.hasNext()) {
				System.out.println("Too many arguments.");
				System.exit(1);
			}
		}
	}
	
	public void run(final String[] args) {
		parseArguments(args);
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(this.configfile);
		sl.loadNetwork();
		sl.loadPopulation();
		NetworkImpl network = sl.getScenario().getNetwork();
		this.config = sl.getScenario().getConfig();

		final PopulationImpl plans = (PopulationImpl) sl.getScenario().getPopulation();	
		
		
		for (Person person : plans.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()){
					if(element instanceof Activity) {
						((ActivityImpl) element).setLinkId(null);
					}
				}
			}
		}
		
		for (Person person : plans.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()){
					if(element instanceof Leg){
						((LegImpl) element).setRoute(null);
					}
				}
			}
		}
		
		plans.setIsStreaming(false);
		final PopulationWriter plansWriter = new PopulationWriter(plans, network);
		plans.addAlgorithm(plansWriter);
		plans.printPlansCount();
		plansWriter.write(this.config.findParam("plans", "outputPlansFile"));

		System.out.println("done.");
		
	}
	
	public static void main(String[] args) {
		new DeleteRoutes().run(args);

	}

}
