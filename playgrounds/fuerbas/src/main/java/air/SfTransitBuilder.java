package air;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesImpl;

public class SfTransitBuilder {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		SfTransitBuilder builder = new SfTransitBuilder();
		builder.createSchedule("/home/soeren/workspace/oagEuroFlights.txt");
		
//			German domestic flights only
//		builder.createSchedule("/home/soeren/workspace/oagGermanFlights.txt");
		
	}
		
	public void createSchedule(String inputOagData) throws IOException {
		
		Scenario scen = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
		Config config = scen.getConfig();
		config.network().setInputFile("/home/soeren/workspace/euroAirNetwork.xml");
//		Germany only
//		config.network().setInputFile("/home/soeren/workspace/germanAirNetwork.xml");
		ScenarioUtils.loadScenario(scen);		
		Network network = scen.getNetwork();
		scen.getConfig().scenario().setUseTransit(true);
		scen.getConfig().scenario().setUseVehicles(true);
		
		BufferedReader br = new BufferedReader(new FileReader(new File(inputOagData)));
		
		TransitScheduleFactory sf = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = sf.createTransitSchedule();
		
		Vehicles veh = new VehiclesImpl();
				
		Map<Id, List<Id>> linkListMap = new HashMap<Id, List<Id>>(); 
		Map<Id, List<TransitRouteStop>> stopListMap = new HashMap<Id, List<TransitRouteStop>>();
		Map<Id, NetworkRoute> netRouteMap = new HashMap<Id, NetworkRoute>();
		Map<Id, TransitRoute> transRouteMap = new HashMap<Id, TransitRoute>();
		
		while (br.ready()) {
			
			String oneLine = br.readLine();
			String[] lineEntries = oneLine.split("\t");
			String[] airportCodes = lineEntries[0].split("_");
			String origin = airportCodes[0];
			String destination = airportCodes[1];
			String transitRoute = lineEntries[0];
			String transitLine = lineEntries[1];
			double departureTime = Double.parseDouble(lineEntries[3]);
			Id originId = new IdImpl(origin);
			Id destinationId = new IdImpl(destination);
			Id routeId = new IdImpl(transitRoute);	//origin IATA code + destination IATA code
			Id transitLineId = new IdImpl(transitLine);		//origin IATA code + destination IATA code + airline IATA code
			Id flightNumber = new IdImpl(lineEntries[2]);	//flight number
			Id vehTypeId = new IdImpl(lineEntries[5]+"_"+lineEntries[6]);	//IATA aircraft code + seats avail
			int aircraftCapacity = Integer.parseInt(lineEntries[6]);
			List<Id> linkList = new ArrayList<Id>();	//evtl in Map mit Route als key verpacken
			List<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>();	//evtl in Map mit Route als key verpacken
			
			//nur ausführen, wenn stopListMap noch keinen entspechenden key enthält
			
			if (!stopListMap.containsKey(routeId)) {			
				TransitStopFacility transStopFacil = sf.createTransitStopFacility(originId, network.getNodes().get(originId).getCoord(), false);
				transStopFacil.setLinkId(originId);
				TransitRouteStop transStop = sf.createTransitRouteStop(transStopFacil, 0, 0);
				stopList.add(transStop);				
				TransitStopFacility transStopFacil2 = sf.createTransitStopFacility(destinationId, network.getNodes().get(destinationId).getCoord(), false);
				transStopFacil2.setLinkId(destinationId);
				TransitRouteStop transStop2 = sf.createTransitRouteStop(transStopFacil2, 0, 0);
				stopList.add(transStop2);	
				if (!schedule.getFacilities().containsKey(originId)) schedule.addStopFacility(transStopFacil);
				if (!schedule.getFacilities().containsKey(destinationId)) schedule.addStopFacility(transStopFacil2);
				stopListMap.put(routeId, stopList);
			}
				
			//nur ausführen, wenn linkListMap noch keinen entsprechenden key enthält
			
			if (!linkListMap.containsKey(routeId)) {
				linkList.add(new IdImpl(origin+"taxiOutbound"));
				linkList.add(new IdImpl(origin+"runwayOutbound"));
				linkList.add(new IdImpl(origin+destination));
				linkList.add(new IdImpl(destination+"runwayInbound"));
				linkList.add(new IdImpl(destination+"taxiInbound"));
				linkListMap.put(routeId, linkList);
			}
			
			if (!netRouteMap.containsKey(transitLineId)) {
				NetworkRoute netRoute = new LinkNetworkRouteImpl(new IdImpl(origin), new IdImpl(destination));		
				netRoute.setLinkIds(new IdImpl(origin), linkListMap.get(routeId), new IdImpl(destination));
				netRouteMap.put(transitLineId, netRoute);
			}			
			
			if (!transRouteMap.containsKey(transitLineId)) {
				TransitRoute transRoute = sf.createTransitRoute(new IdImpl(transitRoute), netRouteMap.get(transitLineId), stopListMap.get(routeId), "pt");
				transRouteMap.put(transitLineId, transRoute);
			}
						
			Departure departure = sf.createDeparture(flightNumber, departureTime);
			departure.setVehicleId(flightNumber);
			transRouteMap.get(transitLineId).addDeparture(departure);
						
			if (!schedule.getTransitLines().containsKey(transitLineId)) {
				TransitLine transLine = sf.createTransitLine(transitLineId);
				transLine.addRoute(transRouteMap.get(transitLineId));	
				schedule.addTransitLine(transLine);
			}
			
			if (!veh.getVehicleTypes().containsKey(vehTypeId)) {
				VehicleType type = veh.getFactory().createVehicleType(vehTypeId);
				VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
				cap.setSeats(aircraftCapacity);
				type.setCapacity(cap);
				veh.getVehicleTypes().put(vehTypeId, type); 
			}
			
			veh.getVehicles().put(flightNumber, veh.getFactory().createVehicle(flightNumber, veh.getVehicleTypes().get(vehTypeId)));
			
		}
		
		TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(schedule);
		scheduleWriter.write("/home/soeren/workspace/euroFlightSchedule.xml");
//		Germany only		
//		scheduleWriter.write("/home/soeren/workspace/germanFlightSchedule.xml");

		
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(veh);
		vehicleWriter.writeFile("/home/soeren/workspace/euroFlightVehicles.xml");
//		Germany only
//		vehicleWriter.writeFile("/home/soeren/workspace/GermanFlightVehicles.xml");
			
	}


}
