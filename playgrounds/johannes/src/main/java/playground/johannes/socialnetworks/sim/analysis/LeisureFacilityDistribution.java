/* *********************************************************************** *
 * project: org.matsim.*
 * LeisureFacilityDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.util.TXTWriter;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;

/**
 * @author illenberger
 * 
 */
public class LeisureFacilityDistribution implements PlansAnalyzerTask {

	private static final Logger logger = Logger.getLogger(LeisureFacilityDistribution.class);

	// private Set<Facility> leisureFacilities;

	private Quadtree quadTree;

	private final String actType;

	private Discretizer discretizer = new LinearDiscretizer(1000.0);

	private String output;

	// private final GeometryFactory factory = new GeometryFactory();

	public LeisureFacilityDistribution(String actType, String facType, String output, ActivityFacilities facilities) {
		this.actType = actType;
		this.output = output;

		quadTree = new Quadtree();
		for (ActivityFacility fac : facilities.getFacilities().values()) {
			if (fac.getActivityOptions().containsKey(facType)) {
				quadTree.insert(new Envelope(new Coordinate(fac.getCoord().getX(), fac.getCoord().getY())), fac);
			}
		}
	}

	@Override
	public void analyze(Set<Plan> plans, Map<String, Double> stats) {
		TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
		int n = 0;
		int N = plans.size();

		for (Plan plan : plans) {
			if (plan.getPlanElements().size() > 2) {
				for (int i = 2; i < plan.getPlanElements().size(); i += 2) {
					Activity act1 = (Activity) plan.getPlanElements().get(i - 2);
					Activity act2 = (Activity) plan.getPlanElements().get(i);

//					if (act2.getType().equalsIgnoreCase(actType)) {
					if(act2.getType().startsWith(actType)) {
						Coord c1 = act1.getCoord();
						double dx = c1.getX() - act2.getCoord().getX();
						double dy = c1.getY() - act2.getCoord().getY();
						double d = Math.sqrt(dx * dx + dy * dy);
						d = discretizer.discretize(d);

						Envelope env = new Envelope(c1.getX() - d, c1.getX() + d, c1.getY() - d, c1.getY() + d);
						List<Facility> result = quadTree.query(env);

						int cnt = 0;
						for (Facility fac : result) {
							dx = c1.getX() - fac.getCoord().getX();
							dy = c1.getY() - fac.getCoord().getY();

							double d2 = Math.sqrt(dx * dx + dy * dy);
							d2 = discretizer.discretize(d2);

							if (d == d2)
								cnt++;
						}

						hist.adjustOrPutValue(d, cnt, cnt);
					}
				}
			}
			n++;
			if (n % 100 == 0)
				logger.info(String.format("Processed %1$s of %2$s plans (%3$.4f).", n, N, n / (double) N));
		}

		try {
			TXTWriter.writeMap(hist, "d", "n", output + "/facilityDistr.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(args[0]);
		MatsimFacilitiesReader facreader = new MatsimFacilitiesReader(scenario);
		facreader.readFile(args[1]);
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile(args[2]);

		

		LeisureFacilityDistribution task = new LeisureFacilityDistribution(
				"l",
				"leisure",
				args[3],
				scenario.getActivityFacilities());
		PlansAnalyzer.analyzeSelectedPlans(scenario.getPopulation(), task);
	}
}
