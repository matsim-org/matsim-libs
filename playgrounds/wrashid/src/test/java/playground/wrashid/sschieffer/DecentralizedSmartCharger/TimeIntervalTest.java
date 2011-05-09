/* *********************************************************************** *
 * project: org.matsim.*
 * TimeInterval.java
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

import junit.framework.TestCase;

/**
 * tests all methods of TimeInterval
 * <li> overlap
 * <li> intervalLength
 * <li> isParking/Driving/Charging
 * <li> testIfOverlapWithLoadDistributionIntervalReturnOverlap
 * <li> testTimeIsGreaterThanStartAndSmallerThanEnd
 * <li> testTimeIsEqualOrGreaterThanStartOrEqualOrSmallerThanEnd
 * @author Stella
 *
 */
public class TimeIntervalTest extends TestCase{

	private TimeInterval t;
	public TimeIntervalTest(){
		
	}
	
	public ParkingInterval makeParkingInterval(double start, double end){
		return new ParkingInterval(start, end, null);
	}
	
	public DrivingInterval makeDrivingInterval(double start, double end){
		return new DrivingInterval(start, end, 0);
	}
	
	public LoadDistributionInterval makeLoadDistributionInterval(double start, double end){
		return new LoadDistributionInterval(start, end, null, false);
	}
	
	
	public ChargingInterval makeChargingInterval(double start, double end){
		return new ChargingInterval(start, end);
	}
	
	public void testOverlap(){
		TimeInterval t1= makeParkingInterval(0, 10);
		TimeInterval t2= makeParkingInterval(5, 10);
		TimeInterval t3= makeParkingInterval(10, 20);
		TimeInterval t4= makeParkingInterval(11, 20);
		assertEquals(t1.overlap(t2), true);
		
		assertEquals(t1.overlap(t3), false);
		
		assertEquals(t1.overlap(t4), false);
		
	}
	
	
	public void testIfOverlapWithLoadDistributionIntervalReturnOverlap(){
		TimeInterval t1= makeParkingInterval(0, 10);
		LoadDistributionInterval l1= makeLoadDistributionInterval(5,10);
		LoadDistributionInterval l2= makeLoadDistributionInterval(11,12);
		
		LoadDistributionInterval lAns= 
			t1.ifOverlapWithLoadDistributionIntervalReturnOverlap(l1);
		assertEquals(lAns.getStartTime(), 5.0);
		assertEquals(lAns.getEndTime(), 10.0);
		
		lAns= t1.ifOverlapWithLoadDistributionIntervalReturnOverlap(l2);
		assertEquals(lAns, null);
		
	}
	
	
	public void testTimeIsGreaterThanStartAndSmallerThanEnd(){
		TimeInterval t1= makeParkingInterval(0, 10);
		
		boolean ans;
		
		ans= t1.timeIsGreaterThanStartAndSmallerThanEnd(1.0);
		assertEquals(ans, true);
		
		ans= t1.timeIsGreaterThanStartAndSmallerThanEnd(0.0);
		assertEquals(ans, false);
		
		ans= t1.timeIsGreaterThanStartAndSmallerThanEnd(10.0);
		assertEquals(ans, false);
		
		ans= t1.timeIsGreaterThanStartAndSmallerThanEnd(11.0);
		assertEquals(ans, false);
		
	}
	
	
	public void testTimeIsEqualOrGreaterThanStartOrEqualOrSmallerThanEnd(){
		TimeInterval t1= makeParkingInterval(0, 10);
		
		boolean ans;
		ans= t1.timeIsEqualOrGreaterThanStartAndEqualOrSmallerThanEnd(0.0);
		assertEquals(ans, true);
		
		ans= t1.timeIsEqualOrGreaterThanStartAndEqualOrSmallerThanEnd(10.0);
		assertEquals(ans, true);
		
		ans= t1.timeIsEqualOrGreaterThanStartAndEqualOrSmallerThanEnd(11.0);
		assertEquals(ans, false);
	}
	
	
	public void testGetIntervalLength(){
		TimeInterval t1= makeParkingInterval(0, 10);
		assertEquals(t1.getIntervalLength(), 10.0);
	}
	
	public void testIsParking(){
		ParkingInterval p1= makeParkingInterval(0.0, 10.0);
		
		DrivingInterval d1= makeDrivingInterval(0.0, 1.0);
		
		assertEquals(p1.isParking(), true);
		assertEquals(d1.isParking(), false);
	}
		
		
	
	public void testIsDriving(){
		ParkingInterval p1= makeParkingInterval(0.0, 10.0);
		
		DrivingInterval d1= makeDrivingInterval(0.0, 1.0);
		
		assertEquals(p1.isDriving(), false);
		assertEquals(d1.isDriving(), true);
	}
	
	
	public void testIsCharging(){
		ParkingInterval p1= makeParkingInterval(0.0, 10.0);
		
		ChargingInterval c1= makeChargingInterval(0.0, 1.0);
		
		assertEquals(p1.isCharging(), false);
		assertEquals(c1.isCharging(), true);
	}
	
	public void isLoadDistributionInterval(){
		ParkingInterval p1= makeParkingInterval(0.0, 10.0);
		
		LoadDistributionInterval l1= makeLoadDistributionInterval(0.0, 1.0);
		
		assertEquals(p1.isLoadDistributionInterval(), false);
		assertEquals(l1.isLoadDistributionInterval(), true);
	}
	
}
