package playground.mmoyo.utils;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.xml.sax.SAXException;

public class DataLoader {

	//this is to be used when one need to work with a scenario outside a controler. v.gr. at util classes
	public ScenarioImpl loadScenarioWithTrSchedule(final String configFile) {
		ScenarioImpl scenario = this.loadTransitScenario(configFile);

		//load transit schedule by config
		TransitSchedule schedule = scenario.getTransitSchedule();
		try {
			new TransitScheduleReaderV1(schedule, scenario.getNetwork(), scenario).parse(scenario.getConfig().getParam("transit", "transitScheduleFile"));
		} catch (SAXException e) {e.printStackTrace();
		} catch (ParserConfigurationException e) {e.printStackTrace();
		} catch (IOException e) {e.printStackTrace();
		}
		new CreateVehiclesForSchedule(schedule, scenario.getVehicles()).run();

		return scenario;
	}

	//Use this with the controler. The transit schedule will be loaded by controler, but the config object is needed as parameter
	public ScenarioImpl loadTransitScenario(final String configFile) {
		ScenarioImpl scenario = this.loadScenario(configFile);
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		return scenario;
	}

	public TransitSchedule readTransitSchedule(final String networkFile, final String transitScheduleFile) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);
		TransitSchedule schedule = readTransitSchedule(scenario.getNetwork(), transitScheduleFile);
		scenario = null;
		matsimNetReader = null;
		return schedule;
	}

	public TransitSchedule readTransitSchedule(final NetworkImpl network, final String transitScheduleFile) {
		TransitScheduleFactoryImpl transitScheduleFactoryImpl = new TransitScheduleFactoryImpl();
		TransitSchedule transitSchedule = transitScheduleFactoryImpl.createTransitSchedule();
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
		transitScheduleFactoryImpl = null;
		transitScheduleReaderV1 = null;
		return transitSchedule;
	}

	public NetworkImpl readNetwork (final String networkFile){
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);
		NetworkImpl network = scenario.getNetwork(); 
		scenario = null;
		matsimNetReader = null;
		return network;
	}

	public Population readPopulation(final String populationFile){
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(populationFile);
		Population population = scenario.getPopulation();
		scenario = null;
		popReader = null;
		return population;
	}

	public ScenarioImpl readNetwork_Population(String networkFile, String populationFile) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(populationFile);

		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);
		return scenario;
	}

	public ScenarioImpl loadScenario (final String configFile){
		ScenarioLoaderImpl scenarioLoader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(configFile);
		scenarioLoader.loadScenario();
		return scenarioLoader.getScenario();
	}

	//returns a transitRoute object of the schedule
	public TransitRoute getTransitRoute(final String strRouteId, final TransitSchedule schedule){
		Id lineId = new IdImpl(strRouteId.split("\\.")[0]);
		return schedule.getTransitLines().get(lineId).getRoutes().get(new IdImpl(strRouteId));
	}

	public Counts readCounts (String countFile){
		Counts counts = new Counts();
		MatsimCountsReader matsimCountsReader = new MatsimCountsReader(counts);
		matsimCountsReader.readFile(countFile);
		matsimCountsReader = null;
		return counts;
	}

}