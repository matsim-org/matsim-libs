
/* *********************************************************************** *
 * project: org.matsim.*
 * Main.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

//package playground.wrashid.sschieffer;
package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math.analysis.solvers.NewtonSolver;
import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.SimpleVectorialValueChecker;
import org.apache.commons.math.optimization.VectorialConvergenceChecker;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModel;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelPSL;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import java.util.*;


public class Main {
	
	
	public static LinkedListValueHashMap<Id, Vehicle> vehicles;
	public static ParkingTimesPlugin parkingTimesPlugin;
	public static EnergyConsumptionPlugin energyConsumptionPlugin;
	
	
	
	
	// start
	public static void main(String[] args) {
		
				
		
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";
		Controler controler=new Controler(configPath);
		
		final String outputPath="C:\\Users\\stellas\\Output\\V1G\\";
		
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		final double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		final double emissionPerLiterEngine = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		
		final double bufferBatteryCharge=0.0;
		
		final double MINCHARGINGLENGTH=5*60;//5 minutes
		
		
		EnergyConsumptionInit e= new EnergyConsumptionInit(phev, ev, combustion);
		vehicles= e.getVehicles();
		energyConsumptionPlugin=e.getEnergyConsumptionPlugin();
		controler.addControlerListener(e);
		
		
		parkingTimesPlugin = new ParkingTimesPlugin(controler);
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		controler.setOverwriteFiles(true);
		
		controler.addControlerListener(new IterationEndsListener() {
			
			
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
							event.getControler(), 
							parkingTimesPlugin,
							energyConsumptionPlugin,
							outputPath, 
							MINCHARGINGLENGTH, 
							vehicles,
							gasJoulesPerLiter,
							emissionPerLiterEngine);
					
					
					
					myDecentralizedSmartCharger.run();
					
					LinkedListValueHashMap<Id, Schedule> agentSchedule= 
						myDecentralizedSmartCharger.getAllAgentChargingSchedules();
					
					
					LinkedList<Id> agentsWithEVFailure = 
						myDecentralizedSmartCharger.getIdsOfEVAgentsWithFailedOptimization();
					
					LinkedList<Id> agentsWithEV = myDecentralizedSmartCharger.getAllAgentsWithEV();
					
					if(agentsWithEV.isEmpty()==false){
						Id id= agentsWithEV.get(0);
						myDecentralizedSmartCharger.getAgentChargingSchedule(id).printSchedule();
					}
					
					LinkedList<Id> agentsWithPHEV = myDecentralizedSmartCharger.getAllAgentsWithEV();
					
					if(agentsWithEV.isEmpty()==false){
						
						Id id= agentsWithEV.get(0);
						myDecentralizedSmartCharger.getAgentChargingSchedule(id).printSchedule();
						System.out.println("Total consumption from battery [joules]" 
								+ myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgentFromBattery(id));
						
						System.out.println("Total consumption from engine [joules]" +
								myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgentFromOtherSources(id));
						
						System.out.println("Total emissions from this agent [joules]" + 
								myDecentralizedSmartCharger.joulesToEmissionInKg(
										myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgentFromOtherSources(id)));
								
						
						System.out.println("Total consumption [joules]" +
								myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgent(id));
						
						
						
					}
					
					
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		
				
		controler.run();		
				
	}
	
		
		

}
