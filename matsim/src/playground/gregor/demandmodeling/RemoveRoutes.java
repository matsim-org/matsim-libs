package playground.gregor.demandmodeling;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.scenario.ScenarioLoaderImpl;

public class RemoveRoutes {
	
	
	public static void main(String [] args) {
		
		String cf = "../../inputs/configs/eafEvac.xml";
		ScenarioImpl sc = new ScenarioLoaderImpl(cf).getScenario();
		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(net).readFile(sc.getConfig().network().getInputFile());
		
		new PopulationReaderMatsimV4(sc).readFile(sc.getConfig().plans().getInputFile());
		PopulationImpl pop = sc.getPopulation();
		for (Person pers : pop.getPersons().values()) {
			Plan plan = pers.getSelectedPlan();
			((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity()).setRoute(null);
			((PlanImpl) plan).getNextActivity(((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity())).setType("h");
		}
		
		new PopulationWriter(pop).write(sc.getConfig().plans().getOutputFile());
		
	}

}
