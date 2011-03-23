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

package playground.wrashid.sschieffer.V1G;
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
import playground.wrashid.sschieffer.Main;


public class HubLoadDistributionReader {
	
	private int numberOfHubs;
	
	private HubLinkMapping hubLinkMapping;
	
	LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution = new LinkedListValueHashMap<Integer, Schedule>();
	Controler controler;
	
	/**
	 * Reads in load data for all hubs and stores PolynomialFunctions 
	 * of load valleys and peak load times
	 * @throws IOException 
	 * @throws OptimizationException 
	 */
	public HubLoadDistributionReader(Controler controler) throws IOException, OptimizationException{
		this.controler=controler;
		
		readHubs();// so far reads in 4 bogus hubs
		//PolynomialFunction baseLoadFunction= readLoadDistribution();
		
		mapHubs();// depending on location x y coordinate
		
		
	}
	
	
	public void readHubs() throws IOException{
		hubLoadDistribution.put(1, makeBullshitSchedule());
		hubLoadDistribution.put(2, makeBullshitSchedule());
		hubLoadDistribution.put(3, makeBullshitSchedule());
		hubLoadDistribution.put(4, makeBullshitSchedule());
		
		numberOfHubs=hubLoadDistribution.size();
	}
	
	
	public Schedule makeBullshitSchedule() throws IOException{
		
		Schedule bullShitSchedule= new Schedule();
		
		double[] bullshitCoeffs = new double[]{100, 5789, 56};// 
		double[] bullshitCoeffs2 = new double[]{-22, 44.6, -32.5};
		
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
				playground.wrashid.sschieffer.V1G.Main.SECONDSPERDAY,
				bullShitFunc2,//p
				false//boolean
		);
		l2.makeXYSeries();
		bullShitSchedule.addTimeInterval(l2);
		
		bullShitSchedule.visualizeLoadDistribution("BullshitSchedule");	
		return bullShitSchedule;
	}
	
	
	public void mapHubs(){
		//NetworkImpl network = GeneralLib.readNetwork("C:/Users/stellas/StellasWorkspace/playgrounds/wrashid/test/scenarios/equil/network.xml");
		hubLinkMapping=new HubLinkMapping(numberOfHubs);
		
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
		ChartUtilities.saveChartAsPNG(new File(playground.wrashid.sschieffer.V1G.Main.outputPath+ "hubSchedule.png") , chart, 800, 600);
		/*ChartFrame frame1=new ChartFrame("XYLine Chart",chart);
        frame1.setVisible(true);
        frame1.setSize(300,300); */
		return slotBaseLoad;
	}
	
	
	
	public PolynomialFunction fitCurve(double [][] data) throws OptimizationException{
		
		for (int i=0;i<data.length;i++){
			playground.wrashid.sschieffer.V1G.Main.polyFit.addObservedPoint(1.0, data[i][0], data[i][1]);
			
		  }		
		 
		
		 PolynomialFunction poly = playground.wrashid.sschieffer.V1G.Main.polyFit.fit();
		
		return poly;
	}
	
	
	
}
