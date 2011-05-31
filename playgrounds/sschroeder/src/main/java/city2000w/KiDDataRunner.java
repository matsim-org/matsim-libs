package city2000w;

import java.io.FileNotFoundException;
import java.io.IOException;

import kid.KiDDataReader;
import kid.KiDSchema;
import kid.ScheduledTransportChain;
import kid.ScheduledVehicle;
import kid.ScheduledVehicles;
import kid.TransportLeg;
import kid.Vehicle;
import kid.filter.And;
import kid.filter.BusinessSectorFilter;
import kid.filter.Or;
import kid.filter.VehicleFilter;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;





public class KiDDataRunner {
	
	public static class MobileVehicleFilter implements VehicleFilter{

		public boolean judge(Vehicle vehicle) {
			int mobilityIndex = Integer.parseInt(vehicle.getAttributes().get(KiDSchema.VEHICLE_MOBILITY));
			if(mobilityIndex == 1){
				return true;
			}
			return false;
		}
		
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		ScheduledVehicles vehicles = new ScheduledVehicles();
		KiDDataReader kidReader = new KiDDataReader(vehicles);
		
		String directory = "/Volumes/projekte/2000-Watt-City/Daten/KiD/";
		kidReader.setVehicleFile(directory + "KiD_2002_Fahrzeug-Datei.txt");
		kidReader.setTransportChainFile(directory + "KiD_2002_Fahrtenketten-Datei.txt");
		kidReader.setTransportLegFile(directory + "KiD_2002_(Einzel)Fahrten-Datei.txt");
		kidReader.setVehicleFilter(new Or(new And(new MobileVehicleFilter(),new BusinessSectorFilter()), 
					new BusinessSectorFilter()));
		kidReader.run();
		
		Id exampleVehicleId = new IdImpl(2354048);
		ScheduledVehicle exampleVehicle = vehicles.getScheduledVehicles().get(exampleVehicleId);
		System.out.println("vehicleId=" + exampleVehicleId + " adresse=[Long=" + exampleVehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_LOCATION_GEO_LONG) + ", LAT=" + 
				exampleVehicle.getVehicle().getAttributes().get(KiDSchema.VEHICLE_LOCATION_GEO_LAT) + "]");
		for(ScheduledTransportChain sTC : exampleVehicle.getScheduledTransportChains()){
			System.out.println("transportChainId=" + sTC.getTransportChain().getId() + " chainLength=" + sTC.getTransportChain().getAttributes().get(KiDSchema.CHAIN_LENGTH));
			for(TransportLeg leg : sTC.getTransportLegs()){
				System.out.println("legId=" + leg.getId() + " length=" + leg.getAttributes().get(KiDSchema.LEG_LENGTH) + " location=[Long=" + leg.getAttributes().get(KiDSchema.LEG_QUELLADRESSE_GEO_LONG) + 
						", LAT=" + leg.getAttributes().get(KiDSchema.LEG_QUELLADRESSE_GEO_LAT) + "]");
			}
		}
	}

}
