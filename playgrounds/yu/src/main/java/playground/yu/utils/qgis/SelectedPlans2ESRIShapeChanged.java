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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

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
	private double legBlurFactor = 0;
	protected String outputDir;
	private ArrayList<PlanImpl> outputSamplePlans;
	private PointFeatureFactory actFactory;
	private PolylineFeatureFactory legFactory;
	protected GeometryFactory geofac;
	private Network network;

	public SelectedPlans2ESRIShapeChanged(Population population, Network network,
			CoordinateReferenceSystem crs, String outputDir) {
		super(population, network, crs, outputDir);
		this.outputDir = outputDir;
		this.geofac = new GeometryFactory();
		this.network = network;
		initFeatureType();
	}

	protected void writeLegs() throws IOException {
		String outputFile = this.getOutputDir() + "/legs.shp";
		ArrayList<SimpleFeature> fts = new ArrayList<SimpleFeature>();
		for (PlanImpl plan : this.getOutputSamplePlans()) {
			String id = plan.getPerson().getId().toString();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getRoute().getDistance() > 0) {
						fts.add(getLegFeature(leg, id));
					}
				}
			}
		}
		ShapeFileWriter.writeGeometries(fts, outputFile);
	}

	protected SimpleFeature getLegFeature(final Leg leg, final String id) {
		String mode = leg.getMode();
		Double depTime = leg.getDepartureTime();
		Double travTime = leg.getTravelTime();
		Double dist = leg.getRoute().getDistance();

		List<Id<Link>> linkIds = ((NetworkRoute) leg.getRoute()).getLinkIds();
		Coordinate[] coords = new Coordinate[linkIds.size() + 1];
		for (int i = 0; i < linkIds.size(); i++) {
			Link link = this.network.getLinks().get(linkIds.get(i));
			Coord c = link.getFromNode().getCoord();
			double rx = MatsimRandom.getRandom().nextDouble()
					* this.legBlurFactor;
			double ry = MatsimRandom.getRandom().nextDouble()
					* this.legBlurFactor;
			Coordinate cc = new Coordinate(c.getX() + rx, c.getY() + ry);
			coords[i] = cc;
		}

		Coord c = this.network.getLinks().get(linkIds.get(linkIds.size() - 1)).getToNode().getCoord();
		double rx = MatsimRandom.getRandom().nextDouble() * this.legBlurFactor;
		double ry = MatsimRandom.getRandom().nextDouble() * this.legBlurFactor;
		Coordinate cc = new Coordinate(c.getX() + rx, c.getY() + ry);
		coords[linkIds.size()] = cc;

		LineString ls = this.getGeofac().createLineString(coords);

		return this.legFactory.createPolyline(ls, new Object[] { id, mode, depTime, travTime, dist }, null);
	}

	protected void initFeatureType() {
		this.actFactory = new PointFeatureFactory.Builder().
			setCrs(this.getCrs()).
			setName("activities").
			addAttribute("PERS_ID", String.class).
			addAttribute("TYPE", String.class).
			addAttribute("LINK_ID", String.class).
			addAttribute("START_TIME", Double.class).
			addAttribute("END_TIME", Double.class).
			create();
		
		this.legFactory = new PolylineFeatureFactory.Builder().
			setCrs(this.getCrs()).
			setName("legs").
			addAttribute("PERS_ID", String.class).
			addAttribute("MODE", String.class).
			addAttribute("DEP_TIME", Double.class).
			addAttribute("TRAV_TIME", Double.class).
			addAttribute("DIST", Double.class).
			create();

	}

	public GeometryFactory getGeofac() {
		return this.geofac;
	}

	public CoordinateReferenceSystem getCrs() {
		return this.crs;
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
