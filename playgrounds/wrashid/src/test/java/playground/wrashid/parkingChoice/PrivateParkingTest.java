package playground.wrashid.parkingChoice;

import java.util.LinkedList;

import junit.framework.TestCase;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public class PrivateParkingTest extends TestCase {

	public void testBaseCase(){
		assertEquals(2410, walkingDistanceFor3CarScenarioWithVariableParkingCapacity(1),5.0);
	}
	
	public void testHigherParkingCapacityMakesWalkingDistanceShorter(){
		assertEquals(998, walkingDistanceFor3CarScenarioWithVariableParkingCapacity(3),5.0);
	}
	
	public void testMakingTheCapacityHigherThanNumberOfCarsWillNotMakeWalkingDistanceShorter(){
		assertEquals(998, walkingDistanceFor3CarScenarioWithVariableParkingCapacity(10),5.0);
	}
	
	private double walkingDistanceFor3CarScenarioWithVariableParkingCapacity(int parkingCapacity) {
		Config config=ConfigUtils.loadConfig("test/input/playground/wrashid/parkingChoice/chessConfig.xml");
		Controler controler=new Controler(config);
		
		// setup parking infrastructure
		LinkedList<Parking> parkingCollection = new LinkedList<Parking>();

		for (int i=0;i<10;i++){
			for (int j=0;j<10;j++){
				ParkingImpl parking = new ParkingImpl(new CoordImpl(i*1000+500,j*1000+500));
				parking.setMaxCapacity(parkingCapacity);
				parkingCollection.add(parking);
			}
		}
			 
		PrivateParking privateParking=new PrivateParking(new CoordImpl(8500.0,9000),new ActInfo(new IdImpl(36), "work"));
		privateParking.setMaxCapacity(parkingCapacity);
		parkingCollection.add(privateParking);
		
		
		ParkingModule parkingModule = new ParkingModule(controler,parkingCollection);
		
		controler.setOverwriteFiles(true);
		
		controler.run();
		
		return parkingModule.getAverageWalkingDistance();
	}
	
	public void testChagingTheActTypeOfPrivateParkingShouldLeadToLongerWalkingDistances(){
		Config config=ConfigUtils.loadConfig("test/input/playground/wrashid/parkingChoice/chessConfig.xml");
		Controler controler=new Controler(config);
		
		// setup parking infrastructure
		LinkedList<Parking> parkingCollection = new LinkedList<Parking>();

		for (int i=0;i<10;i++){
			for (int j=0;j<10;j++){
				ParkingImpl parking = new ParkingImpl(new CoordImpl(i*1000+500,j*1000+500));
				parking.setMaxCapacity(1);
				parkingCollection.add(parking);
			}
		}
			 
		PrivateParking privateParking=new PrivateParking(new CoordImpl(8500.0,9000),new ActInfo(new IdImpl(36), "home"));
		privateParking.setMaxCapacity(1);
		parkingCollection.add(privateParking);
		
		
		ParkingModule parkingModule = new ParkingModule(controler,parkingCollection);
		
		controler.setOverwriteFiles(true);
		
		controler.run();
		
		assertEquals(3155,parkingModule.getAverageWalkingDistance(),5.0);
	}
	
}
