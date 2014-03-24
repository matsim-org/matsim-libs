package playground.wrashid.bsc.vbmh.SFAnpassen;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class agentsentfernen {

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		String outputFileName = "input/SF/Siouxfalls_population_reduziert_random.xml";
		int anzahl_agents = 1500;
		Random zufall = new Random();
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("input/SF/config_SF_1.xml"));
		Scenario schreib_scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("input/SF/config_SF_2.xml"));
		PopulationImpl population = new PopulationImpl((ScenarioImpl)schreib_scenario);
		int i = 0;
		for (Person p : scenario.getPopulation().getPersons().values()) {
			PersonImpl pa = (PersonImpl) p;
			System.out.println(pa.getCarAvail());
			if(pa.getCarAvail()!="never" && zufall.nextDouble()<0.02){
				population.addPerson(pa);
				
				System.out.println("Autofahrer hinzugefuegt");
				i+=1;
			}
			if(i==anzahl_agents-1){
				break;
			}
		}
		
		int j=0;
		for (Person pneu : population.getPersons().values()) {
			System.out.println(pneu.toString());
			j++;
			System.out.println(j);
		}
		PopulationWriter writer = new PopulationWriter(population, schreib_scenario.getNetwork());
		
		
		writer.writeV5(outputFileName);
		System.out.println("Achtung: Ueberschreibt nicht richtig; Ausgabedatei sollte vorher geloescht werden.");
		

	}

}
