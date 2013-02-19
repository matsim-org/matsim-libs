package playground.mmoyo.algorithms;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.router.TransitActsRemover;

import playground.mmoyo.utils.CarPlansRemover;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.FirstPersonsExtractor;
import playground.mmoyo.utils.NonPtRouteNullifier;
import playground.mmoyo.utils.NonSelectedPlansRemover;
import playground.mmoyo.utils.PlanSelectedChanger;

/**removes non selected plans, transit acts and scores in a population*/
public class PopulationCleaner extends AbstractPersonAlgorithm {
	
	PlanAlgorithm transitActsRemover = new TransitActsRemover();
	NonSelectedPlansRemover nonSelectedPlansRemover = new NonSelectedPlansRemover();
	ActLinkNullifier actLinkNullifier = new ActLinkNullifier();
	
	CarPlansRemover carPlansRemover = new CarPlansRemover();
	NonPtPlanFilter nonPtPlanFilter = new NonPtPlanFilter();
	NonPtRouteNullifier nonPtRouteNullifier = new NonPtRouteNullifier();
	ActFacilityNullifier actFacilityNullifier = new ActFacilityNullifier();
	PlanSelectedChanger planSelectedChanger = new PlanSelectedChanger(0);
	
	@Override
	public void run(Person person) {
		planSelectedChanger.run(person);
		nonSelectedPlansRemover.run(person);  	  //remove non selected plans
		Plan selectedPlan = person.getSelectedPlan();
		transitActsRemover.run(selectedPlan);     //remove transit acts
		nonPtRouteNullifier.run(selectedPlan);    //nullify non pt routes
		//actLinkNullifier.run(person);			  //nullify link
		//actFacilityNullifier.run(person);		  //nullify facility
		selectedPlan.setScore(null);			  //nullify score
	}
	
	public void clean(Population pop){
		carPlansRemover.run(pop);
		nonPtPlanFilter.run(pop); //get ride of persons without pt plans
	}
	
	public static void main(String[] args) {
		String netFilePath;
		String popFilePath;
		
		if (args.length>0){
			netFilePath = args[0];
			popFilePath = args[1];
		}else{
			netFilePath = "../../";
			popFilePath = "../../";
		}
		
		DataLoader dataLoader = new DataLoader();
		Scenario scn = dataLoader.readNetwork_Population(netFilePath, popFilePath);
		Population pop =scn.getPopulation(); 
		PopulationCleaner popCleaner = new PopulationCleaner();
		//popCleaner.clean(pop);
		popCleaner.run(pop);
		popCleaner.run(scn.getPopulation());
		
		//write file
		System.out.println("writing output plan file...");
		PopulationWriter popwriter = new PopulationWriter(pop, scn.getNetwork());
		File file = new File(popFilePath);
		popwriter.write(file.getParent() + "/" + file.getName() + "cleaned.xml.gz") ;
		System.out.println("done");
		
		//write a sample of first 10 persons
		Population popSample = new FirstPersonsExtractor().run(pop, 100);
		System.out.println("writing sample plan file...");
		popwriter = new PopulationWriter(popSample, scn.getNetwork());
		popwriter.write(file.getParent() + "/" + file.getName() + "cleanedSmallnSample.xml");
		System.out.println("done");
	}

}
