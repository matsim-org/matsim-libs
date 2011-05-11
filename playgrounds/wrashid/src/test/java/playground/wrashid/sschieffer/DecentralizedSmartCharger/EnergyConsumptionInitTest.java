package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;

import lpsolve.LpSolveException;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

/**
 * tests if the vehicles List produced by the class EnergyConsumptionInitTest
 * really generates a vehicle list according to the given percentages of phevs, evs and combustion engine cars
 * (tolerated error =1 person)
 * 
 * @author Stella
 *
 */
public class EnergyConsumptionInitTest extends MatsimTestCase{

	 // 100 agents
	static String configPath="test/input/playground/wrashid/sschieffer/config.xml";
	final static Controler controler=new Controler(configPath);
	
	
	public static void testMain(String[] args) throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException {
		
	}
	
	public void testVehiclePercentages(){
		double phev= 0.3;
		double ev= 0.2;
		double combustion =0.5;
		int error=1;
		
		ParkingTimesPlugin parkingTimesPlugin= new ParkingTimesPlugin(controler);
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		EnergyConsumptionInit energyInit= new EnergyConsumptionInit(
				phev, ev, combustion);
		
		controler.addControlerListener(energyInit);
		controler.setOverwriteFiles(true);		
		controler.run();
		
		LinkedListValueHashMap<Id, Vehicle> vehicles =energyInit.getVehicles();
		
		int countPHEV=0;
		int countEV=0;
		int countCom=0;
		
		for (Id id: vehicles.getKeySet()){
			if (vehicles.getValue(id).getClass().equals(PlugInHybridElectricVehicle.class)){
				countPHEV++;
			}else{
				if (vehicles.getValue(id).getClass().equals(ElectricVehicle.class)){
					countEV++;
				}else{
					countCom++;
				}
			}
		}
				
		
		assertEquals((countPHEV>30-error && countPHEV<30+error), true);
		assertEquals((countEV>20-error && countEV<20+error), true);
		assertEquals((countCom>50-error && countCom<50+error), true);
		
	}
	
}
