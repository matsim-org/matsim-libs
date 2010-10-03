package playground.mmoyo.utils;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

public class DataLoader {

	public ScenarioImpl loadScenarioWithTrSchedule(final String configFile) {
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
	
	public ScenarioImpl loadScenario(final String configFile) {
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = scenarioLoader.getScenario();
		scenarioLoader.loadScenario();
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		return scenario;	
	}
	
	public TransitSchedule readTransitSchedule(final String networkFile, final String transitScheduleFile) {
		ScenarioImpl scenario = new ScenarioImpl();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);
		return readTransitSchedule(scenario.getNetwork(), transitScheduleFile);
	}
	
	public TransitSchedule readTransitSchedule(final NetworkImpl network, final String transitScheduleFile) {
		TransitSchedule transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 transitScheduleReaderV1 = new TransitScheduleReaderV1(transitSchedule, network);
		try {
			transitScheduleReaderV1.readFile(transitScheduleFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return transitSchedule;
	}
	
	public NetworkImpl readNetwork (final String networkFile){
		ScenarioImpl scenario = new ScenarioImpl();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);
		return scenario.getNetwork();
	}

	public Population readPopulation(final String populationFile){ 
		ScenarioImpl scenario = new ScenarioImpl();
		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(populationFile);
		return scenario.getPopulation();
	}
	
	public ScenarioImpl readScenario (final String configFile){
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
		scenarioLoader.loadScenario();
		return scenarioLoader.getScenario();
	}
	

}

