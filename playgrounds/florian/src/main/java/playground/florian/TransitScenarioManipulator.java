package playground.florian;

import java.util.TreeSet;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

class TransitScenarioManipulator {

	
	public static void main(String[] args) {
		// READ SCENARIO
		Config c = ConfigUtils.loadConfig("./config_gtfs.xml");
		ScenarioImpl sc = (ScenarioImpl)(ScenarioUtils.createScenario(c));
		new VehicleReaderV1(sc.getVehicles()).readFile("./transitVehicles_gtfs.xml");
		new TransitScheduleReader(sc).readFile("./transitSchedule_gtfs.xml");
		// MANIPULATE
		TransitScenarioManipulator tsman = new TransitScenarioManipulator(sc);
//		ScenarioImpl newSc = tsman.extractTransitVehicleTypes(new String[]{"dummy_Subway","dummy_Rail","dummy_Tram","dummy_Funicular"});
		ScenarioImpl newSc = tsman.extractTransitVehicleTypes(new String[]{"dummy_Tram"});
		// WRITE NEW SCENARIO
		VehicleWriterV1 vw = new VehicleWriterV1(newSc.getVehicles());
		vw.writeFile("./transitVehicles_new.xml");
		TransitScheduleWriter tsw = new TransitScheduleWriter(newSc.getTransitSchedule());
		tsw.writeFile("./transitSchedule_new.xml");
	}
	
	//#######################################################################################
	
	private ScenarioImpl sc;
	
	public TransitScenarioManipulator(ScenarioImpl sc){
		this.sc = sc;
	}
	
	public ScenarioImpl extractTransitVehicleTypes(String[] vehicleTypes){
		ScenarioImpl result = (ScenarioImpl) ScenarioUtils.createScenario(sc.getConfig());
		Vehicles vh = sc.getVehicles();
		TransitSchedule ts = sc.getTransitSchedule();
		TreeSet<String> vt = new TreeSet<String>();
		for(String type: vehicleTypes){
			vt.add(type);
			if(sc.getVehicles().getVehicleTypes().containsKey(new IdImpl(type))){
				VehicleType vehTyp = sc.getVehicles().getVehicleTypes().get(new IdImpl(type));
				result.getVehicles().getVehicleTypes().put(vehTyp.getId(), vehTyp);
			}else{
				System.out.println(type + " doesn't exist as a vehicletype");
			}
			
		}
		for(TransitLine tl: ts.getTransitLines().values()){
			boolean toBeAdded = false;
			TransitLine newLine = ts.getFactory().createTransitLine(tl.getId());
			for(TransitRoute tr: tl.getRoutes().values()){
				TransitRoute newRoute = ts.getFactory().createTransitRoute(tr.getId(), tr.getRoute(), tr.getStops(), tr.getTransportMode());
				for(Departure d: tr.getDepartures().values()){
					Vehicle v = vh.getVehicles().get(d.getVehicleId());
					if(vt.contains(v.getType().getId().toString())){
						toBeAdded = true;
						newRoute.addDeparture(d);
						result.getVehicles().getVehicles().put(v.getId(), v);
					}
				}
				if(toBeAdded){
					newLine.addRoute(newRoute);
					for(TransitRouteStop stop: tr.getStops()){
						if(!(result.getTransitSchedule().getFacilities().containsKey(stop.getStopFacility().getId()))){
							result.getTransitSchedule().addStopFacility(stop.getStopFacility());
						}
					}
				}
			}
			if(toBeAdded){
				result.getTransitSchedule().addTransitLine(newLine);
			}
		}		
		return result;
	}

}
