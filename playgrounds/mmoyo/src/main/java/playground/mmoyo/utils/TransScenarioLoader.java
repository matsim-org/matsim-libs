package playground.mmoyo.utils;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.xml.sax.SAXException;

public class TransScenarioLoader {

	public ScenarioImpl loadScenarioWithTrSchedule(String configFile) {
		ScenarioImpl scenario = this.loadScenario(configFile);

		//load transit schedule by config
		TransitSchedule schedule = scenario.getTransitSchedule();
		try {
			new TransitScheduleReaderV1(schedule, scenario.getNetwork()).parse(scenario.getConfig().getParam("transit", "transitScheduleFile"));
		} catch (SAXException e) {e.printStackTrace();
		} catch (ParserConfigurationException e) {e.printStackTrace();
		} catch (IOException e) {e.printStackTrace();
		}
		new CreateVehiclesForSchedule(schedule, scenario.getVehicles()).run();

		return scenario;	
	}
	
	public ScenarioImpl loadScenario(String configFile) {
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = scenarioLoader.getScenario();
		scenarioLoader.loadScenario();
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		return scenario;	
	}
	
}
