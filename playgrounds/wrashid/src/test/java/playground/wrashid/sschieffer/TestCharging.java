package playground.wrashid.sschieffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ConventionalVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import junit.framework.TestCase;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.junit.Test;


public class TestCharging extends TestCase {

	
	double [][]valleyTimes  =  {{5*Main.slotLength,8*Main.slotLength},{13*Main.slotLength,15*Main.slotLength},{15*Main.slotLength, 23*Main.slotLength}};
	
	/*
	 * check getFeasibleChargingIntervalInParkingInterval(list, parkingTimesCurrentAgent[i][0], parkingTimesCurrentAgent[i][1], valleys2ComareTo);
	 */
	public void testGetFeasibleChargingIntervalInParkingInterval() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo( 100, 100);
		ArrayList<double[][]> list= new ArrayList<double[][]>(0);
		list=myInfo.getFeasibleChargingIntervalInParkingInterval(list, 5*Main.slotLength, 6*Main.slotLength, valleyTimes);
		double [][] entry = list.get(0);
		
		//assertEquals(6*Main.slotLength, entry[0][1]);
		assertEquals(5*Main.slotLength, entry[0][0]);
		
		list= new ArrayList<double[][]>(0);
		list=myInfo.getFeasibleChargingIntervalInParkingInterval(list, 5*Main.slotLength, 22*Main.slotLength, valleyTimes);
		entry = list.get(0);
		assertEquals(5*Main.slotLength, entry[0][0]);
		assertEquals(8*Main.slotLength, entry[0][1]);
		entry = list.get(1);
		assertEquals(13*Main.slotLength, entry[0][0]);
		entry = list.get(2);
		double d=entry[0][1];
		assertEquals(22*Main.slotLength, d);
	}
	
	public void testGetFeasibleCHargingTimeInIntervalFirstLower() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo( 100, 100);
		double feasibleTime1=myInfo.getFeasibleChargingTimeInInterval(4*Main.slotLength, 6*Main.slotLength,valleyTimes);
		assertEquals(feasibleTime1, 1*Main.slotLength);
		
	}
	
	public void testGetFeasibleCHargingTimeInIntervalBothEqual() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo( 100, 100);

		double feasibleTime2=myInfo.getFeasibleChargingTimeInInterval(13*Main.slotLength, 15*Main.slotLength,valleyTimes);
		assertEquals(feasibleTime2, 2*Main.slotLength);

	}
	
	public void testGetFeasibleCHargingTimeInIntervalSecondHigher() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo( 100, 100);

		double feasibleTime3=myInfo.getFeasibleChargingTimeInInterval(16*Main.slotLength, 24*Main.slotLength,valleyTimes);
		assertEquals(feasibleTime3, 7*Main.slotLength);
		
		
	}
	
	public void testGetFeasibleCHargingTimeInIntervalFirstLowerSecondigher() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo( 100, 100);
		
		double feasibleTime4=myInfo.getFeasibleChargingTimeInInterval(14*Main.slotLength, 24*Main.slotLength, valleyTimes);
		assertEquals(feasibleTime4, 8*Main.slotLength);
	}
	
	public void testGetFeasibleCHargingTimeInIntervalWithStartSecondBiggerEndSecond() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo( 100, 100);
		
		double feasibleTime5=myInfo.getFeasibleChargingTimeInInterval(20*Main.slotLength, 6*Main.slotLength, valleyTimes);
		assertEquals(feasibleTime5, 4*Main.slotLength);
	}
	
	public void testCheckIfSlotWithinParkingTimeOfAgent() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo( 100, 100);
		
		//double [][]valleyTimes  =  {{5*Main.slotLength,8*Main.slotLength},{13*Main.slotLength,15*Main.slotLength},{15*Main.slotLength, 23*Main.slotLength}};
		// should be true
		double [][] trySlot={{5*Main.slotLength, 6*Main.slotLength}};
		assertTrue(myInfo.checkIfSlotWithinParkingTimeOfAgent(trySlot, valleyTimes));
		// should be false
		double [][] trySlot2={{3*Main.slotLength, 6*Main.slotLength}};
		assertFalse(myInfo.checkIfSlotWithinParkingTimeOfAgent(trySlot2, valleyTimes));
		
	}
	
	public void testCheckIfOverlappingSlots() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo( 100, 100);
		double [][] trySlot={{5*Main.slotLength, 6*Main.slotLength}};
		//double [][]valleyTimes  =  {{5*Main.slotLength,8*Main.slotLength},{13*Main.slotLength,15*Main.slotLength},{15*Main.slotLength, 23*Main.slotLength}};
		
		boolean result= myInfo.checkForOverlappingSlots(trySlot, valleyTimes, valleyTimes.length);
		assertEquals(result, false);
	}
	
	//public double[][] minAtStartMaxAtEnd(double [][]d, int elementsPerRow){
	//public double [][] removeEntryIFromDoubleArray(double [][] d, int i, int elementsPerRow){
	
	
	
}
