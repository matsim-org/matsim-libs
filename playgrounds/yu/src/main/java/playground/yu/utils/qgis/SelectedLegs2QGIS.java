/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author yu
 */
public class SelectedLegs2QGIS extends SelectedPlans2ESRIShapeChanged {
	private final static Logger log = Logger.getLogger(SelectedLegs2QGIS.class);
	private final Network network;

	public SelectedLegs2QGIS(Population population, Network network,
			CoordinateReferenceSystem crs, String outputDir) {
		super(population, network, crs, outputDir);
		this.network = network;
	}

	@Override
	protected void writeLegs() throws IOException {
		String outputFile = this.getOutputDir() + "/legs.shp";
		ArrayList<Feature> fts = new ArrayList<Feature>();
		for (PlanImpl plan : this.getOutputSamplePlans()) {
			if (plan.getFirstActivity().getEndTime() == 21600.0) {
				String id = plan.getPerson().getId().toString();
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						double dist = (leg.getRoute() instanceof NetworkRoute ? RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), this.network) : 0.0);
						if (dist > 0) {
							fts.add(getLegFeature(leg, id));
						}
					}
				}
			}

		}
		ShapeFileWriter.writeGeometries(fts, outputFile);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String populationFilename = "../runs_SVN/run674/it.1000/1000.plans.xml.gz";
		final String networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String outputDir = "../runs_SVN/run674/it.1000/1000.analysis/";
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		SelectedPlans2ESRIShapeChanged sp = new SelectedPlans2ESRIShapeChanged(
				population, network, crs, outputDir);
		sp.setOutputSample(1);
		sp.setActBlurFactor(100);
		sp.setLegBlurFactor(100);
		sp.setWriteActs(false);
		sp.setWriteLegs(true);

		sp.write();
	}

}
