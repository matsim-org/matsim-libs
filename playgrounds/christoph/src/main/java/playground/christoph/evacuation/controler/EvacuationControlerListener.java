/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.controler;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.MultiModalControlerListener;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.mobsim.qsim.qnetsimengine.MissedJointDepartureWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.opengis.feature.simple.SimpleFeature;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.EvacuationQSimFactory;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.InformedHouseholdsTracker;
import playground.christoph.evacuation.mobsim.MobsimDataProvider;
import playground.christoph.evacuation.mobsim.ReplanningTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataGrabber;
import playground.christoph.evacuation.mobsim.decisionmodel.DecisionModelRunner;
import playground.christoph.evacuation.router.util.AffectedAreaPenaltyCalculator;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;

import com.vividsolutions.jts.geom.Geometry;

public class EvacuationControlerListener implements StartupListener {

	private static final Logger log = Logger.getLogger(EvacuationControlerListener.class);
	
	private final WithinDayControlerListener withinDayControlerListener;
	private final MultiModalControlerListener multiModalControlerListener;
	
	/*
	 * Data collectors and providers
	 */
	private ReplanningTracker replanningTracker;
	private MobsimDataProvider mobsimDataProvider;
	private JointDepartureOrganizer jointDepartureOrganizer;
	private MissedJointDepartureWriter jointDepartureWriter;
	private HouseholdsTracker householdsTracker;
//	private VehiclesTracker vehiclesTracker;
	private DecisionDataGrabber decisionDataGrabber;
	private DecisionModelRunner decisionModelRunner;
	
	// Geography stuff
	private CoordAnalyzer coordAnalyzer;
	private AffectedAreaPenaltyCalculator penaltyCalculator;
	private Geometry affectedArea;
	
//	protected MissedJointDepartureWriter jointDepartureWriter;
	private InformedHouseholdsTracker informedHouseholdsTracker;
//	protected SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
//	protected ModeAvailabilityChecker modeAvailabilityChecker;
	
	/*
	 * Data
	 */
	private ObjectAttributes householdObjectAttributes;
	
	public EvacuationControlerListener(WithinDayControlerListener withinDayControlerListener, 
			MultiModalControlerListener multiModalControlerListener) {
		this.withinDayControlerListener = withinDayControlerListener;
		this.multiModalControlerListener = multiModalControlerListener;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {

		// load household object attributes
		this.householdObjectAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(this.householdObjectAttributes).parse(EvacuationConfig.householdObjectAttributesFile);
		
		initGeographyStuff(event.getControler().getScenario());
		
		initDataGrabbersAndProviders(event.getControler());
		
		/*
		 * Use a MobsimFactory which creates vehicles according to available vehicles per
		 * household and adds the replanning Manager as mobsim engine.
		 */
		MobsimFactory mobsimFactory = new EvacuationQSimFactory(this.withinDayControlerListener.getWithinDayEngine(), 
				this.jointDepartureOrganizer, this.multiModalControlerListener.getMultiModalTravelTimes());
		event.getControler().setMobsimFactory(mobsimFactory);
	}

	private void initGeographyStuff(Scenario scenario) {
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		SHPFileUtil util = new SHPFileUtil();
		for (String file : EvacuationConfig.evacuationArea) {
			features.addAll(ShapeFileReader.getAllFeatures(file));		
		}
		this.affectedArea = util.mergeGeometries(features);
		log.info("Size of affected area: " + affectedArea.getArea());
		
		this.penaltyCalculator = new AffectedAreaPenaltyCalculator(scenario.getNetwork(), affectedArea, 
				EvacuationConfig.affectedAreaDistanceBuffer, EvacuationConfig.affectedAreaTimePenaltyFactor);
		
		this.coordAnalyzer = new CoordAnalyzer(affectedArea);
	}
	
	private void initDataGrabbersAndProviders(Controler controler) {
				
		this.mobsimDataProvider = new MobsimDataProvider();
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(mobsimDataProvider);
		
		this.jointDepartureOrganizer = new JointDepartureOrganizer();
		this.jointDepartureWriter = new MissedJointDepartureWriter(this.jointDepartureOrganizer);
		controler.addControlerListener(this.jointDepartureWriter);
		
		this.informedHouseholdsTracker = new InformedHouseholdsTracker(controler.getPopulation(),
				((ScenarioImpl) controler.getScenario()).getHouseholds());
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(informedHouseholdsTracker);
		controler.getEvents().addHandler(this.informedHouseholdsTracker);
		
		this.replanningTracker = new ReplanningTracker(this.informedHouseholdsTracker);
		controler.getEvents().addHandler(this.replanningTracker);
		
		this.householdsTracker = new HouseholdsTracker(controler.getScenario());
		controler.getEvents().addHandler(householdsTracker);
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(householdsTracker);
		
		this.decisionDataGrabber = new DecisionDataGrabber(controler.getScenario(), this.coordAnalyzer.createInstance(), 
				this.householdsTracker, this.householdObjectAttributes);		
		this.decisionModelRunner = new DecisionModelRunner(controler.getScenario(), this.decisionDataGrabber);
		controler.addControlerListener(this.decisionModelRunner);
		
//		this.vehiclesTracker = new VehiclesTracker(controler.getNetwork());
//		controler.getEvents().addHandler(vehiclesTracker);
	}
}
