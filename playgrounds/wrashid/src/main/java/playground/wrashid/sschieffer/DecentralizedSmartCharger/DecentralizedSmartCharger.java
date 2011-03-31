package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import lpsolve.LpSolveException;

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
	 * stores results of charging algorithm in LinkedListValueHashMap
 * @author Stella
 *
 */
public class DecentralizedSmartCharger {
	
	public DifferentiableMultivariateVectorialOptimizer optimizer;
	public VectorialConvergenceChecker checker= new SimpleVectorialValueChecker(10000,10000);
	public static SimpsonIntegrator functionIntegrator= new SimpsonIntegrator();
	public NewtonSolver newtonSolver= new NewtonSolver();	
	public GaussNewtonOptimizer gaussNewtonOptimizer= new GaussNewtonOptimizer(true); //useLU - true, faster  else QR more robust
		
	public static PolynomialFitter polyFit;
	
	
	final public static double SECONDSPERMIN=60;
	final public static double SECONDSPER15MIN=15*60;
	final public static double SECONDSPERDAY=24*60*60;
	final public static int MINUTESPERDAY=24*60;
	
		
	public static HubLoadDistributionReader myHubLoadReader;
	public static ChargingSlotDistributor myChargingSlotDistributor;
	public static AgentTimeIntervalReader myAgentTimeReader;
	
	private LinkedListValueHashMap<Id, Schedule> agentParkingAndDrivingSchedules = new LinkedListValueHashMap<Id, Schedule>(); 
	private LinkedListValueHashMap<Id, Schedule> agentChargingSchedules = new LinkedListValueHashMap<Id, Schedule>();
	
	public double MINCHARGINGLENGTH;
	
	public double EMISSIONCOUNTER=0.0;
	
	final Controler controler;
	
	public LPEV lpev;
	public LPPHEV lpphev;
	public LPCombustion lpcombustion;
	
	public LinkedList<Id> chargingFailureEV=new LinkedList<Id>();
	public LinkedList<Id> agentsWithEV=new LinkedList<Id>();
	public LinkedList<Id> agentsWithPHEV=new LinkedList<Id>();
	public LinkedList<Id> agentsWithCombustion=new LinkedList<Id>();
	
	public static String outputPath;
	
	final public static DrawingSupplier supplier = new DefaultDrawingSupplier();
	
	
	public LinkedListValueHashMap<Id, Vehicle> vehicles;
	public ParkingTimesPlugin parkingTimesPlugin;
	public EnergyConsumptionPlugin energyConsumptionPlugin;
	
	private double gasJoulesPerLiter;
	private double emissionPerLiterEngine;
	
	private double batterySizeEV;
	private double batterySizePHEV;
	private double batteryMinEV;
	private double batteryMinPHEV;
	private double batteryMaxEV;
	private double batteryMaxPHEV;

	
	
	
	//***********************************************************************
	
	public DecentralizedSmartCharger(Controler controler, 
			ParkingTimesPlugin parkingTimesPlugin,
			EnergyConsumptionPlugin energyConsumptionPlugin,
			String outputPath,
			double gasJoulesPerLiter,
			double emissionPerLiterEngine			
			
	) throws IOException, OptimizationException{
		
		this.controler=controler;
						
		this.outputPath=outputPath;		
		
		gaussNewtonOptimizer.setMaxIterations(10000000);		
		gaussNewtonOptimizer.setConvergenceChecker(checker);		
		optimizer=gaussNewtonOptimizer;
		polyFit= new PolynomialFitter(24, optimizer);
		
			
		myAgentTimeReader= new AgentTimeIntervalReader(
				parkingTimesPlugin, 
				energyConsumptionPlugin);
		
		this.gasJoulesPerLiter=gasJoulesPerLiter;
		this.emissionPerLiterEngine=emissionPerLiterEngine;
		
		try {
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	
	public void setBatteryConstants(double batterySizeEV, 
			double batterySizePHEV,
			
			double batteryMinEV,
			double batteryMinPHEV,
			
			double batteryMaxEV,
			double batteryMaxPHEV){
		
		this.batterySizeEV=batterySizeEV;
		this.batterySizePHEV=batterySizePHEV;
		this.batteryMinEV=batteryMinEV;
		this.batteryMinPHEV=batteryMinPHEV;
		this.batteryMaxEV=batteryMaxEV;
		this.batteryMaxPHEV=batteryMaxPHEV;
	}
	
	
	public void initializeLP(double buffer){
		lpev=new LPEV(buffer);
		lpphev=new LPPHEV();
		lpcombustion= new LPCombustion();
	}
	
	
	public void initializeChargingSlotDistributor(double minChargingLength){
		this.MINCHARGINGLENGTH=minChargingLength; 
		myChargingSlotDistributor=new ChargingSlotDistributor(minChargingLength);
	}
	
	
	
	public void setLinkedListValueHashMapVehicles(LinkedListValueHashMap<Id, Vehicle> vehicles){
		this.vehicles=vehicles;
	}
	
	public void initializeHubLoadDistributionReader(
			HubLinkMapping hubLinkMapping, LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution) throws OptimizationException, IOException{
		
		myHubLoadReader=new HubLoadDistributionReader(controler, hubLinkMapping, hubLoadDistribution);
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
		readAgentSchedules();
		findRequiredChargingTimes();
		assignChargingTimes();
		
	}
	
	
	/*
	 * GET RESULTS
	 */
	
	public LinkedListValueHashMap<Id, Schedule> getAllAgentParkingAndDrivingSchedules(){
		return agentParkingAndDrivingSchedules;
	}
	
	
	/**
	 * returns LinkedListValueHashMap<Id, Schedule> agentChargingSchedules
	 */
	public LinkedListValueHashMap<Id, Schedule> getAllAgentChargingSchedules(){
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
		return agentChargingSchedules.getValue(id);
	}
	
	
	public LinkedList<Id> getIdsOfEVAgentsWithFailedOptimization(){
		return chargingFailureEV;
	}
	
	
	
	public double getTotalDrivingConsumptionOfAgent(Id id){
		
		return agentChargingSchedules.getValue(id).getTotalConsumption()
		+agentChargingSchedules.getValue(id).getTotalConsumptionFromEngine();
	}
	
	
	
	public double getTotalDrivingConsumptionOfAgentFromBattery(Id id){
		
		return agentChargingSchedules.getValue(id).getTotalConsumption();
	}
	
	
	
	public double getTotalDrivingConsumptionOfAgentFromOtherSources(Id id){
		
		return agentChargingSchedules.getValue(id).getTotalConsumptionFromEngine();
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
			System.out.println("getAgentSchedule: "+ id.toString());
			agentParkingAndDrivingSchedules.put(id,myAgentTimeReader.readParkingAndDrivingTimes(id));
			
		}		
		
	}
	
	
	
	public void findRequiredChargingTimes() throws LpSolveException{
		
		System.out.println("Find required charging times - LP");
		
		for (Id id : controler.getPopulation().getPersons().keySet()){
			
			
			/*
			 * IF COMBUSTION
			 */
			if(vehicles.getValue(id).getClass().equals(
					new ConventionalVehicle(null, new IdImpl(2)).getClass())){
				
								
				lpcombustion.updateSchedule(agentParkingAndDrivingSchedules.getValue(id));
				double consumption = lpcombustion.getDrivingConsumption();
				
				double emissionContribution= joulesToEmissionInKg(consumption);
				// get entire driving Joules and transform to emissions
				EMISSIONCOUNTER+= emissionContribution;
				agentsWithCombustion.add(id);
				
			}else{
				/*
				 * EV OR PHEV
				 */
				
				
				double joulesFromEngine=0;
				
				double batterySize;
				double batteryMin;
				double batteryMax; 
				
				if(vehicles.getValue(id).getClass().equals(
						new PlugInHybridElectricVehicle(new IdImpl(1)).getClass())){
					
					PlugInHybridElectricVehicle thisPHEV= (PlugInHybridElectricVehicle)vehicles.getValue(id);										
					batterySize=batterySizePHEV;//thisPHEV.getBatterySizeInJoule();
					batteryMin=batteryMinPHEV;//thisEV.getBatteryMinThresholdInJoule();
					batteryMax=batteryMaxPHEV; 
					agentsWithPHEV.add(id);
					
				}else{
					
					ElectricVehicle thisEV= (ElectricVehicle)vehicles.getValue(id);
										
					batterySize=batterySizeEV;//thisEV.getBatterySizeInJoule();
					batteryMin=batteryMinEV;//thisEV.getBatteryMinThresholdInJoule();
					batteryMax=batteryMaxEV; 
					
					agentsWithEV.add(id);
				}
				
				//try EV first
				
				Schedule s= lpev.solveLP(agentParkingAndDrivingSchedules.getValue(id),id, batterySize, batteryMin, batteryMax);
				if (s !=null){
					// if successful --> saave
					
					agentParkingAndDrivingSchedules.put(id, s);
					EMISSIONCOUNTER= joulesToEmissionInKg(joulesFromEngine); // still 0
					
					
				}else{					
					//if fails, try PHEV
					if(vehicles.getValue(id).getClass().equals(
							new ElectricVehicle(null, new IdImpl(1)).getClass())){
						
						chargingFailureEV.add(id);
					}
					
										
					s= lpphev.solveLP(agentParkingAndDrivingSchedules.getValue(id),id, batterySize, batteryMin, batteryMax);
					agentParkingAndDrivingSchedules.put(id, s);
					
					joulesFromEngine= lpphev.getEnergyFromCombustionEngine();
					EMISSIONCOUNTER+= joulesToEmissionInKg(joulesFromEngine);
					
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
			
			
			Schedule chargingSchedule=myChargingSlotDistributor.distribute(agentParkingAndDrivingSchedules.getValue(id));
			
			
			agentChargingSchedules.put(id, chargingSchedule);
			visualizeAgentChargingProfile(agentParkingAndDrivingSchedules.getValue(id), 
					chargingSchedule, 
					id);
		}
		
		printGraphChargingTimesAllAgents();
		
	}
	
	
	
	/**
	 * plots daily schedule and charging times of agent 
	 * 
	 * @param dailySchedule
	 * @param chargingSchedule
	 * @param id
	 * @throws IOException
	 */
	public void visualizeAgentChargingProfile(Schedule dailySchedule, Schedule chargingSchedule, Id id) throws IOException{
		
		// 1 charging, 2 suboptimal, 3 optimal, 4 driving
		
		XYSeriesCollection agentOverview= new XYSeriesCollection();
		
		
		//************************************
		//GET EXTRA CONSUMPTION TIMES - MAKE RED LATER
		
		int extraConsumptionCount=0;
		
		
		if(vehicles.getValue(id).getClass().equals(
				new PlugInHybridElectricVehicle(new IdImpl(1)).getClass())){
			
			
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
				false, true, false);
		
		
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
        
        
        ChartUtilities.saveChartAsPNG(new File(outputPath+ "agent "+ id.toString()+"_dayPlan.png") , chart, 1000, 1000);
		  
	}
	
	
	
	
	public void printGraphChargingTimesAllAgents() throws IOException{
		
		XYSeriesCollection allAgentsOverview= new XYSeriesCollection();
		
		int seriesCount=0;
		
		for(Id id : controler.getPopulation().getPersons().keySet()){
			
			Schedule s1= agentChargingSchedules.getValue(id);
			
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
        
        ChartUtilities.saveChartAsPNG(new File(outputPath + "_allChargingTimes.png"), chart, 2000, 2000);	
	
	}
	
	
	
	public void validateChargingDistribution() throws IOException{
		
		double [] count= new double[MINUTESPERDAY];
		
		for(int i=0; i<MINUTESPERDAY; i++){
			for(Id id : controler.getPopulation().getPersons().keySet()){
				Schedule thisAgentCharging = agentChargingSchedules.getValue(id);
				if(thisAgentCharging.isSecondWithinOneInterval(i*SECONDSPERMIN)){
					count[i]=count[i]+1;
				}
			}
		}
		
		
		
		// make graph out of it
		XYSeriesCollection chargingDistributionTotal= new XYSeriesCollection();
		
		XYSeries chargingDistributionAgentSet= new XYSeries("");
		
		for(int i=0; i<count.length;i++){
			chargingDistributionAgentSet.add(i*SECONDSPERMIN, count[i]);
		}
		
		chargingDistributionTotal.addSeries(chargingDistributionAgentSet);
		
		JFreeChart chart = ChartFactory.createXYLineChart("Count of all agents charging on first second in minute", 
				"time of day [s]", 
				"total count of charging vehicles", 
				chargingDistributionTotal, 
				PlotOrientation.VERTICAL, 
				false, true, false);
		
		
		chart.setBackgroundPaint(Color.white);
		
		final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray);
		
        
        
        
        plot.getRenderer().setSeriesPaint(0, Color.black);
    	plot.getRenderer().setSeriesStroke(
	            0, 
	          
	            new BasicStroke(
	                2.0f,  //float width
	                BasicStroke.CAP_ROUND, //int cap
	                BasicStroke.JOIN_ROUND, //int join
	                1.0f, //float miterlimit
	                new float[] {2.0f, 0.0f}, //float[] dash
	                0.0f //float dash_phase
	            )
	        );
        
  
    ChartUtilities.saveChartAsPNG(new File(outputPath+ "validation_chargingdistribution.png") , chart, 800, 600);
	  
			
	}
	
	
	
	public double joulesToEmissionInKg(double joules){
		
		double mass=1/(gasJoulesPerLiter)*joules; // 1kgBenzin/43MJ= xkg/joules
		
		double emission= emissionPerLiterEngine*mass; // 23,2kg/10l= xx/mass   1kg=1l
				
		return emission;
	}
	
	
	public void clearResults(){
		agentParkingAndDrivingSchedules = new LinkedListValueHashMap<Id, Schedule>(); 
		agentChargingSchedules = new LinkedListValueHashMap<Id, Schedule>();
		
		EMISSIONCOUNTER=0.0;
		
		chargingFailureEV=new LinkedList<Id>();
		agentsWithEV=new LinkedList<Id>();
		agentsWithPHEV=new LinkedList<Id>();
		agentsWithCombustion=new LinkedList<Id>();
		
	}
	
}
