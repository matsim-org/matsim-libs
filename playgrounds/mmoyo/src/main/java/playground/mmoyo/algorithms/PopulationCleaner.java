package playground.mmoyo.algorithms;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.router.TransitActsRemover;

import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.NonPtRouteNullifier;
import playground.mmoyo.utils.NonSelectedPlansRemover;

/**removes non selected plans, transit acts and scores in a population*/
public class PopulationCleaner extends AbstractPersonAlgorithm {
	
	PlanAlgorithm transitActsRemover = new TransitActsRemover();
	NonSelectedPlansRemover nonSelectedPlansRemover = new NonSelectedPlansRemover();
	NonPtRouteNullifier nonPtRouteRemover = new NonPtRouteNullifier();
	
	@Override
	public void run(Person person) {
		nonSelectedPlansRemover.run(person);  	  //no selected plans
		Plan selectedPlan = person.getSelectedPlan();
		transitActsRemover.run(selectedPlan);     //transit acts
		nonPtRouteRemover.run(selectedPlan);	  //car, walk, bike routes
		selectedPlan.setScore(null);			  //score
	}
	
	public static void main(String[] args) {
		String netFilePath;
		String popFilePath;
		String output;
		
		if (args.length>0){
			netFilePath = args[0];
			popFilePath = args[1];
			output = args[2];
		}else{
			netFilePath = "../../input/newDemand/network.final.xml.gz";
			popFilePath = "../../input/newDemand/bvg.run190.25pct.100Cleaned.plans.xml.gz";
			output = "../../input/newDemand/bvg.run190.25pct.100.plans.selected.cleaned.xml.gz";
		}
		
		DataLoader dataLoader = new DataLoader();
		Scenario scn = dataLoader.readNetwork_Population(netFilePath, popFilePath);
		new PopulationCleaner().run(scn.getPopulation());
		new PopulationWriter(scn.getPopulation(), scn.getNetwork()).write(output);
	}

}
