package patryk.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;


public class HomeActivityEndCollector {
	
	private final static String EVENTSFILE = "600.events.xml.gz";
	private final static String ENDTIMESFILE = "homeActEndTimes.txt"; 
	
	public static void main(String[] args) {
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		HomeActivityEnd handler = new HomeActivityEnd();
		eventsManager.addHandler(handler);

		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(EVENTSFILE);
		
        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile("600.plans.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
		
		ArrayList<Double> leaveHomeTimes = handler.getLeaveHomeTimes();
		ArrayList<Id<Person>> personIDs = handler.getPersonIDs();
		 
		 try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(ENDTIMESFILE, false)))) {
		   for (int i=0; i<leaveHomeTimes.size(); i++) {
			   Person person = population.getPersons().get(personIDs.get(i));
			   Activity act = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			   Coord homeCoord = act.getCoord();
			   out.println(homeCoord.getX() + ";" + homeCoord.getY() + ";" + leaveHomeTimes.get(i));
		   }
		}catch (IOException e) {
		    // ...
		}			

	}

}
