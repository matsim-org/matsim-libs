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
package playground.dgrether.signalsystems.cottbus.commuterdemand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
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
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonPrepareForSim;
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
	private HashMap<String,Feature> municipalityMap;
	private List<CommuterDataElement> demand;
	private Scenario scenario;
	private Population population;
	private double scalefactor = 1.0;
	private double workStartTime = 8.0 * 3600.0;
	private double durationWork = 8.5 * 3600.0;
	private Random random;
	private PersonPrepareForSim pp4s;
	private boolean createMutateableActivities = false;
	private Map<String, String> workActivityTypesStartTimeMap = new HashMap<String, String>();
	

	public CommuterDemandWriter(Scenario sc, Set<Feature> gemeindenFeatures,
			CoordinateReferenceSystem featuresCrs, 
			List<CommuterDataElement> demand, CoordinateReferenceSystem targetCrs) {
		this.scenario = sc;
		this.random = MatsimRandom.getLocalInstance();
		this.demand = demand;
		this.tranformFeaturesAndInitMunicipalityMap(gemeindenFeatures, featuresCrs, targetCrs);
		final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(sc.getConfig().planCalcScore());
		PlansCalcRoute router = new PlansCalcRoute(sc.getConfig().plansCalcRoute(), sc.getNetwork(), timeCostCalc, timeCostCalc, new DijkstraFactory(), ((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).getModeRouteFactory());
		this.pp4s = new PersonPrepareForSim(router, (NetworkImpl) sc.getNetwork());
	}
	
	
	private void tranformFeaturesAndInitMunicipalityMap(Set<Feature> gemeindenFeatures, CoordinateReferenceSystem featuresCrs, CoordinateReferenceSystem targetCrs){
		try {
			MathTransform transformation = CRS.findMathTransform(featuresCrs, targetCrs, true);
			this.municipalityMap = new HashMap<String, Feature>();
			for (Feature ft : gemeindenFeatures){
				String gemeindeId = ft.getAttribute("NR").toString();
				Geometry geometry = JTS.transform(ft.getDefaultGeometry(), transformation);
				ft.setDefaultGeometry(geometry);
				this.municipalityMap.put(gemeindeId, ft);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
	}

	public void writeDemand(String filename) {
		log.error("Watch out! This version of the demand generator turns the whole BA data into car commuters which isn't exactly accurate. Adjusting the CommuterDemandWriter --> Scalefactor to your needs might make sense.");
		population = this.scenario.getPopulation();
		generatePopulation();
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(),
				scenario.getNetwork());
		populationWriter.write(filename);
		log.info("population written to " + filename);

		if (this.createMutateableActivities){
			log.info("");
			log.info("activity types: " );
			for (Entry<String, String> e : this.workActivityTypesStartTimeMap.entrySet()){
				System.out.println("<param name=\"activityType_\"");
				//<param name="activityType_80"           value="l13" />
				System.out.println(e.getKey() + e.getValue());
				//<param name="activityLatestStartTime_46"     value="08:00:00" />
			}
		}
	}

	private void generatePopulation() {
		int pnr = 0;
		for (CommuterDataElement commuterDataElement : demand) {
			for (int i = 0; i < commuterDataElement.getCommuters() * scalefactor; i++) {
				generatePlanForZones(pnr, commuterDataElement.getFromId(), commuterDataElement.getToId());
				pnr++;
			}

			log.info("Created " + commuterDataElement.getCommuters() + " commuters from "
					+ commuterDataElement.getFromName() + " (" + commuterDataElement.getFromId() + ") to "
					+ commuterDataElement.getToName() + " (" + commuterDataElement.getToId() + ")");
		}
		log.info("Created " + pnr + " commuters in total.");

	}

	private void generatePlanForZones(int pnr, String home, String work) {
		Person p;
		Id id;
		Plan plan;
//		double workStart = this.calculateNormallyDistributedTime(this.workStartTime);
		double workStart = this.workStartTime + (3600.0 * random.nextDouble());
		double workEnd = workStart + this.durationWork;
		id = scenario.createId(pnr + "_" + home.toString() + "_" + work.toString());
		p = population.getFactory().createPerson(id);
		plan = generatePlan(home, work, workStart, workEnd);
		p.addPlan(plan);
		this.pp4s.run(p);
		this.correctHomeEndTime(p);
		population.addPerson(p);

	}

	private Plan generatePlan(String home, String work, double workStartTime, double workEndTime) {
		Plan plan = population.getFactory().createPlan();
		Feature feature = this.municipalityMap.get(home);
		Coord homeCoord = MGC.coordinate2Coord(this.getRandomPointInFeature(feature.getDefaultGeometry()));
		feature = this.municipalityMap.get(work);
		Coord workCoord = MGC.coordinate2Coord(this.getRandomPointInFeature(feature.getDefaultGeometry()));
		plan.addActivity(this.createActivity("home", 0.0, workStartTime, homeCoord));
		Leg leg = population.getFactory().createLeg(TransportMode.car);
		plan.addLeg(leg);
		if (this.createMutateableActivities){
			String workStartTimeString = Time.writeTime(workStartTime);
			String activityType = "work_" + workStartTimeString;
			plan.addActivity(this.createActivity(activityType, workStartTime, workEndTime, workCoord));
			this.workActivityTypesStartTimeMap.put(activityType, workStartTimeString);
		}
		else {
			plan.addActivity(this.createActivity("work", workStartTime, workEndTime, workCoord));
		}
		leg = population.getFactory().createLeg(TransportMode.car);
		plan.addLeg(leg);
		plan.addActivity(this.createActivity("home", workEndTime, 24.0 * 3600, homeCoord));
		return plan;

	}

	private Activity createActivity(String type, Double start, Double end, Coord coord) {
		Activity activity = population.getFactory().createActivityFromCoord(type, coord);
		activity.setStartTime(start);
		activity.setEndTime(end);
		return activity;
	}
	
	private void correctHomeEndTime(Person p) {
		Activity homeAct = (Activity) p.getPlans().get(0).getPlanElements().get(0);
		Activity workAct = (Activity) p.getPlans().get(0).getPlanElements().get(2);
		Leg leg = (Leg) p.getPlans().get(0).getPlanElements().get(1);
		homeAct.setEndTime(workAct.getStartTime() - leg.getTravelTime());
//		leg.setRoute(null);
//		leg = (Leg) p.getPlans().get(0).getPlanElements().get(3);
//		leg.setRoute(null);
	}

	
	private double calculateNormallyDistributedTime(double i) {
		Random random = new Random();
		//draw two random numbers [0;1] from uniform distribution
		double r1 = random.nextDouble();
		double r2 = random.nextDouble();
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		//linear transformation in order to optain N[i,7200²]
		double endTimeInSec = i + 60 * 60 * normal ;
		return endTimeInSec;
	}

	private Coordinate getRandomPointInFeature(Geometry g) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + this.random.nextDouble()
					* (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + this.random.nextDouble()
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

}
