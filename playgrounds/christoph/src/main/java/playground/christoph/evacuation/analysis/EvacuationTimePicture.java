/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationTimePicture.java
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

package playground.christoph.evacuation.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vis.kml.KMZWriter;

import playground.christoph.evacuation.config.EvacuationConfig;

/**
 * Identifies persons who are inside (or cross) the evacuation area after the
 * evacuation has started.
 *  
 * @author cdobler
 */
public class EvacuationTimePicture implements AgentDepartureEventHandler, ActivityStartEventHandler,
	LinkEnterEventHandler, SimulationInitializedListener, IterationEndsListener {
	
	private static final Logger log = Logger.getLogger(EvacuationTimePicture.class);
	
	private final Scenario scenario;
	private final Set<String> transportModes;
	private final CoordAnalyzer coordAnalyzer;

	private final Map<String, Map<Id, Double>> lastLeft;			// <TransportMode, <PersonId, last time the Evacuation Area was left>>
	private final Map<Id, Id> positionAtEvacuationStartLinks;		// <PersonId, LinkId>
	private final Map<Id, Id> positionAtEvacuationStartFacilities;	// <PersonId, FacilityId>
	private final Map<Id, Id> currentPosition;						// <PersonId, FacilityId or LinkId>
	private final Map<Id, String> currentTransportMode;				// <PersonId, TransportMode>
	private final Set<Id> affectedFacilities;
	private final Set<Id> affectedLinks;
	
	private String kmzFile = "EvacuationTime.kmz";
	
	public EvacuationTimePicture(Scenario scenario, Set<String> transportModes, CoordAnalyzer coordAnalyzer) {
		this.scenario = scenario;
		this.transportModes = transportModes;
		this.coordAnalyzer = coordAnalyzer;
		
		this.affectedLinks = new HashSet<Id>();
		this.affectedFacilities = new HashSet<Id>();

		this.currentPosition = new HashMap<Id, Id>();
		this.currentTransportMode = new HashMap<Id, String>();
		this.positionAtEvacuationStartLinks = new HashMap<Id, Id>();
		this.positionAtEvacuationStartFacilities = new HashMap<Id, Id>();
		
		this.lastLeft = new HashMap<String, Map<Id, Double>>();
		for (String transportMode : transportModes) this.lastLeft.put(transportMode, new HashMap<Id, Double>());
	}
	
	/*
	 * Get those facilities and links that are located in the affected area.
	 */
	private void getAffectedLinksAndFacilities() {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (coordAnalyzer.isLinkAffected(link)) affectedLinks.add(link.getId());
		}
		log.info("Found " + affectedLinks.size() + " links in affected area.");
		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			if (coordAnalyzer.isCoordAffected(facility.getCoord())) affectedFacilities.add(facility.getId());
		}
		log.info("Found " + affectedFacilities.size() + " facilities in affected area.");
	}
		
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();
		String transportMode = event.getLegMode();
		double time = event.getTime();
		
		// set transport mode
		currentTransportMode.put(personId, transportMode);
		
		if (time < EvacuationConfig.evacuationTime) {
			positionAtEvacuationStartFacilities.remove(personId);
			positionAtEvacuationStartLinks.put(personId, linkId);
		} else {
			/*
			 * If the facility where the activity was performed is affected but the
			 * link where the facility is connected to is not affected, the agent
			 * has left the affected area.
			 */
			if (affectedFacilities.contains(currentPosition.get(personId)) &&
				!affectedLinks.contains(linkId)) {
				 lastLeft.get(transportMode).put(personId, time);
			}			
		}
		// update current position
		currentPosition.put(personId, linkId);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();
		Id facilityId = event.getFacilityId();
		double time = event.getTime();

		// reset current transport mode
		String transportMode = currentTransportMode.remove(personId);
		
		if (time < EvacuationConfig.evacuationTime) {
			positionAtEvacuationStartLinks.remove(personId);
			positionAtEvacuationStartFacilities.put(personId, facilityId);
		} else {
			
			/*
			 * If the links where the facility is connected to is affected but the
			 * facility where the activity is performed is connected to is not affected, 
			 * the agent has left the affected area.
			 */
			if (affectedLinks.contains(currentPosition.get(personId)) &&
				!affectedFacilities.contains(linkId)) {
				 lastLeft.get(transportMode).put(personId, time);
			}			
		}		

		// update current position
		currentPosition.put(personId, facilityId);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id personId = event.getPersonId();
		Id linkId = event.getLinkId();
		double time = event.getTime();
		
		if (time < EvacuationConfig.evacuationTime) {
			positionAtEvacuationStartLinks.put(personId, linkId);
		} else {
			String transportMode = currentTransportMode.get(personId);
			/*
			 * If the last link was affected but this one is not, the agent has left
			 * the affected area.
			 */
			boolean a = affectedLinks.contains(currentPosition.get(personId));
			boolean b = affectedLinks.contains(linkId);
			
			if (affectedLinks.contains(currentPosition.get(personId)) &&
				!affectedLinks.contains(linkId)) {
				lastLeft.get(transportMode).put(personId, time);
			}	
		}
		// update current position
		currentPosition.put(personId, linkId);
	}
		
	@Override
	public void reset(int iteration) {
		this.positionAtEvacuationStartFacilities.clear();
		this.positionAtEvacuationStartLinks.clear();
		this.currentPosition.clear();
		this.currentTransportMode.clear();
		this.affectedFacilities.clear();
		this.affectedLinks.clear();
		this.lastLeft.clear();
		for (String transportMode : transportModes) this.lastLeft.put(transportMode, new HashMap<Id, Double>());
	}

	/*
	 * Get the initial position of each agent and 
	 * the links and facilities in the affected area.
	 */
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		for (MobsimAgent agent : ((QSim)e.getQueueSimulation()).getAgents()) {
			Activity firstActivity = (Activity) ((PlanAgent) agent).getSelectedPlan().getPlanElements().get(0);
			positionAtEvacuationStartFacilities.put(agent.getId(), firstActivity.getFacilityId());
			currentPosition.put(agent.getId(), firstActivity.getFacilityId());
		}
		
		getAffectedLinksAndFacilities();
	}
	
	/*
	 * Write KML File
	 */
	private void createKML(String file) {
		try {
			ObjectFactory kmlObjectFactory = new ObjectFactory();
			KMZWriter kmzWriter = new KMZWriter(file);
			
			KmlType mainKml = kmlObjectFactory.createKmlType();
			DocumentType mainDoc = kmlObjectFactory.createDocumentType();
			mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
			
			CoordinateTransformation coordTransform = new CH1903LV03toWGS84();
			EvacuationTimePictureWriter writer = new EvacuationTimePictureWriter(this.scenario, coordTransform, kmzWriter, mainDoc);
			
			/*
			 * Get all agents for whose an entry in the last left map exists.
			 */
			Map<Id, BasicLocation> positionAtEvacuationStart = new HashMap<Id, BasicLocation>();
			for (Map<Id, Double> map : lastLeft.values()) {
				for (Id personId : map.keySet()) {
					Id linkId = positionAtEvacuationStartLinks.get(personId);
					Id facilityId = positionAtEvacuationStartFacilities.get(personId);
					if (linkId != null) {
						positionAtEvacuationStart.put(personId, this.scenario.getNetwork().getLinks().get(linkId));
					} else if (facilityId != null) {
						positionAtEvacuationStart.put(personId, ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(facilityId));
					} else {
						log.warn("No initial position for agent " + personId.toString() + " was found!");
					}
				}
			}

			// print some statistics
			log.info("Agents crossing the affected area after the evacation has started: " + positionAtEvacuationStart.size());
			log.info("Agents performing an activity when the evacuation starts: " + positionAtEvacuationStartFacilities.size());
			log.info("Agents performing a leg when the evacution starts: " + positionAtEvacuationStartLinks.size());
			
			FolderType linkFolderType = writer.getLinkFolder(positionAtEvacuationStart, lastLeft);
			mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(linkFolderType));
			
			kmzWriter.writeMainKml(mainKml);
			kmzWriter.close();
		} catch(IOException e) { }
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), kmzFile);
		createKML(fileName);
	}

}