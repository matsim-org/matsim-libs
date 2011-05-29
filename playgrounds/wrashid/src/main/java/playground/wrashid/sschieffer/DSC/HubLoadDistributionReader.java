/* *********************************************************************** *
 * project: org.matsim.*
 * HubLoadDistributionReader.java
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

package playground.wrashid.sschieffer.DSC;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolver;
import org.apache.commons.math.analysis.solvers.UnivariateRealSolverFactory;
import org.apache.commons.math.optimization.OptimizationException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.Effect3D;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.lib.obj.LinkedListValueHashMap;



/**
 * keeps track of all deterministic and stochastic loads and prices for EVs and PHEVs
 *  over the day.
 * 
 * @author Stella
 *
 */
public class HubLoadDistributionReader {
	
	Controler controler;
	private String outputPath;
	
	UnivariateRealSolverFactory factory = UnivariateRealSolverFactory.newInstance();
	UnivariateRealSolver solverNewton = factory.newNewtonSolver();
		
	private HubLinkMapping hubLinkMapping;
	
	HashMap<Integer, Schedule> deterministicHubLoadDistribution;
	HashMap<Integer, Schedule> deterministicHubLoadDistributionAfterContinuous;
	HashMap<Integer, Schedule> deterministicHubLoadDistributionPHEVAdjusted;
	public HashMap<Integer, TimeDataCollector> deterministicHubLoadAfter15MinBins;
	
	public HashMap<Integer, Schedule> stochasticHubLoadDistribution;
	//public HashMap<Integer, Schedule> stochasticHubLoadDistributionAfterContinuous;
	public HashMap<Integer, Schedule> stochasticHubLoadAfterVehicleAndHubSources;
	public HashMap<Integer, TimeDataCollector> stochasticHubLoadAfter15MinBins;
	
	public HashMap<Id, GeneralSource> locationSourceMapping;
	public HashMap<Id, TimeDataCollector> locationSourceMappingAfter15MinBins;
	
	public HashMap<Id, Schedule> agentVehicleSourceMapping;
	//public HashMap<Id, Schedule> agentVehicleSourceMappingAfterContinuous;
	public HashMap<Id, TimeDataCollector> agentVehicleSourceAfter15MinBins;
	
	HashMap<Integer, Schedule> pricingHubDistribution;
	HashMap<Integer, TimeDataCollector> connectivityHubDistribution;
	
	VehicleTypeCollector myVehicleTypeCollector;
	
	private double minPricePerkWhAllHubs, maxPricePerkWhAllHubs;
		 
	
	/** 
	 * Stores all load information of the various hubs
	 * <li> determinisitc load before and after charging
	 * <li> stochastic loads before and after V2G
	 * 
	 * of load valleys and peak load times
	 * @throws IOException 
	 * @throws OptimizationException 
	 * @throws InterruptedException 
	 */
	public HubLoadDistributionReader(Controler controler, 
			HubLinkMapping hubLinkMapping,
			HashMap<Integer, Schedule> deterministicHubLoadDistribution,			
			HashMap<Integer, Schedule> pricingHubDistribution,		
			VehicleTypeCollector myVehicleTypeCollector,
			String outputPath,
			double minPricePerkWhAllHubs,
			double maxPricePerkWhAllHubs) throws IOException, OptimizationException, InterruptedException{
		
		this.minPricePerkWhAllHubs= minPricePerkWhAllHubs;
		this.maxPricePerkWhAllHubs = maxPricePerkWhAllHubs;
		
		this.controler=controler;
		
		this.hubLinkMapping=hubLinkMapping;
		
		this.deterministicHubLoadDistribution=deterministicHubLoadDistribution; // continuous functions
		
		initializeDeterministicHubLoadDistributionAfter();
		
		this.pricingHubDistribution=pricingHubDistribution; // continuous functions with same intervals as deterministic HubLoadDistribution!!!
		
		this.myVehicleTypeCollector= myVehicleTypeCollector;
		
		this.outputPath=outputPath;
		
		solverNewton.setMaximalIterationCount(10000);
		checkForPlugInHybrid();// if PHEV then deterministicLoad function changes for this one
		
		visualizeLoadDistributionGeneralAndPHEV();
		
		if (false==checkIfPricingAndDeterministicHaveSameTimeIntervals()){
			System.out.println("WRONG INPUT: Deterministic Load Distribution " +
					"does not have same time intervals as pricing Distribution");
			controler.wait();
		}
	
		initializeConnectivityHubDistribution();
		
	}
	
	public double getMinPricePerKWHAllHubs(){
		return minPricePerkWhAllHubs;
	}
	
	public double getMaxPricePerKWHAllHubs(){		
		return maxPricePerkWhAllHubs;
	}
	
	/**
	 * initializes deterministicHubLoadDistributionAfter with the values initially passed in
	 */
	public void initializeDeterministicHubLoadDistributionAfter(){
		deterministicHubLoadDistributionAfterContinuous= new HashMap<Integer, Schedule> ();
		deterministicHubLoadAfter15MinBins= new HashMap<Integer, TimeDataCollector> ();
		
		for(Integer hub: deterministicHubLoadDistribution.keySet()){
			
			deterministicHubLoadDistributionAfterContinuous.put(
					hub, deterministicHubLoadDistribution.get(hub).cloneSchedule());
			
			deterministicHubLoadAfter15MinBins.put(hub, 
					make96BinCollectorFromDayLoadSchedule(deterministicHubLoadDistribution.get(hub)));
					
		}
		
	}
	
	
	
	/**
	 * if there are PHEVs in the simulation the deterministic load is adjusted for them
	 * for all periods of time where charging electricity is necessarily more expensive than driving on gas
	 * </br>
	 * PHEVS use deterministicHubLoadDistributionPHEVAdjusted to guide their charging decisions
	 *
	 * @throws IOException
	 */
	public void checkForPlugInHybrid() throws IOException{
		
		PlugInHybridElectricVehicle p= new PlugInHybridElectricVehicle(new IdImpl(1));
		
		if (myVehicleTypeCollector.containsVehicleTypeForThisVehicle(p)){			
			
			GasType phevGasType= myVehicleTypeCollector.getGasType(p);
			double engineWatt =  myVehicleTypeCollector.getWattOfEngine(p);
			double engineEfficiency = myVehicleTypeCollector.getEfficiencyOfEngine(p);
			
			// gasPriceInCostPerSecond;= cost/second = cost/liter * liter/joules * joules/second    could still take into account * 1/efficiency			
			double  gasPriceInCostPerSecond=phevGasType.getPricePerLiter() * 1/(phevGasType.getJoulesPerLiter()) 
											 * engineWatt;// *(1/engineEfficiency)
						
			deterministicHubLoadDistributionPHEVAdjusted=getPHEVDeterministicHubLoad(gasPriceInCostPerSecond);
			visualizePricingAndGas(gasPriceInCostPerSecond);
			
		}else{
			deterministicHubLoadDistributionPHEVAdjusted=deterministicHubLoadDistribution;
		}
		
	}
	
	
	
	/**
	 * <li>initializes the stochastic loads before and after
	 * <li>sums up location sources and stochastic hub loads - since they are all relevant on the hub level
	 * 	
	 * 
	 * @param stochasticHubLoadDistribution
	 * @param locationSourceMapping
	 * @param agentVehicleSourceMapping
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public void setStochasticSources(
			HashMap<Integer, Schedule> stochasticHubLoadDistribution,
			HashMap<Id, GeneralSource> locationSourceMapping,
			HashMap<Id, Schedule> agentVehicleSourceMapping) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		this.stochasticHubLoadDistribution=stochasticHubLoadDistribution; // continuous functions		
		this.locationSourceMapping=locationSourceMapping;
		this.agentVehicleSourceMapping=agentVehicleSourceMapping;
		//sumStochasticGridAndLocationSources();
		
		// initialize 
		initializeStochasticHubLoadDistributionAfter();
	}
	
	
	/**
	 * initialize the stochastic hub loads after
	 * 
	 */
	public void initializeStochasticHubLoadDistributionAfter(){
		if (stochasticHubLoadDistribution!=null){
			//stochasticHubLoadDistributionAfterContinuous= new HashMap<Integer, Schedule> ();
			stochasticHubLoadAfter15MinBins= new HashMap<Integer, TimeDataCollector> ();
			for(Integer hub: stochasticHubLoadDistribution.keySet()){
				
				stochasticHubLoadAfter15MinBins.put(hub, 
						make96BinCollectorFromDayLoadSchedule(stochasticHubLoadDistribution.get(hub)));
						
				/*stochasticHubLoadDistributionAfterContinuous.put(
						hub, stochasticHubLoadDistribution.get(hub).cloneSchedule());*/
			}
		}
		
		if (agentVehicleSourceMapping!=null){
			//agentVehicleSourceMappingAfterContinuous = new HashMap<Id, Schedule> ();
			agentVehicleSourceAfter15MinBins = new HashMap<Id, TimeDataCollector> ();
			// make a copy of agentVehicleSourceMapping 
			for(Id id: agentVehicleSourceMapping.keySet()){				
				agentVehicleSourceAfter15MinBins.put(id, 
						make96BinCollectorFromDayLoadSchedule(agentVehicleSourceMapping.get(id)));
				
				/*agentVehicleSourceMappingAfterContinuous.put(
						id, agentVehicleSourceMapping.get(id).cloneSchedule());*/
			}
		}
		
		if (locationSourceMapping!=null){			
			locationSourceMappingAfter15MinBins = new HashMap<Id, TimeDataCollector> ();
		
			for(Id i: locationSourceMapping.keySet()){				
				locationSourceMappingAfter15MinBins.put(i, 
						make96BinCollectorFromDayLoadSchedule(locationSourceMapping.get(i).getLoadSchedule()));
				
			}
		}
	}
	
	
	public TimeDataCollector make96BinCollectorFromDayLoadSchedule(Schedule s){
		
		TimeDataCollector dataC= new TimeDataCollector(96);
		for(int i=0; i<96; i++){
			double sec= (60.0*15)*i;
			int interval= s.timeIsInWhichInterval(sec);
			dataC.addDataPoint(i, 
					sec, 
					((LoadDistributionInterval)s.timesInSchedule.get(interval)).getPolynomialFunction().value(sec));
			
		}
		return dataC;
	}
	
	
	
	public boolean isHubSourceAtHub(int hub){
		if (locationSourceMapping==null){
			return false;
		}
		for(Id linkId: locationSourceMapping.keySet()){
			if(getHubForLinkId(linkId)==hub){
				return true;
			}
		}
		return false;
	}
	
	
	public void recalculateStochasticHubLoadCurveAfterVehicleAndHubSources() throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		stochasticHubLoadAfterVehicleAndHubSources= new HashMap<Integer, Schedule>();
				
		for (Integer hub: stochasticHubLoadAfter15MinBins.keySet()){
			/*
			 * if no vehicle or hub loads are in the system
			 * copy the load schedule to stochasticHubLoadAfterVehicleAndHubSources to avoid unnecessary information loss by fitting
			 */
			if (agentVehicleSourceMapping==null && !isHubSourceAtHub(hub)){
				stochasticHubLoadAfterVehicleAndHubSources.put(hub, stochasticHubLoadDistribution.get(hub).cloneSchedule());
								
				DecentralizedSmartCharger.visualizeTwoXYLoadSeriesBeforeAfter(
						stochasticHubLoadDistribution.get(hub), 					
						stochasticHubLoadAfterVehicleAndHubSources.get(hub).makeXYSeriesFromLoadSchedule("stochastic hub load after vehicle and hub sources"),
						"stochastic hub load original",
						outputPath+"V2G/stochasticHubSourceBeforeAfterVehicleHubSource_"+hub+".png", 
						"StochasticHubSourceBeforeAfterVehicleHubSource_"+hub); 
			}
			else{
				/*
				 * if vehicle or hub loads are in the system
				 * if the initial stochasticHubLoad was passed as discrete loadintervals then its likely, that there are significant steps in the function
				 this cannot be accurately displayed with a Polynomial FUnction
				 THus, the initial intervals in the schedule will be used to fit the updated stochastic load
				 */
				if(stochasticHubLoadDistribution.get(hub).getNumberOfEntries()==1){
					PolynomialFunction newFit= stochasticHubLoadAfter15MinBins.get(hub).getFunction();
					
					stochasticHubLoadAfterVehicleAndHubSources.put(hub, LoadFileReader.makeSchedule(newFit));
					DecentralizedSmartCharger.visualizeTwoXYLoadSeriesBeforeAfter(
							stochasticHubLoadDistribution.get(hub), 					
							stochasticHubLoadAfter15MinBins.get(hub).getXYSeriesFromFunction("stochastic hub load after vehicle and hub sources"), 
							"stochastic hub load original",
							outputPath+"V2G/stochasticHubSourceBeforeAfterVehicleHubSource_"+hub+".png", 
							"StochasticHubSourceBeforeAfterVehicleHubSource_"+hub); ;
				}else{
					stochasticHubLoadAfterVehicleAndHubSources.put(hub,
							stochasticHubLoadAfter15MinBins.get(hub).reFitFunctionInIntervalsOfSchedule96Bin(stochasticHubLoadDistribution.get(hub)));
					
					// from schedule XYSeries
					DecentralizedSmartCharger.visualizeTwoXYLoadSeriesBeforeAfter(
							stochasticHubLoadDistribution.get(hub), 					
							stochasticHubLoadAfterVehicleAndHubSources.get(hub).makeXYSeriesFromLoadSchedule("stochastic hub load after vehicle and hub sources"),
							"stochastic hub load original",
							outputPath+"V2G/stochasticHubSourceBeforeAfterVehicleHubSource_"+hub+".png", 
							"StochasticHubSourceBeforeAfterVehicleHubSource_"+hub); 
				}
			}
			
		}
	}	
	
	
	/**
	 * initialize the connectivityHubDistribution
	 * the  HashMap<Integer, TimeDataCollector> connectivityHubDistribution has a time data collector object for every hub
	 * and records for the first second in every minute, if an agent is parking in the particular hub
	 */
	public void initializeConnectivityHubDistribution(){
		connectivityHubDistribution= new HashMap<Integer, TimeDataCollector> ();
		
		for(Integer i: deterministicHubLoadDistribution.keySet()){
			connectivityHubDistribution.put(i, new TimeDataCollector(DecentralizedSmartCharger.MINUTESPERDAY));
		}
		
	}
	
	
	
	/**
	 * Pricing functions should have same time intervals as deterministic time intervals
	 * this function checks returns true if this requirement is correct
	 * @return
	 */
	private boolean checkIfPricingAndDeterministicHaveSameTimeIntervals(){
		boolean isSame=false;
		
		if(pricingHubDistribution.keySet().size()!= 
			deterministicHubLoadDistribution.keySet().size()){
			return isSame;
		}else{
			
			for(Integer i: pricingHubDistribution.keySet()){
				isSame=pricingHubDistribution.get(i).sameTimeIntervalsInThisSchedule(
						deterministicHubLoadDistribution.get(i));				
				
			}
		}
		return isSame;
		
	}
	
	
	
	
	
	/**
	 * return the Hub corresponding to a certain idLink
	 * @param idLink
	 * @return
	 */
	public int getHubForLinkId(Id idLink){
		
		int hubNumber= (int) hubLinkMapping.getHubNumber(idLink.toString()); //returns Number
		
		return hubNumber;
	}
	
	
	
	/**
	 * returns the keySet with all Hub Integer Ids
	 * @return
	 */
	public Set<Integer> getHubKeySet(){
		return deterministicHubLoadDistribution.keySet();
	}
	
	
	public Schedule getDeterministicHubLoadDistribution(int hubId){
		return deterministicHubLoadDistribution.get(hubId);
	}
	
	public Schedule getDeterministicHubLoadDistributionPHEVAdjusted(int hubId){
		return deterministicHubLoadDistributionPHEVAdjusted.get(hubId);
	}
	
	
	/**
	 * gets the loadDistribution Function for a specific time interval and location idLink
	 * @param idLink
	 * @param t
	 * @return
	 */
	public ArrayList <PolynomialFunction> getDeterministicLoadPolynomialFunctionAtLinkAndTime(Id agentId, Id idLink, TimeInterval t){
		
		int hub= getHubForLinkId(idLink);
		Schedule hubLoadSchedule;
		
		if(DecentralizedSmartCharger.hasAgentEV(agentId)){
			hubLoadSchedule = deterministicHubLoadDistribution.get(hub);
		}else{
			hubLoadSchedule = deterministicHubLoadDistributionPHEVAdjusted.get(hub);
		}
		
		
		ArrayList <Integer> intervalList = hubLoadSchedule.intervalIsInWhichTimeInterval(t);
		
		ArrayList <PolynomialFunction> funcList = new ArrayList <PolynomialFunction> (0);
		for(int i=0; i< intervalList.size(); i++){
			LoadDistributionInterval l1= (LoadDistributionInterval) hubLoadSchedule.timesInSchedule.get(intervalList.get(i));
			funcList.add(l1.getPolynomialFunction());
		}
		
		return funcList;
		
	}
	

	
	/**
	 * gets the loadDistribution intervals for a specific time interval and location idLink
	 * @param idLink
	 * @param t
	 * @return
	 */
	public ArrayList <LoadDistributionInterval> getDeterministicLoadDistributionIntervalsAtLinkAndTime(Id agentId, Id idLink, TimeInterval t){
		
		int hub= getHubForLinkId(idLink);
		Schedule hubLoadSchedule;
		
		if(DecentralizedSmartCharger.hasAgentEV(agentId)){
			hubLoadSchedule = deterministicHubLoadDistribution.get(hub);
		}else{
			hubLoadSchedule = deterministicHubLoadDistributionPHEVAdjusted.get(hub);
		}
		
		
		ArrayList <Integer> intervalList = hubLoadSchedule.intervalIsInWhichTimeInterval(t);
		
		ArrayList <LoadDistributionInterval> funcList = new ArrayList <LoadDistributionInterval> (0);
		for(int i=0; i< intervalList.size(); i++){
			LoadDistributionInterval l1= (LoadDistributionInterval) hubLoadSchedule.timesInSchedule.get(intervalList.get(i));
			funcList.add(l1);
		}
		
		return funcList;
		
	}
	
	
	
	/**
	 * gets the pricing Function for a specific time interval and location idLink
	 * @param idLink
	 * @param t
	 * @return
	 */
	public ArrayList <PolynomialFunction> getPricingPolynomialFunctionAtLinkAndTime(Id idLink, TimeInterval t){
		
		int hub= getHubForLinkId(idLink);
		
		Schedule hubLoadSchedule = pricingHubDistribution.get(hub);
		
		ArrayList <Integer> intervalList = hubLoadSchedule.intervalIsInWhichTimeInterval(t);
		
		ArrayList <PolynomialFunction> funcList = new ArrayList <PolynomialFunction> (0);
		for(int i=0; i< intervalList.size(); i++){
			LoadDistributionInterval l1= (LoadDistributionInterval) hubLoadSchedule.timesInSchedule.get(intervalList.get(i));
			funcList.add(l1.getPolynomialFunction());
		}
		
		return funcList;
		
	}
	
	
	public double getValueOfPricingPolynomialFunctionAtLinkAndTime(Id idLink, double time){
		
		int hub= getHubForLinkId(idLink);
		
		Schedule hubLoadSchedule = pricingHubDistribution.get(hub);
		
		int intervalOfTime= hubLoadSchedule.timeIsInWhichInterval(time);
		
		LoadDistributionInterval importantLoad=(LoadDistributionInterval)hubLoadSchedule.timesInSchedule.get(intervalOfTime);
				
		return importantLoad.getPolynomialFunction().value(time);		
		
	}
	
	
	/**
	 * gets the pricing LoadDistributionIntervals for a specific time interval and location idLink
	 * @param idLink
	 * @param t
	 * @return
	 */
	public ArrayList <LoadDistributionInterval> getPricingLoadDistributionIntervalsAtLinkAndTime(Id idLink, TimeInterval t){
		
		int hub= getHubForLinkId(idLink);
		
		Schedule hubLoadSchedule = pricingHubDistribution.get(hub);
		
		ArrayList <Integer> intervalList = hubLoadSchedule.intervalIsInWhichTimeInterval(t);
		
		ArrayList <LoadDistributionInterval> funcList = new ArrayList <LoadDistributionInterval> (0);
		for(int i=0; i< intervalList.size(); i++){
			LoadDistributionInterval l1= (LoadDistributionInterval) hubLoadSchedule.timesInSchedule.get(intervalList.get(i));
			funcList.add(l1);
		}
		
		return funcList;
		
	}
	
	
	/**
	 * returns the deterministicHubLoadDistribution relevant for the current position of the agent (idLink--> hub)
	 * 
	 * for PHEV agents, the deterministicHubLoadDistribution is altered to reflect the preference for gas in those periods, 
	 * where gas is always cheaper than charging electricity
	 * 
	 * @param agentId - Id agent
	 * @param idLink - location Link
	 * @return
	 */
	public Schedule getLoadDistributionScheduleForHubId(Id agentId, Id idLink){
		int hub= getHubForLinkId(idLink);
		if(DecentralizedSmartCharger.hasAgentEV(agentId)){
			return deterministicHubLoadDistribution.get(hub);
		}else{
			return deterministicHubLoadDistributionPHEVAdjusted.get(hub);
		}
		
		
	}
	

		
	
	
	
	/**
	 * VISUALIZATION PURPOSE
	 * updates the double[] loadAfterDeterministicChargingDecision with charging activities of agents; 
	 * only done for first second in minute, to get an overall idea of load distribution for visualization purposes over the day
	 * 
	 * @param linkId
	 * @param minInDay
	 * @param wattReduction
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 */
	public void updateLoadAfterDeterministicChargingDecision(double start, double end, 
			Id linkId, double chargingSpeed, boolean continuous) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		int hubId= getHubForLinkId(linkId);
		if(continuous){
			Schedule toChange=deterministicHubLoadDistributionAfterContinuous.get(hubId);
			//toChange.printSchedule();
			toChange.addLoadDistributionIntervalToExistingLoadDistributionSchedule(
					new LoadDistributionInterval(start, end, new PolynomialFunction(new double[]{-chargingSpeed}), false));
			deterministicHubLoadDistributionAfterContinuous.put(hubId, toChange);
		}
		
	
		deterministicHubLoadAfter15MinBins.get(hubId).
			increaseYEntryOf96EntryBinCollectorBetweenSecStartEnd(start, end,-chargingSpeed);
		
		
	}
	
	
	
	
	
	public void cleanUpDeterministicAfterSchedules(){
		for(Integer i: deterministicHubLoadDistributionAfterContinuous.keySet()){			 
			deterministicHubLoadDistributionAfterContinuous.get(i).cleanUpLoadSchedule();
		}
	}
	
	
	/**
	 * 
	 * provide minute in which agent is parking, and hub at which he is parking
	 * and the corresponding data value in connectivityHubDistribution will be increased by 1
	 * @param minute
	 * @param hub
	 */
	public void recordParkingAgentAtHubInMinute(
			int minute,
			int hub
			){
		
		double second= minute*DecentralizedSmartCharger.SECONDSPERMIN;
		double before= connectivityHubDistribution.get(hub).getYAtEntry(minute);
		connectivityHubDistribution.get(hub).addDataPoint(minute, second, before+1);
	}
	
	
	
	/**
	 * extrapolates a value for the estimated number of parking vehicles at the hub
	 * from the recorded data in connectivityHubDistribution
	 * 
	 * <p> this function is called in the Decentralized Smart Charger within the function checkHubStochasticLoads()</p>
	 * @param hub
	 * @param time
	 * @return
	 */
	public double getExpectedNumberOfParkingAgentsAtHubAtTime(int hub, double time){
		/*
		 * using the function is not accurate enough
		 * extrapolation of values works better
		 */	
		return connectivityHubDistribution.get(hub).extrapolateValueAtTimeFromDataCollectorEveryMin( time);
		
	}
	
	
	
	
	/**
	 * visualizes the connectivity of agents at hubs from recorded connectivityHubDistribution data
	 * called after findChargingDistribution() in the Decentralized Smart Charger
	 * @throws OptimizationException
	 * @throws IOException
	 */
	public void calculateAndVisualizeConnectivityDistributionsAtHubsInHubLoadReader() throws OptimizationException, IOException{
		
		XYSeriesCollection connectivity= new XYSeriesCollection();
		
		for(Integer i: connectivityHubDistribution.keySet()){
			
			TimeDataCollector data= connectivityHubDistribution.get(i);
			data.fitFunction();
			if(DecentralizedSmartCharger.debug){
				System.out.println("Parking Vehicles at Hub"+ i);
				System.out.println(data.getFunction().toString());
			}
			
			XYSeries xx= data.getXYSeries("Parking Vehicles at Hub"+ i);
			connectivity.addSeries(xx);
			
		/*	XYSeries xxFit= data.getXYSeriesFromFunction("Parking Vehicles at Hub"+ i+ "(fitted function)");			
			connectivity.addSeries(xxFit);*/
		}
		
		JFreeChart chart = ChartFactory.createXYLineChart("Number of parking agents at hubs over the day", 
				"time [s]", 
				"number of agents", 
				connectivity, 
				PlotOrientation.VERTICAL, 
				true, true, false);
		
		chart.setBackgroundPaint(Color.white);
		
		final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray);
		
     
        for(Integer i:  connectivityHubDistribution.keySet()){
        	
        	plot.getRenderer().setSeriesPaint(i, Color.black);//after
        	
        	plot.getRenderer().setSeriesStroke(
                    i, 
                  
                    new BasicStroke(
                        3.0f,  //float width
                        BasicStroke.CAP_ROUND, //int cap
                        BasicStroke.JOIN_ROUND, //int join
                        1.0f, //float miterlimit
                        new float[] {1.0f, 1.0f}, //float[] dash
                        0.0f //float dash_phase
                    )
                );
      
        }
        String s= outputPath+ "Hub\\connectivityOfAgentsOverDay.png";
        ChartUtilities.saveChartAsPNG(new File(s) , chart, 1000, 1000);
       
	}
	
	
	
	/**
	 * method looks at the pricing functions.
	 * if the prices for charging are above the gas price, the determinstic load distribution profile will be adjusted for the PHEV case.
	 * those time intervals will be turned into suboptimal parking slots
	 * @return
	 */
	private HashMap<Integer, Schedule> getPHEVDeterministicHubLoad(double gasPriceInCostPerSecond){
		
		HashMap<Integer, Schedule> hubLoadDistributionPHEVAdjusted= new HashMap<Integer, Schedule> ();
		
		for(Integer i : pricingHubDistribution.keySet()){
			
			Schedule pricingS= pricingHubDistribution.get(i);
			
			Schedule deterministicSchedule=deterministicHubLoadDistribution.get(i);
			
			Schedule sPHEV= new Schedule();
			for(int j=0; j<pricingS.getNumberOfEntries(); j++){
				
				LoadDistributionInterval currentDeterministicLoadInterval= (LoadDistributionInterval)deterministicSchedule.timesInSchedule.get(j);
				
				LoadDistributionInterval currentPricingLoadInterval= (LoadDistributionInterval)pricingS.timesInSchedule.get(j);
				
				PolynomialFunction func= currentPricingLoadInterval.getPolynomialFunction();
				
				double [] d= func.getCoefficients();
				d[0]=d[0]-gasPriceInCostPerSecond;
				
				PolynomialFunction f = new PolynomialFunction(d);
				
				ArrayList<ChargingInterval> badIntervals= new ArrayList<ChargingInterval> ();
				
				checkRoot(currentDeterministicLoadInterval, 
						f, 
						gasPriceInCostPerSecond,
						badIntervals);
				
				PolynomialFunction pBad= new PolynomialFunction(new double[]{-1000000});
				
				if(badIntervals.size()==0){
					sPHEV.addTimeInterval(currentDeterministicLoadInterval);
					
				}else{
					
					
					// IF BAD INTERVALS DONT START AT BEGINNING OF INTERVAL
					if(currentDeterministicLoadInterval.getStartTime()<badIntervals.get(0).getStartTime()){
						
						LoadDistributionInterval lReplaceSubOptimal= new LoadDistributionInterval(currentDeterministicLoadInterval.getStartTime(), 
								badIntervals.get(0).getStartTime(), 
								currentDeterministicLoadInterval.getPolynomialFunction(), 
								currentDeterministicLoadInterval.isOptimal());
						sPHEV.addTimeInterval(lReplaceSubOptimal);
					}
					
					//FOR ALL BAD INTERVALS
					
					for(int u=0; u<badIntervals.size(); u++){
						
						TimeInterval t= badIntervals.get(u);
						LoadDistributionInterval lReplaceSubOptimal= new LoadDistributionInterval(t.getStartTime(), 
								t.getEndTime(), 
								pBad, 
								false);
						
						sPHEV.addTimeInterval(lReplaceSubOptimal);
						
						
						//****************good interval between to  consecutive bad intervals
						// add good interval
						
						if(u<badIntervals.size()-1){// if second to last or before
							
							if(badIntervals.get(u).getEndTime() < badIntervals.get(u+1).getStartTime()){
								LoadDistributionInterval lReplaceSubOptimal2= new LoadDistributionInterval(
										badIntervals.get(u).getEndTime(), 
										badIntervals.get(u+1).getStartTime(), 
										currentDeterministicLoadInterval.getPolynomialFunction(), 
										currentDeterministicLoadInterval.isOptimal());
								sPHEV.addTimeInterval(lReplaceSubOptimal2);
							}
						}
						
					}
					
					// IF BAD INTERVALS DONT STOP AT END OF INTERVAL
					double currentEnd= currentDeterministicLoadInterval.getEndTime();
					double badIntervalEnd= badIntervals.get(badIntervals.size()-1 ).getEndTime();
					
					if(currentEnd>badIntervalEnd){
						
						// if objective f >0 in last intervcal, then PHEV is bad interval
						LoadDistributionInterval lReplaceSubOptimal;
						if(f.value( (currentEnd+badIntervalEnd)/2)>0){
							lReplaceSubOptimal= new LoadDistributionInterval(
									badIntervals.get(badIntervals.size()-1 ).getEndTime(), 
									currentDeterministicLoadInterval.getEndTime(), 
									pBad,
									currentDeterministicLoadInterval.isOptimal());
						}else{
							lReplaceSubOptimal= new LoadDistributionInterval(
									badIntervals.get(badIntervals.size()-1 ).getEndTime(), 
									currentDeterministicLoadInterval.getEndTime(), 
									currentDeterministicLoadInterval.getPolynomialFunction(), 
									currentDeterministicLoadInterval.isOptimal());
						}
						
						sPHEV.addTimeInterval(lReplaceSubOptimal);
					}
					
				}
				
			}
			
			sPHEV.sort();		
			hubLoadDistributionPHEVAdjusted.put(i, sPHEV);
			
		}
		
		return hubLoadDistributionPHEVAdjusted;
		
	}




	/**
	 * method is called within getPHEVDeterministicHubLoad()
	 * 
	 *it finds the roots=the times, where the price equals the gas price
	 *
	 * @param l
	 * @param objective
	 * @param badIntervals
	 */
	private void checkRoot(LoadDistributionInterval l, 
			PolynomialFunction objective, 
			double gasPriceInCostPerSecond, // function - gasprice
			ArrayList<ChargingInterval> badIntervals){
		
		
		//*********************************
		
		if(objective.degree()==0){
			//constant
			if(objective.getCoefficients()[0]>0.0){
				// then entire interval is 
				badIntervals.add(new ChargingInterval(l.getStartTime(), l.getEndTime()));
			}
			
		}else{
			if(objective.degree()==1){
				//linear
				double c;
				try {
					c = solverNewton.solve(objective, l.getStartTime(), l.getEndTime());
					if(c<=l.getEndTime() && c>=l.getStartTime()){
						
						//contains bad interval
						
						if(objective.value((l.getStartTime()+c)/2)>0){
							badIntervals.add(new ChargingInterval(l.getStartTime(), c));
						}else{
							badIntervals.add(new ChargingInterval( c,l.getEndTime()));
						}
						
					}
				} catch (ConvergenceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FunctionEvaluationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				
				
			}else{
				//anything else
				ArrayList<Double> roots = new ArrayList<Double>(0);
				
				//
				for(double i=l.getStartTime(); i<=l.getEndTime();  ){
					
					if(Math.abs(objective.value(i))<0.0001){
						
						try {
							double c = solverNewton.solve(objective, l.getStartTime(), l.getEndTime(), i);
							
							//System.out.println("c: "+c);
							//System.out.println("start: "+l.getStartTime()+", end: "+ l.getEndTime()+ " guess "+ i);
							if(c<=l.getEndTime() && c>=l.getStartTime()){
								// if roots are found multiple times
								// check first, if 'same (error 1 minute)' root has been founded before already
								if (roots.size()>1){
									if(Math.abs(roots.get(roots.size()-1))-c>60.0 ){
										roots.add(c);
									}
								}
								if(roots.size()==0){
									roots.add(c);
								}
								
							}
						} catch (ConvergenceException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (FunctionEvaluationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
					i+=60;
				}
				
				double start= l.getStartTime();
				boolean good;
				if(objective.value(l.getStartTime())<0)
				{good=true;}else{good=false;}
				
				
				// now clean up root array... find bad intervals
				for(int i=0; i<roots.size();i++){
					
					if(start!=roots.get(i) ){
						if(good){
							start=roots.get(i);
							good=false;
						}else{
							badIntervals.add(new ChargingInterval(start, roots.get(i)));
							start=roots.get(i);
							good=true;
						}
					}
					
				}
				if(start!=l.getEndTime()){
					//add last interval
					if(!good){
						badIntervals.add(new ChargingInterval(start, l.getEndTime()));
					}
				}
				
				
			}
			
		}
	}
	

	
	
	/**
	 * prints an arrayList of Charging Intervals
	 * good for debugging
	 * @param b
	 */
	private void printArrayList(ArrayList<ChargingInterval> b){
		for(int i=0; i<b.size();i++){
			b.get(i).printInterval();
		}
	}

	
	
	/**
	 * visualizes the price distribution of gas and electricitz at the different hubs over the day
	 * 
	 * <p> for every hub a .png file is saved under: outputPath+ "Hub\\pricesHub_"+ i.toString()+".png" </p>
	 * @param gasPriceInCostPerSecond
	 * @throws IOException
	 */
	private void visualizePricingAndGas(double gasPriceInCostPerSecond) throws IOException{
		for( Integer i : pricingHubDistribution.keySet() ){
			XYSeriesCollection onlyChargingPrices= new XYSeriesCollection();
			XYSeriesCollection prices= new XYSeriesCollection();
			//************************************
			//AFTER//BEFORE
			
			XYSeries hubPricing= new XYSeries("electricity prices at hub"+i.toString());
			XYSeries gasPriceXY= new XYSeries("gas price");
			
			gasPriceXY.add(0, gasPriceInCostPerSecond);
			gasPriceXY.add(DecentralizedSmartCharger.SECONDSPERDAY,gasPriceInCostPerSecond);
			
			for(int j=0; j<pricingHubDistribution.get(i).getNumberOfEntries(); j++){
				
				LoadDistributionInterval l= (LoadDistributionInterval)pricingHubDistribution.get(i).timesInSchedule.get(j);
				
				for(double time=l.getStartTime(); time<l.getEndTime(); time++){
					hubPricing.add(time, l.getPolynomialFunction().value(time)); 
				}
				
				
			}
			onlyChargingPrices.addSeries(hubPricing);
			prices.addSeries(hubPricing);
			prices.addSeries(gasPriceXY);
			
			JFreeChart chartOnlyCharging = ChartFactory.createXYLineChart("Prices at Hub "+ i.toString(), 
					"time [s]", 
					"price [CHF/s]", 
					onlyChargingPrices, 
					PlotOrientation.VERTICAL, 
					true, true, false);
			
			chartOnlyCharging.setBackgroundPaint(Color.white);
			
			final XYPlot plot = chartOnlyCharging.getXYPlot();
	        plot.setBackgroundPaint(Color.white);
	        plot.setDomainGridlinePaint(Color.gray); 
	        plot.setRangeGridlinePaint(Color.gray);
			
	        plot.getRenderer().setSeriesPaint(0, Color.black);//after
	        
	        plot.getRenderer().setSeriesPaint(1, Color.red);//after
	        
        	plot.getRenderer().setSeriesStroke(
	            0, 
	          
	            new BasicStroke(
	                1.0f,  //float width
	                BasicStroke.CAP_ROUND, //int cap
	                BasicStroke.JOIN_ROUND, //int join
	                1.0f, //float miterlimit
	                new float[] {1.0f, 0.0f}, //float[] dash
	                0.0f //float dash_phase
	            )
	        );
        	
        	
        	ChartUtilities.saveChartAsPNG(new File(outputPath+ "Hub\\electricityPricesHub_"+ i.toString()+".png") , chartOnlyCharging, 1000, 1000);
            
			
			//************************************
			JFreeChart chart = ChartFactory.createXYLineChart("Electricity And Gas Prices at Hub "+ i.toString(), 
					"time [s]", 
					"price [CHF/s]", 
					prices, 
					PlotOrientation.VERTICAL, 
					true, true, false);
			
			chart.setBackgroundPaint(Color.white);
			
			final XYPlot  plot2 = chart.getXYPlot();
	        plot2.setBackgroundPaint(Color.white);
	        plot2.setDomainGridlinePaint(Color.gray); 
	        plot2.setRangeGridlinePaint(Color.gray);
			
	        plot2.getRenderer().setSeriesPaint(0, Color.black);//after
	        
	        plot2.getRenderer().setSeriesPaint(1, Color.red);//after
	        
        	plot2.getRenderer().setSeriesStroke(
	            0, 
	          
	            new BasicStroke(
	                1.0f,  //float width
	                BasicStroke.CAP_ROUND, //int cap
	                BasicStroke.JOIN_ROUND, //int join
	                1.0f, //float miterlimit
	                new float[] {1.0f, 0.0f}, //float[] dash
	                0.0f //float dash_phase
	            )
	        );
        	
        	
        	ChartUtilities.saveChartAsPNG(new File(outputPath+ "Hub\\pricesHub_"+ i.toString()+".png") , chart, 1000, 1000);
            
		}
		
	}

	
	
	/**
	 * saves a visualization of the deterministic load as applicable for EVs and PHEVs
	 * under:
	 * outputPath+ "Hub\\hubDeterministicforEVs_PHEVsLoad_"+ i.toString()+".png"
	 * @throws IOException
	 */
	private void visualizeLoadDistributionGeneralAndPHEV() throws IOException{
				
		for( Integer i : deterministicHubLoadDistribution.keySet()){
			
			XYSeriesCollection deterministicGeneral= new XYSeriesCollection();
			
			//************************************
			//AFTER//BEFORE
			
			XYSeries hubDeterministic= new XYSeries("hub"+i.toString()+"Deterministic");
						
			for(int j=0; j<deterministicHubLoadDistribution.get(i).getNumberOfEntries(); j++){
				
				LoadDistributionInterval l= (LoadDistributionInterval)deterministicHubLoadDistribution.get(i).timesInSchedule.get(j);
				
				for(double time=l.getStartTime(); time<l.getEndTime(); time++){
					hubDeterministic.add(time, l.getPolynomialFunction().value(time)); 
				}
			}
			
			deterministicGeneral.addSeries(hubDeterministic);
			
			XYSeries hubDeterministicPHEV= new XYSeries("hub"+i.toString()+"DeterministicPHEV");
			//deterministicHubLoadDistributionPHEVAdjusted.getValue(i).printSchedule();
			
			for(int j=0; j<deterministicHubLoadDistributionPHEVAdjusted.get(i).getNumberOfEntries(); j++){
				
				LoadDistributionInterval l= (LoadDistributionInterval)deterministicHubLoadDistributionPHEVAdjusted.get(i).timesInSchedule.get(j);
				
				for(double time=l.getStartTime(); time<l.getEndTime(); time++){
					hubDeterministicPHEV.add(time, l.getPolynomialFunction().value(time)); 
				}
				
				
			}
			
			deterministicGeneral.addSeries(hubDeterministicPHEV);
					
			//************************************
			JFreeChart chart = ChartFactory.createXYLineChart("Deterministic LoadDistribution "+ i.toString(), 
					"time [s]", 
					"load [W]", 
					deterministicGeneral, 
					PlotOrientation.VERTICAL, 
					true, true, false);
			
			chart.setBackgroundPaint(Color.white);
			
			final XYPlot plot = chart.getXYPlot();
	        plot.setBackgroundPaint(Color.white);
	        plot.setDomainGridlinePaint(Color.gray); 
	        plot.setRangeGridlinePaint(Color.gray);
			
	        plot.getRenderer().setSeriesPaint(0, Color.black);//after
	        plot.getRenderer().setSeriesPaint(1, Color.red);//after
	        
        	plot.getRenderer().setSeriesStroke(
	            0, 
	          
	            new BasicStroke(
	                1.0f,  //float width
	                BasicStroke.CAP_ROUND, //int cap
	                BasicStroke.JOIN_ROUND, //int join
	                1.0f, //float miterlimit
	                new float[] {1.0f, 0.0f}, //float[] dash
	                0.0f //float dash_phase
	            )
	        );
        	
        	plot.getRenderer().setSeriesStroke(
    	            1, 
    	          
    	            new BasicStroke(
    	                1.0f,  //float width
    	                BasicStroke.CAP_ROUND, //int cap
    	                BasicStroke.JOIN_ROUND, //int join
    	                1.0f, //float miterlimit
    	                new float[] {6.0f, 6.0f}, //float[] dash
    	                0.0f //float dash_phase
    	            )
    	        );
        	
        	
        	ChartUtilities.saveChartAsPNG(new File(outputPath+ "Hub\\hubDeterministicforEVs_PHEVsLoad_"+ i.toString()+".png") , chart, 1000, 1000);
            
		}
		}
		
	}
	

