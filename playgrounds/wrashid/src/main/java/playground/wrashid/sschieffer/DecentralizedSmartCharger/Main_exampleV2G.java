
/* *********************************************************************** *
 * project: org.matsim.*
 * Main_exampleV2G.java
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


import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import java.util.*;


/**
 * This class highlights how to use V2G functions of this package:
 * 
 * V2G can only follow after the Decentralized Smart Charger has been run
 * since its based on the charging schedules planned by the Decentralized Smart Charger
 * 
 * For all specified stochastic loads the V2G procedure will then check if rescheduling is possible 
 * and if the utility of the agent can be increased by rescheduling.
 * 
 * 
 * PLease provide the following folders in the output path to store results locally
 * please provide a folder Output with the following sub-folders
 * <ul>
 * 		<li>DecentralizedCharger
 * 			<ul>
 * 				<li>agentPlans
 * 				<li>LP	
 * 					<ul><li>EV <li> PHEV</ul>
 * 			</ul>
 * 		<li>Hub
 * 		<li>V2G
 * 			<ul>
 * 				<li>agentPlans
 * 				<li>LP
 * 					<ul><li>EV <li> PHEV</ul>
 *  		</ul>
 * </ul>
 * @author Stella
 *
 */
public class Main_exampleV2G {
	
	
	
	public static void main(String[] args) throws IOException {
		
		/**
		 * SPECIFY WHAT PERCENTAGE OF THE POPULATION PROVIDES V2G
		 * <li> no V2G
		 * <li> only down (only charging)
		 * <li> up and down (charging and discharging)
		 */
		final double xPercentNone=0.0;
		final double xPercentDown=0.0;
		final double xPercentDownUp=1.0;
		
		/**
		 * see descriptions in Main_exampleDecentralizedSmartCharger for setup instructions
		 */
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		
		
		final Controler controler;
		final ParkingTimesPlugin parkingTimesPlugin;					
		
		SetUpVehicleCollector sv= new SetUpVehicleCollector();
		final VehicleTypeCollector myVehicleTypes = sv.setUp();
		
		
		/*
		 * LP optimization parameters
		 * - battery buffer for charging (e.g. 0.2=20%, agent will have charged 20% more 
		 * than what he needs before starting the next trip ); 
		 * 
		 */
		final double bufferBatteryCharge=0.0;
		
		/*
		 * Charging Distribution
		 * - minimum charging length [s]
		 * - time resolution of optimization
		 */
		final double minChargingLength=5*60;//5 minutes
		
		
		/*
		 * Network  - Electric Grid Information
		 * 
		 * - distribution of free load [W] available for charging over the day (deterministicHubLoadDistribution)
		 * this is given in form of a LinkedListValueHashMap, where Integer corresponds to a hub and 
		 * the Schedule includes LoadDistributionIntervals which represent the free load 
		 * (LoadDistributionIntervals indicate a time interval: start second, end second and also have a PolynomialFunction)
		 * 
		 * - pricing (pricingHubDistribution)
		 * is also given as LinkedListValueHashMap, where Integer corresponds to a hub and
		 * the Schedule includes LoadDistributionIntervals which represent the price per second over the day
		 */
		DetermisticLoadAndPricingCollector dlpc= new DetermisticLoadAndPricingCollector();
		
		final LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution
			= dlpc.getDeterminisitcHubLoad();
		
		final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution
			= dlpc.getDeterminisitcPriceDistribution();
		
				
		final String outputPath="D:\\ETH\\MasterThesis\\Output\\"; //"C:\\Users\\stellas\\Output\\V1G\\";
		
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";
		controler=new Controler(configPath);
		
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
					
					// map linkIds to hubs once the scenario is read in from the config file
					StellasHubMapping setHubLinkMapping= new StellasHubMapping(controler);
					final HubLinkMapping hubLinkMapping=setHubLinkMapping.mapHubs();
					
					
					/******************************************
					 * for Decentralized Smart Charging
					 * *****************************************
					 */
					DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
							event.getControler(), //controler
							parkingTimesPlugin, //ParkingTimesPlugIn
							e.getEnergyConsumptionPlugin(),
							outputPath, 
							myVehicleTypes
							);
					
					
					myDecentralizedSmartCharger.initializeLP(bufferBatteryCharge);
					
					myDecentralizedSmartCharger.initializeChargingSlotDistributor(minChargingLength);
					
					myDecentralizedSmartCharger.setLinkedListValueHashMapVehicles(
							e.getVehicles());
					
					myDecentralizedSmartCharger.initializeHubLoadDistributionReader(
							hubLinkMapping, 
							deterministicHubLoadDistribution,							
							pricingHubDistribution
							);
					
					//RUN DecentralizedSmartCharger
					myDecentralizedSmartCharger.run();
					
					
					/******************************************
					 * SETUP V2G
					 * *****************************************
					 * V2G can only follow after Decentralized Smart Charger has been run
					 * since it relies on the planned charging schedules
					 * 
					 * For all stochastic loads the V2G procedure will then check if rescheduling is possible 
					 * and if the utility of the agent can be increased by rescheduling.
					 * 
					 * 
					 */
					
				
					/*
					 * ************************
					 * Stochastic Sources
					 * they need to be defined per scenario. dummy values for the demonstration
					 * have been entered in StochasticLoadCollector
					 * 
					 * StochasticLoadSources can be defined
					 * <li> general hub Stochastic Load
					 * <li> for vehicles (i.e. solar roof)
					 * <li> local sources for hubs (wind turbine, etc)
					 * 
					 * the sources are given as Schedules of LoadDistributionIntervalsdistribution [Watt]
					 * <li> negative values mean, that the source needs energy --> regulation up
					 * <li> positive values mean, that the source has too much energy--> regulation down
					 * 
					 * 
					 * THe LoadDistributionIntervals indicate 
					 * <li> a time interval: start second, end second 
					 * <li>PolynomialFunction indicating the free Watts over the time interval
					 * <li> an optimality boolean(true, if stochastic load is positive=electricity for charging available and false if not)
					 * </br>
					 */
					StochasticLoadCollector slc= new StochasticLoadCollector(controler);
					
					LinkedListValueHashMap<Integer, Schedule> stochasticHubLoadDistribution=
						slc.getStochasticHubLoad();
						
					LinkedListValueHashMap<Integer, Schedule> locationSourceMapping= 
						slc.getStochasticHubSources();
					
					LinkedListValueHashMap<Id, Schedule> agentVehicleSourceMapping= 
						slc.getStochasticAgentVehicleSources();
						
					// SET STOCHASTIC LOADS
					myDecentralizedSmartCharger.setStochasticSources(
							stochasticHubLoadDistribution,
							locationSourceMapping,
							agentVehicleSourceMapping);
					
					
					/*
					 * ************************
					 * Agent contracts
					 * the convenience class AgentContractCollector 
					 * helps you to create the necessary List
					 * LinkedListValueHashMap<Id, ContractTypeAgent> agentContracts
					 * 
					 * provide the compensationPerKWHRegulationUp/Down in your currency
					 */
					double compensationPerKWHRegulationUp=0.15;
					double compensationPerKWHRegulationDown=0.15;
					 
					AgentContractCollector myAgentContractsCollector= new AgentContractCollector (
							myDecentralizedSmartCharger,
							 compensationPerKWHRegulationUp,
							 compensationPerKWHRegulationDown);
					
					
					LinkedListValueHashMap<Id, ContractTypeAgent> agentContracts= 
						myAgentContractsCollector.makeAgentContracts(
								controler,
								xPercentNone,
								xPercentDown,
								xPercentDownUp);
						
					//set the agent contracts
					myDecentralizedSmartCharger.setAgentContracts(agentContracts);
					
					
					/**
					 * RUN V2G
					 */
					myDecentralizedSmartCharger.initializeAndRunV2G();
					
					/*
					 * Example how to Use V2G
					 */
					LinkedListValueHashMap<Id, Double> agentRevenuesFromV2G=
						myDecentralizedSmartCharger.getAgentV2GRevenues();
					
					
					//*****************************************
					//END
					//*****************************************
					myDecentralizedSmartCharger.clearResults();
					
				
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
			}
		});
		
				
		controler.run();		
				
	}
	
	


}
