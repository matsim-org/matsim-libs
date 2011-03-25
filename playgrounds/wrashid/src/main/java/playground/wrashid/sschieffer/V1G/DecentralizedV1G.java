package playground.wrashid.sschieffer.V1G;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;

import lpsolve.LpSolveException;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.optimization.DifferentiableMultivariateVectorialOptimizer;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;


import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
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
	 * stores results of charging algorithm in LinkedListValueHashMap
 * @author Stella
 *
 */
public class DecentralizedV1G {
	
		
	public static HubLoadDistributionReader myHubLoadReader;
	public static ChargingSlotDistributor myChargingSlotDistributor;
	public static AgentTimeIntervalReader myAgentTimeReader;
	
	private LinkedListValueHashMap<Id, Schedule> agentParkingAndDrivingSchedules = new LinkedListValueHashMap<Id, Schedule>(); 
	private LinkedListValueHashMap<Id, Schedule> agentChargingSchedules = new LinkedListValueHashMap<Id, Schedule>();
	
	final Controler controler;
	
	public final LPEV lpev=new LPEV();
	public final  LPPHEV lpphev=new LPPHEV();
	public final  LPCombustion lpcombustion= new LPCombustion();
	
	
	
	public DecentralizedV1G(Controler controler) throws IOException, OptimizationException{
		this.controler=controler;
		
		myHubLoadReader=new HubLoadDistributionReader(controler);
		// TODO  initilaize Facilities? and FUnctions
		
		myAgentTimeReader= new AgentTimeIntervalReader();
		
		try {
			myChargingSlotDistributor=new ChargingSlotDistributor();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	
	
	/**
	 * Loops over all agents
	 * Calls AgentChargingTimeReader to read in their schedule
	 * saves the schedule in agentParkingAndDrivingSchedules
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 */
	public void getAgentSchedules() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		for (Id id : controler.getPopulation().getPersons().keySet()){
			System.out.println("getAgentSchedule: "+ id.toString());
			agentParkingAndDrivingSchedules.put(id,myAgentTimeReader.readParkingAndDrivingTimes(id));
			
		}		
		
	}
	
	
	
	public void findRequiredChargingTimes() throws LpSolveException{
		
		System.out.println("Find required charging times - LP");
		
		for (Id id : controler.getPopulation().getPersons().keySet()){
			
			//Vehicle v = playground.wrashid.sschieffer.V1G.Main.vehicles.getValue(id);
			
			if(playground.wrashid.sschieffer.V1G.Main.vehicles.getValue(id).getClass().equals(
					new PlugInHybridElectricVehicle(new IdImpl(1)).getClass())){
				lpphev.solveLP(agentParkingAndDrivingSchedules.getValue(id), id);
				double joulesFromEngine= lpphev.getEnergyFromCombustionEngine();
				
				playground.wrashid.sschieffer.V1G.Main.EMISSIONCOUNTER= joulesToEmissionInKg(joulesFromEngine);
				
				
				
			}else{
				if(playground.wrashid.sschieffer.V1G.Main.vehicles.getValue(id).getClass().equals(
						new ElectricVehicle(null, new IdImpl(1)).getClass())){
					
					agentParkingAndDrivingSchedules.put(id, 
							lpev.solveLP(agentParkingAndDrivingSchedules.getValue(id), id));
					
					
					
				}else{
					lpcombustion.updateSchedule(agentParkingAndDrivingSchedules.getValue(id));
					
					// get entire driving Joules and transform to emissions
					playground.wrashid.sschieffer.V1G.Main.EMISSIONCOUNTER= 
						joulesToEmissionInKg(lpcombustion.getDrivingConsumption());
					
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
			
			/*if(Integer.parseInt(id.toString())==2){
				System.out.println("agent =2 "); 
			}*/
			System.out.println("Assign charging times agent "+ id.toString());
			//System.out.println("current schedule "); 
			//agentParkingAndDrivingSchedules.getValue(id).printSchedule();
			
			
			Schedule chargingSchedule=myChargingSlotDistributor.distribute(agentParkingAndDrivingSchedules.getValue(id));
			//chargingSchedule.printSchedule();
			
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
        
        txt1.setFont(new Font("Arial", Font.PLAIN, 14));
        txt2.setFont(new Font("Arial", Font.PLAIN, 14));
        txt3.setFont(new Font("Arial", Font.PLAIN, 14));
        txt4.setFont(new Font("Arial", Font.PLAIN, 14));
        //public Font(String name,int style,int size)
            
        plot.addAnnotation(txt1);
        plot.addAnnotation(txt2);
        plot.addAnnotation(txt3);
        plot.addAnnotation(txt4);
        
        
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(3600));
        xAxis.setRange(0, playground.wrashid.sschieffer.V1G.Main.SECONDSPERDAY);
        
        
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(0, 5);
        yAxis.setTickUnit(new NumberTickUnit(1));
        yAxis.setVisible(false);
        
        int numSeries=dailySchedule.getNumberOfEntries()+chargingSchedule.getNumberOfEntries();
        
        for(int j=0; j<numSeries; j++){
        	
        	plot.getRenderer().setSeriesPaint(j, Color.black);
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
        
        
        /*ChartFrame frame1=new ChartFrame("XYLine Chart",chart);
        frame1.setVisible(true);
        frame1.setSize(300,300);  */ 
        ChartUtilities.saveChartAsPNG(new File(playground.wrashid.sschieffer.V1G.Main.outputPath+ "agent "+ id.toString()+"_dayPlan.png") , chart, 1000, 1000);
		  
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
        plot.setDrawingSupplier(playground.wrashid.sschieffer.V1G.Main.supplier);
        
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(3600));
        xAxis.setRange(0, playground.wrashid.sschieffer.V1G.Main.SECONDSPERDAY);
        
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
        
        ChartUtilities.saveChartAsPNG(new File(playground.wrashid.sschieffer.V1G.Main.outputPath + "_allChargingTimes.png"), chart, 2000, 2000);	
	
	}
	
	
	
	public void validateChargingDistribution() throws IOException{
		
		double [] count= new double[playground.wrashid.sschieffer.V1G.Main.MINUTESPERDAY];
		
		for(int i=0; i<playground.wrashid.sschieffer.V1G.Main.MINUTESPERDAY; i++){
			for(Id id : controler.getPopulation().getPersons().keySet()){
				Schedule thisAgentCharging = agentChargingSchedules.getValue(id);
				if(thisAgentCharging.isSecondWithinOneInterval(i*playground.wrashid.sschieffer.V1G.Main.SECONDSPERMIN)){
					count[i]=count[i]+1;
				}
			}
		}
		
		
		
		// make graph out of it
		XYSeriesCollection chargingDistributionTotal= new XYSeriesCollection();
		
		XYSeries chargingDistributionAgentSet= new XYSeries("");
		
		for(int i=0; i<count.length;i++){
			chargingDistributionAgentSet.add(i*playground.wrashid.sschieffer.V1G.Main.SECONDSPERMIN, count[i]);
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
        
  
    ChartUtilities.saveChartAsPNG(new File(playground.wrashid.sschieffer.V1G.Main.outputPath+ "validation_chargingdistribution.png") , chart, 800, 600);
	  
			
	}
	
	
	
	public double joulesToEmissionInKg(double joules){
		// Benzin 42,7–44,2 MJ/kg
		double mass=1/(43*1000000)*joules; // 1kgBenzin/43MJ= xkg/joules
		
		
		// für Benzin etwa 23,2 kg/10 l
		double emission= 23.2/10*mass; // 23,2kg/10l= xx/mass   1kg=1l
		//in kg
		
		
		return emission;
	}
	
}
