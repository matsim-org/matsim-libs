package playground.balac.avignon;

import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class ScoreStatistics {
	
	
	public void run(String input, String attributes) throws IOException {
		double centerX = 683217.0; 
		double centerY = 247300.0;
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).parse(attributes);	
	//	for (String plansFilePath : input) {
			ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

			PopulationReader populationReader = new MatsimPopulationReader(scenario);
			populationReader.readFile(input);
	
			int count = 0;
			
			double sum = 0.0;
		
			ArrayList<Double> a = new ArrayList<Double>();
			Population pop = scenario.getPopulation();	
			for (Person p:pop.getPersons().values()) {
				
				
				
				if (bla.getAttribute(p.getId().toString(), "subpopulation") == null && ((PersonImpl)p).isEmployed()) {
					
					//boolean grocery = false;
					for (PlanElement pe: p.getSelectedPlan().getPlanElements()) {
						if (pe instanceof Activity) {
							if (((Activity) pe).getType().equals("shopgrocery")) {
								//grocery = true;
							}
						}
					}
					//if (grocery) {
						a.add(p.getSelectedPlan().getScore());
						sum += p.getSelectedPlan().getScore();
						count++;
					//}
				}
			}
			
			double average = sum / (double)count;
			sum = 0.0;
			for (double d: a) {
				
				sum += Math.pow(d - average, 2);
				
			}
			double variance = sum/(double)a.size();
			double standardDeviation = Math.sqrt(variance);
			
			System.out.println(average);
			System.out.println(variance);
			System.out.println(standardDeviation);
		//}
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		ScoreStatistics m = new ScoreStatistics();
		m.run(args[0], args[1]);
	}
	
}
