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
 * This class avoids manual works by automatically: 
 * 	-reading txt file with utlcorrection, 
 * -assign it to persons, 
 * -svd calculation of travel parameters
 * -launch the svd scoring based simualtion 
 *
 */

public class SvdValuesFromUtlTxtFile {

	public static void main(String[] args) {
		String inputUtlCorrectionsFile;
		String configFile;		
		if (args.length>0){
			configFile = args[0];
			inputUtlCorrectionsFile = args[1];
		}else{
			configFile = "../../ptManuel/calibration/my_config2.xml";
			inputUtlCorrectionsFile = "../../input/choiceM44/10plans/10corrections.txt";
		}

		//read utilities corrections from txt
		UtilCorrectonReader2 reader= new UtilCorrectonReader2();
		try {
			reader.readFile(inputUtlCorrectionsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//load config
		DataLoader loader = new DataLoader(); 
		final Config config = loader.readConfig(configFile);
		String popFile = config.plans().getInputFile();
		
		//Set utCorr to plans  
		SetUtl2pop setUtl2pop = new SetUtl2pop();
		String netFile = config.network().getInputFile();
		String popWutlCorr = setUtl2pop.setUtCorrections(popFile, reader.getCorrecMap() , netFile);

		//svd calculation of travel parameters and write solutions file 
		ScenarioImpl scnWOpop = (ScenarioImpl) loader.createScenario();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scnWOpop);
		matsimNetReader.readFile(netFile);
		Network net = scnWOpop.getNetwork(); 		
		TransitSchedule schedule = loader.readTransitSchedule(config.transit().getTransitScheduleFile());
		MySVDcalculator solver = new MySVDcalculator(net, schedule);
		new PopSecReader(scnWOpop, solver).readFile(popWutlCorr);    //sequencial calculation
		final String path = new File(popWutlCorr).getPath();
		solver.writeSolutionTXT(path + "SVDSolutions.txt");   //write solutions txt file
		final String svdSolutionXML = path + "SVDSolutions.xml.gz";
		solver.writeSolutionObjAttr(svdSolutionXML);
		
	}

}
