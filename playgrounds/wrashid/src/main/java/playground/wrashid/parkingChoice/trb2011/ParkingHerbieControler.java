package playground.wrashid.parkingChoice.trb2011;

import java.util.LinkedList;

import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.parkingChoice.ParkingModule;
import playground.wrashid.parkingChoice.infrastructure.FlatParkingFormatReaderV1;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import herbie.running.controler.HerbieControler;

public class ParkingHerbieControler {

	public static void main(String[] args) {
		
		HerbieControler hControler=new HerbieControler(args);
		
		//FlatParkingFormatReaderV1 flatParkingFormatReaderV1 = new FlatParkingFormatReaderV1();
		//flatParkingFormatReaderV1.parse("");
		
//		LinkedList<Parking> parkingCollection= flatParkingFormatReaderV1.getParkings();
		
		LinkedList<Parking> parkingCollection= new LinkedList<Parking>();
		
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				ParkingImpl parking = new ParkingImpl(new CoordImpl(i * 1000 + 500, j * 1000 + 500));
				parking.setMaxCapacity(30000);
				parkingCollection.add(parking);
			}
		}
		
		ParkingModule parkingModule=new ParkingModule(hControler, parkingCollection);
		
//		hControler.setOverwriteFiles(true);

		hControler.run();
		
	}
	
}
