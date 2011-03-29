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

package playground.wrashid.sschieffer.DecentralizedSmartCharger;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.LinkedListValueHashMap;



public class HubLoadDistributionReader {
	
		
	private HubLinkMapping hubLinkMapping;
	
	LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution;
	
	Controler controler;
	
	/**
	 * Reads in load data for all hubs and stores PolynomialFunctions 
	 * of load valleys and peak load times
	 * @throws IOException 
	 * @throws OptimizationException 
	 */
	public HubLoadDistributionReader(Controler controler, 
			HubLinkMapping hubLinkMapping,
			LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution) throws IOException, OptimizationException{
		
		this.controler=controler;
		
		this.hubLinkMapping=hubLinkMapping;
		
		this.hubLoadDistribution=hubLoadDistribution;
		
				
	}
	
	
	
	
	
	public int getHubForLinkId(Id idLink){
		return hubLinkMapping.getHubNumber(idLink.toString());
	}
	
	
	public PolynomialFunction getPolynomialFunctionAtLinkAndTime(Id idLink, double startTime, double endTime){
		
		int hub= getHubForLinkId(idLink);
		
		Schedule hubLoadSchedule = hubLoadDistribution.getValue(hub);
		int interval = hubLoadSchedule.timeIsInWhichInterval(startTime);
		
		LoadDistributionInterval l1= (LoadDistributionInterval) hubLoadSchedule.timesInSchedule.get(interval);
		
		return l1.getPolynomialFunction();
		
		
	}
	

	public Schedule getLoadDistributionScheduleForHubId(Id idLink){
		int hub= getHubForLinkId(idLink);
		return hubLoadDistribution.getValue(hub);
	}
	
	
	
	
	public PolynomialFunction readLoadDistribution() throws IOException, OptimizationException{
		double [][] slotBaseLoad= loadBaseLoadCurveFromTextFile();
		return fitCurve(slotBaseLoad);
	}
	
	
	
	
	public double [][] loadBaseLoadCurveFromTextFile() throws IOException{
		// read in double matrix with time and load value
		double [][] slotBaseLoad = GeneralLib.readMatrix(96, 2, false, "test\\input\\playground\\wrashid\\sschieffer\\baseLoadCurve15minBinsSecLoad.txt");
		
		
		XYSeries loadFigureData = new XYSeries("Baseload Data from File");
		XYSeriesCollection dataset = new XYSeriesCollection();
		
		for(int i=0; i<slotBaseLoad.length; i++){
			slotBaseLoad[i][1]= slotBaseLoad[i][1]*100000;
			loadFigureData.add(slotBaseLoad[i][0], slotBaseLoad[i][1]);
		}
		
		
		dataset.addSeries(loadFigureData);		
	     
		JFreeChart chart = ChartFactory.createXYLineChart("Base Load from File", "time of day [s]", "Load [W]", dataset, PlotOrientation.VERTICAL, true, true, false);
		ChartUtilities.saveChartAsPNG(new File(DecentralizedSmartCharger.outputPath+ "hubSchedule.png") , chart, 800, 600);
		/*ChartFrame frame1=new ChartFrame("XYLine Chart",chart);
        frame1.setVisible(true);
        frame1.setSize(300,300); */
		return slotBaseLoad;
	}
	
	
	
	public PolynomialFunction fitCurve(double [][] data) throws OptimizationException{
		
		for (int i=0;i<data.length;i++){
			DecentralizedSmartCharger.polyFit.addObservedPoint(1.0, data[i][0], data[i][1]);
			
		  }		
		 
		
		 PolynomialFunction poly = DecentralizedSmartCharger.polyFit.fit();
		
		return poly;
	}
	
	
	
}
