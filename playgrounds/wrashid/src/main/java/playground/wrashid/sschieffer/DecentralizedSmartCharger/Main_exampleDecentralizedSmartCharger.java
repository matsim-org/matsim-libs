

/* *********************************************************************** *
 * project: org.matsim.*
 * Main_exampleDecentralizedSmartCharger.java
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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import java.util.*;

/**
 * This package guides charging decisions for electric vehicles (EV & PHEV) and 
 * provides an optimized charging schedule for all agents as well as the associated charging costs. 
 * It also calculates the total emissions produced by the vehicles.
 * 
 * To set up using these functions
 * - first create a DecentralizedSmartCharger object within your simulation
 * - set its input parameters
 * 
 * 
 * To use the function
 * - run() it after the iteration(s) as demonstrated in the code below
 * 
 * 
 */
public class Main_exampleDecentralizedSmartCharger {
	
		
	public static void main(String[] args) throws IOException {
		
		final double optimalPrice=0.4275/(3600); // //0,142500 €/kWh - 1 hour at 1kW cost/second - CAREFUL would have to implement different multipliers for high speed or regular connection
		final double suboptimalPrice=optimalPrice*3; // cost/second  
		
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		/**
		 * INPUT PARAMETERS
		 */
		
		/*
		 * Simulation variables: 
		 * - Controler of the simulation in which the charging optimization is embedded
		 * - ParkingTimesPlugin
		 * 
		 */
		final Controler controler;
		final ParkingTimesPlugin parkingTimesPlugin;					
		
		/*
		 * Constants
		 * - Gas Price [currency]
		 * - Joules per liter in gas [J]
		 * - emissions of Co2 per liter gas [kg]
		 
		 */
		final double gasPricePerLiter= 0.25; 
		final double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		final double emissionPerLiterEngine = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		
		
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
		 * Battery characteristics:
		 * - full capacity [J]
		 * e.g. common size is 24kWh = 24kWh*3600s/h*1000W/kW = 24*3600*1000Ws= 24*3600*1000J
		 * - minimum level of state of charge, avoid going below this SOC= batteryMin
		 * (0.1=10%)
		 * - maximum level of state of charge, avoid going above = batteryMax
		 * (0.9=90%)
		 */
		final double batterySizeEV= 24*3600*1000; 
		final double batterySizePHEV= 24*3600*1000; 
		final double batteryMinEV= 0.1; 
		final double batteryMinPHEV= 0.1; 
		final double batteryMaxEV= 0.9; 
		final double batteryMaxPHEV= 0.9; 		
		
		
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
		 * 
		 * determinisitcHubLoadDistribution and pricingHubDistribution should have the same timeIntervals
		 * i.e. loadInterval from 0-1000 seconds corresponding to price in Interval 0-1000 seconds
		 */
		final LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution= readHubs();
		final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution=readPricingHubDistribution(optimalPrice, suboptimalPrice);
		
		
		// HubLinkMapping links linkIds (Id) to Hubs (Integer)
		final HubLinkMapping hubLinkMapping=new HubLinkMapping(deterministicHubLoadDistribution.size());//= new HubLinkMapping(0);
		
		
		/*
		 * Output path to store results locally
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
		 * 
		 * 
		 */
		
		final String outputPath="D:\\ETH\\MasterThesis\\Output\\"; //"C:\\Users\\stellas\\Output\\V1G\\";
		
		
		/**
		 * END INPUT PARAMETERS
		 */		

		
		
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
					mapHubs(controler,hubLinkMapping);
					
					
					/*
					 * for Decentralized Smart Charging
					 * 
					 * initialize parameters
					 */
				
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
							event.getControler(), //controler
							parkingTimesPlugin, //ParkingTimesPlugIn
							e.getEnergyConsumptionPlugin(),
							outputPath, 
							gasJoulesPerLiter,
							emissionPerLiterEngine,
							gasPricePerLiter
							);
					
					
					myDecentralizedSmartCharger.setBatteryConstants(
							batterySizeEV, 
							batterySizePHEV,
							batteryMinEV,
							batteryMinPHEV,
							batteryMaxEV,
							batteryMaxPHEV);
						
					
					myDecentralizedSmartCharger.initializeLP(bufferBatteryCharge);
					
					myDecentralizedSmartCharger.initializeChargingSlotDistributor(minChargingLength);
					
					myDecentralizedSmartCharger.setLinkedListValueHashMapVehicles(
							e.getVehicles());
					
					myDecentralizedSmartCharger.initializeHubLoadDistributionReader(
							hubLinkMapping, 
							deterministicHubLoadDistribution,							
							pricingHubDistribution
							);
					
					//RUN
					myDecentralizedSmartCharger.run();					
					
					
					/*
					 * Examples how to use
					 */
					
					//CHRONOLOGICAL SCHEDULES OF AGENTS where each schedule has parking and driving intervals
					LinkedListValueHashMap<Id, Schedule> agentSchedules= 
						myDecentralizedSmartCharger.getAllAgentChargingSchedules();
					
					
					//CHARGING COSTS
					LinkedListValueHashMap<Id, Double> agentChargingCosts= 
						myDecentralizedSmartCharger.getChargingCostsForAgents();
					
					
					//CHARGING SCHEDULES FOR EVERY AGENT
					for (Id id: controler.getPopulation().getPersons().keySet()){
						Schedule agentSchedule= myDecentralizedSmartCharger.getAgentChargingSchedule(id);//.printSchedule();
					}
					
					//LIST OF AGENTS WITH EV WHERE LP FAILED, i.e. where battery swap would be necessary
					LinkedList<Id> agentsWithEVFailure = 
						myDecentralizedSmartCharger.getIdsOfEVAgentsWithFailedOptimization();
					
					
					// GET ALL IDs OF AGENTS WITH EV, PHEV or Combustion engine car
					LinkedList<Id> agentsWithEV = myDecentralizedSmartCharger.getAllAgentsWithEV();
					LinkedList<Id> agentsWithPHEV = myDecentralizedSmartCharger.getAllAgentsWithPHEV();
					LinkedList<Id> agentsWithConventionalCar = myDecentralizedSmartCharger.getAllAgentsWithCombustionVehicle();
					
					
					// DETAILED DATA PER AGENT
					if(agentsWithEV.isEmpty()==false){
						
						// DRIVING CONSUMPTION [Joules]
						Id id= agentsWithEV.get(0);
						myDecentralizedSmartCharger.getAgentChargingSchedule(id).printSchedule();
						System.out.println("Total consumption from battery [joules]" 
								+ myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgentFromBattery(id));
						
						
						// TOTAL DRIVING CONSUMPTION NOT FROM ELECTRIC ENGINE[Joules]
						// either from combustion engine or through potential battery swap for EVs
						System.out.println("Total consumption from engine [joules]" +
								myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgentFromOtherSources(id));
						
						System.out.println("Total emissions from this agent [joules]" + 
								myDecentralizedSmartCharger.joulesToEmissionInKg(
										myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgentFromOtherSources(id)));
								
					}
					
					//TOTAL EMISSIONS
					System.out.println("Total emissions [kg]" +
							myDecentralizedSmartCharger.getTotalEmissions());
					
					
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
	
	
	/**
	 * deterministic distribution of free load over one day
	 * @return
	 * @throws IOException
	 */
	public static LinkedListValueHashMap<Integer, Schedule> readHubs() throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitSchedule());
		hubLoadDistribution1.put(2, makeBullshitSchedule());
		hubLoadDistribution1.put(3, makeBullshitSchedule());
		hubLoadDistribution1.put(4, makeBullshitSchedule());
		return hubLoadDistribution1;
		
	}
	
	
	public static Schedule makeBullshitSchedule() throws IOException{
		
		Schedule bullShitSchedule= new Schedule();
		
		double[] bullshitCoeffs = new double[]{100*3500, 500*3500/(62490.0), 0};// 
		double[] bullshitCoeffs2 = new double[]{914742, -100*3500/(DecentralizedSmartCharger.SECONDSPERDAY-62490.0), 0};
		//62490*(100*3500)/(24*3600-62490))
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
		
		//bullShitSchedule.visualizeLoadDistribution("BullshitSchedule");	
		return bullShitSchedule;
	}
	

	
	/**
	 * fill hubLinkMapping 
	 * assign hubIds to the different Links--> hubLinkMapping.addMapping(link, hub)
	 * according to scenario relevant hublocations
	 * 
	 * @param deterministicHubLoadDistribution
	 * @param hubLinkMapping
	 * @param controler
	 */
	public static void mapHubs(Controler controler, HubLinkMapping hubLinkMapping){
		
		
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
	
		
	
	public static LinkedListValueHashMap<Integer, Schedule> makeBullshitSourceHub(){
		LinkedListValueHashMap<Integer, Schedule> hubSource= new LinkedListValueHashMap<Integer, Schedule>();
		
		Schedule bullShit= new Schedule();
		PolynomialFunction p = new PolynomialFunction(new double[] {3500});
		
		bullShit.addTimeInterval(new LoadDistributionInterval(50000.0, 62490.0, p, true));
		
		hubSource.put(1, bullShit);		
		
		return hubSource;
		
	}
	
	
	
	public static LinkedListValueHashMap<Id, Schedule> makeBullshitAgentVehicleSource(Controler controler){
		LinkedListValueHashMap<Id, Schedule> agentSource= new LinkedListValueHashMap<Id, Schedule>();
		
		
		//Id
		for(Id id : controler.getPopulation().getPersons().keySet()){
			if(Math.random()<0.5){
				Schedule bullShitPlus= new Schedule();
				PolynomialFunction pPlus = new PolynomialFunction(new double[] {3500.0});
				bullShitPlus.addTimeInterval(new LoadDistributionInterval(25000, 26000.0, pPlus, true));
				
				agentSource.put(id, bullShitPlus);	
			}else{
				Schedule bullShitMinus= new Schedule();
				PolynomialFunction pMinus = new PolynomialFunction(new double[] {-3500.0});
				bullShitMinus.addTimeInterval(new LoadDistributionInterval(0, 2000.0, pMinus, true));
				
				agentSource.put(id, bullShitMinus);	
			}
			
		}
		
		return agentSource;
		
	}
	
	
	public static LinkedListValueHashMap<Integer, Schedule> readPricingHubDistribution(double optimal, double suboptimal) throws IOException{
		
		LinkedListValueHashMap<Integer, Schedule> pricing= readHubs();
		
		
		
		
		for(Integer i: pricing.getKeySet()){
			for(int j=0; j<pricing.getValue(i).getNumberOfEntries(); j++){
				
					LoadDistributionInterval l = (LoadDistributionInterval) pricing.getValue(i).timesInSchedule.get(j);
					
					if(l.isOptimal()){
						PolynomialFunction pOpt = new PolynomialFunction(new double[] {optimal});
						pricing.getValue(i).timesInSchedule.set(j, 
								new LoadDistributionInterval(
								l.getStartTime(),
								l.getEndTime(), 
								pOpt, 
								true));
						
						
					}else{
						PolynomialFunction pSubopt = new PolynomialFunction(new double[] {suboptimal});
						pricing.getValue(i).timesInSchedule.set(j, 
								new LoadDistributionInterval(
										l.getStartTime(),
										l.getEndTime(), 
										pSubopt, 
										false));
												
					}
				
			}
			//pricing.getValue(i).printSchedule();
		}
		return pricing;
	/*	final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution;
		final LinkedListValueHashMap<Integer, Schedule> connectivityHubDistribution;*/
		
	}
	
	
	
	
	
	public static LinkedListValueHashMap<Id, ContractTypeAgent>  getAgentContracts(Controler controler){
		LinkedListValueHashMap<Id, ContractTypeAgent> list = new LinkedListValueHashMap<Id, ContractTypeAgent>();
		for(Id id : controler.getPopulation().getPersons().keySet()){
			
			ContractTypeAgent contract= new ContractTypeAgent(true, // up
					true,// down
					true);//reschedule
			list.put(id, contract);
		}
		
		return list;
	}

}
