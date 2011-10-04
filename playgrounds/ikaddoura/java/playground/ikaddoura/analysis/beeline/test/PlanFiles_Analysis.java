package playground.ikaddoura.analysis.beeline.test;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class PlanFiles_Analysis {

	String netFile = "input/output_network.xml";
	String plansFile1 = "input/0.plans.xml";
	String plansFile2 = "input/500.plans.xml";
			
	public static void main(String[] args) {
		PlanFiles_Analysis analyse = new PlanFiles_Analysis();
		analyse.run();
		}
	
	public void run() {
		
		Population population1 = getPopulation(netFile, plansFile1);
		Population population2 = getPopulation(netFile, plansFile2);

		SortedMap<String,Modus> modiMap1 = getModiMap(population1);
		SortedMap<String,Modus> modiMap2 = getModiMap(population2);
		
		TextFileWriter writer = new TextFileWriter();
		for (Modus modus : modiMap1.values()){
			writer.writeFile("0.plans", modus.getLuftlinien(), modus.getDistances(), modus.getModeName());
		}
		for (Modus modus : modiMap2.values()){
			writer.writeFile("500.plans", modus.getLuftlinien(), modus.getDistances(), modus.getModeName());
		}
		
	}

//------------------------------------------------------------------------------------------------------------------------
	
	private Population getPopulation(String netFile, String plansFile){
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();
		return population;
	}
	
	private SortedMap<String, Modus> getModiMap(Population population) {
		SortedMap<String,Modus> modiMap = new TreeMap<String, Modus>();
		for(Person person : population.getPersons().values()){
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				if (pE instanceof Leg){
					Leg leg = (Leg) pE;
					String mode = leg.getMode();
					if (modiMap.containsKey(mode)){
						//nothing
					}
					else {
						Modus modus = new Modus(mode);
						modus.setLuftlinien(population);
						modus.setDistances(population);
						modiMap.put(mode,modus);
					}
				}
			}
		}
		return modiMap;
	}

}
