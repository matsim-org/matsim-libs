/* *********************************************************************** *
 * project: org.matsim.*
 * ModeTextLayer4QGIS.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.utils.qgis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import playground.yu.analysis.PlanModeJudger;

/**
 * @author yu
 * 
 */
public class ModeTextLayer4QGIS extends TextLayer4QGIS {
	/**
	 * dummy constructor, please don't use it.
	 */
	public ModeTextLayer4QGIS() {

	}

	/**
	 * @param textFilename
	 */
	public ModeTextLayer4QGIS(String textFilename) {
		super(textFilename);
		writer.writeln("mode");
	}

	public ModeTextLayer4QGIS(String textFilename, RoadPricingScheme toll) {
		super(textFilename, toll);
		writer.writeln("mode");
	}

	@Override
	public void run(Plan plan) {
		Coord homeLoc = ((PlanImpl) plan).getFirstActivity().getCoord();
		String mode = "";
		if (PlanModeJudger.useCar(plan)) {
			mode = TransportMode.car;
		} else if (PlanModeJudger.usePt(plan)) {
			mode = TransportMode.pt;
		} else if (PlanModeJudger.useWalk(plan)) {
			mode = TransportMode.walk;
		}
		writer.writeln(homeLoc.getX() + "\t" + homeLoc.getY() + "\t" + mode);
	}

	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run684/it.1000/1000.plans.xml.gz";
		final String textFilename = "../runs_SVN/run684/it.1000/1000.analysis/mode_Kanton.txt";
		String tollFilename = "../schweiz-ivtch-SVN/baseCase/roadpricing/KantonZurich.xml";
		// final String netFilename = "../matsimTests/scoringTest/network.xml";
		// final String plansFilename =
		// "../matsimTests/scoringTest/output/ITERS/it.100/100.plans.xml.gz";
		// final String textFilename =
		// "../matsimTests/scoringTest/output/ITERS/it.100/mode.txt";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

//        ConfigUtils.addOrGetModule(scenario.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setUseRoadpricing(true);
        RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(
				(RoadPricingSchemeImpl) scenario.getScenarioElement(RoadPricingScheme.ELEMENT_NAME));
		tollReader.parse(tollFilename);

		ModeTextLayer4QGIS mtl = new ModeTextLayer4QGIS(textFilename,
				(RoadPricingScheme) scenario.getScenarioElement(RoadPricingScheme.ELEMENT_NAME));
		mtl.run(population);
		mtl.close();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
