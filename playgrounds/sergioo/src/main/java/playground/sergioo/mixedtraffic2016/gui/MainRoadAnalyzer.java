package playground.sergioo.mixedtraffic2016.gui;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;

import playground.sergioo.mixedtraffic2016.RoadVehiclesAnalyzer;

public class MainRoadAnalyzer extends Thread {

	public MainRoadAnalyzer(Road road) {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws Exception {
		File configFile = new File(args[0]);
		Scenario scenario =ScenarioUtils.createScenario(ConfigUtils.loadConfig(configFile.getAbsolutePath()));
		EventsManager events = EventsUtils.createEventsManager();
		Id<Link> linkId = Id.createLinkId(args[1]);
		RoadVehiclesAnalyzer analyzer = new RoadVehiclesAnalyzer(linkId);
		events.addHandler(analyzer);
		new MatsimEventsReader(events).readFile(args[2]);
		File networkFile = new File(configFile.getParentFile(),scenario.getConfig().network().getInputFile());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile.getAbsolutePath());
		Link link = scenario.getNetwork().getLinks().get(linkId);
		File vehiclesFile = new File(configFile.getParentFile(),scenario.getConfig().vehicles().getVehiclesFile());
		new VehicleReaderV1(scenario.getVehicles()).readFile(vehiclesFile.getAbsolutePath());
		Road road = new Road(scenario.getConfig().qsim().getLinkDynamics()==LinkDynamics.FIFO?Road.TypeRoad.MODE_SHARED:Road.TypeRoad.MODE_INDEPENDENT, scenario.getVehicles().getVehicleTypes().values(), link.getLength(), link.getFreespeed(), analyzer.vehicles.values());
		Animation animation = new Animation(road);
		Window window = new Window(road, animation);
		window.setVisible(true);
		animation.setWindow(window);
		animation.start();
	}
	
}
