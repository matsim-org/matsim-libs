package playground.artemc.networkTools;

import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

public class TransitScheduleModifier {

	private ScenarioImpl sc;	
	private NetworkImpl network;
	private TransitSchedule transitSchedule;
	public ScenarioImpl getSc() {
		return sc;
	}

	public TransitSchedule getTransitSchedule() {
		return transitSchedule;
	}

	private TransitScheduleFactory transitScheduleFactory = new TransitScheduleFactoryImpl();

	public static void main(String[] args) throws IOException {

		String inputNetwork = args[0];
		String inputTransitSchedule = args[1];
		String inputVehicle = args[2];
		String outputTransitSchedule = args[3];
		String outputVehicles = args[4];
		String serviceInterval = args[5];

		TransitScheduleModifier modifier = new TransitScheduleModifier(inputNetwork, inputTransitSchedule, inputVehicle);
		modifier.changeHeadway(serviceInterval);
		
		/*Create vehicles*/
		modifier.createVehicles(modifier.getTransitSchedule());
		
		/*Write created transit schedule and vehicle files*/
		new TransitScheduleWriter(modifier.getTransitSchedule()).writeFile(outputTransitSchedule);
		new VehicleWriterV1(modifier.getSc().getTransitVehicles()).writeFile(outputVehicles);
	}

	private void changeHeadway(String newServiceInterval) {
		
		double arrival = Time.parseTime("00:00:00");
		double startTime = Time.parseTime("24:00:00");
		double endTime = Time.parseTime("00:00:00");
		double frequency = Time.parseTime(newServiceInterval);


		for (TransitLine line : this.transitSchedule.getTransitLines().values()) {
			
			String transitLineName = line.getId().toString();
			
			for (TransitRoute route : line.getRoutes().values()) {
				
				String mode = route.getTransportMode();
				ArrayList<Departure> departureList = new ArrayList<Departure>();
				
				for (Departure departure : route.getDepartures().values()) {
					departure.getDepartureTime();
					if(departure.getDepartureTime() < startTime) startTime = departure.getDepartureTime();
					if(departure.getDepartureTime() > endTime) endTime = departure.getDepartureTime();
					departureList.add(departure);
				}
				
				for(Departure departure:departureList){
					route.removeDeparture(departure);
				}				

				
				int id = 1;
				for(Double time = startTime; time<endTime; time=time+frequency){
					Departure vehicleDeparture = transitScheduleFactory.createDeparture(Id.create(id, Departure.class), time);
					vehicleDeparture.setVehicleId(Id.create(mode+"_"+transitLineName+"_"+id, Vehicle.class));
					route.addDeparture(vehicleDeparture);
					id++;
				}
			}
		}
	}

	public TransitScheduleModifier(String networkFile, String transitScheduleFile, String vehicleFile){
		this.sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config config = this.sc.getConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);

		new MatsimNetworkReader(sc).readFile(networkFile);
		this.network = (NetworkImpl) sc.getNetwork();

		this.transitSchedule = readTransitSchedule(this.network, transitScheduleFile);
	
		//Vehicles vehicles = sc.getVehicles();
		//new VehicleReaderV1(vehicles).readFile(vehicleFile);
		
	}

	public static TransitSchedule readTransitSchedule(Network network, String transitScheduleFile) {
		TransitSchedule transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 transitScheduleReaderV1 = new TransitScheduleReaderV1(transitSchedule, network);
		transitScheduleReaderV1.readFile(transitScheduleFile);
		return transitSchedule;
	}
	
	private void createVehicles(TransitSchedule ts) {
		Vehicles vehicles = this.sc.getTransitVehicles();
		VehiclesFactory vehicleFactory = vehicles.getFactory();
		VehicleType standardBus = vehicleFactory.createVehicleType(Id.create("Bus MAN NL323F", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(38));
		capacity.setStandingRoom(Integer.valueOf(52));
		standardBus.setCapacity(capacity);
		standardBus.setAccessTime(1.0);
		standardBus.setEgressTime(1.0);
		vehicles.addVehicleType(standardBus);
		for (TransitLine line : ts.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					Vehicle veh = vehicleFactory.createVehicle(departure.getVehicleId(), standardBus);
					vehicles.addVehicle(veh);
				}
			}
		}		
	}
}



