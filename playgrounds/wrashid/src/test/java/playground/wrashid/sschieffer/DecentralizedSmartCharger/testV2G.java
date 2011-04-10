package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;

import junit.framework.TestCase;

public class testV2G extends TestCase{
	
	public  V2G someV2G= new V2G(null);
	
	public testV2G(){
		
	}
	
	public void testAllV2G() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		testV2GCutSchedule();
		testV2GCutChargingSchedule();
	}
	
	
	public void testV2GCutSchedule() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule someSchedule= setDummySchedule();
		System.out.println("schedule before");
		someSchedule.printSchedule();
		someSchedule=someV2G.cutScheduleAtTime(someSchedule, 40);
		System.out.println("schedule 1st half after cut at 40");
		someSchedule.printSchedule();
		
		assertEquals(4, someSchedule.getNumberOfEntries());
		assertEquals(40.0, someSchedule.timesInSchedule.get(3).getEndTime());
		
		
		someSchedule= setDummySchedule();
		System.out.println("schedule before");
		someSchedule.printSchedule();
		someSchedule=someV2G.cutScheduleAtTimeSecondHalf(someSchedule, 40);
		System.out.println("schedule second half after cut at 40");
		someSchedule.printSchedule();
		
		assertEquals(1, someSchedule.getNumberOfEntries());
		assertEquals(50.0, someSchedule.timesInSchedule.get(0).getEndTime());
		assertEquals(40.0, someSchedule.timesInSchedule.get(0).getStartTime());
	
	}
	
	
	
	public void testV2GCutChargingSchedule(){
		
		
		Schedule someChargingSchedule= makeSimpleChargingSchedule();
		System.out.println("Schedule ");
		someChargingSchedule.printSchedule();
		
		System.out.println("cutting Schedule at 15, firstHalf");
		someChargingSchedule=someV2G.cutChargingScheduleAtTime(someChargingSchedule, 15);
		someChargingSchedule.printSchedule();
		
		assertEquals(1,someChargingSchedule.getNumberOfEntries());
		assertEquals(10.0,someChargingSchedule.timesInSchedule.get(0).getEndTime());
		
		someChargingSchedule= makeSimpleChargingSchedule();
		System.out.println("cutting Schedule at 25, firstHalf");
		someChargingSchedule=someV2G.cutChargingScheduleAtTime(someChargingSchedule, 25);
		someChargingSchedule.printSchedule();
		
		assertEquals(2,someChargingSchedule.getNumberOfEntries());
		assertEquals(10.0,someChargingSchedule.timesInSchedule.get(0).getEndTime());
		assertEquals(25.0,someChargingSchedule.timesInSchedule.get(1).getEndTime());
		assertEquals(20.0,someChargingSchedule.timesInSchedule.get(1).getStartTime());
		
		someChargingSchedule= makeSimpleChargingSchedule();
		System.out.println("cutting Schedule at 5, secondHalf");
		someChargingSchedule=someV2G.cutChargingScheduleAtTimeSecondHalf(someChargingSchedule, 5);
		someChargingSchedule.printSchedule();
		assertEquals(2,someChargingSchedule.getNumberOfEntries());
		assertEquals(10.0,someChargingSchedule.timesInSchedule.get(0).getEndTime());
		assertEquals(5.0,someChargingSchedule.timesInSchedule.get(0).getStartTime());
		assertEquals(30.0,someChargingSchedule.timesInSchedule.get(1).getEndTime());
		assertEquals(20.0,someChargingSchedule.timesInSchedule.get(1).getStartTime());
		
		
	}
	
	
	
	
	public Schedule makeSimpleChargingSchedule(){
		Schedule someChargingSchedule= new Schedule();
		someChargingSchedule.addTimeInterval(new ChargingInterval(0, 10));
		someChargingSchedule.addTimeInterval(new ChargingInterval(20, 30));
		return someChargingSchedule;
	}
	
	public Schedule setDummySchedule(){
		Schedule s1= new Schedule();
		
		s1.addTimeInterval(new ParkingInterval(0, 10, null));
		s1.addTimeInterval(new DrivingInterval(10,15, 100.0) );
		s1.addTimeInterval(new ParkingInterval(15, 30, null));
		s1.addTimeInterval(new ParkingInterval(30, 50, null));
		
		
		return s1;
	}
	
}
