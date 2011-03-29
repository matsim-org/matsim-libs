
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
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
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
import org.matsim.api.core.v01.network.Link;
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

import playground.wrashid.PSF.data.HubLinkMapping;
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
	
	
	
	public static void main(String[] args) {
		
		final LinkedListValueHashMap<Id, Vehicle> vehicles;
		final ParkingTimesPlugin parkingTimesPlugin;
		final EnergyConsumptionPlugin energyConsumptionPlugin;
		
		final HubLinkMapping hubLinkMapping= new HubLinkMapping(0);
		
		final LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution = new  LinkedListValueHashMap<Integer, Schedule>();
		
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";
				
		final String outputPath="C:\\Users\\stellas\\Output\\V1G\\";
		
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		final double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		final double emissionPerLiterEngine = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		
		final double bufferBatteryCharge=0.0;
		
		final double MINCHARGINGLENGTH=5*60;//5 minutes
		
		final Controler controler=new Controler(configPath);
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		
		parkingTimesPlugin = new ParkingTimesPlugin(controler);
		
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		
		
		final EnergyConsumptionInit e= new EnergyConsumptionInit(
				phev, ev, combustion);
				
		
		controler.addControlerListener(e);
		
		
				
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		controler.setOverwriteFiles(true);
		
		controler.addControlerListener(new IterationEndsListener() {
			
			
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					readHubs(hubLoadDistribution);
					mapHubs(hubLoadDistribution, 
							hubLinkMapping, 
							controler);
					
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
							event.getControler(), 
							parkingTimesPlugin,
							e.getEnergyConsumptionPlugin(),
							outputPath, 
							MINCHARGINGLENGTH, 
							e.getVehicles(),
							gasJoulesPerLiter,
							emissionPerLiterEngine,
							hubLinkMapping,
							hubLoadDistribution);
					
					
					
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
					
					LinkedList<Id> agentsWithPHEV = myDecentralizedSmartCharger.getAllAgentsWithPHEV();
					
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
	
	
	
	public static void readHubs(LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution) throws IOException{
		hubLoadDistribution = new LinkedListValueHashMap<Integer, Schedule>();		
		hubLoadDistribution.put(2, makeBullshitSchedule());
		hubLoadDistribution.put(3, makeBullshitSchedule());
		hubLoadDistribution.put(4, makeBullshitSchedule());
		
		
	}
	
	
	public static Schedule makeBullshitSchedule() throws IOException{
		
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
		l1.makeXYSeries();
		bullShitSchedule.addTimeInterval(l1);
		
		
		LoadDistributionInterval l2= new LoadDistributionInterval(					
				62490.0,
				DecentralizedSmartCharger.SECONDSPERDAY,
				bullShitFunc2,//p
				false//boolean
		);
		l2.makeXYSeries();
		bullShitSchedule.addTimeInterval(l2);
		
		bullShitSchedule.visualizeLoadDistribution("BullshitSchedule");	
		return bullShitSchedule;
	}
	

	public static void mapHubs(LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution, 
			HubLinkMapping hubLinkMapping, 
			Controler controler){
		
		hubLinkMapping=new HubLinkMapping(hubLoadDistribution.size());
		
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
