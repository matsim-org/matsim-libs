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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.gis.ShapeFileWriter;
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
 *         this a changed copy of
 *         org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape.java
 *         of Mr. Laemmel with some changes.
 */
public class SelectedPlans2ESRIShapeChanged extends
		org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape {

	protected CoordinateReferenceSystem crs;
	// private final Population population = null;
	// private double outputSample = 1;
	// private double actBlurFactor = 0;
	private double legBlurFactor = 0;
	protected String outputDir;
	private ArrayList<PlanImpl> outputSamplePlans;
	private FeatureType featureTypeAct;
	private FeatureType featureTypeLeg;
	protected GeometryFactory geofac;

	//
	// public SelectedPlans2ESRIShape() {
	// this.crs = null;
	// this.population = null;
	// this.outputDir = null;
	// this.geofac = null;
	// }

	public SelectedPlans2ESRIShapeChanged(PopulationImpl population, Network network,
			CoordinateReferenceSystem crs, String outputDir) {
		super(population, network, crs, outputDir);
		this.outputDir = outputDir;
		this.geofac = new GeometryFactory();
		initFeatureType();
	}

	// private void drawOutputSample() {
	// this.setOutputSamplePlans(new ArrayList<Plan>());
	// for (Person pers : this.population.getPersons().values()) {
	// if (MatsimRandom.random.nextDouble() <= this.outputSample) {
	// this.getOutputSamplePlans().add(pers.getSelectedPlan());
	// }
	// }
	// }
	//
	// private void writeActs() throws IOException {
	// String outputFile = this.getOutputDir() + "/acts.shp";
	// ArrayList<Feature> fts = new ArrayList<Feature>();
	// for (Plan plan : this.getOutputSamplePlans()) {
	// String id = plan.getPerson().getId().toString();
	// ActIterator iter = plan.getIteratorAct();
	// while (iter.hasNext()) {
	// Act act = (Act) iter.next();
	// fts.add(getActFeature(id, act));
	// }
	// }
	//
	// ShapeFileWriter.writeGeometries(fts, outputFile);
	// }

	protected void writeLegs() throws IOException {
		String outputFile = this.getOutputDir() + "/legs.shp";
		ArrayList<Feature> fts = new ArrayList<Feature>();
		for (PlanImpl plan : this.getOutputSamplePlans()) {
			String id = plan.getPerson().getId().toString();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof LegImpl) {
					LegImpl leg = (LegImpl) pe;
					if (leg.getRoute().getDistance() > 0) {
						fts.add(getLegFeature(leg, id));
					}
				}
			}
		}
		ShapeFileWriter.writeGeometries(fts, outputFile);
	}

	// private Feature getActFeature(final String id, final Act act) {
	// String type = act.getType();
	// String linkId = act.getLinkId().toString();
	// Double startTime = act.getStartTime();
	// Double dur = act.getDuration();
	// Double endTime = act.getEndTime();
	// double rx = MatsimRandom.random.nextDouble() * this.actBlurFactor;
	// double ry = MatsimRandom.random.nextDouble() * this.actBlurFactor;
	// Coord cc = act.getLink().getCenter();
	// Coord c = new CoordImpl(cc.getX() + rx, cc.getY() + ry);
	// try {
	// return this.getFeatureTypeAct().create(
	// new Object[] { MGC.coord2Point(c), id, type, linkId,
	// startTime, dur, endTime });
	// } catch (IllegalAttributeException e) {
	// e.printStackTrace();
	// }
	//
	// return null;
	// }

	protected Feature getLegFeature(final LegImpl leg, final String id) {
		TransportMode mode = leg.getMode();
		Double depTime = leg.getDepartureTime();
		Double travTime = leg.getTravelTime();
		Double arrTime = leg.getArrivalTime();
		Double dist = leg.getRoute().getDistance();

		List<Link> links = ((NetworkRouteWRefs) leg.getRoute()).getLinks();
		Coordinate[] coords = new Coordinate[links.size() + 1];
		for (int i = 0; i < links.size(); i++) {
			Coord c = links.get(i).getFromNode().getCoord();
			double rx = MatsimRandom.getRandom().nextDouble()
					* this.legBlurFactor;
			double ry = MatsimRandom.getRandom().nextDouble()
					* this.legBlurFactor;
			Coordinate cc = new Coordinate(c.getX() + rx, c.getY() + ry);
			coords[i] = cc;
		}

		Coord c = links.get(links.size() - 1).getToNode().getCoord();
		double rx = MatsimRandom.getRandom().nextDouble() * this.legBlurFactor;
		double ry = MatsimRandom.getRandom().nextDouble() * this.legBlurFactor;
		Coordinate cc = new Coordinate(c.getX() + rx, c.getY() + ry);
		coords[links.size()] = cc;

		LineString ls = this.getGeofac().createLineString(coords);

		try {
			return this.getFeatureTypeLeg().create(
					new Object[] { ls, id, mode, depTime, travTime, arrTime,
							dist });
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

		AttributeType[] attrLeg = new AttributeType[7];
		attrLeg[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",
				LineString.class, true, null, null, this.getCrs());
		attrLeg[1] = AttributeTypeFactory.newAttributeType("PERS_ID",
				String.class);
		attrLeg[2] = AttributeTypeFactory
				.newAttributeType("MODE", String.class);
		attrLeg[3] = AttributeTypeFactory.newAttributeType("DEP_TIME",
				Double.class);
		attrLeg[4] = AttributeTypeFactory.newAttributeType("TRAV_TIME",
				Double.class);
		attrLeg[5] = AttributeTypeFactory.newAttributeType("ARR_TIME",
				Double.class);
		attrLeg[6] = AttributeTypeFactory
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

	public GeometryFactory getGeofac() {
		return this.geofac;
	}

	public void setFeatureTypeLeg(final FeatureType featureTypeLeg) {
		this.featureTypeLeg = featureTypeLeg;
	}

	public FeatureType getFeatureTypeLeg() {
		return this.featureTypeLeg;
	}

	public CoordinateReferenceSystem getCrs() {
		return this.crs;
	}

	public void setFeatureTypeAct(final FeatureType featureTypeAct) {
		this.featureTypeAct = featureTypeAct;
	}

	public FeatureType getFeatureTypeAct() {
		return this.featureTypeAct;
	}

	public String getOutputDir() {
		return this.outputDir;
	}

	public void setOutputSamplePlans(final ArrayList<PlanImpl> outputSamplePlans) {
		this.outputSamplePlans = outputSamplePlans;
	}

	public ArrayList<PlanImpl> getOutputSamplePlans() {
		return this.outputSamplePlans;
	}

}
