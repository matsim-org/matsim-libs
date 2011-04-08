package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.matsim.api.core.v01.Id;

import junit.framework.TestCase;

public class testSchedule extends TestCase{

	
	private Schedule s;
	
	public testSchedule(){		
		
	}
	
	
	
	public void testScheduleMethods(){
		s= setDummySchedule();
		
		assertEquals(50, s.getTotalTimeOfIntervalsInSchedule());
		
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
		
		assertEquals(s.sameTimeIntervalsInThisSchedule(s), true);
		assertEquals(s.sameTimeIntervalsInThisSchedule(s2), false);
		
		
		
		System.out.println("Test Insert Charging Schedule intob bigParkingInterval method");
		Schedule testInsertChargingSchedule= setBigDummySchedule();
		System.out.println("parkinInterval");
		testInsertChargingSchedule.printSchedule();
		
		System.out.println("to insert");
		setDummyChargingSchedule().printSchedule();
		
		System.out.println("result");
		testInsertChargingSchedule.insertChargingIntervalsIntoParkingIntervalSchedule(setDummyChargingSchedule());
		testInsertChargingSchedule.printSchedule();
		
		assertEquals(5, testInsertChargingSchedule.getNumberOfEntries());
		assertEquals(testInsertChargingSchedule.timesInSchedule.get(0).getIntervalLength(), 5.0);
		assertEquals(testInsertChargingSchedule.timesInSchedule.get(4).getIntervalLength(), 50.0);
	}
	
	
	
	
	
	public Schedule setDummySchedule(){
		Schedule s1= new Schedule();
		
		s1.addTimeInterval(new ParkingInterval(30, 50, null));
		s1.addTimeInterval(new ParkingInterval(0, 10, null));
		s1.addTimeInterval(new DrivingInterval(10,15, 100) );
		s1.addTimeInterval(new ParkingInterval(15, 30, null));
		
		return s1;
	}
	
	
	public Schedule setDummyChargingSchedule(){
		Schedule s1= new Schedule();
		
		s1.addTimeInterval(new ChargingInterval(30, 50));
		s1.addTimeInterval(new ChargingInterval(5, 10));
		
		s1.sort();
		
		return s1;
	}
	
	
	public Schedule setBigDummySchedule(){
		Schedule s1= new Schedule();
		ParkingInterval p= new ParkingInterval(0, 100, null);
		
		s1.addTimeInterval(p);
		
		return s1;
		
	}
	
	public TimeInterval setDummyInterval(){
		
		return new ParkingInterval(50, 70, null);
		
		
	}
	
}
