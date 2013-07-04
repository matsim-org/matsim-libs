/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.mmoyo.taste_variations;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;

/**
 * 	-reads txt file with utl correction, 
 * -assign it to persons, 
 * -svd calculation of travel parameters
 * 
 * -if you want to calculate svd values directly from scores use class MySVDCalculatior
 */

public class SvdValuesFromScores {

	public static void main(String[] args) {
		String inputUtlCorrectionsFile;
		String configFile;
		if (args.length>0){
			configFile = args[0];
			inputUtlCorrectionsFile = args[1];
		}else{
			configFile = "../../";
			inputUtlCorrectionsFile = "../../";
		}

		//load config
		DataLoader loader = new DataLoader(); 
		final Config config = loader.readConfig(configFile);
		String popFile = config.plans().getInputFile();
		String netFile = config.network().getInputFile();
		
		//read utilities corrections from txt file and set them to plans   //comment out if the plans already contain the score as utility
		UtilCorrectonReader2 reader= new UtilCorrectonReader2( config.strategy().getMaxAgentPlanMemorySize() );
		try {
			reader.readFile(inputUtlCorrectionsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		SetUtl2pop setUtl2pop = new SetUtl2pop();
		String popWutlCorr = setUtl2pop.setUtCorrections(popFile, reader.getCorrecMap() , netFile);
		
		//svd calculation of travel parameters 
		ScenarioImpl scnWOpop = (ScenarioImpl) loader.createScenario();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scnWOpop);
		matsimNetReader.readFile(netFile);
		Network net = scnWOpop.getNetwork();
		matsimNetReader = null;
		TransitSchedule schedule = loader.readTransitSchedule(config.transit().getTransitScheduleFile());
		MyLeastSquareSolutionCalculator solver = new MyLeastSquareSolutionCalculator(net, schedule, MyLeastSquareSolutionCalculator.SVD);
		new PopSecReader(scnWOpop, solver).readFile(popWutlCorr);    //Sequential calculation
		
		//write solutions file in the folder where the Corrections File is
		final String path = new File(popFile).getPath();
		solver.writeSolutionTXT(path + "SVDSolutions.txt");   //write solutions txt file
		final String svdSolutionXML = path + "SVDSolutions.xml.gz";
		solver.writeSolutionObjAttr(svdSolutionXML);
		
	}

}
