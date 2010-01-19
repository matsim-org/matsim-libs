package playground.mmoyo.analysis;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;

/**reads a multi-modal network and eliminates agents outside the street network area*/
public class PopulationFilter {
	private double minX;
	private double maxX;
	private double minY;
	private double maxY;
	
	private String STREET_PREFIX = "miv_";
	
	private void run (Scenario scenario){
		//set street net work coordinates 
		boolean first = true;
		for (Link link: scenario.getNetwork().getLinks().values()){
			if (link.getId().toString().startsWith(STREET_PREFIX)){
				double x= link.getFromNode().getCoord().getX(); 
				double y= link.getFromNode().getCoord().getY();
				//link.getCoord();
				if (!first){
					if (x<minX)minX=x;
					if (x>maxX)maxX=x;
					if (y<minY)minY=y;
					if (y>maxY)maxY=y;
				}else{
					minX = x;
					maxX = x;
					minY = y;
					maxY = y;
					first= false;
				}
			}
		}
	
		//eliminates plans with acts outside the region
		Population population = scenario.getPopulation();
		List<Id> wrongPersons = new ArrayList<Id>();
		for (Person person : population.getPersons().values()){
			for (Plan plan :person.getPlans()){
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						double x = act.getCoord().getX(); 
						double y = act.getCoord().getY();
						if (x< minX || x > maxX || y< minY || y>maxY){
							wrongPersons.add(person.getId());
						}
					}
				}
			}
		}
		for (Id id : wrongPersons){
			population.getPersons().remove(id);
		}
		
	}
	
	public static void main(String[] args) {
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/NullFallAlles/configRouted.xml";
		
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = scenarioLoader.getScenario();
		scenarioLoader.loadScenario();
		PopulationFilter populationFilter = new PopulationFilter();
		populationFilter.run(scenario);
		
		String outputFile = scenario.getConfig().plans().getInputFile() + ".insideArea.xml";
		System.out.println("writing output plan file..." + outputFile);
		PopulationWriter popwriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		popwriter.write(outputFile) ;
		System.out.println("done");

	}

}
