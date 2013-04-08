/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.barbellscenario;

import java.util.List;
import java.util.Vector;
import org.matsim.core.utils.charts.XYLineChart;

import playground.anhorni.utils.Utils;

public class Analyzer { 
	private String path;
	
	private List<Double> avgNetTTs = new Vector<Double>();
	private List<Double> avgLinkTTs = new Vector<Double>();
	
	public Analyzer(String path) {
		this.path = path;
	}
	
	public void addAvgNetTT(double avgTT) {
		this.avgNetTTs.add(avgTT);
	}
	
	public void addAvgLinkTT(double avgTT) {
		this.avgLinkTTs.add(avgTT);
	}
					
	public void runBPR(double [] xs, double [] tBPR0, double [] tBPR1) {		
		XYLineChart chart = new XYLineChart("avg link tt [min]", "Demand/Capacity [-]", "avg tt", false);		
		chart.addSeries("avg link 3 tt", xs, Utils.convert(this.avgLinkTTs));
//		chart.addSeries("tt BPR beta = ", xs, tBPR0);	
		chart.saveAsPng(this.path + "/out/linkTTBPR.png", 700, 500);
		
		XYLineChart chart0 = new XYLineChart("avg network tt [min]", "Demand/Capacity [-]", "avg tt", false);		
		chart0.addSeries("avg net tt", xs, Utils.convert(this.avgNetTTs));	
		chart0.saveAsPng(this.path + "/out/netTTBPR.png", 700, 500);
	}
	
	public void runC(double [] xs) {		
		XYLineChart chart = new XYLineChart("avg link tt [min]", "Capacity [veh/min]", "avg tt", false);		
		chart.addSeries("avg link 3 tt", xs, Utils.convert(this.avgLinkTTs));
		chart.saveAsPng(this.path + "/out/linkTT.png", 700, 500);
		
		XYLineChart chart0 = new XYLineChart("avg network tt [min]", "Capacity [veh/min]", "avg tt", false);		
		chart0.addSeries("avg net tt", xs, Utils.convert(this.avgNetTTs));	
		chart0.saveAsPng(this.path + "/out/netTT.png", 700, 500);
	}
}
