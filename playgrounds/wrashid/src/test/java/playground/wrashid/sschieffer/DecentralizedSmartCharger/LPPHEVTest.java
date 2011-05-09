/* *********************************************************************** *
 * project: org.matsim.*
 * LPPHEVTest.java
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import junit.framework.TestCase;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;

/**
 * checks if the function calcEnergyUsageFromCombustionEngine produces the 
 * expected output for example case
 * 
 * @author Stella
 *
 */
public class LPPHEVTest extends TestCase{
	
	final String outputPath="D:\\ETH\\MasterThesis\\TestOutput\\";
	String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
	final Controler controler=new Controler(configPath);
	
	
	public Id agentOne=null;
	
	public static DecentralizedSmartCharger myDecentralizedSmartCharger;
	
	private Schedule s;
	LPPHEV lp= new LPPHEV();
	
	public LPPHEVTest() {		
		
	}
		
	
	/*
	 * calcEnergyUsageFromCombustionEngine
	 * check energy use from engine/battery for one case
	 */
	public void testRunLPPHEV() throws LpSolveException{
		
		double [] solution = setUpTestLPPHEV();
		lp.setSchedule(s);
		double eEngine= lp.calcEnergyUsageFromCombustionEngine(solution);
		assertEquals(eEngine, 15000.0);
		// * parking times-->  the required charging times is adjusted;
		// dependent on charing speed.. currently 3500 but should be flexible in future..
		// so not implemented right now test for this
		
		
		// * driving times --> consumption from engine for an interval is reduced 
		DrivingInterval d= (DrivingInterval) s.timesInSchedule.get(1);
		
		assertEquals(15000.0, d.getExtraConsumption());
		assertEquals(35000.0, d.getConsumption());
		
	}
	
	
	
	
	public double [] setUpTestLPPHEV(){
		
		s= new Schedule();
		
		s.addTimeInterval(new ParkingInterval(0, 10, null));
		
		s.addTimeInterval(new DrivingInterval(10, 20, 50000));
		
		s.addTimeInterval(new ParkingInterval(20, 30, null));
		s.addTimeInterval(new ParkingInterval(30, 40, null));
		
		for (int i=0; i<s.getNumberOfEntries(); i++){
			if (s.timesInSchedule.get(i).isParking()){
				
				ParkingInterval p= (ParkingInterval) s.timesInSchedule.get(i);
				p.setRequiredChargingDuration(0.0);
			}
		}
		
		double[] solution={0.0, 10.0, 1.0, 0.0, 0.0};
		return solution;
	}
	
	
	
	
	public static LinkedListValueHashMap<Integer, Schedule> readStochasticLoad(int num){
			
			LinkedListValueHashMap<Integer, Schedule> stochastic= new LinkedListValueHashMap<Integer, Schedule>();
			
			Schedule bullShitStochastic= new Schedule();
			PolynomialFunction p = new PolynomialFunction(new double[] {3500});
			
			bullShitStochastic.addTimeInterval(new LoadDistributionInterval(0, 24*3600, p, true));
			for (int i=0; i<num; i++){
				stochastic.put(i+1, bullShitStochastic);
			}
			return stochastic;
		
			
		}
		
			
		

	public static Schedule makeBullshitPricingScheduleTest(double optimal, double suboptimal) throws IOException{
		
		Schedule bullShitSchedule= new Schedule();
		
		PolynomialFunction pOpt = new PolynomialFunction(new double[] {optimal});	
		PolynomialFunction pSubopt = new PolynomialFunction(new double[] {suboptimal});
		
		
		LoadDistributionInterval l1= new LoadDistributionInterval(
				0.0,
				62490.0,
				pOpt,//p
				true//boolean
		);
		
		bullShitSchedule.addTimeInterval(l1);
		
		
		LoadDistributionInterval l2= new LoadDistributionInterval(					
				62490.0,
				DecentralizedSmartCharger.SECONDSPERDAY,
				pSubopt,//p
				false//boolean
		);

		bullShitSchedule.addTimeInterval(l2);
		bullShitSchedule.printSchedule();
		
		return bullShitSchedule;
	}
	
	
	
public static Schedule makeBullshitScheduleTest() throws IOException{
		
		Schedule bullShitSchedule= new Schedule();
		
		double[] bullshitCoeffs = new double[]{100, 5789, 56};// 
		double[] bullshitCoeffs2 = new double[]{-22, 5.6, -2.5};
		
		PolynomialFunction bullShitFunc= new PolynomialFunction(bullshitCoeffs);
		PolynomialFunction bullShitFunc2= new PolynomialFunction(bullshitCoeffs2);
		LoadDistributionInterval l1= new LoadDistributionInterval(
				0.0,
				62490.0,
				bullShitFunc,//p
				true//boolean
		);
		
		bullShitSchedule.addTimeInterval(l1);
		
		
		LoadDistributionInterval l2= new LoadDistributionInterval(					
				62490.0,
				DecentralizedSmartCharger.SECONDSPERDAY,
				bullShitFunc2,//p
				false//boolean
		);
	
		bullShitSchedule.addTimeInterval(l2);
		bullShitSchedule.printSchedule();
		
		return bullShitSchedule;
	}
	
	public static LinkedListValueHashMap<Integer, Schedule> readHubsTest() throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitScheduleTest());
		hubLoadDistribution1.put(2, makeBullshitScheduleTest());
		hubLoadDistribution1.put(3, makeBullshitScheduleTest());
		hubLoadDistribution1.put(4, makeBullshitScheduleTest());
		return hubLoadDistribution1;
		
	}
	
	
	public static LinkedListValueHashMap<Integer, Schedule> readHubsPricingTest(double optimal, double suboptimal) throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(2, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(3, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(4, makeBullshitPricingScheduleTest(optimal, suboptimal));
		return hubLoadDistribution1;
		
	}
	
	
	public void mapHubsTest(Controler controler, HubLinkMapping hubLinkMapping){
		
		
		double maxX=5000;
		double minX=-20000;
		double diff= maxX-minX;
		
		for (Link link:controler.getNetwork().getLinks().values()){
			// x values of equil from -20000 up to 5000
			if (link.getCoord().getX()<(minX+diff)/4){
				
				hubLinkMapping.addMapping(link.getId().toString(), 1);
			}else{
				if (link.getCoord().getX()<(minX+diff)*2/4){
					hubLinkMapping.addMapping(link.getId().toString(), 2);
				}else{
					if (link.getCoord().getX()<(minX+diff)*3/4){
						hubLinkMapping.addMapping(link.getId().toString(), 3);
					}else{
						hubLinkMapping.addMapping(link.getId().toString(), 4);
					}
				}
			}
			
		}
	}
	
}
