
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


public class Main {
	
	
	
	public static void main(String[] args) throws IOException {
		
		
		final ParkingTimesPlugin parkingTimesPlugin;		
		
		/*
		 * //0,142500 €/kWh - 1 hour at 1kW
		// 0,4275 1 hour at 3.5kW
		//  per second =  0,4275/(3600)
		 */
		final double optimalPrice=0.4275/(3600); // cost/second - CAREFUL would have to implement different multipliers for high speed or regular connection
		final double suboptimalPrice=optimalPrice*3; // cost/second    
		final double gasPricePerLiter= 0.25;
		
		final LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution= readHubs();
		final LinkedListValueHashMap<Integer, Schedule> stochasticHubLoadDistribution=readStochasticLoad(deterministicHubLoadDistribution.size());
		final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution=readPricingHubDistribution(optimalPrice, suboptimalPrice);
		final LinkedListValueHashMap<Integer, Schedule> connectivityHubDistribution;
		
		
		
		final HubLinkMapping hubLinkMapping=new HubLinkMapping(deterministicHubLoadDistribution.size());//= new HubLinkMapping(0);
		
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";
				
		final String outputPath="C:\\Users\\stellas\\Output\\V1G\\";
		
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		final double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		final double emissionPerLiterEngine = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		
		
		
		final double bufferBatteryCharge=0.0;
		
		final double batterySizeEV= 17*3600*1000; 
		final double batterySizePHEV= 17*3600*1000; 
		final double batteryMinEV= 0.1; 
		final double batteryMinPHEV= 0.1; 
		final double batteryMaxEV= 0.9; 
		final double batteryMaxPHEV= 0.9; 
		
		
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
					
					
					/*
					 * TODO
					 * MAPPING AND READING HUBS
					 * Redo reading and mapping hubs dependent on scenario
					 * 
					 * CHARGING SPEED HUBS
					 * //TODO in scenario charging speed must be mapped to facilities
					// this was not the case in the first scenario and thus not yet possible
					// current default is 3500 W;
					//getChargingSpeed()
					 */
					
					
					
					/*
					 * TODO
					 * PRICE
					 * Discussions on Price not clear...
					 * Price is also dependent on scenario
					 * @Rashid - you have to be clear on how you would like this to be implemented
					 * 
					 * I can compute internally easily...
					 * but we have to define price functions similar to 
					 * LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution
					 * I JUST ASSUMED SOME FUNCTIONS NOW BASED ON DUAL TARIFF SCHEME
					 */
					
					/*
					 * TODO
					 * VEHICLE CLASS
					 * NO CONSUMPTION VALUES FOR COMBUSTION CARS
					 * --> emissions
					 * 
					 */
					
					/*
					 * TODO
					 * VEHICLE
					 * BATTERY SIZE AD MIN AND MAX CHARGE NOT WORKING... always 0 values
					 * 
					 */
					
					
					//*****************************************
					//How to initialize and run
					//*****************************************
					
					
					
					//*****************************************
					//for V2G
					//*****************************************
				
					LinkedListValueHashMap<Integer, Schedule> locationSourceMapping= makeBullshitSourceHub();
					//hub/LoadDIstributionSchedule
					
					LinkedListValueHashMap<Id, Schedule> agentVehicleSourceMapping= makeBullshitAgentVehicleSource(controler);
					//agent/LoadDIstributionSchedule
					LinkedListValueHashMap<Id, ContractTypeAgent> agentContracts= getAgentContracts(controler);
					
					
					
					//*****************************************
					//for Decentralized Smart Charging
					//*****************************************
					mapHubs(controler,hubLinkMapping);
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
							event.getControler(), 
							parkingTimesPlugin,
							e.getEnergyConsumptionPlugin(),
							outputPath, 
							gasJoulesPerLiter,
							emissionPerLiterEngine,
							gasPricePerLiter
							);
					
					
					myDecentralizedSmartCharger.setAgentContracts(agentContracts);
					
					myDecentralizedSmartCharger.setBatteryConstants(
							batterySizeEV, 
							batterySizePHEV,
							batteryMinEV,
							batteryMinPHEV,
							batteryMaxEV,
							batteryMaxPHEV);
						
					
					myDecentralizedSmartCharger.initializeLP(bufferBatteryCharge);
					
					myDecentralizedSmartCharger.initializeChargingSlotDistributor(MINCHARGINGLENGTH);
					
					myDecentralizedSmartCharger.setLinkedListValueHashMapVehicles(
							e.getVehicles());
					
					myDecentralizedSmartCharger.initializeHubLoadDistributionReader(
							hubLinkMapping, 
							deterministicHubLoadDistribution,
							stochasticHubLoadDistribution,
							pricingHubDistribution,
							locationSourceMapping,
							agentVehicleSourceMapping);
					// pricing and deterministicHubLoadDistribution  have to have same time intervals
				
					
					myDecentralizedSmartCharger.run();
					
					
					
					
					//*****************************************
					//Examples how to use
					//*****************************************
					LinkedListValueHashMap<Id, Double> agentCharginCosts= 
						myDecentralizedSmartCharger.getChargingCostsForAgents();
					
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
						
						System.out.println("Total emissions [kg]" +
								myDecentralizedSmartCharger.getTotalEmissions());
						
						myDecentralizedSmartCharger.clearResults();
					}
					
					
					
					
					
					
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
		
		Schedule bullShit= new Schedule();
		PolynomialFunction p = new PolynomialFunction(new double[] {3500});
		
		bullShit.addTimeInterval(new LoadDistributionInterval(50000.0, 62490.0, p, true));
		
		//Id
		for(Id id : controler.getPopulation().getPersons().keySet()){
			agentSource.put(id, bullShit);	
		}
		
		return agentSource;
		
	}
	
	
	public static LinkedListValueHashMap<Integer, Schedule> readPricingHubDistribution(double optimal, double suboptimal) throws IOException{
		
		LinkedListValueHashMap<Integer, Schedule> pricing= readHubs();
		
		PolynomialFunction pOpt = new PolynomialFunction(new double[] {optimal});
		PolynomialFunction pSubopt = new PolynomialFunction(new double[] {suboptimal});
		
		for(Integer i: pricing.getKeySet()){
			for(int j=0; j<pricing.getValue(i).getNumberOfEntries(); j++){
				
					LoadDistributionInterval l = (LoadDistributionInterval) pricing.getValue(i).timesInSchedule.get(j);
					
					if(l.isOptimal()){
						pricing.getValue(i).timesInSchedule.set(j, 
								new LoadDistributionInterval(
								l.getStartTime(),
								l.getEndTime(), 
								pOpt, 
								true));
						
						
					}else{
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
			
			ContractTypeAgent contract= new ContractTypeAgent(false , // up
					true,// down
					false);//reschedule
			list.put(id, contract);
		}
		
		return list;
	}

}
