/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.joint;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;

import playground.agarwalamit.analysis.modalShare.ModalShareFromPlans;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class JointDemandModalSplit {

	public static void main(String[] args) {
		new JointDemandModalSplit().run();
	}
	
	private void run (){
		String dir = "/Users/amit/Documents/cluster/ils4/agarwal/patnaIndia/run108/calibration/";
		String folder = "c1/ITERS/it.";
		String itNr = "100";
		String plansFile = dir+folder+itNr+"/"+itNr+".plans.xml.gz";
		
		Scenario sc = LoadMyScenarios.loadScenarioFromPlans(plansFile);
		Population pop = sc.getPopulation();
		ModalShareFromPlans msg = new ModalShareFromPlans(pop);
		msg.run();
		
		String outFile = dir+folder+itNr+"/"+itNr+".modalSplit.txt";
		
		msg.writeResults(outFile);
	}
}
