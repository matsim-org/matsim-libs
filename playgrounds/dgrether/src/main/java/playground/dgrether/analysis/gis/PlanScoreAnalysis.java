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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.referencing.factory.GeotoolsFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

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

	private AttributeType geom;

	private AttributeType id;

	private AttributeType score;

	private AttributeType[] attType;

	private FeatureType ftHome;

	private final GeometryFactory geofac = new GeometryFactory();

	private long counter = 0;
	private long nextMsg = 1;

	private void createOGCTypes() {
		CRSFactory fac = new GeotoolsFactory();
		// this.coordinateReferenceSystem = fac.;

		this.geom = DefaultAttributeTypeFactory.newAttributeType("Point",
				Point.class, true, null, null, this.coordinateReferenceSystem);

		// this.geom =
		// DefaultAttributeTypeFactory.newAttributeType("MultiPolygon",MultiPolygon.class,
		// true, null, null, this.coordinateReferenceSystem);
		this.id = AttributeTypeFactory.newAttributeType("PersonID", String.class);
		this.score = AttributeTypeFactory.newAttributeType("Score", Double.class);

		this.attType = new AttributeType[] { this.geom, this.id, this.score };
		try {
			this.ftHome = FeatureTypeFactory.newFeatureType(this.attType, "ftHome");

		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}

	private Feature createHomeFeature(final double score, final Coord loc, final Id id) {
		Coordinate coord = new Coordinate(loc.getX(), loc.getY());
		CoordinateSequence coords = new CoordinateArraySequence(
				new Coordinate[] { coord });
		Point point = new Point(coords, this.geofac);
		try {
			return this.ftHome.create(new Object[] { point, id.toString(), score });
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void doAnalysis() {
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer net = scenario.getNetwork();
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(NETWORK);
//		Gbl.getWorld().setNetworkLayer(net);
//		Gbl.getWorld().complete();

		ScenarioImpl scenario1 = new ScenarioImpl();
		scenario1.setNetwork(net);
		Population plans = scenario1.getPopulation();
		new MatsimPopulationReader(scenario1).readFile(PLANS);


		ScenarioImpl scenario2 = new ScenarioImpl();
		scenario2.setNetwork(net);
		Population plans2 = scenario2.getPopulation();
		new MatsimPopulationReader(scenario2).readFile(PLANS2);


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
				loc = ((ActivityImpl) plan.getPlanElements().get(0)).getCoord();
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

	private void writeShapeFile(final Collection<Feature> features, final String filename)
			throws IOException, FactoryException, SchemaException {
		URL fileURL = (new File(filename)).toURL();
		ShapefileDataStore datastore = new ShapefileDataStore(fileURL);
		Feature feature = features.iterator().next();
		datastore.createSchema(feature.getFeatureType());

		FeatureStore featureStore = (FeatureStore) (datastore
				.getFeatureSource(feature.getFeatureType().getTypeName()));
		FeatureReader aReader = DataUtilities.reader(features);

		featureStore.addFeatures(aReader);

	}

	public static void main(final String[] args) {
		PlanScoreAnalysis ana = new PlanScoreAnalysis();
		ana.doAnalysis();
	}
}
