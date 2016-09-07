/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusFanCreator
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
package playground.dgrether.signalsystems.cottbus.footballdemand;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.util.Collection;
import java.util.Random;


/**
 * @author dgrether
 *
 */
public class SimpleCottbusFanCreator implements CottbusFanCreator {

	private SimpleFeature cbFeature;
	private SimpleFeature spnFeature;
	private Random random;
	private int fanId = 1;
	private double cottbusFansPercentage = 0.25;
	private Geometry sdfGeometry;

	public SimpleCottbusFanCreator(String kreisShapeFile) {
		this.random = MatsimRandom.getLocalInstance();
		loadShapeAndGetCBSPNFeatures(kreisShapeFile);
		initSDFShape();
	}

	private void initSDFShape() {
		GeometryFactory geometryFactory= new GeometryFactory();
		WKTReader wktReader = new WKTReader(geometryFactory);
		try {
			CoordinateReferenceSystem sourceCRS = MGC.getCRS(TransformationFactory.WGS84);
			CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
			MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
			//WGS 84 coords
			this.sdfGeometry = wktReader.read("MULTIPOLYGON (((14.34342 51.750052, 14.351832 51.752443, 14.346853 51.75595, 14.34239 51.755658, 14.34342 51.750052)))");
			this.sdfGeometry = JTS.transform(this.sdfGeometry, transform);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (TransformException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (FactoryException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void loadShapeAndGetCBSPNFeatures(String kreisShapeFile) {
		ShapeFileReader shapeReader = new ShapeFileReader();
		try {
			Collection<SimpleFeature> gemeindenFeatures = shapeReader.readFileAndInitialize(kreisShapeFile);
			for (SimpleFeature f : gemeindenFeatures){
				if (f.getAttribute("Nr").toString().equals("12052000")){
					this.cbFeature = f;
				}
				else if (f.getAttribute("Nr").toString().equals("12071000")){
					this.spnFeature = f;
				}
			}
			CoordinateReferenceSystem crs = shapeReader.getCoordinateSystem();
			CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
			MathTransform shapeToNetTransformation = CRS.findMathTransform(crs, targetCRS, true);
			
			Geometry geometry = JTS.transform((Geometry) this.cbFeature.getDefaultGeometry(), shapeToNetTransformation);
			this.cbFeature.setDefaultGeometry(geometry);
		
			geometry = JTS.transform((Geometry) this.spnFeature.getDefaultGeometry(), shapeToNetTransformation);
			this.spnFeature.setDefaultGeometry(geometry);
		
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} 
		
	}

	@Override
	public Population createAndAddFans(Scenario sc, int numberOfFans) {
		int numberOfCbFans = (int) (numberOfFans * cottbusFansPercentage);
		int numberOfSPNFans = numberOfFans - numberOfCbFans;
		
		final FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(sc.getConfig().planCalcScore());
		PlanAlgorithm router =
				new PlanRouter(
						new TripRouterFactoryBuilderWithDefaults().build(
								sc ).get(
						) );
		PersonPrepareForSim pp4s = new PersonPrepareForSim(router, sc);
		
//		Scenario sc2 = ScenarioUtils.createScenario(sc.getConfig());
		Population fanPop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		Population pop = sc.getPopulation();
		Person p;
		Plan plan;
		Coord homeCoord;
		Coord stadiumCoord;
		Coordinate stadiumCoordinate;
		Coordinate homeCoordinate;
		for (int i = 0; i < numberOfCbFans; i++){
			p = pop.getFactory().createPerson(Id.create(Integer.toString(fanId) + "_" + CottbusFootballStrings.CB2FB, Person.class));
			pop.addPerson(p);
			fanPop.addPerson(p);
			this.fanId++;
			homeCoordinate = this.getRandomPointInFeature(this.random, (Geometry) this.cbFeature.getDefaultGeometry(), this.sdfGeometry);
			homeCoord = MGC.coordinate2Coord(homeCoordinate);
			stadiumCoordinate = this.getRandomPointInFeature(this.random, this.sdfGeometry, null);
			stadiumCoord = MGC.coordinate2Coord(stadiumCoordinate);
			plan = this.createFootballPlan(sc, homeCoord, stadiumCoord);
			p.addPlan(plan);
			pp4s.run(p);
			this.correctHomeEndTime(p);
//			sc2.getPopulation().addPerson(p);
		}
		for (int i = 0; i < numberOfSPNFans; i++){
			p = pop.getFactory().createPerson(Id.create(Integer.toString(fanId) + "_" + CottbusFootballStrings.SPN2FB, Person.class));
			pop.addPerson(p);
			fanPop.addPerson(p);
			this.fanId++;
			homeCoordinate = this.getRandomPointInFeature(this.random, (Geometry) this.spnFeature.getDefaultGeometry(), (Geometry) this.cbFeature.getDefaultGeometry());
			homeCoord = MGC.coordinate2Coord(homeCoordinate);
			stadiumCoordinate = this.getRandomPointInFeature(this.random, this.sdfGeometry, null);
			stadiumCoord = MGC.coordinate2Coord(stadiumCoordinate);
			plan = this.createFootballPlan(sc, homeCoord, stadiumCoord);
			p.addPlan(plan);
			pp4s.run(p);
			this.correctHomeEndTime(p);
//			sc2.getPopulation().addPerson(p);
		}
		return fanPop;
	}
	
	private void correctHomeEndTime(Person p) {
		Activity homeAct = (Activity) p.getPlans().get(0).getPlanElements().get(0);
		Activity footballAct = (Activity) p.getPlans().get(0).getPlanElements().get(2);
		Leg leg = (Leg) p.getPlans().get(0).getPlanElements().get(1);
		homeAct.setEndTime(footballAct.getStartTime() - leg.getTravelTime());
	}

	private Plan createFootballPlan(Scenario sc, Coord homeCoord, Coord stadiumCoord){
		PopulationFactory popfac = sc.getPopulation().getFactory();
		double fbArrivalTime = this.chooseFootballArrivalTime();
		double fbDepartureTime = this.chooseFootballEndTime();
		Plan plan = popfac.createPlan();
		Activity homeAct = popfac.createActivityFromCoord("home", homeCoord);
		homeAct.setEndTime(fbArrivalTime);
		plan.addActivity(homeAct);
		Leg leg = popfac.createLeg(TransportMode.car);
		plan.addLeg(leg);
		Activity fbAct = popfac.createActivityFromCoord("fb", stadiumCoord);
		fbAct.setStartTime(fbArrivalTime);
		fbAct.setEndTime(fbDepartureTime);
		plan.addActivity(fbAct);
		leg = popfac.createLeg(TransportMode.car);
		plan.addLeg(leg);
		homeAct = popfac.createActivityFromCoord("home", homeCoord);
		plan.addActivity(homeAct);
		return plan;
	}
	
	private double chooseFootballArrivalTime(){
		double offset = 3600.0 * this.random.nextDouble();
		double start = 3600.0 * 17.0;
		return start + offset;
	}
	
	private double chooseFootballEndTime(){
		double offset = 3600.0 * this.random.nextDouble();
		double end = 3600.0 * 19.75;
		return end + offset;
	}
	

	private Coordinate getRandomPointInFeature(Random rnd, Geometry g, Geometry exclude) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble()
					* (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble()
					* (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p) || (exclude != null && exclude.contains(p)));
		return p.getCoordinate();
	}

	
	
}
