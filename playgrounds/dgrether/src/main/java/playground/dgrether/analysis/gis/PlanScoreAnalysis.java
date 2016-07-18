/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether.analysis.gis;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author dgrether
 *
 */
public class PlanScoreAnalysis {

	 private static final String NETWORK =
	 "/Volumes/data/work/cvsRep/vsp-cvs/studies/schweiz-ivtch/network/ivtch.xml";
	 //beta_travel_pt -3
	 private static final String PLANS2 =
	 "/Volumes/data/work/cvsRep/vsp-cvs/runs/run272/100.plans.xml.gz";

	 //beta_travel_pt -6
	 private static final String PLANS =
		 "/Volumes/data/work/cvsRep/vsp-cvs/runs/run271/100.plans.xml.gz";

//	private static final String NETWORK = "./examples/equil/network.xml";

//	private static final String PLANS = "./examples/equil/plans100.xml";

//	private static final String PLANS2 = "./examples/equil/plans100.xml";

	private static final String OUTFILE = "./output/test.shp";

	private static final String OUTFILETXT = "./output/compare.txt";

	private CoordinateReferenceSystem coordinateReferenceSystem;

	private PointFeatureFactory factory = null; 

	private long counter = 0;
	private long nextMsg = 1;

	private void createOGCTypes() {
		this.factory = new PointFeatureFactory.Builder().
				setCrs(this.coordinateReferenceSystem).
				setName("ftHome").
				addAttribute("PersonID", String.class).
				addAttribute("Score", Double.class).
				create();
	}

	private SimpleFeature createHomeFeature(final double score, final Coord loc, final Id id) {
		return this.factory.createPoint(loc, new Object[] {id.toString(), score}, id.toString());
	}

	private void doAnalysis() {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = scenario.getNetwork();
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile(NETWORK);
//		Gbl.getWorld().setNetworkLayer(net);
//		Gbl.getWorld().complete();

		Scenario scenario1 = new ScenarioBuilder( ConfigUtils.createConfig() ).setNetwork(net).build() ;
		Population plans = scenario1.getPopulation();
		new PopulationReader(scenario1).readFile(PLANS);

		Scenario scenario2 = new ScenarioBuilder( ConfigUtils.createConfig() ).setNetwork(net).build() ;
		Population plans2 = scenario2.getPopulation();
		new PopulationReader(scenario2).readFile(PLANS2);


		Logger.getLogger(PlanScoreAnalysis.class).info("plans read");
		try {
			PlanScoreAnalysisTableWriter writer = new PlanScoreAnalysisTableWriter(
					OUTFILETXT);

			this.createOGCTypes();

//			Collection<Feature> features = new ArrayList<Feature>(plans.getPersons()
//					.size());

			Plan plan;
			double score1, score2;
			Coord loc;
			Id id;
			for (Person person : plans.getPersons().values()) {
				plan = person.getSelectedPlan();
				id = person.getId();
				score1 = plan.getScore().doubleValue();
				score2 = plans2.getPersons().get(id).getSelectedPlan().getScore().doubleValue();
				loc = ((Activity) plan.getPlanElements().get(0)).getCoord();
				writer.addLine(id.toString(), Double.toString(loc.getX()), Double
						.toString(loc.getY()), Double.toString(score1), Double
						.toString(score2));
				this.counter++;
//				features.add(this.createHomeFeature(score1, loc, id));
				if (this.counter % this.nextMsg == 0) {
					this.nextMsg *= 2;
					Logger.getLogger(PlanScoreAnalysis.class).info("plans: " + this.counter);
				}
			}
			writer.close();
//			this.writeShapeFile(features, OUTFILE);
		} catch (IOException e) {
			e.printStackTrace();
		} /*catch (FactoryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}*/

		Logger.getLogger(PlanScoreAnalysis.class).info(
				"completed analysis of plans");
	}

	private void writeShapeFile(final Collection<SimpleFeature> features, final String filename)
			throws IOException, FactoryException, SchemaException {
		ShapeFileWriter.writeGeometries(features, filename);
	}

	public static void main(final String[] args) {
		PlanScoreAnalysis ana = new PlanScoreAnalysis();
		ana.doAnalysis();
	}
}
