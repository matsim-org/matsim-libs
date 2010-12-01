package playground.wrashid.sschieffer;
import java.io.IOException;
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
import org.apache.commons.math.optimization.OptimizationException;
import org.junit.Test;


public class TestCharging extends TestCase {

	public static DecentralizedChargerV1 decentralizedChargerV1;
	
	double [][]valleyTimes  =  {{5*Main.slotLength,8*Main.slotLength},{13*Main.slotLength,15*Main.slotLength},{15*Main.slotLength, 23*Main.slotLength}};
	
	
	public void testGetFeasibleCHargingTimeInIntervalFirstLower() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo(100, 100, 100, 0.1, 0.13);
		double feasibleTime1=myInfo.getFeasibleChargingTimeInInterval(4*Main.slotLength, 6*Main.slotLength,valleyTimes);
		assertEquals(feasibleTime1, 1*Main.slotLength);
		
	}
	
	public void testGetFeasibleCHargingTimeInIntervalBothEqual() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo(100, 100, 100, 0.1, 0.13);

		double feasibleTime2=myInfo.getFeasibleChargingTimeInInterval(13*Main.slotLength, 15*Main.slotLength,valleyTimes);
		assertEquals(feasibleTime2, 2*Main.slotLength);

	}
	
	public void testGetFeasibleCHargingTimeInIntervalSecondHigher() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo(100, 100, 100, 0.1, 0.13);

		double feasibleTime3=myInfo.getFeasibleChargingTimeInInterval(16*Main.slotLength, 24*Main.slotLength,valleyTimes);
		assertEquals(feasibleTime3, 7*Main.slotLength);
		
		
	}
	
	public void testGetFeasibleCHargingTimeInIntervalFirstLowerSecondigher() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo(100, 100, 100, 0.1, 0.13);
		
		double feasibleTime4=myInfo.getFeasibleChargingTimeInInterval(14*Main.slotLength, 24*Main.slotLength, valleyTimes);
		assertEquals(feasibleTime4, 8*Main.slotLength);
	}
	
	public void testGetFeasibleCHargingTimeInIntervalWithStartSecondBiggerEndSecond() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{
		DecentralizedChargerInfo myInfo= new DecentralizedChargerInfo(100, 100, 100, 0.1, 0.13);
		
		double feasibleTime5=myInfo.getFeasibleChargingTimeInInterval(20*Main.slotLength, 6*Main.slotLength, valleyTimes);
		assertEquals(feasibleTime5, 4*Main.slotLength);
	}
	
}
