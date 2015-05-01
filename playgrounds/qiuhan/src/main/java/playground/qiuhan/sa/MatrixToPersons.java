/* *********************************************************************** *
 * project: org.matsim.*
 * MatrixToPersons.java
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

package playground.qiuhan.sa;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class MatrixToPersons {

	private final Matrix m;
	private final Map<Id, Person> persons;
	private Map<String, Coord> zoneIdCoords = null;
	private Map<String, SimpleFeature> zoneIdFeatures = null;
	private static String DUMMY = "dummy";
	private final Random random;
	private Set<String> legModes = null;
	private final NetworkImpl network;

	/**
	 * @param m
	 *            a Matrix in one time interval (e.g. hour)
	 * @param zoneIdCoords
	 */
	private MatrixToPersons(Matrix m, Map<String, Coord> zoneIdCoords,
			NetworkImpl network) {
		this.m = m;
		persons = new HashMap<Id, Person>();
		this.zoneIdCoords = zoneIdCoords;
		random = MatsimRandom.getRandom();
		this.network = network;
	}

	/**
	 * @param m
	 *            a Matrix in one time interval (e.g. hour)
	 * @param zoneIdCoords
	 * @param legModes
	 *            if legModes==null, the mode of legs will be automatically set
	 *            to "pseudo-pt"
	 */
	public MatrixToPersons(Matrix m, Map<String, Coord> zoneIdCoords,
			NetworkImpl network, Set<String> legModes) {

		this(m, zoneIdCoords, network);
		this.legModes = legModes;

	}

	/**
	 * @param smallM
	 * @param fts
	 * @param network2
	 * @param legModes2
	 */
	public MatrixToPersons(Matrix m, SimpleFeatureSource fts, NetworkImpl network,
			Set<String> legModes) {
		this(m, null, network);
		zoneIdFeatures = new HashMap<String, SimpleFeature>();

		// Iterator to iterate over the features from the shape file
		try {
			SimpleFeatureIterator it = fts.getFeatures().features();
			while (it.hasNext()) {
				SimpleFeature ft = it.next();
				String zoneId = ft.getAttribute("NO").toString();
				zoneIdFeatures.put(zoneId, ft);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.legModes = legModes;
	}

	public Map<Id, Person> createPersons() {
		for (String fromZoneId : m.getFromLocations().keySet()) {
			if (zoneIdCoords == null)/* zoneIdFeatures!=0 */{
				SimpleFeature fromZoneFeature = zoneIdFeatures.get(fromZoneId.toString());

				for (Entry entry : m.getFromLocEntries(fromZoneId)) {
					String toZoneId = entry.getToLocation();
					SimpleFeature toZoneFeature = zoneIdFeatures.get(toZoneId.toString());
					int numberPersons = (int) entry.getValue();
					for (int i = 0; i < numberPersons; i++) {
						Id<Person> personId = Id.create(m.getId() + "-" + fromZoneId
								+ "-" + toZoneId + "-" + i, Person.class);
						Coord fromCoord = this.getRandomPointInFeature(fromZoneFeature);
						Coord toCoord = this
								.getRandomPointInFeature(toZoneFeature);
						createPerson(personId, fromCoord, toCoord);
					}
				}
			} else {

				Coord fromZone = zoneIdCoords.get(fromZoneId.toString());

				for (Entry entry : m.getFromLocEntries(fromZoneId)) {
					String toZoneId = entry.getToLocation();
					Coord toZone = zoneIdCoords.get(toZoneId.toString());

					int numberPersons = (int) entry.getValue();

					for (int i = 0; i < numberPersons; i++) {
						Id<Person> personId = Id.create(m.getId() + "-" + fromZoneId
								+ "-" + toZoneId + "-" + i, Person.class);
						createPerson(personId, fromZone, toZone);
					}
				}

			}
		}
		return persons;
	}

	private Coord getRandomPointInFeature(SimpleFeature ft) {
		Point p = null;
		double x, y;
		do {
			x = ft.getBounds().getMinX() + random.nextDouble()
					* (ft.getBounds().getMaxX() - ft.getBounds().getMinX());
			y = ft.getBounds().getMinY() + random.nextDouble()
					* (ft.getBounds().getMaxY() - ft.getBounds().getMinY());
			p = MGC.xy2Point(x, y);
		} while (((Geometry) ft.getDefaultGeometry()).contains(p));
		return new CoordImpl(x, y);
	}

	/**
	 * @param personId
	 * @param from
	 * @param to
	 */
	private void createPerson(Id<Person> personId, Coord from, Coord to) {
		Person per = new PersonImpl(personId);

		createPlans(per, from, to);

		persons.put(personId, per);
	}

	private void createPlans(Person per, Coord from, Coord to) {
		if (legModes != null && !legModes.isEmpty()) {
			for (String legMode : legModes) {
				per.addPlan(createPlan(legMode, from, to));
			}

		} else {
			per.addPlan(createPlan(TransportMode.car, from, to));
			// TODO with pseudo pt or not?
			// per.addPlan(createPlan(TransportMode.pt, from, to));
		}
	}

	private Plan createPlan(String legMode, Coord from, Coord to) {
		Plan plan = new PlanImpl();
		((PlanImpl) plan).setType(legMode);

		Activity firstAct = new ActivityImpl(DUMMY, from,
				XY2NearestPassableLink.getNearestPassableLink(from, network)
						.getId());
		int time = Integer.parseInt(m.getId());

		double endTime = (time - 1) * 3600 + random.nextDouble() * 3600d;
		firstAct.setEndTime(endTime);

		plan.addActivity(firstAct);

		Leg leg = new LegImpl(legMode);
		plan.addLeg(leg);

		Activity lastAct = new ActivityImpl(DUMMY, to, XY2NearestPassableLink
				.getNearestPassableLink(to, network).getId());
		plan.addActivity(lastAct);

		return plan;
	}
}
