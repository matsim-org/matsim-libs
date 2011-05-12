/* *********************************************************************** *
 * project: org.matsim.*
 * TransportModeAnalysis.java
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

package playground.mrieser.iatbr;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class TransportModeAnalysis extends AbstractPersonAlgorithm {

	private final BufferedWriter out;

	public TransportModeAnalysis(final BufferedWriter out) {
		this.out = out;
	}

	@Override
	public void run(final Person person) {
		boolean hasCarLeg = false;
		boolean hasTransitLeg = false;
		boolean hasWalkLeg = false;
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				String mode = leg.getMode();
				if (TransportMode.car.equals(mode)) {
					hasCarLeg = true;
				} else if (TransportMode.pt.equals(mode)) {
					hasTransitLeg = true;
				} else if (TransportMode.walk.equals(mode)) {
					hasWalkLeg = true;
				}
			}
		}
		String type;
		if (hasCarLeg) {
			type = "car";
		} else if (hasTransitLeg) {
			type = "transit";
		} else if (hasWalkLeg) {
			type = "walk";
		} else {
			type = "undefined";
		}
		Activity homeAct = (Activity) person.getSelectedPlan().getPlanElements().get(0);
		try {
			this.out.write(homeAct.getCoord().getX() + "\t" +
					homeAct.getCoord().getY() + "\t" +
					person.getId() + "\t" +
					type + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) throws FileNotFoundException, IOException {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Logger log = Logger.getLogger(TransportModeAnalysis.class);
		log.info("reading network");
		new MatsimNetworkReader(scenario).parse("/Volumes/Data/VSP/projects/diss/runs/tr100pct1NoTr/output_network.xml.gz");
		log.info("analyzing plans");
		BufferedWriter infoFile = IOUtils.getBufferedWriter("/Volumes/Data/VSP/projects/diss/runs/tr100pct1NoTr/coords.txt");
		infoFile.write("X\tY\tID\tTYPE\n");
		PopulationImpl pImpl = (PopulationImpl) scenario.getPopulation();
		pImpl.setIsStreaming(true);
		pImpl.addAlgorithm(new TransportModeAnalysis(infoFile));
		new MatsimPopulationReader(scenario).parse("/Volumes/Data/VSP/projects/diss/runs/tr100pct1NoTr/output_plans.xml.gz");
		pImpl.printPlansCount();
		infoFile.close();
		log.info("done");
	}

}
