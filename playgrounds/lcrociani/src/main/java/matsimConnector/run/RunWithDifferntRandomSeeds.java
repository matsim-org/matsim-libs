/* *********************************************************************** *
 * project: org.matsim.*
 * RunWithDifferntRandomSeeds.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package matsimConnector.run;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import matsimConnector.utility.Variance;

import org.matsim.core.utils.misc.StringUtils;

import pedCA.utility.Constants;

public class RunWithDifferntRandomSeeds {
	
	
	public static void main(String [] args) throws IOException {
		String pwd = System.getProperty("user.dir");
		String dataDir = "/Users/laemmel/devel/plaueexp/dec2010_trajectories/sim/";
		String outPutDataFile = matsimConnector.utility.Constants.OUTPUT_PATH + "/Output/Fundamental_Diagram/Classical_Voronoi/rho_v_Voronoi_agentTrajectoriesFlippedTranslatedCleaned.txt_id_1.dat";
		
		//1. run simulations
		for (int i = 0; i < 1000; i++) {
			Constants.RANDOM_SEED = i*42;
			LoadAndRunCASimulation.main(args);
			Files.copy(Paths.get(outPutDataFile), 
					Paths.get(dataDir+"/90degRhoVRndSeed" +i),
					StandardCopyOption.REPLACE_EXISTING);
			File newPwd = new File(pwd).getAbsoluteFile();
			if (System.setProperty("user.dir", newPwd.getAbsolutePath()) == null) {
				throw new RuntimeException("could not change working directory");
			}
			
		}
		//2. average and variance
		Map<Integer,Variance> frames = new TreeMap<Integer, Variance>();
		Map<Integer,Variance> frames2 = new TreeMap<Integer, Variance>();
		for (int i = 0; i < 1000; i++) {
			String file = dataDir+"/90degRhoVRndSeed" +i;
			BufferedReader br = new BufferedReader(new FileReader(new File(file)));
			String l = br.readLine();
			while (l != null) {
				if (!l.startsWith("#")){
					String[] expl = StringUtils.explode(l, '\t');
					int frame = Integer.parseInt(expl[0]);
					Variance v = frames.get(frame);
					Variance v2 = frames2.get(frame);
					if (v == null) {
						v = new Variance();
						frames.put(frame, v);
						v2 = new Variance();
						frames2.put(frame, v2);
					}
					double rho = Double.parseDouble(expl[1]);
					double spd = Double.parseDouble(expl[2])/16/0.3;
					v.addVar(rho);
					v2.addVar(spd);
				}
				l = br.readLine();
			}
			br.close();
		}
		
		//3. write results
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(dataDir + "/meanVar.dat")));
		for (Entry<Integer, Variance> e : frames.entrySet()) {
			Variance v2 = frames2.get(e.getKey());
			System.out.println(e.getKey() + " " + e.getValue().getMean() + " " + e.getValue().getVar());
			bw.append(e.getKey() + " " + e.getValue().getMean() + " " + e.getValue().getVar() + " " + v2.getMean() + " " + v2.getVar() + "\n");
		}
		bw.close();
	}

}
