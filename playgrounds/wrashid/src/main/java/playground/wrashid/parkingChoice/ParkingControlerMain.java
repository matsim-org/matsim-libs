package playground.wrashid.parkingChoice;

import java.util.LinkedList;

import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

//TODO: class only used in tests -> remove this class and incorporate code directly in tests...

public class ParkingControlerMain {
public static void main(String[] args) {
		
		Controler controler=new Controler(args);
		
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
		
		ParkingModule parkingModule=new ParkingModule(controler, parkingCollection);
		
		controler.setOverwriteFiles(true);

		controler.run();
		
	}
}
