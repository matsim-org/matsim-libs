package playground.mmoyo.analysis.counts.rieser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.xml.sax.SAXException;

import playground.mrieser.pt.analysis.RouteOccupancy;
import playground.mrieser.pt.analysis.VehicleTracker;

public class OccupancyCounts {

	private static final String SERVERNAME = "OcuppancyCounter";

	public static void play(final ScenarioImpl scenario, final EventsManager events) {
		scenario.getConfig().simulation().setSnapshotStyle("queue");
		final QSim sim = new QSim(scenario, ((EventsManagerImpl) events));
		sim.getQSimTransitEngine().setUseUmlaeufe(true);
		sim.run();
	}

	public static void main(final String[] args) throws SAXException, ParserConfigurationException, IOException {
		String configFile = args[0]; 
		String transitLineStrId = args[1];
		String transitRouteStrId1 = args[2];
		String transitRouteStrId2 = args[3];
		
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(configFile);
		ScenarioImpl scenario = sl.getScenario();

		NetworkImpl network = scenario.getNetwork();
		network.getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		sl.loadScenario();
		scenario.getConfig().simulation().setSnapshotPeriod(0.0);
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		//scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());

		new TransitScheduleReaderV1(scenario.getTransitSchedule(), scenario.getNetwork()).parse(scenario.getConfig().getParam("transit", "transitScheduleFile"));
		new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getVehicles()).run();

		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()){
			System.out.println("line :"  +  line.getId());
				for (TransitRoute route: line.getRoutes().values()){
					System.out.println("		route : "  +  route.getId());
				}
		}
		//////////////////
		
		EventsManagerImpl events = new EventsManagerImpl();
		VehicleTracker vehTracker = new VehicleTracker();
		events.addHandler(vehTracker);
		TransitRoute route1 = scenario.getTransitSchedule().getTransitLines().get(new IdImpl(transitLineStrId)).getRoutes().get(new IdImpl(transitRouteStrId1));
		TransitRoute route2 = scenario.getTransitSchedule().getTransitLines().get(new IdImpl(transitLineStrId)).getRoutes().get(new IdImpl(transitRouteStrId2));
		RouteOccupancy analysis1 = new RouteOccupancy(route1, vehTracker);
		RouteOccupancy analysis2 = new RouteOccupancy(route2, vehTracker);
		events.addHandler(analysis1);
		events.addHandler(analysis2);

		QSim sim = new QSim(scenario, events);
		sim.addFeature(new OTFVisMobsimFeature(sim));
		sim.run();

		///////////show and save results/////////////////////
		String[] data = new String[route1.getStops().size() + 1];
		data[0]= "stop\t#exitleaving\t#enter\t#inVehicle \n"; 
		System.out.println(data[0]);
		int inVehicle = 0;
		int i=0;
		for (TransitRouteStop stop : route1.getStops()) {
			Id stopId = stop.getStopFacility().getId();
			int enter = analysis1.getNumberOfEnteringPassengers(stopId);
			int leave = analysis1.getNumberOfLeavingPassengers(stopId);
			inVehicle = inVehicle + enter - leave;
			data[++i] = stopId + "\t" + leave + "\t" + enter + "\t" + inVehicle + "\n";
			System.out.print( data[i]);
		}

		String [] data2 = new String[route2.getStops().size() + 1];
		data2[0]= data[0];
		System.out.println(data2[0]);
		inVehicle = 0;
		i=0;
		
		for (TransitRouteStop stop : route2.getStops()) {
			Id stopId = stop.getStopFacility().getId();
			int enter = analysis2.getNumberOfEnteringPassengers(stopId);
			int leave = analysis2.getNumberOfLeavingPassengers(stopId);
			inVehicle = inVehicle + enter - leave;
			data2[++i] = stopId + "\t" + leave + "\t" + enter + "\t" + inVehicle + "\n";
			System.out.print( data2[i]);
		}
	
		try { 
			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(scenario.getConfig().controler().getOutputDirectory() + "/" + new File( scenario.getConfig().plans().getInputFile() ).getName() + "_counts.txt")); 
			 for(i = 0; i < data.length; i++) {
	              bufferedWriter.write(data[i]);
	        }    
			 bufferedWriter.write("\n");
			 for(i = 0; i < data2.length; i++) {
	              bufferedWriter.write(data2[i]);
	        }    
			bufferedWriter.close(); 
		} catch (IOException e) {
			
		} 
	}
}
