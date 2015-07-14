package playground.sergioo.ptVehiclesEditor2012;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

public class VehicleTypeChanger {
	
	private enum MODES_VEHICLES {
		BUS(new String[]{"simpleBusVehicle"}),
		SUBWAY(new String[]{"simpleMrtVehicle"}),
		TRAM(new String[]{"simpleLrtVehicle"}),
		RAIL(new String[]{"simpleLrtVehicle"});
		public String[] vehicleTypes;
		private MODES_VEHICLES(String[] vehicleTypes) {
			this.vehicleTypes = vehicleTypes;
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		TransitScheduleReader readerTransit = new TransitScheduleReader(scenario);
		readerTransit.readFile(args[0]);
		Vehicles vehicles = ((ScenarioImpl)scenario).getTransitVehicles();
		VehicleReaderV1 readerVehicles = new VehicleReaderV1(vehicles);
		readerVehicles.readFile(args[1]);
		TransitSchedule transitSchedule = ((ScenarioImpl)scenario).getTransitSchedule();
		/*for (TransitLine line : transitSchedule.getTransitLines().values())
			for (TransitRoute route : line.getRoutes().values())
				for(MODES_VEHICLES mode_vehicles:MODES_VEHICLES.values())
					if(route.getTransportMode().trim().equals(mode_vehicles.name().toLowerCase()))
						for (Departure departure : route.getDepartures().values())
							((VehicleImpl)vehicles.getVehicles().get(departure.getVehicleId())).setType(vehicles.getVehicleTypes().get(Id.create(mode_vehicles.vehicleTypes[(int) (mode_vehicles.vehicleTypes.length*Math.random())], VehicleType.class)));*/
		VehicleWriterV1 writer2 = new VehicleWriterV1(vehicles);
		writer2.writeFile(args[2]);
	}

}