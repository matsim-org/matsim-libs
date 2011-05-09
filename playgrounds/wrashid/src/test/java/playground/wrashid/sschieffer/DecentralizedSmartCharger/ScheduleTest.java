/* *********************************************************************** *
 * project: org.matsim.*
 * Schedule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.matsim.api.core.v01.Id;

import junit.framework.TestCase;

/**
 * the test class checks if all Schedule methods work as expected
 * <li>sort
 * <li>edit schedules 
 * <li> add Interval to existing schedule
 * <li> merge schedules
 * <li> calculate total time in schedule
 * @author Stella
 *
 */
public class ScheduleTest extends TestCase{

	
	private Schedule s;
	/**
	 * testGetTotalTimeOfIntervalsInSchedule,
		 * testSorting,
		 * testGetTotalConsumption,
		 * testGetOverlaps,
		 * testAddLoadDistributionIntervalToSchedule
	 */
	public ScheduleTest(){		
		/*
		 * testGetTotalTimeOfIntervalsInSchedule
		 * testSorting
		 * testGetTotalConsumption
		 * testGetOverlaps
		 * testAddLoadDistributionIntervalToSchedule
		 * 
		 */
	}
	
	
	/*
	 * 
	 */
	public void testGetTotalTimeOfIntervalsInSchedule(){
		s= setDummySchedule();
		s.sort();
		//******************************
		//getTotalTimeOfIntervalsInSchedule()
		//******************************
		double total= s.getTotalTimeOfIntervalsInSchedule();
		assertEquals(50.0, total);
	}
	
	
	public void testSort(){
		s= setDummySchedule();
		
		s.printSchedule();
		
		// first entry as created in dummy schedule
		assertEquals(30.0, s.timesInSchedule.get(0).getStartTime());
		
		s.sort();
		assertEquals(0.0, s.timesInSchedule.get(0).getStartTime());
	}
	
	
	public void testTotalConsumption(){
		s= setDummySchedule();
		s.sort();
		assertEquals(s.getTotalConsumption(), 100.0);
	}
	
	
	public void testMergeOverLapTimeIsInWhichInterval(){
		s= setDummySchedule();
		s.sort();
		
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
	}
	
	
	public void testInsertChargingSchedule(){
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
	
	
	public void testAddLoadDistributionIntervalToExistingLoadDistributionSchedule() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule loads= setDummyLoadDistributionSchedule(); // 0-20  and 40-50
		System.out.println("Start:");
		loads.printSchedule();
		
		
		// **************
		// NO OVERLAP
		//**************
		LoadDistributionInterval noOverlap= new LoadDistributionInterval(20, 30, new PolynomialFunction( new double[]{-10}), false);
		
		loads.addLoadDistributionIntervalToExistingLoadDistributionSchedule(noOverlap);
		System.out.println("After insertion of noOverlap LoadInterval");
		loads.printSchedule();
		assertEquals(3, loads.getNumberOfEntries());
		
		
		// **************
		// OVERLAP case 1
		//**************
		loads= setDummyLoadDistributionSchedule(); 
		LoadDistributionInterval withinFirst= new LoadDistributionInterval(10, 20, new PolynomialFunction( new double[]{-5}), false);
		loads.addLoadDistributionIntervalToExistingLoadDistributionSchedule(withinFirst);
		System.out.println("After insertion of Overlap LoadInterval");
		loads.printSchedule();
		assertEquals(3, loads.getNumberOfEntries());
		PolynomialFunction func=((LoadDistributionInterval)loads.timesInSchedule.get(1)).getPolynomialFunction();
		assertEquals(func.getCoefficients()[0], 5.0);
		
		
		// **************
		// OVERLAP CAse 2
		//**************
		loads= setDummyLoadDistributionSchedule(); 
		LoadDistributionInterval withinFirstAndEmptyInterval= new LoadDistributionInterval(10, 30, new PolynomialFunction( new double[]{-5}), false);
		loads.addLoadDistributionIntervalToExistingLoadDistributionSchedule(withinFirstAndEmptyInterval);
		System.out.println("After insertion of long Overlap LoadInterval");
		loads.printSchedule();
		assertEquals(4, loads.getNumberOfEntries());
		func=((LoadDistributionInterval)loads.timesInSchedule.get(1)).getPolynomialFunction();
		assertEquals(func.getCoefficients()[0], 5.0);
		func=((LoadDistributionInterval)loads.timesInSchedule.get(2)).getPolynomialFunction();
		assertEquals(func.getCoefficients()[0], -5.0);
		func=((LoadDistributionInterval)loads.timesInSchedule.get(3)).getPolynomialFunction();
		assertEquals(func.getCoefficients()[0], 10.0);
		
	}
	
	
	
	
	public Schedule setDummySchedule(){
		Schedule s1= new Schedule();
		
		s1.addTimeInterval(new ParkingInterval(30, 50, null));
		s1.addTimeInterval(new ParkingInterval(0, 10, null));
		s1.addTimeInterval(new DrivingInterval(10,15, 100.0) );
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
	
	
	public Schedule setDummyLoadDistributionSchedule(){
		Schedule s1= new Schedule();
		s1.addTimeInterval(new LoadDistributionInterval(0.0, 20.0, new PolynomialFunction(new double[]{10}), true));
		s1.addTimeInterval(new LoadDistributionInterval(40.0, 50.0, new PolynomialFunction(new double[]{10}), true));
		return s1;
	}
	
}
