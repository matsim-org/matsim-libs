package playground.mmoyo.analysis.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.utils.PlanFragmenter;

/**Calculates and saves the global travel time, travel distance, transfers, det-transfers, walk distance and waiting time of a population*/ 
public class PopulationPtAnalyzer {
	String outputFile;
	Scenario scenario;
	
	public PopulationPtAnalyzer(Scenario scenario, String outputFile){
		this.scenario = scenario;
		this.outputFile = outputFile;
	}
	
	public void run (){
		double travelTime = 0.0;
		double travelDistance = 0.0;
		double waitTime = 0.0;
		double walkTime = 0.0;
		int transfers = 0;
		int detTransfers = 0;
		int directWalks=0;
		int walkDistance = 0;
		
		Population population = this.scenario.getPopulation();
		final int plansBefFragm = population.getPersons().size();
		
		/*
		//Get rid of only car plans
		PlansFilterByLegMode plansFilter = new PlansFilterByLegMode( TransportMode.car, PlansFilterByLegMode.FilterType.removeAllPlansWithMode) ;
		plansFilter.run(population);
		//write this real working plan
		PopulationWriter popwriter = new PopulationWriter(population, scenario.getNetwork());
		popwriter.write("../playgrounds/mmoyo/output/marcel/job14/Berlin5xRemovedautos.xml") ;
		
		System.out.println("after removing cars: " + population.getPersons().size());
		*/
		
		//plans must be fragmented
		new PlanFragmenter().run(population);
		 
		for (Person person : population.getPersons().values() ){
			Plan plan =  person.getSelectedPlan();  
			Activity aAct = ((Activity)plan.getPlanElements().get(0));
			Activity bAct = ((Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1));
			
			int i = 0;
			Activity lastAct= null;
			Activity nextAct;
			Leg lastLeg = null;
			Leg nextLeg;
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity) {
					Activity act = (Activity)pe;
					if (act!= aAct  && act!=bAct ){ 
						nextAct= (Activity) plan.getPlanElements().get(i+2);
						nextLeg= (Leg) plan.getPlanElements().get(i+1);
						if(act.getType().equals("pt interaction") && lastAct.getType().equals("pt interaction") && nextAct.getType().equals("pt interaction") && lastLeg.getMode().equals(TransportMode.transit_walk) && nextLeg.getMode().equals(TransportMode.transit_walk)){
							transfers++;
						}
					}
					lastAct= act;
					
				}else{
					LegImpl leg = (LegImpl)pe;
					nextAct= (Activity) plan.getPlanElements().get(i+1);
					///find out walk distances
					if (leg.getMode().equals(TransportMode.transit_walk)){
						double legWalkDist=  CoordUtils.calcDistance(lastAct.getCoord() , nextAct.getCoord());
						walkDistance += legWalkDist;
						walkTime += leg.getTravelTime();
						
						//System.out.println("legWalkDist:" + legWalkDist);
						if(lastAct.equals(aAct) && nextAct.equals(bAct)){
							directWalks++;
						}else if (lastAct.getType().equals("pt interaction") || lastAct.getType().equals("pt interaction")){ 
							if (legWalkDist>0.0){
								detTransfers++;
							}else{
								transfers++;
							}
						}
					}else{ //if (leg.getMode().equals(TransportMode.pt)) {
						if (leg.getRoute()!= null){
							travelDistance += leg.getRoute().getDistance();
							
							//org.matsim.api.core.v01.population.Route genericRouteImpl = new org.matsim.core.population.routes.GenericRouteImpl(leg.getRoute().getStartLinkId(), leg.getRoute().getEndLinkId());	
							//travelDistance +=  RouteUtils.calcDistance( (NetworkRoute) genericRouteImpl, scenario.getNetwork());
							travelTime +=  leg.getTravelTime();
						}
					}
					
					lastLeg= leg;
				}
				i++;
			}//for planelement
		}//	for person

		System.out.println("plansBeforeFragm:  \t" + plansBefFragm);
		System.out.println("trips:  \t" + population.getPersons().size());
		System.out.println("PTconnections:\t" + (population.getPersons().size()- directWalks));
		System.out.println("directWalks:  \t" + directWalks);
		System.out.println("travelTime:  \t" + travelTime);
		System.out.println("travelDist:  \t" + travelDistance);
		System.out.println("transfers:   \t" + transfers);
		System.out.println("detTransfers:\t" + detTransfers);
		System.out.println("walkTime:    \t" + walkTime);
		System.out.println("walkDistance:\t" + walkDistance);
		System.out.println("waitTime:\t" + waitTime);

		String TAB= "\t";
		try { 
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(scenario.getConfig().controler().getOutputDirectory()+ "/" + outputFile + ".txt")); 
			bufferedWriter.write("plansBefFragm:\ttrips\tPTconnections:\tdirectWalks\travelTime:\ttravelDist:\ttransfers:\tdetTransfers:\twalkTime:\twalkDistance:\twaitTime:\n");
			bufferedWriter.write("\n" + plansBefFragm + TAB + population.getPersons().size() + TAB + (population.getPersons().size()- directWalks) + TAB+ directWalks + TAB + travelTime + TAB + travelDistance + TAB + transfers + TAB + detTransfers + TAB + walkTime + TAB + walkDistance + TAB + waitTime); 
			bufferedWriter.close(); 
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	private static void loadManyScenarios(String configFile){
		File folder = new File ("../playgrounds/mmoyo/output/fouth");
		String PREFIX = "routedPlan";
		for (int i=0; i< folder.list().length ; i++){
			String file = folder.list()[i];
			if (file.startsWith(PREFIX)){
				System.out.println ("\n\n\n\n\n\n\n" + folder + "/" + file);
				ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
				ScenarioImpl scenario = scenarioLoader.getScenario();
				scenario.getConfig().plans().setInputFile(folder + "/" + file);
				scenarioLoader.loadScenario();
				new PopulationPtAnalyzer (scenario, "PTanalysis_" + file).run();
			}
		}
	}
	
	private static void loadOneScenario(String configFile){
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = scenarioLoader.getScenario();
		scenarioLoader.loadScenario();
		new PopulationPtAnalyzer (scenario, "results.txt").run();
	}
	
	public static void main(String[] args) throws IOException {
		String configFile = null;
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../playgrounds/mmoyo/output/runs/config.xml";
		}
		
		//for many scenarios resulted from incrementing time priority
		//loadManyScenarios(configFile);

		//for one scenario
		loadOneScenario(configFile);
		
	}
}
