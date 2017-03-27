/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
 * 
 */
package playground.michalm.drt.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.taxi.util.stats.TimeProfileCharts.ChartType;
import org.matsim.contrib.util.chart.ChartSaveUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class VehicleOccupancyEvaluator implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ActivityEndEventHandler {

	
	private final int bins;
	private int startTime;
	private int maxcap = 8;
	private int smoothSeconds = 300;

	private Map<Id<Vehicle>,int[]> vehicleOccupancy = new HashMap<>(); 
	private Set<Id<Person>> taxiDrivers = new HashSet<>();
	private Map<Id<Person>,Double> boardingTimes = new HashMap<>();
	
	
	@Inject
	public VehicleOccupancyEvaluator(Config config, EventsManager events) {
		this.startTime = (int) config.qsim().getStartTime();
		if (startTime<0) startTime = 0;
		double endTime;
		if (config.qsim().getEndTime()==Time.UNDEFINED_TIME)
		{
			endTime = 36*3600;	
		}
		else endTime = config.qsim().getEndTime();
		this.bins = (int) (endTime-startTime);
		events.addHandler(this);
	}
	
	public VehicleOccupancyEvaluator(double start, double end, int maxCapacity) {
		this.bins = (int) (end-start);
		this.startTime = (int) start;
		this.maxcap = maxCapacity;
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		vehicleOccupancy.clear();
		taxiDrivers.clear();
		boardingTimes.clear();
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.ActivityEndEventHandler#handleEvent(org.matsim.api.core.v01.events.ActivityEndEvent)
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)){
			
			Id<Vehicle> vid = Id.createVehicleId(event.getPersonId().toString());
			vehicleOccupancy.put(vid, new int[bins]);
			taxiDrivers.add(event.getPersonId());
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonLeavesVehicleEvent)
	 */
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (boardingTimes.containsKey(event.getPersonId())){
			double boardingTime = boardingTimes.remove(event.getPersonId());
			if (!this.vehicleOccupancy.containsKey(event.getVehicleId())){
				throw new RuntimeException ("Vehicle v "+event.getVehicleId() + " not registered");
			}
			int startBin = getBin(boardingTime);
			int endBin = getBin(event.getTime());
			for (int i = startBin; i<endBin;i++){
				vehicleOccupancy.get(event.getVehicleId())[i]++;
				if (vehicleOccupancy.get(event.getVehicleId())[i]>4)
				{
					System.out.println(event.getTime() + " " + event.getVehicleId());
				}
			}
			
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonEntersVehicleEvent)
	 */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.vehicleOccupancy.containsKey(event.getVehicleId())){
			if (!this.taxiDrivers.contains(event.getPersonId())){
				boardingTimes.put(event.getPersonId(), event.getTime());
			}
		}
	}
	
	private int getBin(double time)
	{
		int bin = (int) (time-startTime);
		if (bin<0) throw new RuntimeException("invalid timeframe");
		return bin;
		}
	
	public void writeDetailedOccupancyFiles(String fileNamePrefix){
		for (Entry<Id<Vehicle>, int[]> e : this.vehicleOccupancy.entrySet()){
			String fileName = fileNamePrefix+e.getKey()+".txt";
			BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
			try {
				bw.write("time;occupancy");
				
				for (int i = 0; i<e.getValue().length;i++){
					bw.newLine();
					bw.write(startTime+i + ";"+e.getValue()[i]);
				}
				bw.flush();
				bw.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	public void calcAndWriteFleetStats(String statsFileName){
		int[][] occupancyOverTime = new int[bins][maxcap+1];
		
		DecimalFormat format = new DecimalFormat();
	    format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
	    format.setMinimumIntegerDigits(1);
	    format.setMaximumFractionDigits(2);
		
	    DescriptiveStatistics stats[] = new DescriptiveStatistics[bins];
		for (int i= 0; i<bins;i++){
			stats[i] = new DescriptiveStatistics();
			for (int[] occ : vehicleOccupancy.values()){
				stats[i].addValue(occ[i]);
				occupancyOverTime[i][occ[i]]++;
			}
		}
		BufferedWriter bw = IOUtils.getBufferedWriter(statsFileName+".csv");
		try {
			bw.write("time;mean;median;min;max");
			for (int i = 0; i<=maxcap;i++){
				bw.write(";"+i+"_pax");
			}
			for (int i = 0; i<bins;i++){
				bw.newLine();
				int time = i+startTime;
				double mean = stats[i].getMean();
				double median = stats[i].getGeometricMean();
				double min = stats[i].getMin();
				double max = stats[i].getMax();
				bw.write(time+";"+format.format(mean)+";"+format.format(median)+";"+min+";"+max);
				for (int ii = 0; ii<=maxcap;ii++){
					bw.write(";"+occupancyOverTime[i][ii]);
				}
			}
			
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double[][] occupancyOverTimeAverage = calcAverage(occupancyOverTime,smoothSeconds);
		
		DefaultTableXYDataset dataset = new DefaultTableXYDataset();
		XYSeries[] seriesArray = new XYSeries[maxcap+1];
		for (int s = 0; s < maxcap+1; s++) {
			seriesArray[s] = new XYSeries(s+" pax", false, false);
			for (int t = 0; t<occupancyOverTimeAverage.length;t++){
				seriesArray[s].add(startTime+t*smoothSeconds, occupancyOverTimeAverage[t][s]);
			}
			dataset.addSeries(seriesArray[s]);
		}
		JFreeChart chart = chartProfile(bins, dataset, ChartType.StackedArea);
		JFreeChart chartl = chartProfile(bins, dataset, ChartType.Line);
		
		ChartSaveUtils.saveAsPNG(chart, statsFileName, 1500, 1000);
		ChartSaveUtils.saveAsPNG(chartl, statsFileName+"_lines", 1500, 1000);
	}
	

	
	 /**
	 * @param occupancyOverTime
	 * @param seconds to smooth
	 * @return
	 */
	private double[][] calcAverage(int[][] occupancyOverTime, int seconds) {
		int newbins = occupancyOverTime.length / seconds;
		double[][] averageOccupancy =  new double[newbins][maxcap+1];
		int[] currentSum = new int[maxcap+1]; 
		for (int i = 0; i<occupancyOverTime.length; i++){
			for (int s = 0;s<=maxcap;s++){
				int v = occupancyOverTime[i][s];
				currentSum[s]+=v;
			}
			if (i%seconds == 0){
				for (int s = 0;s<=maxcap;s++){
					averageOccupancy[i/seconds][s]=((double)currentSum[s]/(double)seconds);
					currentSum[s]=0;
				}
			}
		}
		
		return averageOccupancy;
	}

	private JFreeChart chartProfile(int length, DefaultTableXYDataset dataset, ChartType type) {
			JFreeChart chart;
			switch (type) {
				case Line:
					chart = ChartFactory.createXYLineChart("TimeProfile", "Time [s]", "Values", dataset,
							PlotOrientation.VERTICAL, true, false, false);
					break;

				case StackedArea:
					chart = ChartFactory.createStackedXYAreaChart("TimeProfile", "Time [s]", "Values", dataset,
							PlotOrientation.VERTICAL, true, false, false);
					break;

				default:
					throw new IllegalArgumentException();
			}

			XYPlot plot = chart.getXYPlot();
			plot.setRangeGridlinesVisible(false);
			plot.setDomainGridlinesVisible(false);
			plot.setBackgroundPaint(Color.white);

			NumberAxis xAxis = (NumberAxis)plot.getDomainAxis();
			xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

			NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
			yAxis.setAutoRange(true);

			XYItemRenderer renderer = plot.getRenderer();
			for (int s = 0; s < length; s++) {
				renderer.setSeriesStroke(s, new BasicStroke(2));
			}

			return chart;
		}
	 

}
