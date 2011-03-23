
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
package playground.wrashid.sschieffer.V1G;

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
	
		
	final public static String outputPath="C:\\Users\\stellas\\Output\\V1G\\";
	final public static String configPath="test/input/playground/wrashid/sschieffer/config.xml";
	
	public static DifferentiableMultivariateVectorialOptimizer optimizer;
	public static VectorialConvergenceChecker checker= new SimpleVectorialValueChecker(10000,10000);
	public static SimpsonIntegrator functionIntegrator= new SimpsonIntegrator();
	public static NewtonSolver newtonSolver= new NewtonSolver();	
	public static GaussNewtonOptimizer gaussNewtonOptimizer= new GaussNewtonOptimizer(true); //useLU - true, faster  else QR more robust
		
	public static PolynomialFitter polyFit;
	
	
	final public static double PERCENTAGEPHEV=0.0;
	final public static double PERCENTAGEEV=1.0;
	final public static double PERCENTAGECOMBUNSTIONVEHICLE=0.0;
	
	
	final public static double SECONDSPERMIN=60;
	final public static double SECONDSPER15MIN=15*60;
	final public static double SECONDSPERDAY=24*60*60;
	final public static int MINUTESPERDAY=24*60;
	
	final public static double MINCHARGINGLENGTH=5*SECONDSPERMIN;
	
	final public static double EMISSIONCOUNTER=0.0;
	
	public static LinkedListValueHashMap<Id, Vehicle> vehicles;
	public static ParkingTimesPlugin parkingTimesPlugin;
	public static EnergyConsumptionPlugin energyConsumptionPlugin;
	
	final public static DrawingSupplier supplier = new DefaultDrawingSupplier();
	
	// start
	public static void main(String[] args) {
		
		gaussNewtonOptimizer.setMaxIterations(10000000);		
		gaussNewtonOptimizer.setConvergenceChecker(checker);		
		optimizer=gaussNewtonOptimizer;
		polyFit= new PolynomialFitter(24, optimizer);
		
		//System.out.println(System.getProperty("user.dir"));
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		Controler controler=new Controler(configPath);
		
		parkingTimesPlugin = new ParkingTimesPlugin(controler);
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		
		controler.addControlerListener(new EnergyConsumptionInit());
		controler.addControlerListener(eventHandlerAtStartupAdder);
		controler.setOverwriteFiles(true);
		controler.addControlerListener(new IterationEndsListener() {
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				DecentralizedV1G myDecentralizedV1G;
				try {
					
					myDecentralizedV1G = new DecentralizedV1G(event.getControler());
					
					myDecentralizedV1G.getAgentSchedules();
					myDecentralizedV1G.findRequiredChargingTimes();
					myDecentralizedV1G.assignChargingTimes();
					myDecentralizedV1G.validateChargingDistribution();
					
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		
				
		controler.run();		
				
	}
	
		
		

}
