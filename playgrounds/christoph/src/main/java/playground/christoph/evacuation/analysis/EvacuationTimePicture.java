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

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.withinday.mobsim.MobsimDataProvider;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.mobsim.Tracker.Position;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

/**
 * Identifies persons who are inside (or cross) the evacuation area after the
 * evacuation has started.
 * 
 * All utilized transport modes for each of those persons are logged.
 *  
 * @author cdobler
 */
public class EvacuationTimePicture implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler, PersonEntersVehicleEventHandler, 
		PersonLeavesVehicleEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler,
		MobsimAfterSimStepListener, IterationEndsListener {
	
	private static final Logger log = Logger.getLogger(EvacuationTimePicture.class);
	private static final String separator = "\t";
	private static final String newLine = "\n";
	private static final Charset charset = Charset.forName("UTF-8");
	
	private final Scenario scenario;
	private final CoordAnalyzer coordAnalyzer;
	private final AgentsTracker agentsTracker;
	private final MobsimDataProvider mobsimDataProvider;

	private final Map<Id, AgentInfo> agentInfos;
	private final Set<Id> agentsToUpdate;
	
	private boolean writeKMZFile = false;
	private boolean writeTXTFile = true;
	
	private String kmzFile = "EvacuationTime.kmz";
	private String txtFile = "EvacuationTime.txt.gz";
	
	// just a cache flag
	private boolean evacuationStarted = false;
	
	public EvacuationTimePicture(Scenario scenario, CoordAnalyzer coordAnalyzer, 
			AgentsTracker agentsTracker, MobsimDataProvider mobsimDataProvider) {
		this.scenario = scenario;
		this.coordAnalyzer = coordAnalyzer;
		this.agentsTracker = agentsTracker;
		this.mobsimDataProvider = mobsimDataProvider;

		this.agentInfos = new HashMap<Id, AgentInfo>();
		this.agentsToUpdate = new HashSet<Id>();		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (evacuationStarted) agentsToUpdate.add(event.getPersonId());
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (evacuationStarted) agentsToUpdate.add(event.getPersonId());
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (evacuationStarted) agentsToUpdate.add(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (evacuationStarted) agentsToUpdate.add(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (evacuationStarted) agentsToUpdate.add(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (evacuationStarted) agentsToUpdate.add(event.getPersonId());
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (evacuationStarted) agentsToUpdate.add(event.getPersonId());
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (evacuationStarted) agentsToUpdate.add(event.getPersonId());
	}
			
	@Override
	public void reset(int iteration) {
		this.agentInfos.clear();
		this.agentsToUpdate.clear();
	}
	
	private void createFiles(String kmzFile, String txtFile) {
		/*
		 * Get all agents who were in the affected area after the evacuation has started.
		 */
		Map<Id, BasicLocation> positionAtEvacuationStart = new HashMap<Id, BasicLocation>();
		for (AgentInfo agentInfo : this.agentInfos.values()) {
			
			// if the agents was inside the affected area after the evacuation started
			if (agentInfo.leftArea != Time.UNDEFINED_TIME) {
				
				Position positionType = agentInfo.initialPositionType;
				Id positionId = agentInfo.initialPositionId;
				
				/*
				 * We do not expect any Position.VEHICLE entry here since we
				 * converted all of them to Position.LINK...
				 */
				if (positionType.equals(Position.LINK)) {
					positionAtEvacuationStart.put(agentInfo.id, scenario.getNetwork().getLinks().get(positionId));
				}
				else if (positionType.equals(Position.FACILITY)) {
					positionAtEvacuationStart.put(agentInfo.id, scenario.getActivityFacilities().getFacilities().get(positionId));
				}
				else log.warn("Found agent with an undefined initial position type. Type: " + positionType + ", AgentId: " + agentInfo.id);
			}
		}
		
		if (this.writeKMZFile) {
			log.info("Creating kmz output file...");
			createKML(kmzFile, positionAtEvacuationStart);
			log.info("Done.");			
		}
		
		if (this.writeTXTFile) {
			log.info("Creating txt ouput file...");
			createTXT(txtFile, positionAtEvacuationStart);
			log.info("Done.");			
		}
	}
	
	/*
	 * Write KML File
	 */
	private void createKML(String fileName, Map<Id, BasicLocation> positionAtEvacuationStart) {
		
		try {
			ObjectFactory kmlObjectFactory = new ObjectFactory();
			KMZWriter kmzWriter = new KMZWriter(this.kmzFile);
			
			KmlType mainKml = kmlObjectFactory.createKmlType();
			DocumentType mainDoc = kmlObjectFactory.createDocumentType();
			mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
			
			CoordinateTransformation coordTransform = new CH1903LV03toWGS84();
			EvacuationTimePictureWriter writer = new EvacuationTimePictureWriter(this.scenario, coordTransform, kmzWriter, mainDoc);	
			
			FolderType linkFolderType = writer.getLinkFolder(positionAtEvacuationStart, positionAtEvacuationStart, this.agentInfos);
			mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(linkFolderType));
			
			kmzWriter.writeMainKml(mainKml);
			kmzWriter.close();
		} catch(IOException e) { 
			throw new RuntimeException(e);
		}
	}
	
	/*
	 * Write TXT File
	 */
	private void createTXT(String fileName, Map<Id, BasicLocation> positionAtEvacuationStart) {
		
		// identify all utilized modes
		Set<String> modes = new HashSet<String>();
		for (AgentInfo agentInfo : this.agentInfos.values()) modes.addAll(agentInfo.transportModes);
		Set<String> orderedModes = new TreeSet<String>(modes);
		
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			GZIPOutputStream gzos = new GZIPOutputStream(fos);
			OutputStreamWriter osw = new OutputStreamWriter(gzos, charset);
			BufferedWriter bw = new BufferedWriter(osw);
			
			// write header
			bw.write("PersonId");
			bw.write(separator);
			bw.write("enteredArea");
			bw.write(separator);
			bw.write("leftArea");
			bw.write(separator);
			bw.write("initialPositionX");
			bw.write(separator);
			bw.write("initialPositionY");
			bw.write(separator);
			bw.write("initialPositionType");
			for (String mode : orderedModes) {
				bw.write(separator);
				bw.write(mode);
			}
			bw.write(newLine);
			
			// write lines
			for (Entry<Id, BasicLocation> entry : positionAtEvacuationStart.entrySet()) {
				AgentInfo agentInfo = this.agentInfos.get(entry.getKey());
				
				bw.write(entry.getKey().toString());
				bw.write(separator);
				bw.write(String.valueOf(agentInfo.enteredArea));
				bw.write(separator);
				bw.write(String.valueOf(agentInfo.leftArea));
				bw.write(separator);
				bw.write(String.valueOf(entry.getValue().getCoord().getX()));
				bw.write(separator);
				bw.write(String.valueOf(entry.getValue().getCoord().getY()));
				bw.write(separator);
				bw.write(agentInfo.initialPositionType.toString());
				for (String mode : orderedModes) {
					bw.write(separator);
					if (agentInfo.transportModes.contains(mode)) bw.write("1");	// true
					else bw.write("0");	// false
				}
				bw.write(newLine);
			}
			
			bw.close();
			osw.close();
			gzos.close();
			fos.close();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		
		/*
		 * If the evacuation starts in the next time step, we check each agents current position.
		 */
		if (e.getSimulationTime() + 1 == EvacuationConfig.evacuationTime) {
			log.info("Evacuation starts in the next time step. Collecting agents' positions.");
			
			for (Person person : scenario.getPopulation().getPersons().values()) {
				AgentPosition agentPosition = agentsTracker.getAgentPosition(person.getId());
				
				Position positionType = agentPosition.getPositionType();
				Id positionId = agentPosition.getPositionId();
				
				if (positionType.equals(Position.LINK)) {
					Link link = scenario.getNetwork().getLinks().get(positionId);
					boolean isAffected = coordAnalyzer.isLinkAffected(link);

					AgentInfo agentInfo = new AgentInfo();
					agentInfo.id = person.getId();
					agentInfo.transportModes.add(agentPosition.getTransportMode());
					agentInfo.initialPositionId = positionId;
					agentInfo.initialPositionType = positionType;
					if (isAffected) {
						agentInfo.isInsideArea = true;
						agentInfo.enteredArea = EvacuationConfig.evacuationTime;
					}
					this.agentInfos.put(person.getId(), agentInfo);	
				}
				else if (positionType.equals(Position.VEHICLE)) {
					/*
					 * Use the link where the vehicle is currently located.
					 */
					Id linkId = this.mobsimDataProvider.getVehicle(positionId).getCurrentLink().getId();
					Link link = scenario.getNetwork().getLinks().get(linkId);
					boolean isAffected = coordAnalyzer.isLinkAffected(link);
										
					/*
					 * Convert Position.VEHICLE to Position.LINK since a links position
					 * is fixed while a vehicles moves over time. 
					 */
					AgentInfo agentInfo = new AgentInfo();
					agentInfo.id = person.getId();
					agentInfo.transportModes.add(agentPosition.getTransportMode());
					agentInfo.initialPositionId = linkId;
					agentInfo.initialPositionType = Position.LINK;
					if (isAffected) {
						agentInfo.isInsideArea = true;
						agentInfo.enteredArea = EvacuationConfig.evacuationTime;
					}
					this.agentInfos.put(person.getId(), agentInfo);
				}
				else if (positionType.equals(Position.FACILITY)) {
					ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(positionId);
					boolean isAffected = coordAnalyzer.isFacilityAffected(facility);
					
					AgentInfo agentInfo = new AgentInfo();
					agentInfo.id = person.getId();
					agentInfo.initialPositionId = positionId;
					agentInfo.initialPositionType = positionType;
					if (isAffected) {
						agentInfo.isInsideArea = true;
						agentInfo.enteredArea = EvacuationConfig.evacuationTime;
					}
					this.agentInfos.put(person.getId(), agentInfo);
				} else log.warn("Found agent with an undefined position type. AgentId: " + person.getId() + ", time: " + e.getSimulationTime());
			}
			
			// enable agents tracking
			evacuationStarted = true;
		} 
		
		/*
		 * If the evacuation has started, we update the positions of those agents who have moved.
		 */
		else if (e.getSimulationTime() > EvacuationConfig.evacuationTime) {
			for (Id id : this.agentsToUpdate) {
				AgentPosition agentPosition = agentsTracker.getAgentPosition(id);
				
				Position positionType = agentPosition.getPositionType();
				Id positionId = agentPosition.getPositionId();
				AgentInfo agentInfo = this.agentInfos.get(id);
				boolean wasInsideArea = agentInfo.isInsideArea;
				boolean isInsideArea = wasInsideArea;
				
				if (positionType.equals(Position.LINK)) {
					Link link = scenario.getNetwork().getLinks().get(positionId);
					isInsideArea = coordAnalyzer.isLinkAffected(link);

					/*
					 * Add the agents transport mode to the set of utilized modes. 
					 */
					agentInfo.transportModes.add(agentPosition.getTransportMode());
					
					/* 
					 * If the agent entered the affected area for the first time, set the enter time.
					 */
					if (!wasInsideArea && isInsideArea && agentInfo.enteredArea == Time.UNDEFINED_TIME) {
						agentInfo.enteredArea = e.getSimulationTime();
					}
					/*
					 * If the agent left the affected area, set the left time.
					 */
					else if (wasInsideArea && !isInsideArea) agentInfo.leftArea = e.getSimulationTime();					
				} else if (positionType.equals(Position.VEHICLE)) {
					/*
					 * Use the link where the vehicle is currently located.
					 */
					Id linkId = this.mobsimDataProvider.getVehicle(positionId).getCurrentLink().getId();
					Link link = scenario.getNetwork().getLinks().get(linkId);
					isInsideArea = coordAnalyzer.isLinkAffected(link);
					
					/*
					 * Add the agents transport mode to the set of utilized modes. 
					 */
					agentInfo.transportModes.add(agentPosition.getTransportMode());
					
					/* 
					 * If the agent entered the affected area for the first time, set the enter time
					 */
					if (!wasInsideArea && isInsideArea && agentInfo.enteredArea == Time.UNDEFINED_TIME) {
						agentInfo.enteredArea = e.getSimulationTime();
					}
					/*
					 * If the agent left the affected area
					 */
					else if (wasInsideArea && !isInsideArea) agentInfo.leftArea = e.getSimulationTime();					

				} else if (positionType.equals(Position.FACILITY)) {
					ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(positionId);
					isInsideArea = coordAnalyzer.isFacilityAffected(facility);
					
					/* 
					 * If the agent entered the affected area for the first time, set the enter time
					 */
					if (!wasInsideArea && isInsideArea && agentInfo.enteredArea == Time.UNDEFINED_TIME) {
						agentInfo.enteredArea = e.getSimulationTime();
					}
					/*
					 * If the agent left the affected area
					 */
					else if (wasInsideArea && !isInsideArea) agentInfo.leftArea = e.getSimulationTime();
					
				} else log.warn("Found agent with an undefined position type. AgentId: " + id + ", time: " + e.getSimulationTime());
				
				/*
				 * Update agent's is inside area information.
				 */
				agentInfo.isInsideArea = isInsideArea;
			}
			
			this.agentsToUpdate.clear();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String kmzFileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), kmzFile);
		String txtFileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), txtFile);
		this.createFiles(kmzFileName, txtFileName);
	}

	/*package*/ static class AgentInfo {
		Id id = null;
		boolean isInsideArea = false;
		double enteredArea = Time.UNDEFINED_TIME;
		double leftArea = Time.UNDEFINED_TIME;
		Id initialPositionId = null;
		Position initialPositionType = Position.UNDEFINED;
		Set<String> transportModes = new HashSet<String>();
	}
}