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

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.utils.Utils;

public class MultiplerunsControler {
	
	private final static Logger log = Logger.getLogger(MultiplerunsControler.class);
	private int numberOfRuns = 7;
	private Analyzer analyzer = null;
	private double initCapacity = 1000.0;
		
    public static void main (final String[] args) {  	
    	MultiplerunsControler runControler = new MultiplerunsControler();
    	String path = args[0];
    	runControler.run(path);
    }
    
    public Analyzer runDemand(String path, double flowCapacityFactor) {
    	DecimalFormat formatter = new DecimalFormat("0.0");
    	this.analyzer = new Analyzer(path, formatter.format(flowCapacityFactor));
	    	
    	String config[] = {""};
		Controler controler;
			
		List<Double> xs = new Vector<Double>();
							    	   	
    	for (int i = 100; i <= 1000; i+=100) {     		
    		config[0] = path + "/config.xml";
	    	controler = new Controler(config);
			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

			controler.getConfig().setParam("plans", "inputPlansFile", path + i + "plans.xml");
	    	
	    	controler.getConfig().setParam("qsim", "flowCapacityFactor", formatter.format(flowCapacityFactor));
	    	controler.getConfig().setParam("controler", "runId", "demand_" + i +"_fcf_" + formatter.format(flowCapacityFactor));
	    	
	    	TravelTimesControlerListener ttListener = new TravelTimesControlerListener(
	    			Integer.parseInt(controler.getConfig().getParam("controler", "lastIteration")));
	    	controler.addControlerListener(ttListener);
	    	
	    	controler.run();
	    	
	    	List<Double> netTTs = ttListener.getNetTTs();
	    	List<Double> linkTTs = ttListener.getLinkTTs();
	    	
	    	analyzer.addAvgNetTT(this.getAvg(netTTs) / 60.0);
	    	analyzer.addAvgLinkTT(this.getAvg(linkTTs) / 60.0);
	    	
	    	xs.add(i * 1.0 / (flowCapacityFactor * initCapacity));
    	}
    	log.info("Create analysis ...");   	
    	analyzer.runBPR(Utils.convert(xs));  
    	
    	return analyzer;
    }
    
               
    public void run(String path) {
    	DecimalFormat formatter = new DecimalFormat("0.00");
    	final BufferedWriter outLink = IOUtils.getBufferedWriter(path + "/out/0linksummary.txt");
    	final BufferedWriter outNet = IOUtils.getBufferedWriter(path + "/out/0netsummary.txt"); 
    	try {
	    	for (int runIndex = 1; runIndex <= numberOfRuns; runIndex++) {     			    	
		    	double flowCapacityFactor = 0.3 + runIndex * 0.1; 
		    	Analyzer analyzer = this.runDemand(path, flowCapacityFactor);
		    	
		    	List<Double> linkTTs = analyzer.getAvgLinkTTs();
		    	for (double v : linkTTs) {
		    		outLink.write(formatter.format(v) + "\t");
		    	}	    	
		    	outLink.newLine();
		    	outLink.flush();
		    	
		    	List<Double> netTTs = analyzer.getAvgNetTTs();
		    	for (double v : netTTs) {
		    		outNet.write(formatter.format(v) + "\t");
		    	}
		    	outNet.newLine();
		    	outNet.flush();
	    	}
	    	outLink.close();
	    	outNet.close();
		} catch (IOException e) {
				e.printStackTrace();
		}
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
