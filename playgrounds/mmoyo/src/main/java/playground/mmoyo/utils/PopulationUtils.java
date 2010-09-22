package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;

public class PopulationUtils {
	private ScenarioImpl scenario;
	
	public PopulationUtils(ScenarioImpl scenario){	
		this.scenario = scenario;
	}
	
	public void SortAndListPersons(){
		Set<Id> keySet = this.scenario.getPopulation().getPersons().keySet();
		TreeSet<Id> treeSet = new TreeSet<Id>(keySet);
		
		String SEPARATOR = ":\t ";
		int zeroFound =0;
		for (Id id : treeSet){
			double score = this.scenario.getPopulation().getPersons().get(id).getSelectedPlan().getScore();
			System.out.println(id +  SEPARATOR + Double.toString(score));
			if (score==0){
				zeroFound++;
				try {
					throw new RuntimeException("Score equals zero" );
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("Number of persons: " + treeSet.size());
		System.out.println("scores that equal zero: " + zeroFound);
		
	}
	
	//writes a planFile only with the desired agent
	public void CreatePersonFile(String strPersonId){
		if (scenario.getNetwork()==null){
			throw new NullPointerException("The network could not be found in the scenario" );
		}
		PopulationImpl outputPopulation = new PopulationImpl(new ScenarioImpl());
		outputPopulation.addPerson(this.scenario.getPopulation().getPersons().get(new IdImpl(strPersonId)));
		PopulationWriter popwriter = new PopulationWriter(outputPopulation, this.scenario.getNetwork());
		popwriter.write(scenario.getConfig().controler().getOutputDirectory() + "/person" + strPersonId + ".xml");
	}
	
	public static void main(String[] args) {
		String configFile;
		if (args.length>0){
			configFile = args[0];
		}else{
			configFile = "../../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";	
		}
		System.out.println("counting population for config file: " + configFile);
		ScenarioImpl scenario = new TransScenarioLoader().loadScenario(configFile);
		PopulationUtils planUtils = new PopulationUtils(scenario);
		planUtils.SortAndListPersons();
		//planUtils.CreatePersonFile("19142131");
	}
	
}
