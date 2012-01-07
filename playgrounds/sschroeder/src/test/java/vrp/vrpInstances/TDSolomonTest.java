package vrp.vrpInstances;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.matsim.contrib.freight.vrp.basics.Coordinate;

import vrp.vrpInstances.TDSolomon.MyLocations;
import vrp.vrpInstances.TDSolomon.TDCosts;

public class TDSolomonTest extends TestCase{

	TDCosts tdCosts;
	
	MyLocations loc;
	
	public void setUp(){
		loc = new MyLocations();
		loc.addLocation("1", new Coordinate(0,0));
		loc.addLocation("2", new Coordinate(20,0));
		double depotClosingTime = 100.0;
		List<Double> timeBins = new ArrayList<Double>();
		timeBins.add(0.2*depotClosingTime);
		timeBins.add(0.4*depotClosingTime);
		timeBins.add(0.6*depotClosingTime);
		timeBins.add(0.8*depotClosingTime);
		timeBins.add(1.0*depotClosingTime);
		
		List<Double> speedValues = new ArrayList<Double>();
		speedValues.add(1.0);
		speedValues.add(2.0);
		speedValues.add(1.0);
		speedValues.add(2.0);
		speedValues.add(1.0);
		tdCosts = new TDCosts(loc, timeBins, speedValues);
	}
	
	public void testTravelTimeWithinTimeBin(){
		assertEquals(20.0, tdCosts.getGeneralizedCost("1", "2", 0.0));
	}
	
	public void testTravelTimeViaTwoTimeBins(){
		assertEquals(15.0, tdCosts.getGeneralizedCost("1", "2", 10.0));
	}
	
	public void testTravleTimeViaThreeTimeBins(){
		loc.addLocation("3", new Coordinate(0,0));
		loc.addLocation("4", new Coordinate(80,0));
		assertEquals(55.0, tdCosts.getGeneralizedCost("3", "4", 10.0));
	}
	
	public void testBackwardsTravelTime(){
		assertEquals(20.0, tdCosts.getBackwardGeneralizedCost("1", "2", 20.0));
	}
	
	public void testTravelBackwardsTTViaTwoTimeBins(){
		assertEquals(15.0, tdCosts.getBackwardGeneralizedCost("1", "2", 25.0));
	}
	
	public void testBackwardsTravelTimeViaThreeTimeBins(){
		loc.addLocation("3", new Coordinate(0,0));
		loc.addLocation("4", new Coordinate(80,0));
		assertEquals(55.0, tdCosts.getBackwardGeneralizedCost("3", "4", 65.0));
	}
}
