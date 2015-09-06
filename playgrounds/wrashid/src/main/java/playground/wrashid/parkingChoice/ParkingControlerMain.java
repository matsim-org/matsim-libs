package playground.wrashid.parkingChoice;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;

//TODO: class only used in tests -> remove this class and incorporate code directly in tests...

public class ParkingControlerMain {
public static void main(String[] args) {
		
		Controler controler=new Controler(args);
		
		//FlatParkingFormatReaderV1 flatParkingFormatReaderV1 = new FlatParkingFormatReaderV1();
		//flatParkingFormatReaderV1.parse("");
		
//		LinkedList<Parking> parkingCollection= flatParkingFormatReaderV1.getParkings();
		
		LinkedList<PParking> parkingCollection= new LinkedList<PParking>();
		
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				ParkingImpl parking = new ParkingImpl(new Coord((double) (i * 1000 + 500), (double) (j * 1000 + 500)));
				parking.setMaxCapacity(30000.0);
				parkingCollection.add(parking);
			}
		}
		
		ParkingModule parkingModule=new ParkingModule(controler, parkingCollection);

	controler.getConfig().controler().setOverwriteFileSetting(
			true ?
					OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
					OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

	controler.run();
		
	}
}
