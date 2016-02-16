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

package playground.dgrether.koehlerstrehlersignal.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
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
 * @author dgrether removed many "features" of original class  by laemmel 
 */
public class DgSelectedPlans2ESRIShape {
	private final CoordinateReferenceSystem crs;
	private final String outputDir;
	private boolean writeActs = true;
	private boolean writeLegs = true;
	private SimpleFeatureBuilder actBuilder;
	private SimpleFeatureBuilder legBuilder;
	private final GeometryFactory geofac;
	private final Network network;
	private Population population;

	public DgSelectedPlans2ESRIShape(final Population population, final Network network, final CoordinateReferenceSystem crs, final String outputDir) {
		this.population = population;
		this.network = network;
		this.crs = crs;
		this.outputDir = outputDir;
		this.geofac = new GeometryFactory();
		initFeatureType();
	}

	public void setWriteActs(final boolean writeActs) {
		this.writeActs = writeActs;
	}

	public void setWriteLegs(final boolean writeLegs) {
		this.writeLegs = writeLegs;
	}


	public void writeActs(String prefix) {
		String outputFile = this.outputDir + "/" + prefix + "acts.shp";
		ArrayList<SimpleFeature> fts = new ArrayList<SimpleFeature>();
		for (Person p : this.population.getPersons().values()) {
				Plan plan = p.getSelectedPlan();
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

	public void writeLegs(String prefix) throws IOException {
		String outputFile = this.outputDir + "/" + prefix + "legs.shp";
		ArrayList<SimpleFeature> fts = new ArrayList<SimpleFeature>();
		for (Person p : this.population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			String id = plan.getPerson().getId().toString();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getRoute() instanceof NetworkRoute) {
						if (RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) leg.getRoute(), this.network) > 0) {
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

	private SimpleFeature getActFeature(final String id, final Activity act) {
		String type = act.getType();
		String linkId = act.getLinkId().toString();
		Double startTime = act.getStartTime();
		Double endTime = act.getEndTime();
		try {
			return this.actBuilder.buildFeature(null, new Object [] {MGC.coord2Point(act.getCoord()), id, type, linkId, startTime, endTime});
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		return null;
	}

	private SimpleFeature getLegFeature(final Leg leg, final String id) {
		if (!(leg.getRoute() instanceof NetworkRoute)) {
			return null;
		}
		String mode = leg.getMode();
		Double depTime = leg.getDepartureTime();
		Double travTime = leg.getTravelTime();
		Double dist = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) leg.getRoute(), this.network);

		List<Id<Link>> linkIds = ((NetworkRoute) leg.getRoute()).getLinkIds();
		Coordinate [] coords = new Coordinate[linkIds.size() + 1];
		for (int i = 0; i < linkIds.size(); i++) {
			Link link = this.network.getLinks().get(linkIds.get(i));
			Coord c = link.getFromNode().getCoord();
			Coordinate cc = new Coordinate(c.getX() ,c.getY());
			coords[i] = cc;
		}

		Link link = this.network.getLinks().get(linkIds.get(linkIds.size() - 1));
		Coord c = link.getToNode().getCoord();
		Coordinate cc = new Coordinate(c.getX(),c.getY());
		coords[linkIds.size()] = cc;

		LineString ls = this.geofac.createLineString(coords);

		try {
			return this.legBuilder.buildFeature(null, new Object [] {ls,id,mode,depTime,travTime,dist});
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		return null;
	}


	private void initFeatureType() {
		SimpleFeatureTypeBuilder actBuilder = new SimpleFeatureTypeBuilder();
		actBuilder.setName("activity");
		actBuilder.setCRS(this.crs);
		actBuilder.add("location", Point.class);
		actBuilder.add("PERS_ID", String.class);
		actBuilder.add("TYPE", String.class);
		actBuilder.add("LINK_ID", String.class);
		actBuilder.add("START_TIME", Double.class);
		actBuilder.add("END_TIME", Double.class);
		
		SimpleFeatureTypeBuilder legBuilder = new SimpleFeatureTypeBuilder();
		legBuilder.setName("leg");
		legBuilder.setCRS(this.crs);
		legBuilder.add("location", LineString.class);
		legBuilder.add("PERS_ID", String.class);
		legBuilder.add("MODE", String.class);
		legBuilder.add("DEP_TIME", Double.class);
		legBuilder.add("TRAV_TIME", Double.class);
		legBuilder.add("DIST", Double.class);

		this.actBuilder = new SimpleFeatureBuilder(actBuilder.buildFeatureType());
		this.legBuilder = new SimpleFeatureBuilder(legBuilder.buildFeatureType());
	}



}

