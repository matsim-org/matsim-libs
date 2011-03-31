package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.matsim.api.core.v01.Id;

import junit.framework.TestCase;

public class testSchedule extends TestCase{

	
	private Schedule s;
	
	public testSchedule(){		
		
	}
	
	
	
	public void runTest(){
		s= setDummySchedule();
		
		s.printSchedule();
		
		// first entry as created in dummy schedule
		assertEquals(30.0, s.timesInSchedule.get(0).getStartTime());
		
		s.sort();
		assertEquals(0.0, s.timesInSchedule.get(0).getStartTime());
		
		assertEquals(s.getTotalConsumption(), 100.0);
		
		Schedule s2= new Schedule();
		s2.addTimeInterval(setDummyInterval());
		s.mergeSchedules(s2);
		
		assertEquals(s.timesInSchedule.get(s.getNumberOfEntries()-1).getEndTime(), 70.0);
		
		assertEquals(true, s.overlapWithTimeInterval(setDummyInterval()));
		
		assertEquals(false, s.overlapWithTimeInterval(new ChargingInterval(100, 110)));
		
		assertEquals(0, s.timeIsInWhichInterval(0));
		
		assertEquals(1, s.numberOfDrivingTimes());
	}
	
	
	
	
	
	public Schedule setDummySchedule(){
		Schedule s1= new Schedule();
		
		s1.addTimeInterval(new ParkingInterval(30, 50, null));
		s1.addTimeInterval(new ParkingInterval(0, 10, null));
		s1.addTimeInterval(new DrivingInterval(10,15, 100) );
		s1.addTimeInterval(new ParkingInterval(15, 30, null));
		
		return s1;
	}
	
	public TimeInterval setDummyInterval(){
		
		return new ParkingInterval(50, 70, null);
		
		
	}
	
}
