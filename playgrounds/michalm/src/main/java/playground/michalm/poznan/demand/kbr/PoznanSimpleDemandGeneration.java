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

package playground.michalm.poznan.demand.kbr;

import java.util.Map;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.contrib.zone.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrix;

import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.util.array2d.Array2DReader;
import playground.michalm.util.matrices.MatrixUtils;

public class PoznanSimpleDemandGeneration {
	public void generate(String inputDir, String plansFile, String transportMode) {
		String networkFile = inputDir + "Matsim_2013_06/network-cleaned-extra-lanes.xml";
		String zonesXmlFile = inputDir + "Matsim_2013_06/zones.xml";
		String zonesShpFile = inputDir + "Osm_2013_06/zones.SHP";

		String odMatrixFilePrefix = inputDir + "Visum_2012/Total_PrT_Veh_odMatrices/Total_PrT_Veh_";

		int randomSeed = RandomUtils.DEFAULT_SEED;
		RandomUtils.reset(randomSeed);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		Map<Id<Zone>, Zone> zones = Zones.readZones(zonesXmlFile, zonesShpFile);

		ODDemandGenerator dg = new ODDemandGenerator(scenario, zones, false);

		for (int i = 0; i < 24; i++) {
			String odMatrixFile = odMatrixFilePrefix + i + "-" + (i + 1) + ".gz";
			System.out.println("Generation for " + odMatrixFile);

			double[][] odMatrixFromFile = Array2DReader.getDoubleArray(odMatrixFile, zones.size());
			Matrix odMatrix = MatrixUtils.createSparseMatrix("m" + i, zones.keySet(), odMatrixFromFile);

			dg.generateSinglePeriod(odMatrix, "dummy", "dummy", transportMode, i * 3600, 3600, 1);
		}

		dg.write(plansFile);
	}

	public static void main(String[] args) {
		String inputDir = "d:/GoogleDrive/Poznan/";
		String plansFile = "d:/PP-rad/poznan/test/plans.xml.gz";
		String transportMode = TransportMode.car;
		new PoznanSimpleDemandGeneration().generate(inputDir, plansFile, transportMode);
	}
}
