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
	private String suffix;
	
	private List<Double> avgNetTTs = new Vector<Double>();
	private List<Double> avgLinkTTs = new Vector<Double>();
	
	public Analyzer(String path, String suffix) {
		this.path = path;
		this.suffix = suffix;
	}
	
	public void addAvgNetTT(double avgTT) {
		this.avgNetTTs.add(avgTT);
	}
	
	public void addAvgLinkTT(double avgTT) {
		this.avgLinkTTs.add(avgTT);
	}
					
	public void runBPR(double [] xs) {		
		XYLineChart chart = new XYLineChart("avg link tt", "V/C [-]", "avg tt [min]", false);		
		chart.addSeries("", xs, Utils.convert(this.avgLinkTTs));	
		chart.saveAsPng(this.path + "/out/" + suffix + "_linkTTBPR.png", 700, 500);
		
		XYLineChart chart0 = new XYLineChart("avg network tt", "V/C [-]", "avg tt [min]", false);		
		chart0.addSeries("", xs, Utils.convert(this.avgNetTTs));	
		chart0.saveAsPng(this.path + "/out/" + suffix + "_netTTBPR.png", 700, 500);
	}

	public List<Double> getAvgNetTTs() {
		return avgNetTTs;
	}

	public List<Double> getAvgLinkTTs() {
		return avgLinkTTs;
	}
}
