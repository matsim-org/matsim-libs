/* *********************************************************************** *
 * project: org.matsim.*
 * CommuterDemandWriter
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
package playground.vsp.demandde.commuterDemandCottbus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author jbischoff
 * @author dgrether
 * 
 */
public class CommuterDemandWriter {

	private static final Logger log = Logger.getLogger(CommuterDemandWriter.class);
	private HashMap<String, SimpleFeature> municipalityMap;
	private List<CommuterDataElement> demand;
	private double scalefactor = 1.0;
	private double workStartTime = 7.0 * 3600.0;
	private double durationWork = 8.5 * 3600.0;
	private Random timeRandom;
	private Random locationRandom;
	private PersonPrepareForSim pp4s;
	private boolean createMutateableActivities = false;
	private Map<String, String> workActivityTypesStartTimeMap = new HashMap<String, String>();
	private CoordinateReferenceSystem targetCrs;
	private Landuse landuse = null;

	public CommuterDemandWriter(Collection<SimpleFeature> gemeindenFeatures,
			CoordinateReferenceSystem featuresCrs, List<CommuterDataElement> demand,
			CoordinateReferenceSystem targetCrs) {
		this.targetCrs = targetCrs;
		this.locationRandom = MatsimRandom.getLocalInstance();
		this.timeRandom = MatsimRandom.getLocalInstance();
		this.demand = demand;
		this.tranformFeaturesAndInitMunicipalityMap(gemeindenFeatures, featuresCrs, targetCrs);
	}

	private void tranformFeaturesAndInitMunicipalityMap(Collection<SimpleFeature> gemeindenFeatures,
			CoordinateReferenceSystem featuresCrs, CoordinateReferenceSystem targetCrs) {
		try {
			MathTransform transformation = CRS.findMathTransform(featuresCrs, targetCrs, true);
			this.municipalityMap = new HashMap<String, SimpleFeature>();
			for (SimpleFeature ft : gemeindenFeatures) {
				String gemeindeId = ft.getAttribute("NR").toString();
				Geometry geometry = JTS.transform((Geometry) ft.getDefaultGeometry(), transformation);
				ft.setDefaultGeometry(geometry);
				this.municipalityMap.put(gemeindeId, ft);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void computeDemand(Scenario scenario) {
		log.error("Watch out! This version of the demand generator turns the whole BA data into car commuters which isn't exactly accurate. Adjusting the CommuterDemandWriter --> Scalefactor to your needs might make sense.");
		generatePopulation(scenario);

		if (this.createMutateableActivities) {
			log.info("");
			log.info("activity types: ");
			for (Entry<String, String> e : this.workActivityTypesStartTimeMap.entrySet()) {
				System.out.println("<param name=\"activityType_\"");
				// <param name="activityType_80" value="l13" />
				System.out.println(e.getKey() + e.getValue());
				// <param name="activityLatestStartTime_46" value="08:00:00" />
			}
		}
	}

	private void generatePopulation(Scenario scenario) {
		final FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(scenario.getConfig()
				.planCalcScore());
		PlanAlgorithm router = 
				new PlanRouter(
				new TripRouterFactoryBuilderWithDefaults().build(
						scenario ).get(
				) );
		this.pp4s = new PersonPrepareForSim(router, scenario);

		int pnr = 0;
		for (CommuterDataElement commuterDataElement : demand) {
			for (int i = 0; i < commuterDataElement.getCommuters() * scalefactor; i++) {
				Id<Person> id = Id.create(pnr + "_" + commuterDataElement.getFromId() + "_"
						+ commuterDataElement.getToId(), Person.class);
				Person p = scenario.getPopulation().getFactory().createPerson(id);
				Plan plan = generateCommuterPlan(scenario, commuterDataElement.getFromId(),
						commuterDataElement.getToId());
				if (plan != null){
					p.addPlan(plan);
					pnr++;
					this.pp4s.run(p);
					this.correctHomeEndTime(p);
					scenario.getPopulation().addPerson(p);
				}
				if (pnr % 100 == 0) {
					log.info("created person nr: " + pnr);
				}
			}

			log.info("Created " + commuterDataElement.getCommuters() + " commuters from "
					+ commuterDataElement.getFromName() + " (" + commuterDataElement.getFromId() + ") to "
					+ commuterDataElement.getToName() + " (" + commuterDataElement.getToId() + ")");
		}
		log.info("Created " + pnr + " commuters in total.");

	}

	private Plan generateCommuterPlan(Scenario scenario, String homeMunicipalityId,
			String workMunicipalityId) {
		double workStartTime = this.workStartTime + (7200.0 * timeRandom.nextDouble());
		double workEndTime = workStartTime + this.durationWork;
		Plan plan = scenario.getPopulation().getFactory().createPlan();

		SimpleFeature feature = this.municipalityMap.get(homeMunicipalityId);
		Coord homeCoord = null;
		do {
			Coord coord = MGC.coordinate2Coord(this.getRandomPointInFeature((Geometry) feature.getDefaultGeometry()));
			Boolean coordInLanduse = isCoordInLanduse(coord, "home", homeMunicipalityId);
			if (coordInLanduse == null) {
				return null;
			}
			if (coordInLanduse) {
				homeCoord = coord;
			}
		} while (homeCoord == null);
		plan.addActivity(this.createActivity(scenario, "home", 0.0, workStartTime, homeCoord));
		Leg leg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		plan.addLeg(leg);

		feature = this.municipalityMap.get(workMunicipalityId);
		Coord workCoord = null;
		do {
			Coordinate c = this.getRandomPointInFeature((Geometry) feature.getDefaultGeometry());
			Coord coord = MGC.coordinate2Coord(c);
			Boolean coordInLanduse = isCoordInLanduse(coord, "work", workMunicipalityId);
			if (coordInLanduse == null) {
				return null;
			}
			if (coordInLanduse) {
				workCoord = coord;
			}
		} while (workCoord == null);

		if (this.createMutateableActivities) {
			String workStartTimeString = Time.writeTime(workStartTime);
			String activityType = "work_" + workStartTimeString;
			plan.addActivity(this.createActivity(scenario, activityType, workStartTime, workEndTime,
					workCoord));
			this.workActivityTypesStartTimeMap.put(activityType, workStartTimeString);
		}
		else {
			plan.addActivity(this.createActivity(scenario, "work", workStartTime, workEndTime, workCoord));
		}
		leg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		plan.addLeg(leg);

		plan.addActivity(this.createActivity(scenario, "home", workEndTime, 24.0 * 3600, homeCoord));
		return plan;

	}

	private Boolean isCoordInLanduse(Coord coord, String actType, String municipalityId) {
		if (this.landuse == null) {
			return true;
		}
		else {
			Point point = MGC.xy2Point(coord.getX(), coord.getY());
			Collection<SimpleFeature> landuseFeatures = this.landuse.getLanduseFeature(actType, municipalityId);
			if (landuseFeatures == null) {
				log.warn("no landuse for point " + point.getX() + " " + point.getY() + " within municipality id: " + municipalityId + " locating " + actType + " activity somewhere...");
				return true; //return null; if activity and thus person should be removed from population
			}
			else {
				for (SimpleFeature feature : landuseFeatures) {
					if (((Geometry) feature.getDefaultGeometry()).contains(point)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private Activity createActivity(Scenario scenario, String type, Double start, Double end,
			Coord coord) {
		Activity activity = scenario.getPopulation().getFactory().createActivityFromCoord(type, coord);
		activity.setStartTime(start);
		activity.setEndTime(end);
		return activity;
	}

	private void correctHomeEndTime(Person p) {
		Activity homeAct = (Activity) p.getPlans().get(0).getPlanElements().get(0);
		Activity workAct = (Activity) p.getPlans().get(0).getPlanElements().get(2);
		Leg leg = (Leg) p.getPlans().get(0).getPlanElements().get(1);
		homeAct.setEndTime(workAct.getStartTime() - leg.getTravelTime());
		// leg.setRoute(null);
		// leg = (Leg) p.getPlans().get(0).getPlanElements().get(3);
		// leg.setRoute(null);
	}

	private double calculateNormallyDistributedTime(double i) {
		Random random = new Random();
		// draw two random numbers [0;1] from uniform distribution
		double r1 = random.nextDouble();
		double r2 = random.nextDouble();
		// Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		// linear transformation in order to optain N[i,7200²]
		double endTimeInSec = i + 60 * 60 * normal;
		return endTimeInSec;
	}

	private Coordinate getRandomPointInFeature(Geometry g) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + this.locationRandom.nextDouble()
					* (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + this.locationRandom.nextDouble()
					* (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		return p.getCoordinate();
	}

	public void setScalefactor(double scalefactor) {
		this.scalefactor = scalefactor;
	}

	public void setDuration(double duration) {
		this.durationWork = duration;
	}

	public void addLanduse(String activityType, Tuple<Collection<SimpleFeature>, CoordinateReferenceSystem> landuse)
			throws Exception {
		if (this.landuse == null) {
			this.landuse = new Landuse();
		}
		MathTransform transformation = null;
		transformation = CRS.findMathTransform(landuse.getSecond(), this.targetCrs, true);
		for (SimpleFeature ft : landuse.getFirst()) {
			Geometry geometry = JTS.transform((Geometry) ft.getDefaultGeometry(), transformation);
			ft.setDefaultGeometry(geometry);
			for (Entry<String, SimpleFeature> entry : this.municipalityMap.entrySet()) {
				SimpleFeature municipalityFeature = entry.getValue();
				if (((Geometry) municipalityFeature.getDefaultGeometry()).contains((Geometry) ft.getDefaultGeometry())
						|| ((Geometry) municipalityFeature.getDefaultGeometry()).intersects((Geometry) ft.getDefaultGeometry())) {
					this.landuse.addLanduseFeature(activityType, entry.getKey(), ft);
				}
			}
		}
	}

	private class Landuse {

		Map<String, Map<String, Collection<SimpleFeature>>> landuseMap = new HashMap<String, Map<String, Collection<SimpleFeature>>>();

		void addLanduseFeature(String activityType, String municipalityId, SimpleFeature feature) {
			if (! landuseMap.containsKey(activityType)) {
				landuseMap.put(activityType, new HashMap<String, Collection<SimpleFeature>>());
			}
			if (! landuseMap.get(activityType).containsKey(municipalityId)) {
				landuseMap.get(activityType).put(municipalityId, new ArrayList<SimpleFeature>());
			}
			landuseMap.get(activityType).get(municipalityId).add(feature);
		}

		Collection<SimpleFeature> getLanduseFeature(String activityType, String municipalityId) {
			return landuseMap.get(activityType).get(municipalityId);
		}

	}

}
