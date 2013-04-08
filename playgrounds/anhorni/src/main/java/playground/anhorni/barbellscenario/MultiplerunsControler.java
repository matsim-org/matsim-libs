/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.barbellscenario;

import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

import playground.anhorni.utils.Utils;

public class MultiplerunsControler {
	
	private final static Logger log = Logger.getLogger(MultiplerunsControler.class);
	private int numberOfRuns = 31;
	private Analyzer analyzer = null;
	
	private double demand = 300.0; // [veh/h]
	private double capacity = 1000.0; // [veh/h]
	
    public static void main (final String[] args) {  	
    	MultiplerunsControler runControler = new MultiplerunsControler();
    	String path = args[0];
    	runControler.run(path);
    }
               
    public void run(String path) {
    	
    	this.analyzer = new Analyzer(path);
 	    	
    	String config[] = {""};
		Controler controler;
			
		List<Double> caps = new Vector<Double>();
		List<Double> bpr = new Vector<Double>();
		
		List<Double> bpr0 = new Vector<Double>();
		List<Double> bpr1 = new Vector<Double>();
					    	   	
    	for (int runIndex = 0; runIndex < numberOfRuns; runIndex++) {     		
    		config[0] = path + "/config.xml";
	    	controler = new Controler(config);
	    	controler.setOverwriteFiles(true);
	    	
	    	double flowCapFactor = controler.getConfig().getQSimConfigGroup().getFlowCapFactor() + runIndex * 0.1; 
	    	
	    	caps.add(flowCapFactor * capacity / 60.0);	    	
	    	bpr.add(demand / (flowCapFactor * capacity));  
	    	
	    	bpr0.add(5 + Math.pow(demand / (flowCapFactor * capacity / 60.0), 5.0));
	    	bpr1.add(5 + Math.pow(demand / (flowCapFactor * capacity / 60.0), 10.0));
	    	
	    	controler.getConfig().setParam("qsim", "flowCapacityFactor", Double.toString(flowCapFactor));
	    	controler.getConfig().setParam("controler", "runId", "run" + runIndex);
	    	
	    	TravelTimesControlerListener ttListener = new TravelTimesControlerListener(
	    			Integer.parseInt(controler.getConfig().getParam("controler", "lastIteration")));
	    	controler.addControlerListener(ttListener);
	    	
	    	controler.run();
	    	
	    	List<Double> netTTs = ttListener.getNetTTs();
	    	List<Double> linkTTs = ttListener.getLinkTTs();
	    	
	    	analyzer.addAvgNetTT(this.getAvg(netTTs) / 60.0);
	    	analyzer.addAvgLinkTT(this.getAvg(linkTTs) / 60.0);
	    	
    	}
    	log.info("Create analysis ...");   	
    	analyzer.runBPR(Utils.convert(bpr), Utils.convert(bpr0), Utils.convert(bpr1));  
    	analyzer.runC(Utils.convert(caps)); 
    	log.info("All runs finished ******************************");
    }
    
    private double getAvg(List<Double> list) {
    	double avg = 0.0;
    	for (double val:list) {
    		avg += val / list.size();
    	}
    	return avg;
    }
}
