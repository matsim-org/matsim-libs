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

package playground.mzilske.prognose2025;

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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.RouteUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Simple class to convert MATSim plans to ESRI shape files. Activities will be converted into points and
 * legs will be converted into line strings. Parameters as defined in the population xml file will be added
 * as attributes to the shape files. There are also some parameters to configure this converter, please
 * consider the corresponding setters in this class.
 *
 * @author laemmel
 */
public class SelectedPlans2ESRIShape {

	private final CoordinateReferenceSystem crs;
	private final Population population;
	private double outputSample = 1;
	private double legBlurFactor = 0;
	private final String outputDir;
	private boolean writeActs = true;
	private boolean writeLegs = true;
	private ArrayList<Plan> outputSamplePlans;
	private FeatureType featureTypeAct;
	private FeatureType featureTypeLeg;
	private final GeometryFactory geofac;
	private final Network network;

	public SelectedPlans2ESRIShape(final Population population, final Network network, final CoordinateReferenceSystem crs, final String outputDir) {
		this.population = population;
		this.network = network;
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

	public void setLegBlurFactor(final double legBlurFactor) {
		this.legBlurFactor  = legBlurFactor;
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
		this.outputSamplePlans = new ArrayList<Plan>();
		for (Person pers : this.population.getPersons().values()) {
			if (MatsimRandom.getRandom().nextDouble() <= this.outputSample) {
				this.outputSamplePlans.add(pers.getSelectedPlan());
			}
		}
	}

	private void writeActs() throws IOException {
		String outputFile = this.outputDir + "/acts.shp";
		ArrayList<Feature> fts = new ArrayList<Feature>();
		for (Plan plan : this.outputSamplePlans) {
			String id = plan.getPerson().getId().toString();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					fts.add(getActFeature(id, act));
				}
			}
		}

		ShapeFileWriter.writeGeometries(fts, outputFile);
	}

	private void writeLegs() throws IOException {
		String outputFile = this.outputDir + "/legs.shp";
		ArrayList<Feature> fts = new ArrayList<Feature>();
		for (Plan plan : this.outputSamplePlans) {
			String id = plan.getPerson().getId().toString();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getRoute() instanceof NetworkRoute) {
						if (RouteUtils.calcDistance((NetworkRoute) leg.getRoute(), this.network) > 0) {
							fts.add(getLegFeature(leg, id));
						}
					} else if (leg.getRoute().getDistance() > 0) {
						fts.add(getLegFeature(leg, id));
					}
				}
			}
		}
		ShapeFileWriter.writeGeometries(fts, outputFile);
	}

	private Feature getActFeature(final String id, final Activity act) {
		String type = act.getType();
		Coord cc = act.getCoord();
		Coord c = new CoordImpl(cc.getX(), cc.getY());
		try {
			return this.featureTypeAct.create(new Object [] {MGC.coord2Point(c),id, type});
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}

		return null;
	}

	private Feature getLegFeature(final Leg leg, final String id) {
		if (!(leg.getRoute() instanceof NetworkRoute)) {
			return null;
		}
		String mode = leg.getMode();

		List<Id> linkIds = ((NetworkRoute) leg.getRoute()).getLinkIds();
		Coordinate [] coords = new Coordinate[linkIds.size() + 1];
		for (int i = 0; i < linkIds.size(); i++) {
			Link link = this.network.getLinks().get(linkIds.get(i));
			Coord c = link.getFromNode().getCoord();
			double rx = MatsimRandom.getRandom().nextDouble() * this.legBlurFactor;
			double ry = MatsimRandom.getRandom().nextDouble() * this.legBlurFactor;
			Coordinate cc = new Coordinate(c.getX()+rx,c.getY()+ry);
			coords[i] = cc;
		}

		Link link = this.network.getLinks().get(linkIds.get(linkIds.size() - 1));
		Coord c = link.getToNode().getCoord();
		double rx = MatsimRandom.getRandom().nextDouble() * this.legBlurFactor;
		double ry = MatsimRandom.getRandom().nextDouble() * this.legBlurFactor;
		Coordinate cc = new Coordinate(c.getX()+rx,c.getY()+ry);
		coords[linkIds.size()] = cc;

		LineString ls = this.geofac.createLineString(coords);

		try {
			return this.featureTypeLeg.create(new Object[] {ls,id,mode});
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}

		return null;
	}


	private void initFeatureType() {
		AttributeType[] attrAct = new AttributeType[3];
		attrAct[0] = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, this.crs);
		attrAct[1] = AttributeTypeFactory.newAttributeType("PERS_ID", String.class);
		attrAct[2] = AttributeTypeFactory.newAttributeType("TYPE", String.class);

		AttributeType[] attrLeg = new AttributeType[3];
		attrLeg[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, this.crs);
		attrLeg[1] = AttributeTypeFactory.newAttributeType("PERS_ID", String.class);
		attrLeg[2] = AttributeTypeFactory.newAttributeType("MODE", String.class);

		try {
			this.featureTypeAct = FeatureTypeBuilder.newFeatureType(attrAct, "activity");
			this.featureTypeLeg = FeatureTypeBuilder.newFeatureType(attrLeg, "leg");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String [] args) {
		// FIXME hard-coded file names; does this class really need a main-method?
		final String populationFilename = "../prognose_2025/demand/population_pv_1pct.xml";
		final String networkFilename = "../prognose_2025/demand/network_ab_wgs84.xml";
//		final String populationFilename = "./test/scenarios/berlin/plans_hwh_1pct.xml.gz";
//		final String networkFilename = "./test/scenarios/berlin/network.xml.gz";

		final String outputDir = "../playgrounds/mzilske/output";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		CoordinateReferenceSystem crs = MGC.getCRS("WGS84");
		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(scenario.getPopulation(), scenario.getNetwork(), crs, outputDir);
		sp.setOutputSample(0.01);
		sp.setLegBlurFactor(100);
		sp.setWriteActs(true);
		sp.setWriteLegs(false);

		try {
			sp.write();
		} catch (IOException e) {
			Log.error(e.getMessage(), e);
		}
	}

}

