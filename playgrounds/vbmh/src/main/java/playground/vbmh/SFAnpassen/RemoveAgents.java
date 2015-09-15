package playground.vbmh.SFAnpassen;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import playground.vbmh.vmEV.EVControl;

import java.util.Random;

public class RemoveAgents {

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		String outputFileName = "input/SF_Brookings/Pop_Brookings_evs.xml";
		EVControl evControl = new EVControl();
		evControl.startUp("input/SF_PLUS/generalinput/evs.xml", null);
		int anzahl_agents = 1500;
		Random zufall = new Random();
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("input/SF_Brookings/config.xml"));
		Scenario schreib_scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("input/Schreiben/config_SF_SCHREIBEN_LEER.xml"));
        Population population = PopulationUtils.createPopulation(((ScenarioImpl) schreib_scenario).getConfig(), ((ScenarioImpl) schreib_scenario).getNetwork());
		int i = 0;
		for (Person p : scenario.getPopulation().getPersons().values()) {
			Person pa = p;
			//System.out.println(pa.getCarAvail());
			if(evControl.hasEV(pa.getId())){
				ActivityImpl act = (ActivityImpl) pa.getSelectedPlan().getPlanElements().get(0);
				if(act.getFacilityId().toString().contains("B")){

				population.addPerson(pa);
				
				System.out.println("Brookins ev gefunden");
				i+=1;
				}
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
