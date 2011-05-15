package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import lpsolve.LpSolveException;

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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;


import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ConventionalVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.Main;


/* *********************************************************************** *
 * project: org.matsim.*
 * DecentralizedV1G.java
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


/**
 * Controls the charging algorithm
	 * 1) determining and sorting agents schedules
	 * 2) LP
	 * 3) charging slot optimization
	 * 4) V2G 
	 * stores results of charging algorithm in LinkedListValueHashMap
 * @author Stella
 *
 */
public class DecentralizedSmartCharger {
	
	
	private double startTime, agentReadTime, LPTime, distributeTime, wrapUpTime;
	private double startV2G, timeCheckVehicles,timeCheckOtherSources;
	
	public DifferentiableMultivariateVectorialOptimizer optimizer;
	public VectorialConvergenceChecker checker= new SimpleVectorialValueChecker(10000,-10000);//
	//(double relativeThreshold, double absoluteThreshold)
	//In order to perform only relative checks, the absolute tolerance must be set to a negative value. 
	//In order to perform only absolute checks, the relative tolerance must be set to a negative value.
	
	public static SimpsonIntegrator functionIntegrator= new SimpsonIntegrator();
	public NewtonSolver newtonSolver= new NewtonSolver();	
	public GaussNewtonOptimizer gaussNewtonOptimizer= new GaussNewtonOptimizer(true); //useLU - true, faster  else QR more robust
		
	public static PolynomialFitter polyFit;
	public static boolean debug=false;
	
	final public static double SECONDSPERMIN=60;
	final public static double SECONDSPER15MIN=15*60;
	final public static double SECONDSPERDAY=24*60*60;
	final public static int MINUTESPERDAY=24*60;
	
		
	public static HubLoadDistributionReader myHubLoadReader;
	public static ChargingSlotDistributor myChargingSlotDistributor;
	public static AgentTimeIntervalReader myAgentTimeReader;
	public static V2G myV2G;
	
	private HashMap<Id, Schedule> agentParkingAndDrivingSchedules = new HashMap<Id, Schedule>(); 
	private HashMap<Id, Schedule> agentChargingSchedules = new HashMap<Id, Schedule>();
	
	private HashMap<Id, ContractTypeAgent> agentContracts;
	
	public double minChargingLength;
	
	public double emissionCounter=0.0;
	
	final Controler controler;
	
	public LPEV lpev;
	public LPPHEV lpphev;
	public LPCombustion lpcombustion;
	
	public LinkedList<Id> chargingFailureEV=new LinkedList<Id>();
	public LinkedList<Id> agentsWithEV=new LinkedList<Id>();
	public LinkedList<Id> agentsWithPHEV=new LinkedList<Id>();
	public LinkedList<Id> agentsWithCombustion=new LinkedList<Id>();
	
	private HashMap<Id, Double> agentChargingCosts = new HashMap<Id,  Double>();
	
	public static String outputPath;
	
	final public static DrawingSupplier supplier = new DefaultDrawingSupplier();
	
	
	public static LinkedListValueHashMap<Id, Vehicle> vehicles;
	public ParkingTimesPlugin parkingTimesPlugin;
	public EnergyConsumptionPlugin energyConsumptionPlugin;

	private VehicleTypeCollector myVehicleTypes;

	private TimeDataCollector countDriving = new TimeDataCollector(MINUTESPERDAY);
	private TimeDataCollector countParking = new TimeDataCollector(MINUTESPERDAY);
	private TimeDataCollector countCharging= new TimeDataCollector(MINUTESPERDAY);

	
	//***********************************************************************
	
	public DecentralizedSmartCharger(Controler controler, 
			ParkingTimesPlugin parkingTimesPlugin,
			EnergyConsumptionPlugin energyConsumptionPlugin,
			String outputPath,
			VehicleTypeCollector myVehicleTypes
			
	) throws IOException, OptimizationException{
		
		this.controler=controler;
						
		this.outputPath=outputPath;		
		
		gaussNewtonOptimizer.setMaxIterations(100000);		
		gaussNewtonOptimizer.setConvergenceChecker(checker);		
		optimizer=gaussNewtonOptimizer;
		
		polyFit= new PolynomialFitter(20, optimizer);
			
		myAgentTimeReader= new AgentTimeIntervalReader(
				parkingTimesPlugin, 
				energyConsumptionPlugin);
		
		this.myVehicleTypes=myVehicleTypes;
		
		myV2G= new V2G(this);
		
	}
	
	
	
	public void setAgentContracts(HashMap<Id, ContractTypeAgent> agentContracts){
		this.agentContracts= agentContracts;
	}


	/**
	 * turns extra output on or off, that can be helpful for debugging
	 * i.e. understanding because of which agent the simulation was shut down, etc.
	 * @param onOff
	 */
	public void setDebug(boolean onOff){
		debug=onOff;
	}
	
	/**
	 * initialize LPs for EV, PHEV and combustion vehicle
	 * <li>the buffer is important for the EV calculation
	 * <li> the boolean regulates if SOC graphs for all agents over the day are printed after the LPs
	 * @param buffer
	 * @param output
	 */
	public void initializeLP(double buffer, boolean output){
		lpev=new LPEV(buffer, output);
		lpphev=new LPPHEV(output);
		lpcombustion= new LPCombustion();
	}



	public void initializeChargingSlotDistributor(double minChargingLength){
		this.minChargingLength=minChargingLength; 
		myChargingSlotDistributor=new ChargingSlotDistributor(minChargingLength);
	}



	public void setLinkedListValueHashMapVehicles(LinkedListValueHashMap<Id, Vehicle> vehicles){
		this.vehicles=vehicles;
	}



	/**
	 * initializes HubLoadDistributionReader with its basic parameters
	 * @param hubLinkMapping
	 * @param deterministicHubLoadDistribution
	 * @param pricingHubDistribution
	 * @throws OptimizationException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void initializeHubLoadDistributionReader(
			HubLinkMapping hubLinkMapping, 
			HashMap<Integer, Schedule> deterministicHubLoadDistribution,			
			HashMap<Integer, Schedule> pricingHubDistribution
			) throws OptimizationException, IOException, InterruptedException{
		
		myHubLoadReader=new HubLoadDistributionReader(controler, 
				hubLinkMapping, 
				deterministicHubLoadDistribution,				
				pricingHubDistribution,
				myVehicleTypes,
				outputPath
				);
	}

	
	/**
	 * sets the stochastic loads in the hubDistributionReader
	 * @param stochasticHubLoadDistribution
	 * @param locationSourceMapping
	 * @param agentVehicleSourceMapping
	 */
	public void setStochasticSources(
			HashMap<Integer, Schedule> stochasticHubLoadDistribution,
			HashMap<Integer, Schedule> locationSourceMapping,
			HashMap<Id, Schedule> agentVehicleSourceMapping){
		
		myHubLoadReader.setStochasticSources(stochasticHubLoadDistribution,
				locationSourceMapping, 
				agentVehicleSourceMapping);
	}
	

	/**
	 * get agent schedules, find required charging times, assign charging times
	 * 
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws LpSolveException
	 * @throws OptimizationException
	 * @throws IOException
	 */
	public void run() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
		startTime = System.currentTimeMillis();
		
		readAgentSchedules();
		
		agentReadTime = System.currentTimeMillis();
		findRequiredChargingTimes();
		
		LPTime = System.currentTimeMillis();
		assignChargingTimes();
		
		distributeTime = System.currentTimeMillis();
		findChargingDistribution();
		
		calculateChargingCostsAllAgents();
		wrapUpTime = System.currentTimeMillis();
		System.out.println("Decentralized Smart Charger DONE");
	}



	/***********************************************
	 * CALCULATIONS
	 * **********************************************
	 */
	
	
	/**
	 * Loops over all agents
	 * Calls AgentChargingTimeReader to read in their schedule
	 * saves the schedule in agentParkingAndDrivingSchedules
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 */
	public void readAgentSchedules() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		for (Id id : controler.getPopulation().getPersons().keySet()){
			if(DecentralizedSmartCharger.debug){
				System.out.println("getAgentSchedule: "+ id.toString());
			}
			
			agentParkingAndDrivingSchedules.put(id,myAgentTimeReader.readParkingAndDrivingTimes(id));
			
		}		
		
	}



	/***********************************************
	 * CALCULATIONS
	 * **********************************************
	 */
	
	public void findRequiredChargingTimes() throws LpSolveException, IOException{
		
		System.out.println("Find required charging times - LP");
		
		for (Id id : controler.getPopulation().getPersons().keySet()){
			
			String type="";
			/*
			 * IF COMBUSTION
			 */
			if(hasAgentCombustionVehicle(id)){
								
				lpcombustion.updateSchedule(agentParkingAndDrivingSchedules.get(id));
				double consumption = lpcombustion.getEnergyFromCombustionEngine();
				
				double emissionContribution= joulesToEmissionInKg(id, consumption);
				// get entire driving Joules and transform to emissions
				emissionCounter+= emissionContribution;
				agentsWithCombustion.add(id);
				type="combustionVehicle";
				
			}else{
				/*
				 * EV OR PHEV
				 */
				
				double joulesFromEngine=0;
				
				double batterySize= getBatteryOfAgent(id).getBatterySize();
				double batteryMin=getBatteryOfAgent(id).getMinSOC();
				double batteryMax=getBatteryOfAgent(id).getMaxSOC(); 
				
				if(hasAgentPHEV(id)){	
					agentsWithPHEV.add(id);
					type="PHEVVehicle";
					
				}else{
					agentsWithEV.add(id);
					type="EVVehicle";
				}
				
				//try EV first
				
				Schedule scheduleAfterLP= lpev.solveLP(agentParkingAndDrivingSchedules.get(id),
						id, 
						batterySize, batteryMin, batteryMax, 
						type);
				if (scheduleAfterLP !=null){
					// if successful --> save
					
					agentParkingAndDrivingSchedules.put(id, scheduleAfterLP);
					if(hasAgentPHEV(id)){
						// only if agent has PHEV change joules to emissions
						emissionCounter= joulesToEmissionInKg(id,joulesFromEngine); // still 0
												
					}
				}else{					
					//if fails, try PHEV
										
					scheduleAfterLP= lpphev.solveLP(agentParkingAndDrivingSchedules.get(id),id, batterySize, batteryMin, batteryMax, type);
					agentParkingAndDrivingSchedules.put(id, scheduleAfterLP);
					
					joulesFromEngine= lpphev.getEnergyFromCombustionEngine();
					if(hasAgentEV(id)){
						
						chargingFailureEV.add(id);
						
					}else{
						
						emissionCounter+= joulesToEmissionInKg(id, joulesFromEngine);
					}
					
				}
				
			}
		}
	}



	/**
	 * passes schedule with required charging information to
	 * ChargingSlotDistributor to obtain exact charging Slots
	 * Saves charging slots in agentChargignSchedule
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 * @throws IOException 
	 * @throws OptimizationException 
	 */
	public void assignChargingTimes() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException, OptimizationException{
		
				
		for (Id id : controler.getPopulation().getPersons().keySet()){
		
			System.out.println("Assign charging times agent "+ id.toString());
			
			Schedule chargingSchedule=myChargingSlotDistributor.distribute(id, agentParkingAndDrivingSchedules.get(id));
			
			agentChargingSchedules.put(id, chargingSchedule);
		}
		
		printGraphChargingTimesAllAgents();
		
	}

	
	
	/**
	 * visualizes the daily plans (parking driving charging)of all agents and saves the files in the format
	 * outputPath+ "DecentralizedCharger\\agentPlans\\"+ id.toString()+"_dayPlan.png"
	 * @throws IOException
	 */
	public void visualizeDailyPlanForAllAgents() throws IOException{
		for (Id id : controler.getPopulation().getPersons().keySet()){
			
			visualizeAgentChargingProfile(agentParkingAndDrivingSchedules.get(id), 
					agentChargingSchedules.get(id), 
					id);
		}
	}
	

	/**
	 *  visualizes the daily plan for agent with given id and saves the file in the format
	 * outputPath+ "DecentralizedCharger\\agentPlans\\"+ id.toString()+"_dayPlan.png"
	 * @param id
	 * @throws IOException
	 */
	public void visualizeDailyPlanForAgent(Id id ) throws IOException{
			
			visualizeAgentChargingProfile(agentParkingAndDrivingSchedules.get(id), 
					agentChargingSchedules.get(id), 
					id);
		
	}
	
	
	/**
	 * COUNT of charging, driving and parking agents at first second of each minute over the day;
	 * update of loadAfterDetermisticChargingDecicion according to charging habits of agents
	 * 
	 * @throws IOException
	 */
	private void findChargingDistribution() throws IOException{
		
		
		for(int i=0; i<MINUTESPERDAY; i++){
			double thisSecond= i*SECONDSPERMIN;
			for(Id id : controler.getPopulation().getPersons().keySet()){
				
				Schedule thisAgentParkAndDrive = agentParkingAndDrivingSchedules.get(id);
				int interval= thisAgentParkAndDrive.timeIsInWhichInterval(thisSecond);
				
				//PARKING
				if (thisAgentParkAndDrive.timesInSchedule.get(interval).isParking()){
					countParking.addDataPoint(i, 
							thisSecond, 
							countParking.getYAtEntry(i)+1
							);
					
					
					int hub= myHubLoadReader.getHubForLinkId(
							((ParkingInterval)thisAgentParkAndDrive.timesInSchedule.get(interval)).getLocation() 
							);
						
					myHubLoadReader.recordParkingAgentAtHubInMinute(i, hub);
					
					
				}
				//DRIVING
				if (thisAgentParkAndDrive.timesInSchedule.get(interval).isDriving()){
					countDriving.addDataPoint(i, 
							thisSecond, 
							countDriving.getYAtEntry(i)+1
							);
					
				}
				
				Schedule thisAgentCharging = agentChargingSchedules.get(id);
				
				//CHARGING
				if(thisAgentCharging.isSecondWithinOneInterval(thisSecond)){
					countCharging.addDataPoint(i, 
							thisSecond, 
							countCharging.getYAtEntry(i)+1
							);
					
					
					// find Hub for this charging and parking Interval
					interval= thisAgentParkAndDrive.timeIsInWhichParkingInterval(thisSecond);
					ParkingInterval p= (ParkingInterval)thisAgentParkAndDrive.timesInSchedule.get(interval);
					Id linkId = p.getLocation();
					
					double wattReduction=p.getChargingSpeed();
					
					myHubLoadReader.updateLoadAfterDeterministicChargingDecision(linkId, i, wattReduction);
				
					
					
				}
				
			}
		}
		
		visualizeChargingParkingDrivingDistribution();
		
		//visualize Load before and after
		visualizeDeterministicLoadBeforeAfterDecentralizedSmartCharger();
	}



	/**
	 * // for each agent
		// loop over assigned charging times
		// find pricing function for it
		// integrate
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public void calculateChargingCostsAllAgents() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		for(Id id : controler.getPopulation().getPersons().keySet()){
			
			Schedule s= agentParkingAndDrivingSchedules.get(id);
			
			agentChargingCosts.put(id,calculateChargingCostForAgentSchedule(id, s) );
		}		
		
		
	}



	public double calculateChargingCostForAgentSchedule(Id id, Schedule s) {
		double totalCost=0;
		
		for(int i=0; i<s.getNumberOfEntries();i++){
			
			TimeInterval t = s.timesInSchedule.get(i); //charging time
			
			if(t.isParking() && ((ParkingInterval)t).getChargingSchedule()!=null){
				Id linkId = ((ParkingInterval)t).getLocation();
				ArrayList <LoadDistributionInterval> loadList= 
					myHubLoadReader. getPricingLoadDistributionIntervalsnAtLinkAndTime(linkId, t);
				
				Schedule charging= ((ParkingInterval)t).getChargingSchedule();
				
				for(int loadCount=0; loadCount<loadList.size(); loadCount++){
					// overlap charging and loadCount
					LoadDistributionInterval currentLoadInterval= loadList.get(loadCount);
					PolynomialFunction currentPriceFunc= currentLoadInterval.getPolynomialFunction();
					
					Schedule currentOverlapCharging=new Schedule();
					
					currentOverlapCharging= charging.getOverlapWithLoadDistributionInterval(currentLoadInterval);
					
					for(int c=0; c<currentOverlapCharging.getNumberOfEntries(); c++){
						try {
							totalCost+= functionIntegrator.integrate(currentPriceFunc, 
									currentOverlapCharging.timesInSchedule.get(c).getStartTime(),
									currentOverlapCharging.timesInSchedule.get(c).getEndTime()
									);
							
						} catch (Exception e) {
							System.out.println("Method: calculateChargingCostForAgentSchedule");
							System.out.println("current charging Schedule");
							currentOverlapCharging.printSchedule();
							System.out.println("Agent Schedule");
							s.printSchedule();
							e.printStackTrace();
						} 
					}
				}
				
				
			}
			
			if(hasAgentPHEV(id)){
				if(t.isDriving() && ((DrivingInterval)t).getExtraConsumption()>0){
					totalCost +=  joulesExtraConsumptionToGasCosts(id,((DrivingInterval)t).getExtraConsumption());
				}
			}
			if(hasAgentEV(id)){
				if(t.isDriving() && ((DrivingInterval)t).getExtraConsumption()>0){
					if(DecentralizedSmartCharger.debug){
						System.out.println("extra consumption EV price calculation ");
						System.out.println("assigned "+Double.MAX_VALUE+" for agent "+ id.toString());
					}
				
					totalCost += Double.MAX_VALUE;
				}
			}
			
		}
		return totalCost;
	}



	public void initializeAndRunV2G(
			) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
		
		
		startV2G=System.currentTimeMillis();
			
		System.out.println("START CHECKING VEHICLE SOURCES");
		
		
		checkVehicleSources();
		timeCheckVehicles	=System.currentTimeMillis();	
		
		//calculate connectivity distributions at hubs
		myHubLoadReader.calculateAndVisualizeConnectivityDistributionsAtHubsInHubLoadReader();
		
		System.out.println("START CHECKING STOCHASTIC HUB LOADS");
		checkHubStochasticLoads();
		timeCheckOtherSources=System.currentTimeMillis();
		
		
		System.out.println("DONE V2G");
		
	}
	
	
	
	

	public void checkHubStochasticLoads() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, OptimizationException, LpSolveException, IOException{
		
				
		if(myHubLoadReader.stochasticHubLoadDistribution !=null){
			for(Integer h : myHubLoadReader.stochasticHubLoadDistribution.keySet()){
				
				if(DecentralizedSmartCharger.debug){
					System.out.println("check hubSource for Hub "+ h.toString());
				}
				
				Schedule hubStochasticSchedule= myHubLoadReader.stochasticHubLoadDistribution.get(h);
				
				//VISUALIZE SCHEDULE BEFORE
				String strHubLoad="HubStochasticLoad_BeforeV2G_"+h.toString();
				
				hubStochasticSchedule.visualizeLoadDistribution(strHubLoad);
				
				
				for(int j=0; j<hubStochasticSchedule.getNumberOfEntries(); j++){
					
					//each entry needs to be split down into sufficiently small time intervals
					
					LoadDistributionInterval stochasticLoad= (LoadDistributionInterval)hubStochasticSchedule.timesInSchedule.get(j);
					PolynomialFunction func= stochasticLoad.getPolynomialFunction();
					
					int intervals =(int) Math.ceil(stochasticLoad.getIntervalLength()/minChargingLength);
					
					for(int i=0; i<intervals; i++){
						
						double bit=0;
						
						if(i<intervals-1){
							bit=minChargingLength;
							
						}else{// i=intervals-1
							bit=stochasticLoad.getIntervalLength()- (intervals-1)*minChargingLength;
							
						}
						
						
						//*********************************
						//*********************************
						
						//FINALLY HAVE INTERVAL TO LOOK AT IN THIS ITERATION
						double start=stochasticLoad.getStartTime()+i*minChargingLength;
						double end= start+bit;
						
						LoadDistributionInterval currentStochasticLoadInterval= new LoadDistributionInterval(start, 
								end, 
								func, 
								stochasticLoad.isOptimal());
						
						//*********************************
						
												
						double joulesFromSource= functionIntegrator.integrate(func, start, end);
						
						
						double expectedNumberOfParkingAgents=
							myHubLoadReader.getExpectedNumberOfParkingAgentsAtHubAtTime(
									h, 
									currentStochasticLoadInterval.getStartTime());
//						
						double contributionInJoulesPerAgent=Math.abs(joulesFromSource/expectedNumberOfParkingAgents);
						
						if(joulesFromSource<0 ){
							// regulation UP
														
							
							// loop over all agents 
							// find who is in regulation up and do regulation up for him
							
							for(Id agentId : controler.getPopulation().getPersons().keySet()){
								
								double compensationPerJouleRegulationUp= 
									agentContracts.get(agentId).compensationUp()*1/(1000*3600); 
									
								double compensation= contributionInJoulesPerAgent*compensationPerJouleRegulationUp;
								
								String type;
								
								double batterySize = getBatteryOfAgent(agentId).getBatterySize(); 
								double batteryMin =getBatteryOfAgent(agentId).getMinSOC();
								double batteryMax= getBatteryOfAgent(agentId).getMaxSOC();
								
								
								
								if(hasAgentPHEV(agentId)){
									
									type="PHEVStochasticLoadRegulationUp";
									
								}else{
																		
									type="EVStochasticLoadRegulationUp";
								}
								
								if(isAgentRegulationUp(agentId)){
									
									
									myV2G.regulationUpHubLoad(agentId, 
											currentStochasticLoadInterval, 
											agentParkingAndDrivingSchedules.get(agentId), 
											compensation,
											joulesFromSource,
											hasAgentEV(agentId),
											type,
											lpev,
											lpphev,
											batterySize,
											batteryMin,
											batteryMax,
											h);
									
									
									
								}else{
									//Nothing - load remain on hub

								}
							}

							
						}else{// joulesFromSource<0
							
							for(Id agentId : controler.getPopulation().getPersons().keySet()){
								
								double compensationPerJouleRegulationDown= 
									agentContracts.get(agentId).compensationDown()*1/(1000*3600);
									
								double compensation= contributionInJoulesPerAgent*compensationPerJouleRegulationDown;
								
								
								String type;
								
								double batterySize = getBatteryOfAgent(agentId).getBatterySize(); 
								double batteryMin =getBatteryOfAgent(agentId).getMinSOC();
								double batteryMax= getBatteryOfAgent(agentId).getMaxSOC();
								
								if(hasAgentPHEV(agentId)){
									
									type="PHEVStochasticLoadRegulationDown";
									
								}else{
																		
									type="EVStochasticLoadRegulationDown";
								}
								
								if(isAgentRegulationDown(agentId)){
									
									myV2G.regulationDownHubLoad(agentId, 
											currentStochasticLoadInterval, 
											agentParkingAndDrivingSchedules.get(agentId), 
											compensation,
											joulesFromSource,
											hasAgentEV(agentId),
											type,
											lpev,
											lpphev,
											batterySize,
											batteryMin,
											batteryMax,
											h);
									
								}else{
									
									//Nothing - load remain on hub
								}
							}
							
						}
						
						
					}//end for(int i=0; i<intervals; i++){
					
					
					
				}
				
				// visualize effect of V2G.. what is it afterwards?
				hubStochasticSchedule= myHubLoadReader.stochasticHubLoadDistribution.get(h);
				
				//VISUALIZE SCHEDULE BEFORE
				strHubLoad="HubStochasticLoad_AfterV2G_"+h.toString();
				
				hubStochasticSchedule.visualizeLoadDistribution(strHubLoad);
				
			}
		}
		
	}
	
	
	public void checkVehicleSources() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException, OptimizationException{
		
		if(myHubLoadReader.agentVehicleSourceMapping!=null){
			for(Id id : myHubLoadReader.agentVehicleSourceMapping.keySet()){				
				
				if(DecentralizedSmartCharger.debug){
					System.out.println("check VehicleSource for"+ id.toString());
				}
				//ONLY IF AGENT HAS NOT COMBUSTION VEHICLE
				if(hasAgentCombustionVehicle(id)==false){
					
					Schedule electricSource= myHubLoadReader.agentVehicleSourceMapping.get(id);
					
					//VISUALIZE SCHEDULE BEFORE
					String strAgentVehicleLoad="AgentVehicleLoad_BeforeV2G_"+id.toString();
					
					electricSource.visualizeLoadDistribution(strAgentVehicleLoad);
					
					for(int i=0; i<electricSource.getNumberOfEntries(); i++){
						
						LoadDistributionInterval electricSourceInterval= (LoadDistributionInterval)electricSource.timesInSchedule.get(i);
						
						// split up in small intervals of maximum length= mincharging length
						int intervals= (int) Math.ceil(electricSourceInterval.getIntervalLength()/minChargingLength);
						
						for(int intervalNum=0; intervalNum<intervals; intervalNum++){
							
							double bit=0;
							
							if(intervalNum<intervals-1){
								bit=minChargingLength;
								
							}else{// i=intervals-1
								bit=electricSourceInterval.getIntervalLength()- (intervals-1)*minChargingLength;
								
							}
							
							//FINALLY HAVE INTERVAL TO LOOK AT IN THIS ITERATION
							double start=electricSourceInterval.getStartTime()+intervalNum*minChargingLength;
							double end= start+bit;
							
							PolynomialFunction func= new PolynomialFunction(
									electricSourceInterval.getPolynomialFunction().getCoefficients().clone()
									);
							
							LoadDistributionInterval currentStochasticLoadInterval= new LoadDistributionInterval(start, 
									end, 
									func, 
									electricSourceInterval.isOptimal());
							
							
							
							
							double joulesFromSource= functionIntegrator.integrate(func, 
									currentStochasticLoadInterval.getStartTime(), 
									currentStochasticLoadInterval.getEndTime());
							
							String type;
							
							double batterySize= getBatteryOfAgent(id).getBatterySize();
							double batteryMin=getBatteryOfAgent(id).getMinSOC();
							double batteryMax=getBatteryOfAgent(id).getMaxSOC(); 
							
							if(hasAgentPHEV(id)){
								
								type="PHEVRescheduleVehicleSourceAgent_"+id.toString();
								
							}else{
								type="EVRescheduleVehicleSourceAgent_"+id.toString();
							}
							
							// joulesFromSource negative --> SINK = discharging battery - regulation up
							if(joulesFromSource<0 ){
								
								if(isAgentRegulationUp(id)){
									
									double compensationPerJouleRegulationUp= 
										agentContracts.get(id).compensationUp()*1/(1000*3600); 
									
									double compensation= Math.abs(joulesFromSource)*compensationPerJouleRegulationUp;
									
									//agentParkingAndDrivingSchedules.getValue(id).printSchedule();
									myV2G.regulationUpVehicleLoad(id,
												currentStochasticLoadInterval,
												agentParkingAndDrivingSchedules.get(id),
												compensation,
												Math.abs(joulesFromSource),
												hasAgentEV(id),
												type,
												lpev,
												lpphev,
												batterySize,
												batteryMin,
												batteryMax);
								}
//								}else{
//									myV2G.setLoadAsLost(electricSourceInterval.getPolynomialFunction(),
//											electricSourceInterval.getStartTime(),
//											electricSourceInterval.getEndTime(),
//											id);
//								}
								
								
								
							}else{// joulesFromSource<0
							
								// joules>0 local Source to charge car --> regulation down
								if(isAgentRegulationDown(id)){
									
									double compensationPerJouleRegulationDown= 
										agentContracts.get(id).compensationDown()*1/(1000*3600); 
										
									double compensation= joulesFromSource*compensationPerJouleRegulationDown;
									
									//agentParkingAndDrivingSchedules.get(id).printSchedule();
									
									myV2G.regulationDownVehicleLoad(id, 
											currentStochasticLoadInterval, 
											agentParkingAndDrivingSchedules.get(id), 
											compensation,
											Math.abs(joulesFromSource),
											hasAgentEV(id),
											type,
											lpev,
											lpphev,
											batterySize, 
											batteryMin,
											batteryMax);
								}
							}
							
						}	
						
					}
					// check V2G effect
					electricSource= myHubLoadReader.agentVehicleSourceMapping.get(id);
					//VISUALIZE SCHEDULE BEFORE
					strAgentVehicleLoad="AgentVehicleLoad_AfterV2G_"+id.toString();
					
					electricSource.visualizeLoadDistribution(strAgentVehicleLoad);
					
				}
								
			}
		}
		
	}



	public boolean isAgentRegulationUp(Id id){
		return agentContracts.get(id).isUp();
	}
	
	
	
	public boolean isAgentRegulationDown(Id id){
		return agentContracts.get(id).isDown();
	}
	
	
	
	public static boolean hasAgentPHEV(Id id){
		
		Vehicle v= vehicles.getValue(id);
		
		if(v.getClass().equals(PlugInHybridElectricVehicle.class)){
			return true;
		}else{return false;}
	}
	
	
	public static  boolean hasAgentEV(Id id){
		
		Vehicle v= vehicles.getValue(id);
		
		if(v.getClass().equals(ElectricVehicle.class)){
			return true;
		}else{return false;}
	}
	
	
	public static boolean hasAgentCombustionVehicle(Id id){
		
		Vehicle v= vehicles.getValue(id);
		
		if(v.getClass().equals(ConventionalVehicle.class  )){
			return true;
		}else{return false;}
	}
	
	
	
	public HashMap<Id, Double> getChargingCostsForAgents(){
		return agentChargingCosts;
	}
	
	
	public HashMap<Id, Schedule> getAllAgentParkingAndDrivingSchedules(){
		return agentParkingAndDrivingSchedules;
	}
	
	
	/**
	 * returns HashMap<Id, Schedule> agentChargingSchedules
	 */
	public HashMap<Id, Schedule> getAllAgentChargingSchedules(){
		return agentChargingSchedules;
	}
	
	
	public LinkedList <Id> getAllAgentsWithEV(){
		return agentsWithEV;
	}
	
	public LinkedList <Id> getAllAgentsWithPHEV(){
		return agentsWithPHEV;
	}
	
	public LinkedList <Id> getAllAgentsWithCombustionVehicle(){
		return agentsWithCombustion;
	}
	
	
	public Schedule getAgentChargingSchedule(Id id){
		return agentChargingSchedules.get(id);
	}
	
	
	public LinkedList<Id> getIdsOfEVAgentsWithFailedOptimization(){
		return chargingFailureEV;
	}
	
	
	
	public double getTotalDrivingConsumptionOfAgent(Id id){
		
		return agentChargingSchedules.get(id).getTotalConsumption()
		+agentChargingSchedules.get(id).getTotalConsumptionFromEngine();
	}
	
	
	
	public double getTotalDrivingConsumptionOfAgentFromBattery(Id id){
		
		return agentChargingSchedules.get(id).getTotalConsumption();
	}
	
	
	
	public double getTotalDrivingConsumptionOfAgentFromOtherSources(Id id){
		
		return agentChargingSchedules.get(id).getTotalConsumptionFromEngine();
	}
	
	
	public double getTotalEmissions(){
		return emissionCounter;
	}
	
	
	
	public  HashMap<Id, Double> getAgentV2GRevenues(){
		
		return myV2G.getAgentV2GRevenues();
	}
	
	
	/**
	 * clears agentParkingAndDrivingSchedules, agentChargingSchedules, emissions and lists of EV/PHEV/conventional car owners
	 */
	public void clearResults(){
		agentParkingAndDrivingSchedules = new HashMap<Id, Schedule>(); 
		agentChargingSchedules = new HashMap<Id, Schedule>();
		
		emissionCounter=0.0;
		
		chargingFailureEV=new LinkedList<Id>();
		agentsWithEV=new LinkedList<Id>();
		agentsWithPHEV=new LinkedList<Id>();
		agentsWithCombustion=new LinkedList<Id>();
		
	}



	/**
	 * plots daily schedule and charging times of agent 
	 * and save it in: 
	 * outputPath+ "DecentralizedCharger\\agentPlans\\"+ id.toString()+"_dayPlan.png"
		  
	 * @param dailySchedule
	 * @param chargingSchedule
	 * @param id
	 * @throws IOException
	 */
	private void visualizeAgentChargingProfile(Schedule dailySchedule, Schedule chargingSchedule, Id id) throws IOException{
		
		// 1 charging, 2 suboptimal, 3 optimal, 4 driving
		
		XYSeriesCollection agentOverview= new XYSeriesCollection();
		
		//************************************
		//GET EXTRA CONSUMPTION TIMES - MAKE RED LATER
		
		int extraConsumptionCount=0;
		
		
		if(vehicles.getValue(id).getClass().equals(PlugInHybridElectricVehicle.class)){
			
			
			for(int i=0; i<dailySchedule.getNumberOfEntries();i++){
				
				if(dailySchedule.timesInSchedule.get(i).isDriving()){
					
					DrivingInterval thisD= (DrivingInterval) dailySchedule.timesInSchedule.get(i);
					
					if(thisD.hasExtraConsumption()){
						
						
						XYSeries drivingTimesSet= new XYSeries("extra consumption");
						drivingTimesSet.add(thisD.getStartTime(), 3.75);
						drivingTimesSet.add(thisD.getStartTime()+thisD.getEngineTime(), 3.75);
						agentOverview.addSeries(drivingTimesSet);
						extraConsumptionCount++;
					}
					
				}
			}
		}
		//************************************
		
		
		
		//************************************
		// ADD ALL OTHER TIMES DRIVING; PARKING;..
		//************************************
		for(int i=0; i<dailySchedule.getNumberOfEntries();i++){
			if(dailySchedule.timesInSchedule.get(i).isDriving()){
				
				XYSeries drivingTimesSet= new XYSeries("driving times");
				drivingTimesSet.add(dailySchedule.timesInSchedule.get(i).getStartTime(), 4);
				drivingTimesSet.add(dailySchedule.timesInSchedule.get(i).getEndTime(), 4);
				agentOverview.addSeries(drivingTimesSet);
				
			}else{
				
				ParkingInterval p= (ParkingInterval) dailySchedule.timesInSchedule.get(i);
				
				if(p.isInSystemOptimalChargingTime()){
					XYSeries parkingOptimalTimesSet= new XYSeries("parking times during optimal charging time");
					parkingOptimalTimesSet.add(p.getStartTime(), 3);
					parkingOptimalTimesSet.add(p.getEndTime(), 3);
					agentOverview.addSeries(parkingOptimalTimesSet);
					
				}else{
					
					XYSeries parkingSuboptimalTimesSet= new XYSeries("parking times during suboptimal charging time");
					parkingSuboptimalTimesSet.add(p.getStartTime(), 2);
					parkingSuboptimalTimesSet.add(p.getEndTime(), 2);
					agentOverview.addSeries(parkingSuboptimalTimesSet);
					
				}
				
			}
		}
		
		for(int i=0; i<chargingSchedule.getNumberOfEntries(); i++){
			
			XYSeries chargingTimesSet= new XYSeries("charging times");
			chargingTimesSet.add(chargingSchedule.timesInSchedule.get(i).getStartTime(), 1);
			chargingTimesSet.add(chargingSchedule.timesInSchedule.get(i).getEndTime(), 1);
			agentOverview.addSeries(chargingTimesSet);
			
		}
		
		
		
		//************************************
		// MAKE CHART
		//************************************
		JFreeChart chart = ChartFactory.createXYLineChart("Travel pattern agent : "+ id.toString(), 
				"time [s]", 
				"charging, off-peak parking, peak-parking, driving times", 
				agentOverview, 
				PlotOrientation.VERTICAL, 
				true, 
				true, 
				false);
		
		
		chart.setBackgroundPaint(Color.white);
		
		final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray);
        
        
        //TextAnnotation offPeak= new TextAnnotation("Off Peak parking time");
        XYTextAnnotation txt1= new XYTextAnnotation("Charging time", 20000,1.1);
        XYTextAnnotation txt2= new XYTextAnnotation("Driving time", 20000,4.1);
        XYTextAnnotation txt3= new XYTextAnnotation("Optimal parking time", 20000,3.1);
        XYTextAnnotation txt4= new XYTextAnnotation("Suboptimal parking time", 20000,2.1);
        XYTextAnnotation txt5= new XYTextAnnotation("Driving with engine power", 20000,3.85);
        
        txt1.setFont(new Font("Arial", Font.PLAIN, 14));
        txt2.setFont(new Font("Arial", Font.PLAIN, 14));
        txt3.setFont(new Font("Arial", Font.PLAIN, 14));
        txt4.setFont(new Font("Arial", Font.PLAIN, 14));
        txt5.setFont(new Font("Arial", Font.PLAIN, 14));
        //public Font(String name,int style,int size)
        
        txt5.setPaint(Color.red);
        
        plot.addAnnotation(txt1);
        plot.addAnnotation(txt2);
        plot.addAnnotation(txt3);
        plot.addAnnotation(txt4);
        plot.addAnnotation(txt5);
        
        
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(3600));
        xAxis.setRange(0, SECONDSPERDAY);
        
        
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(0, 5);
        yAxis.setTickUnit(new NumberTickUnit(1));
        yAxis.setVisible(false);
        
        int numSeries=dailySchedule.getNumberOfEntries()+chargingSchedule.getNumberOfEntries()+extraConsumptionCount;
        
        for(int j=0; j<numSeries; j++){
        	
        	// IF FROM ENGINE MAKE RED
        	if (j<extraConsumptionCount){
        		plot.getRenderer().setSeriesPaint(j, Color.red);
        		
        	}else{
        		// ALL OTHERS MAKE BLACK
        		plot.getRenderer().setSeriesPaint(j, Color.black);
        	}        	
        	
        	plot.getRenderer().setSeriesStroke(
    	            j, 
    	          
    	            new BasicStroke(
    	                1.0f,  //float width
    	                BasicStroke.CAP_ROUND, //int cap
    	                BasicStroke.JOIN_ROUND, //int join
    	                1.0f, //float miterlimit
    	                new float[] {1.0f, 0.0f}, //float[] dash
    	                0.0f //float dash_phase
    	            )
    	        );
            
        }
        
        
        ChartUtilities.saveChartAsPNG(new File(outputPath+ "DecentralizedCharger\\agentPlans\\"+ id.toString()+"_dayPlan.png") , chart, 1000, 1000);
		  
	}
	
	
	
	
	
	//visualize Load before and after
	
	private void visualizeDeterministicLoadBeforeAfterDecentralizedSmartCharger() throws IOException{
		
		//************************************
		//READ IN VALUES FOR EVERY HUB
		// and make chart before-after
		//************************************
		for( Integer i : myHubLoadReader.loadAfterDeterministicChargingDecision.keySet()){
			
			XYSeriesCollection load= new XYSeriesCollection();
			//************************************
			//AFTER//BEFORE
			
			XYSeries hubXBefore= new XYSeries("hub"+i.toString()+"before");
			
			double [][] dBefore = myHubLoadReader.originalDeterministicChargingDistribution.get(i);
			for(int j=0; j<dBefore.length; j++){
				
				hubXBefore.add(dBefore[j][0], dBefore[j][1]); // time, Watt
				
				
			}
			
			load.addSeries(hubXBefore);
			
			//************************************
			
			//************************************
			JFreeChart chart = ChartFactory.createXYLineChart("Load distribution before and after first charging optimization at Hub "+ i.toString(), 
					"time [s]", 
					"available load [W]", 
					load, 
					PlotOrientation.VERTICAL, 
					true, true, false);
			
			chart.setBackgroundPaint(Color.white);
			
			final XYPlot plot = chart.getXYPlot();
	        plot.setBackgroundPaint(Color.white);
	        plot.setDomainGridlinePaint(Color.gray); 
	        plot.setRangeGridlinePaint(Color.gray);
			
	        plot.getRenderer().setSeriesPaint(0, Color.red);//after
	        
	        
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
        	
        	
        	ChartUtilities.saveChartAsPNG(new File(outputPath+ "Hub\\loadAfterFirstOptimizationAtHubTEST_"+ i.toString()+".png") , chart, 1000, 1000);
            
		}
		
		
		for( Integer i : myHubLoadReader.loadAfterDeterministicChargingDecision.keySet()){
			
			XYSeriesCollection load= new XYSeriesCollection();
			//************************************
			//AFTER//BEFORE
			XYSeries hubXAfter= new XYSeries("hub"+i.toString()+"after");
			XYSeries hubXBefore= new XYSeries("hub"+i.toString()+"before");
			
			double [][] dAfter = myHubLoadReader.loadAfterDeterministicChargingDecision.get(i);
			double [][] dBefore = myHubLoadReader.originalDeterministicChargingDistribution.get(i);
			
			if(dAfter.equals(dBefore)){
				if(DecentralizedSmartCharger.debug){
					System.out.println("SAME");
				}
				
			}
			
			for(int j=0; j<dAfter.length; j++){
				hubXAfter.add(dAfter[j][0], dAfter[j][1]); // time, Watt
				hubXBefore.add(dBefore[j][0], dBefore[j][1]); // time, Watt
				
			}
			load.addSeries(hubXAfter);
			load.addSeries(hubXBefore);
			
			//************************************
			
			//************************************
			JFreeChart chart = ChartFactory.createXYLineChart("Load distribution before and after first charging optimization at Hub "+ i.toString(), 
					"time [s]", 
					"available load [W]", 
					load, 
					PlotOrientation.VERTICAL, 
					true, true, false);
			
			chart.setBackgroundPaint(Color.white);
			
			final XYPlot plot = chart.getXYPlot();
	        plot.setBackgroundPaint(Color.white);
	        plot.setDomainGridlinePaint(Color.gray); 
	        plot.setRangeGridlinePaint(Color.gray);
			
	        plot.getRenderer().setSeriesPaint(0, Color.red);//after
	        plot.getRenderer().setSeriesPaint(1, Color.black);//before
	        
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
    	                5.0f,  //float width
    	                BasicStroke.CAP_ROUND, //int cap
    	                BasicStroke.JOIN_ROUND, //int join
    	                1.0f, //float miterlimit
    	                new float[] {1.0f, 0.0f}, //float[] dash
    	                0.0f //float dash_phase
    	            )
    	        );
        	ChartUtilities.saveChartAsPNG(new File(outputPath+ "Hub\\loadAfterFirstOptimizationAtHub_"+ i.toString()+".png") , chart, 1000, 1000);
            
		}
        
       
	}
	
	
	
	
	private void printGraphChargingTimesAllAgents() throws IOException{
		
		XYSeriesCollection allAgentsOverview= new XYSeriesCollection();
		
		int seriesCount=0;
		
		for(Id id : controler.getPopulation().getPersons().keySet()){
			
			Schedule s1= agentChargingSchedules.get(id);
			
			for(int i=0; i<s1.getNumberOfEntries(); i++){
				
				String strId= id.toString();
				int intId= Integer.parseInt(strId);
			    
				XYSeries chargingTimesSet= new XYSeries("charging time");
								
				chargingTimesSet.add(s1.timesInSchedule.get(i).getStartTime(),intId); 
				chargingTimesSet.add(s1.timesInSchedule.get(i).getEndTime(), intId);
				
				allAgentsOverview.addSeries(chargingTimesSet);
				seriesCount++;
			}
		
		}
		
		JFreeChart chart = ChartFactory.createXYLineChart("Distribution of charging times for all agents by agent Id number", 
				"time [s]", 
				"charging times", 
				allAgentsOverview, 
				PlotOrientation.VERTICAL, 
				false, true, false);
		
		final XYPlot plot = chart.getXYPlot();
        plot.setDrawingSupplier(supplier);
        
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(3600));
        xAxis.setRange(0, SECONDSPERDAY);
        
        NumberAxis yaxis = (NumberAxis) plot.getRangeAxis();
        yaxis.setRange(0, 100);
        
        for(int j=0; j<seriesCount; j++){
        	plot.getRenderer().setSeriesPaint(j, Color.black);
        	
        	plot.getRenderer().setSeriesStroke(
    	            j, 
    	            new BasicStroke(
    	                1.0f, 
    	                BasicStroke.CAP_ROUND, 
    	                BasicStroke.JOIN_ROUND, 
    	                1.0f,
    	                new float[] {1.0f, 0.0f}, 
    	                1.0f
    	            )
    	        );
        }
        
        chart.setTitle(new TextTitle("Distribution of charging times for all agents by agent Id number", 
    		   new Font("Arial", Font.BOLD, 20)));
        
        ChartUtilities.saveChartAsPNG(new File(outputPath + "DecentralizedCharger\\allAgentsChargingTimes.png"), chart, 2000, 2000);	
	
	}
	
	
	
	private void visualizeChargingParkingDrivingDistribution() throws IOException{
		// make graph out of it
		XYSeriesCollection distributionTotal= new XYSeriesCollection();
		
		
		XYSeries chargingDistributionAgentSet= countCharging.getXYSeries("Numbers of Charging agents");
		XYSeries parkingDistributionAgentSet= countParking.getXYSeries("Numbers of Parking agents");
		XYSeries drivingDistributionAgentSet= countDriving.getXYSeries("Numbers of Driving agents");
		
		
		distributionTotal.addSeries(chargingDistributionAgentSet);
		distributionTotal.addSeries(parkingDistributionAgentSet);
		distributionTotal.addSeries(drivingDistributionAgentSet);
		
		JFreeChart chart = ChartFactory.createXYLineChart("Count of all agents charging, parking or driving on first second in minute", 
				"time of day [s]", 
				"total count of agents", 
				distributionTotal, 
				PlotOrientation.VERTICAL, 
				true, true, false);
		
		
		chart.setBackgroundPaint(Color.white);
		
		final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray);
		
        
        //Charging
       
        	 plot.getRenderer().setSeriesPaint(0, Color.black);
         	plot.getRenderer().setSeriesStroke(
     	            0, 
     	            new BasicStroke(
     	                2.0f,  //float width
     	                BasicStroke.CAP_ROUND, //int cap
     	                BasicStroke.JOIN_ROUND, //int join
     	                1.0f, //float miterlimit
     	                new float[] {4.0f, 1.0f}, //float[] dash
     	                0.0f //float dash_phase
     	            )
     	        );
       
       
        
    	//Parking
       
        	
        	plot.getRenderer().setSeriesPaint(1, Color.gray);
         	plot.getRenderer().setSeriesStroke(
     	            1, 
     	            new BasicStroke(
     	                2.0f,  //float width
     	                BasicStroke.CAP_ROUND, //int cap
     	                BasicStroke.JOIN_ROUND, //int join
     	                1.0f, //float miterlimit
     	                new float[] {2.0f, 0.0f}, //float[] dash
     	                0.0f //float dash_phase
     	            )
     	        );
        
    	 
     	
     	//Driving
       
        	 plot.getRenderer().setSeriesPaint(2, Color.red);
          	 plot.getRenderer().setSeriesStroke(
      	            2, 
      	            new BasicStroke(
      	                2.0f,  //float width
      	                BasicStroke.CAP_ROUND, //int cap
      	                BasicStroke.JOIN_ROUND, //int jointe
      	                1.0f, //float miterlimit
      	                new float[] {1.0f, 1.0f}, //float[] dash
      	                0.0f //float dash_phase
      	            )
      	        );
       
     	
    	
  
    	ChartUtilities.saveChartAsPNG(new File(
    			outputPath+ "Hub\\validation_chargingdistribution.png") , 
    			chart, 800, 600);
	  	
	
	}
	
		
	
	
	/**
	 * transforms joules to emissions according to the vehicle and gas type data stored in the vehicleCollector
	 * @param agentId
	 * @param joules
	 * @return
	 */
	public double joulesToEmissionInKg(Id agentId, double joules){
		Vehicle v= vehicles.getValue(agentId);
		GasType vGT= myVehicleTypes.getGasType(v);
		
		double liter=1/(vGT.getJoulesPerLiter())*joules; 
		
		double emission= vGT.getEmissionsPerLiter()*liter; 
				
		return emission;
	}
	
	
	public double joulesExtraConsumptionToGasCosts(Id agentId, double joules){
		
		Vehicle v= vehicles.getValue(agentId);
		GasType vGT= myVehicleTypes.getGasType(v);
		double liter=1/(vGT.getJoulesPerLiter())*joules; // 1kgBenzin/43MJ= xkg/joules
		
		double cost= vGT.getPricePerLiter()*liter; // xx CHF/liter 
				
		return cost;
	}
	
	
	
	public Battery getBatteryOfAgent(Id agentId){
		return myVehicleTypes.getBattery(vehicles.getValue(agentId));
		
	}
	
	
	public GasType getGasTypeOfAgent(Id agentId){
		return myVehicleTypes.getGasType(vehicles.getValue(agentId));
		
	}
	
	public void writeSummary(String configName){
		try{
		    // Create file 
			String title=(outputPath + configName+ "_summary.html");
		    FileWriter fstream = new FileWriter(title);
		    BufferedWriter out = new BufferedWriter(fstream);
		    //out.write("Penetration: "+ Main.penetrationPercent+"\n");
		    out.write("<html>");
		    out.write("<body>");
		    
		    //*************************************
		    out.write("<h1>Summary of run:  </h1>");
		  //*************************************
		  //*************************************
		    out.write("<p>"); //add where read in ferom.. what file..?
		  //*************************************
		    out.write("Time Decentralized Smart Charger </br>");
		    
		    out.write("Standard charging time of [s]:"+ minChargingLength
		    		+"</br>");
		    
		    out.write("Time [ms] reading agent schedules:"+ (agentReadTime-startTime)
		    		+"</br>");
		    out.write("Time [ms] LP:"+ (LPTime-agentReadTime)
		    		+"</br>");
		    out.write("Time [ms] slot distribution:"+ (distributeTime-LPTime)
		    		+"</br>");
		    out.write("Time [ms] wrapping up:"+ (wrapUpTime-distributeTime)
		    		+"</br>");
		    
		  //*************************************
		    out.write("Time V2G </br>");
		    out.write("Time [ms] checking vehicle sources:"+ 
		    		(timeCheckVehicles-startV2G)+"</br>");
		    
		    out.write("Time [ms] checking other sources:"+ 
		    		(timeCheckOtherSources-timeCheckVehicles)+"</br>");
		    
		    out.write("</p>");
		    
		  //*************************************
		  //*************************************
		    /*//Hub\\validation_chargingdistribution.png"
		    String pic= outputPath +"Hub/validation_chargingdistribution.png";
		    
		    out.write("");
		    out.write("<img src='"+pic+"' alt='connectivity' width='80%'");
		   */
		  //*************************************
		    out.write("</body>");
		    out.write("</html>");   
		    
		    
		    //Close the output stream
		    out.close();
		    }catch (Exception e){
		    	//Catch exception if any
		    }
	}
	
	public void setV2G(V2G setV2G){
		myV2G=setV2G;
	}
	
	
public static PolynomialFunction fitCurve(double [][] data) throws OptimizationException{
		
		DecentralizedSmartCharger.polyFit.clearObservations();
		
		for (int i=0;i<data.length;i++){
			DecentralizedSmartCharger.polyFit.addObservedPoint(1.0, data[i][0], data[i][1]);
			
		  }		
		
		PolynomialFunction poly = DecentralizedSmartCharger.polyFit.fit();
		 
		DecentralizedSmartCharger.polyFit.clearObservations();
		return poly;
	}

	public static void linkedListIdPrinter(HashMap<Id, Schedule> list, String info){
		System.out.println("Print LinkedList "+ info);
		for(Id id: list.keySet()){
			list.get(id).printSchedule();
		}
		
	}
	
	public  static void linkedListIntegerPrinter(HashMap<Integer, Schedule> list, String info){
		System.out.println("Print LinkedList "+ info);
		for(Integer id: list.keySet()){
			list.get(id).printSchedule();
		}
	
}
	
}
