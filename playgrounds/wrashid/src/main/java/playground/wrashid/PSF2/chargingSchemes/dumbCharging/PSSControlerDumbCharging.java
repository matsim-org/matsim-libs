/* *********************************************************************** *
 * project: org.matsim.*
 * PSSControlerDumbCharging.java
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

package playground.wrashid.PSF2.chargingSchemes.dumbCharging;

import java.util.HashMap;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.PSS.EventReadControler;
import playground.wrashid.PSF.PSS.PSSControler;
import playground.wrashid.PSF.energy.SimulationStartupListener;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF2.ParametersPSF2;
import playground.wrashid.PSF2.chargingSchemes.ActivityIntervalTracker_NonParallelizableHandler;
import playground.wrashid.PSF2.chargingSchemes.ChargingAfterSimListener;
import playground.wrashid.PSF2.chargingSchemes.ChargingFleetInitializer;
import playground.wrashid.PSF2.chargingSchemes.LinkEnergyConsumptionTracker_NonParallelizableHandler;
import playground.wrashid.PSF2.vehicle.energyConsumption.EnergyConsumptionTable;
import playground.wrashid.lib.obj.GeneralLogObject;

public class PSSControlerDumbCharging extends PSSControler {

	public PSSControlerDumbCharging(String configFilePath, ParametersPSFMutator parameterPSFMutator) {
		super(configFilePath, parameterPSFMutator);
	}

	
	public MatsimServices getControler(){
		return controler;
	}
	
	public void prepareMATSimIterations(){
		// use the right Controler (read parameter
		Config config = ConfigUtils.loadConfig(configFilePath, new ParametersPSF());
		String tempStringValue = config.findParam(ParametersPSF.PSF_MODULE, "main.inputEventsForSimulationPath");
		if (tempStringValue != null) {
			// ATTENTION, this does not work at the moment, because the read
			// link from the
			// event file is null and this causes some probelems in my
			// handlers...
			//
			// (As far as I can tell, the above lines come from Rashid, in 2011. kai, sep'2015) 
			controler = new EventReadControler(config, tempStringValue).getControler();
			ParametersPSF2.isEventsFileBasedControler=true;
			
			setDumbScoringFunctionFactory(controler);
			
		} else {
			controler = new Controler(config);
		}

		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );

		initializeParametersPSF2(controler);

		addSimulationStartupListener(controler, parameterPSFMutator);

		addAfterSimulationListener(controler);

		controler.addControlerListener(new ShutdownListener() {
			@Override
			public void notifyShutdown(ShutdownEvent event) {
//				ParametersPSF2.getPSFGeneralLog().writeFileAndCloseStream(event.getServices().getConfig().services().getLastIteration() + 1);
				ParametersPSF2.getPSFGeneralLog().writeFileAndCloseStream(event.getServices().getConfig().controler().getLastIteration() );
				// not clear to me how the "iteration + 1" can ever have worked. kai, sep'15
			}
		});
	}
	
	public void runControler(){
		controler.run();
	}
	
	@Override
	public void runMATSimIterations() {

		prepareMATSimIterations();

		runControler();

	}

	



	private void addAfterSimulationListener(MatsimServices controler) {
		controler.addControlerListener(new IterationEndsListener() {

			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				ChargingTimes.writeChargingTimes(ParametersPSF2.chargingTimes, event.getServices().getControlerIO()
						.getIterationFilename(event.getIteration(), "chargingLog.txt"));
				ChargingTimes.writeChargingTimes(ParametersPSF2.chargingTimes, event.getServices().getControlerIO()
						.getOutputFilename("chargingLog.txt"));

				double[][] energyUsageStatistics = ChargingTimes.getEnergyUsageStatistics(ParametersPSF2.chargingTimes,
						ParametersPSF.getHubLinkMapping());

				ChargingTimes.writeEnergyUsageStatisticsData(
						event.getServices().getControlerIO()
								.getIterationFilename(event.getIteration(), "vehicleEnergyConsumption.txt"),
						energyUsageStatistics);
				ChargingTimes.writeVehicleEnergyConsumptionStatisticsGraphic(event.getServices().getControlerIO()
						.getIterationFilename(event.getIteration(), "vehicleEnergyConsumption.png"),
						energyUsageStatistics);
			}
		});

	}

	private static void addSimulationStartupListener(MatsimServices controler, ParametersPSFMutator parameterPSFMutator) {
		SimulationStartupListener simulationStartupListener = new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);
		simulationStartupListener.addParameterPSFMutator(parameterPSFMutator);
		simulationStartupListener.addEventHandler(new LinkEnergyConsumptionTracker_NonParallelizableHandler());
		simulationStartupListener.addEventHandler(ParametersPSF2.activityIntervalTracker);
		controler.addControlerListener(new ChargingAfterSimListener());
	}

	private static void initializeParametersPSF2(MatsimServices controler) {
		ParametersPSF2.fleetInitializer = new ChargingFleetInitializer();
		
		
		ParametersPSF2.chargingTimes = new HashMap<>();

		ParametersPSF2.controler = controler;
		ParametersPSF2.activityIntervalTracker = new ActivityIntervalTracker_NonParallelizableHandler();

		controler.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				
				ParametersPSF2.setPSFGeneralLog(new GeneralLogObject(event.getServices(),"PSFGeneralLog.txt"));
				
				String pathToEnergyConsumptionTable = event.getServices().getConfig().getParam("PSF", "pathToEnergyConsumptionTable");
				
				if (pathToEnergyConsumptionTable!=null){
					ParametersPSF2.pathToEnergyConsumptionTable=pathToEnergyConsumptionTable;
				}
				
				ParametersPSF2.energyConsumptionTable = new EnergyConsumptionTable(ParametersPSF2.pathToEnergyConsumptionTable);
				
				ParametersPSF2.energyStateMaintainer = new ARTEMISEnergyStateMaintainer_StartChargingUponArrival(
						ParametersPSF2.energyConsumptionTable);
				
				ParametersPSF2.initVehicleFleet(event.getServices());
			}
		});

		controler.addControlerListener(new BeforeMobsimListener() {
			@Override
			public void notifyBeforeMobsim(BeforeMobsimEvent event) {
				GeneralLogObject iterationLog = new GeneralLogObject(event.getServices(), "PSFIterationLog.txt");
				ParametersPSF2.setPSFIterationLog(iterationLog);
			}
		});

		controler.addControlerListener(new AfterMobsimListener() {
			@Override
			public void notifyAfterMobsim(AfterMobsimEvent event) {
				ParametersPSF2.getPSFIterationLog().writeFileAndCloseStream(event.getIteration());
			}
		});

	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private void setDumbScoringFunctionFactory(Controler controler) {
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				// TODO Auto-generated method stub
				return new ScoringFunction() {
					
					@Override
					public void agentStuck(double time) {
						// TODO Auto-generated method stub
						
					}
					
					@Override
					public void addMoney(double amount) {
						// TODO Auto-generated method stub
						
					}

                    @Override
                    public void handleActivity(Activity activity) {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void handleLeg(Leg leg) {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public void finish() {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }

                    @Override
                    public double getScore() {
                        return 0;  //To change body of implemented methods use File | Settings | File Templates.
                    }

					@Override
					public void handleEvent(Event event) {
						// TODO Auto-generated method stub
						
					}
                };
			}
		});
		
	}
	
	
	
	
}
