/* *********************************************************************** *
 * project: org.matsim.*
 * Plans2ESRIShape.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
import java.util.List;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.jfree.util.Log;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.routes.CarRoute;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * "Simple class to convert MATSim plans to ESRI shape files. Activities will be
 * converted into points and legs will be converted into line strings.
 * Parameters as defined in the population xml file will be added as attributes
 * to the shape files. There are also some parameters to configure this
 * converter, please consider the corresponding setters in this class."
 * 
 * @author laemmel
 * 
 *         this a copy of
 *         org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape.java
 *         of Mr. Laemmel with some changes.
 */
public class SelectedPlans2ESRIShape {

	protected CoordinateReferenceSystem crs;
	private final Population population;
	private double outputSample = 1;
	private double actBlurFactor = 0;
	private double legBlurFactor = 0;
	protected String outputDir;
	private boolean writeActs = true;
	private boolean writeLegs = true;
	private ArrayList<Plan> outputSamplePlans;
	private FeatureType featureTypeAct;
	private FeatureType featureTypeLeg;
	protected GeometryFactory geofac;

	public SelectedPlans2ESRIShape() {
		crs = null;
		population = null;
		outputDir = null;
		geofac = null;
	}

	public SelectedPlans2ESRIShape(final Population population,
			final CoordinateReferenceSystem crs, final String outputDir) {
		this.population = population;
		this.crs = crs;
		this.outputDir = outputDir;
		this.geofac = new GeometryFactory();
		initFeatureType();
	}

	public void setOutputSample(final double sample) {
		this.outputSample = sample;
	}

	public void setWriteActs(final boolean writeActs) {
		this.writeActs = writeActs;
	}

	public void setWriteLegs(final boolean writeLegs) {
		this.writeLegs = writeLegs;
	}

	public void setActBlurFactor(final double actBlurFactor) {
		this.actBlurFactor = actBlurFactor;
	}

	public void setLegBlurFactor(final double legBlurFactor) {
		this.legBlurFactor = legBlurFactor;
	}

	public void write() throws IOException {
		drawOutputSample();
		if (this.writeActs) {
			writeActs();
		}
		if (this.writeLegs) {
			writeLegs();
		}
	}

	private void drawOutputSample() {
		this.setOutputSamplePlans(new ArrayList<Plan>());
		for (Person pers : this.population.getPersons().values()) {
			if (MatsimRandom.random.nextDouble() <= this.outputSample) {
				this.getOutputSamplePlans().add(pers.getSelectedPlan());
			}
		}
	}

	private void writeActs() throws IOException {
		String outputFile = this.getOutputDir() + "/acts.shp";
		ArrayList<Feature> fts = new ArrayList<Feature>();
		for (Plan plan : this.getOutputSamplePlans()) {
			String id = plan.getPerson().getId().toString();
			ActIterator iter = plan.getIteratorAct();
			while (iter.hasNext()) {
				Act act = (Act) iter.next();
				fts.add(getActFeature(id, act));
			}
		}

		ShapeFileWriter.writeGeometries(fts, outputFile);
	}

	protected void writeLegs() throws IOException {
		String outputFile = this.getOutputDir() + "/legs.shp";
		ArrayList<Feature> fts = new ArrayList<Feature>();
		for (Plan plan : this.getOutputSamplePlans()) {
			String id = plan.getPerson().getId().toString();
			LegIterator iter = plan.getIteratorLeg();
			while (iter.hasNext()) {
				Leg leg = (Leg) iter.next();
				if (leg.getRoute().getDist() > 0) {
					fts.add(getLegFeature(leg, id));
				}
			}
		}
		ShapeFileWriter.writeGeometries(fts, outputFile);
	}

	@SuppressWarnings("deprecation")
	private Feature getActFeature(final String id, final Act act) {
		String type = act.getType();
		String linkId = act.getLinkId().toString();
		Double startTime = act.getStartTime();
		Double dur = act.getDuration();
		Double endTime = act.getEndTime();
		double rx = MatsimRandom.random.nextDouble() * this.actBlurFactor;
		double ry = MatsimRandom.random.nextDouble() * this.actBlurFactor;
		Coord cc = act.getLink().getCenter();
		Coord c = new CoordImpl(cc.getX() + rx, cc.getY() + ry);
		try {
			return this.getFeatureTypeAct().create(
					new Object[] { MGC.coord2Point(c), id, type, linkId,
							startTime, dur, endTime });
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("deprecation")
	protected Feature getLegFeature(final Leg leg, final String id) {
		Integer num = leg.getNum();
		BasicLeg.Mode mode = leg.getMode();
		Double depTime = leg.getDepartureTime();
		Double travTime = leg.getTravelTime();
		Double arrTime = leg.getArrivalTime();
		Double dist = leg.getRoute().getDist();

		List<Link> links = ((CarRoute) leg.getRoute()).getLinks();
		Coordinate[] coords = new Coordinate[links.size() + 1];
		for (int i = 0; i < links.size(); i++) {
			Coord c = links.get(i).getFromNode().getCoord();
			double rx = MatsimRandom.random.nextDouble() * this.legBlurFactor;
			double ry = MatsimRandom.random.nextDouble() * this.legBlurFactor;
			Coordinate cc = new Coordinate(c.getX() + rx, c.getY() + ry);
			coords[i] = cc;
		}

		Coord c = links.get(links.size() - 1).getToNode().getCoord();
		double rx = MatsimRandom.random.nextDouble() * this.legBlurFactor;
		double ry = MatsimRandom.random.nextDouble() * this.legBlurFactor;
		Coordinate cc = new Coordinate(c.getX() + rx, c.getY() + ry);
		coords[links.size()] = cc;

		LineString ls = this.getGeofac().createLineString(coords);

		try {
			return this.getFeatureTypeLeg().create(
					new Object[] { ls, id, num, mode, depTime, travTime,
							arrTime, dist });
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected void initFeatureType() {
		AttributeType[] attrAct = new AttributeType[7];
		attrAct[0] = DefaultAttributeTypeFactory.newAttributeType("Point",
				Point.class, true, null, null, this.getCrs());
		attrAct[1] = AttributeTypeFactory.newAttributeType("PERS_ID",
				String.class);
		attrAct[2] = AttributeTypeFactory
				.newAttributeType("TYPE", String.class);
		attrAct[3] = AttributeTypeFactory.newAttributeType("LINK_ID",
				String.class);
		attrAct[4] = AttributeTypeFactory.newAttributeType("START_TIME",
				Double.class);
		attrAct[5] = AttributeTypeFactory.newAttributeType("DUR", Double.class);
		attrAct[6] = AttributeTypeFactory.newAttributeType("END_TIME",
				Double.class);

		AttributeType[] attrLeg = new AttributeType[8];
		attrLeg[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",
				LineString.class, true, null, null, this.getCrs());
		attrLeg[1] = AttributeTypeFactory.newAttributeType("PERS_ID",
				String.class);
		attrLeg[2] = AttributeTypeFactory
				.newAttributeType("NUM", Integer.class);
		attrLeg[3] = AttributeTypeFactory
				.newAttributeType("MODE", String.class);
		attrLeg[4] = AttributeTypeFactory.newAttributeType("DEP_TIME",
				Double.class);
		attrLeg[5] = AttributeTypeFactory.newAttributeType("TRAV_TIME",
				Double.class);
		attrLeg[6] = AttributeTypeFactory.newAttributeType("ARR_TIME",
				Double.class);
		attrLeg[7] = AttributeTypeFactory
				.newAttributeType("DIST", Double.class);

		try {
			this.setFeatureTypeAct(FeatureTypeBuilder.newFeatureType(attrAct,
					"activity"));
			this.setFeatureTypeLeg(FeatureTypeBuilder.newFeatureType(attrLeg,
					"leg"));
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		// FIXME hard-coded file names; does this class really need a
		// main-method?
		final String populationFilename = "./examples/equil/plans100.xml";
		final String networkFilename = "./examples/equil/network.xml";
		// final String populationFilename =
		// "./test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		// final String networkFilename =
		// "./test/scenarios/berlin/network.xml.gz";

		final String outputDir = "./plans/";

		Gbl.createConfig(null);
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFilename);

		Population population = new Population();
		new MatsimPopulationReader(population, network)
				.readFile(populationFilename);

		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(population,
				crs, outputDir);
		sp.setOutputSample(0.05);
		sp.setActBlurFactor(100);
		sp.setLegBlurFactor(100);
		sp.setWriteActs(true);
		sp.setWriteLegs(true);

		try {
			sp.write();
		} catch (IOException e) {
			Log.error(e.getMessage(), e);
		}
	}

	public GeometryFactory getGeofac() {
		return geofac;
	}

	public void setFeatureTypeLeg(FeatureType featureTypeLeg) {
		this.featureTypeLeg = featureTypeLeg;
	}

	public FeatureType getFeatureTypeLeg() {
		return featureTypeLeg;
	}

	public CoordinateReferenceSystem getCrs() {
		return crs;
	}

	public void setFeatureTypeAct(FeatureType featureTypeAct) {
		this.featureTypeAct = featureTypeAct;
	}

	public FeatureType getFeatureTypeAct() {
		return featureTypeAct;
	}

	public String getOutputDir() {
		return outputDir;
	}

	public void setOutputSamplePlans(ArrayList<Plan> outputSamplePlans) {
		this.outputSamplePlans = outputSamplePlans;
	}

	public ArrayList<Plan> getOutputSamplePlans() {
		return outputSamplePlans;
	}

}
