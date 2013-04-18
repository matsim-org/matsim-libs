/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioBundleSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.spacetimegeo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.log4j.Logger;

/**
 * @author benjamin
 *
 */
public class ScenarioBundleSelector {
	private static Logger logger = Logger.getLogger(ScenarioBundleSelector.class);
	
	Map<Integer, ScenarioParams> scenarioId2Params;
	int scenarioBundleId;
	String workingDirectory;

	public void selectScenarioBundle(int sbid, String wd) {
		this.scenarioBundleId = sbid;
		this.workingDirectory = wd;
		
		if(sbid == 1){
			selectScenarioBundle1();
		} else if(sbid == 2){
			selectScenarioBundle2();
		} else {
			throw new RuntimeException("Unsupported scenario Id. Aborting...");
		}
	}

	private void selectScenarioBundle1() {
		String fileName = workingDirectory + "output/parameters" + scenarioBundleId + ".txt";
		File file = new File(fileName);

		scenarioId2Params = new HashMap<Integer, ScenarioParams>();

		Double mu = 1.0;
		Double betaCost = 0.0;
		Double betaTimeBase = 6.0;
		Double betaTimeScaleFactor = 1.0;

		for(int i=1; i<=7;i++){
			ScenarioParams scenarioParams = new ScenarioParams();
			scenarioParams.setMu(mu);
			scenarioParams.setBetaCost(betaCost);

			if(i==1){
				scenarioParams.setChoiceModule("SelectRandom");
				scenarioParams.setBetaPerf(new Random().nextDouble());
				scenarioParams.setBetaLate(new Random().nextDouble());
				scenarioParams.setBetaTraveling(new Random().nextDouble());
			} else {
				scenarioParams.setChoiceModule("SelectExpBeta");
			}

			if(i==2){
				scenarioParams.setBetaPerf(0.0 * betaTimeScaleFactor);
				scenarioParams.setBetaLate(0.0 * betaTimeScaleFactor);
				scenarioParams.setBetaTraveling(0.0 * betaTimeScaleFactor);
			} else if(i==3){
				scenarioParams.setBetaPerf(0.0 * betaTimeScaleFactor);
				scenarioParams.setBetaLate(-betaTimeBase * 10. * betaTimeScaleFactor);
				scenarioParams.setBetaTraveling(0.0 * betaTimeScaleFactor);
			} else if(i==4){
				scenarioParams.setBetaPerf(0.0 * betaTimeScaleFactor);
				scenarioParams.setBetaLate(0.0 * betaTimeScaleFactor);
				scenarioParams.setBetaTraveling(-betaTimeBase * betaTimeScaleFactor);
			} else if(i==5){
				scenarioParams.setBetaPerf(betaTimeBase * betaTimeScaleFactor);
				scenarioParams.setBetaLate(0.0 * betaTimeScaleFactor);
				scenarioParams.setBetaTraveling(0.0 * betaTimeScaleFactor);
			} else if(i==6){
				scenarioParams.setBetaPerf(betaTimeBase * betaTimeScaleFactor);
				scenarioParams.setBetaLate(0.0 * betaTimeScaleFactor);
				scenarioParams.setBetaTraveling(-betaTimeBase * betaTimeScaleFactor);
			} else if(i==7){
				scenarioParams.setBetaPerf(betaTimeBase * betaTimeScaleFactor);
				scenarioParams.setBetaLate(-betaTimeBase * 10. * betaTimeScaleFactor);
				scenarioParams.setBetaTraveling(-betaTimeBase * betaTimeScaleFactor);
			}
			scenarioId2Params.put(i, scenarioParams);
		}
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.append("Scenario Nr. \t Choice module \t betaPerf \t betaLate \t betaTraveling \n");
			for(Entry<Integer, ScenarioParams> entry : scenarioId2Params.entrySet()){
				bw.append(entry.getKey() + "\t" + entry.getValue().getChoiceModule() + "\t" + 
						  entry.getValue().getBetaPerf() + "\t" + entry.getValue().getBetaLate() + "\t" +
						  entry.getValue().getBetaTraveling());
				bw.newLine();
			}
			bw.close();
			logger.info("Finished writing output to " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void selectScenarioBundle2() {
		// TODO Auto-generated method stub
		
	}

	public Map<Integer, ScenarioParams> getScenarioId2Params() {
		return scenarioId2Params;
	}
}