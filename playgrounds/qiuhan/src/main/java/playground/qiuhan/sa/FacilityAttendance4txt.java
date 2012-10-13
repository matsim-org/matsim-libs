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

/**
 * 
 */
package playground.qiuhan.sa;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.yu.analysis.ActivityAlgorithm;
import playground.yu.utils.qgis.TextLayer4QGIS;

/**
 * @author qiuhan extracts the number of attendance of facilities in simulation
 *         time interval to a .txt file, which can be imported in Quantum GIS.
 *         P.S. the first activity facility in plan is disregarded.
 */
public class FacilityAttendance4txt extends TextLayer4QGIS implements
		ActivityAlgorithm {
	private final Map<Coord, Integer> attendances;

	/**
	 * 
	 */
	public FacilityAttendance4txt(String outputFilename) {
		super(outputFilename);
		writeln("attendance");
		this.attendances = new HashMap<Coord, Integer>();
	}

	@Override
	public void run(Plan plan) {
		int cnt = 0;
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				cnt++;
				if (cnt == 1) {
					continue;
				}
				run((ActivityImpl) planElement);
			}
		}
	}

	@Override
	public void run(ActivityImpl activity) {
		Coord coord = activity.getCoord();
		Coord gridCenter = new CoordImpl((int) (coord.getX() / 1000) * 1000,
				(int) (coord.getY() / 1000) * 1000);
		Integer cnt = this.attendances.get(gridCenter);
		this.attendances.put(gridCenter, cnt == null ? 1 : ++cnt);
	}

	@Override
	public void close() {
		for (Entry<Coord, Integer> entry : this.attendances.entrySet()) {
			Coord coord = entry.getKey();
			writeln(coord.getX() + "\t" + coord.getY() + "\t"
					+ entry.getValue());
		}
		super.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFile = "output/matsimNetwork/network.multimodalCombi2.xml"//

		// , PopulationFile = "input/A_NM/plans.xml.gz"//

		, PopulationFile = "output/population/popRoutedOevModellCombi2_10pct.xml.gz"//

		, outputFileBase = "output/comparison/";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		new MatsimPopulationReader(scenario).readFile(PopulationFile);

		// Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		FacilityAttendance4txt fa4t = new FacilityAttendance4txt(outputFileBase
				+ //
					// "AN_facAttend.log"//
				"QS_facAttend.log"//
		);

		fa4t.run(population);
		fa4t.close();
	}

}
