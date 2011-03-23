/* *********************************************************************** *
 * project: org.matsim.*
 * Schedule.java
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
package playground.wrashid.sschieffer.V1G;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;


/**
 * saves activities (driving, parking) of agent in one Object
 * @author Stella
 *
 */
public class Schedule {
	
	double totalJoulesInOptimalParkingTimes=0;
	double totalJoulesInSubOptimalParkingTimes=0;
	
	
	LinkedList<TimeInterval> timesInSchedule= new LinkedList<TimeInterval>();
	
	
	public Schedule(){
		
	}
	
	
	public void addTimeInterval(TimeInterval t){
		timesInSchedule.add(t);
	}
	
	public void sort(){
		Collections.sort(timesInSchedule);
	}
	
	public void printSchedule(){
		for(TimeInterval t: timesInSchedule){
			t.printInterval();
		}
		
		
	}
	
	public int getNumberOfEntries(){
		return timesInSchedule.size();
	}
	
	
	public void mergeSchedules(Schedule schedule2){
		for(int i=0; i< schedule2.getNumberOfEntries(); i++){
			timesInSchedule.add(schedule2.timesInSchedule.get(i));
		}
		
		this.sort();
	}
	
	public void visualizeLoadDistribution(String name) throws IOException{
		XYSeriesCollection loadDistributionIntervals = new XYSeriesCollection();
		/*XYSeriesCollection drivingIntervals = new XYSeriesCollection();
		XYSeriesCollection parkingIntervals = new XYSeriesCollection();
		XYSeriesCollection chargingIntervals = new XYSeriesCollection();*/
		
		for(TimeInterval t: timesInSchedule){
			if(t.getClass().equals(new LoadDistributionInterval(0,0,null,false).getClass())){
				LoadDistributionInterval t2=(LoadDistributionInterval) t;
				loadDistributionIntervals.addSeries(t2.getXYSeries());
			}
		}
		
        
        JFreeChart chart = ChartFactory.createXYLineChart(
        		name, "time of day", "free load [W]", loadDistributionIntervals, 
        		PlotOrientation.VERTICAL, false, true, false);
        
        final XYPlot plot = chart.getXYPlot();
        plot.setDrawingSupplier(Main.supplier);
    
        plot.getRenderer().setSeriesPaint(0, Color.black);
        plot.getRenderer().setSeriesStroke(
	            0, 
	            new BasicStroke(
	                2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
	                1.0f, new float[] {10.0f, 6.0f}, 5.0f
	            )
	        );
	   
        
	    
        chart.setBackgroundPaint(Color.white);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray); 
        
        //ChartFrame frame1=new ChartFrame("XYLine Chart",chart);
        ChartUtilities.saveChartAsPNG(new File(playground.wrashid.sschieffer.V1G.Main.outputPath+ "hubSchedule.png") , chart, 800, 600);
        /*frame1.setVisible(true);
        frame1.setSize(300,300);*/
	}
	
	
	/**
	 * checks if there is an overlap with any of the Timeintervals in the schedule and the specified TimeInterval
	 * 
	 * returns true if overlap
	 * false if no overlap
	 * 
	 * @param t
	 * @return
	 */
	public boolean overlapWithTimeInterval(TimeInterval t){
		boolean overlap=false;
		
		for(int i=0; i<timesInSchedule.size(); i++){
			TimeInterval thisT= timesInSchedule.get(i);
			
			if(   (t.getStartTime()>=thisT.getStartTime()  && t.getStartTime()<=thisT.getEndTime())
					|| (t.getEndTime()>=thisT.getStartTime()  && t.getEndTime()<=thisT.getEndTime())){
				//if overlap
				overlap=true;
				
			}
		}
		
		return overlap;
	}
	
	
	public int timeIsInWhichInterval(double time){
		int solution=-1;
		for (int i=0; i<timesInSchedule.size(); i++){
			if(time<timesInSchedule.get(i).getEndTime() && 
					time>=timesInSchedule.get(i).getStartTime()){
				solution =i;
			}				
		}
		
		if(time==timesInSchedule.get(timesInSchedule.size()-1).getEndTime()){
			solution =timesInSchedule.size()-1;
		}
		return solution;
	}
	
	public void addJoulesToTotalSchedule(double joules){
		if(joules<=0){
			totalJoulesInSubOptimalParkingTimes+=joules;
		}
		else{
			totalJoulesInOptimalParkingTimes+=joules;
		}
	}
	
	
	public int numberOfDrivingTimes(){
		int count=0;
		for(int i=0; i<timesInSchedule.size(); i++){
			if(timesInSchedule.get(i).isDriving()){
				count++;
			}
		}
		return count;
	}
	
	
	public int positionOfIthDrivingTime(int ithTime){
		int sol=0;
		int count=0;
		this.printSchedule();
		
		for(int i=0; i<timesInSchedule.size(); i++){
			
			if(timesInSchedule.get(i).isDriving()){
				if(count == ithTime){
					sol= i;
					count++;
				}
				
				if(count<ithTime){
					count++;
				
				}
			}
		}
		return sol;
	}
	
	
	/**
	 * checks if second is within one interval of the schedule
	 * true if in interval
	 * 
	 * @param sec
	 * @return
	 */
	public boolean isSecondWithinOneInterval(double sec){
		boolean inInterval=false;
		
		for(int i=0; i<getNumberOfEntries(); i++){
			TimeInterval t= timesInSchedule.get(i);
			if(sec<=t.getEndTime() && sec>=t.getStartTime()){
				// in interval
				inInterval=true;
			}
		}
		return inInterval;
		
	}
	
	
	
}
