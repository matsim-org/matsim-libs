/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationConfigReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.config;

import java.io.File;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.christoph.evacuation.router.util.DistanceFuzzyFactorProviderFactory;

public class EvacuationConfigReader extends MatsimXmlParser {

	private static final Logger log = Logger.getLogger(EvacuationConfigReader.class);

	public static String EVACUATIONCONFIG = "evacuationconfig";
	public static String EVACUATIONTIME = "evacuationtime";
	public static String CENTERCOORD = "centercoord";
	public static String HEIGHTINFORMATION = "heightinformation";
	public static String VEHICLEFLEET = "vehiclefleet";
	public static String EVACUATIONAREA = "evacuationarea";
	public static String HOUSEHOLDOBJECTATTRIBUTES = "householdobjectattributes";
	public static String ANALYSIS = "analysis";
	public static String FILE = "file";
	public static String PT = "pt";
	public static String PANIC = "panic";
	public static String PARTICIPATION = "participation";
	public static String DURINGLEGREROUTING = "duringlegrerouting";
	public static String PICKUPAGENTS = "pickupagents";
	public static String FUZZYTRAVELTIMES = "fuzzytraveltimes";
	public static String INFORMAGENTS = "informagents";
	public static String ROADCONDITIONS = "roadconditions";
	public static String EVACUATIONDECISION = "evacuationdecision";
	public static String TRANSITROUTER = "transitrouter";
	
	public static String TIME = "time";
	public static String DELAY = "delay";
	public static String X = "x";
	public static String Y = "y";
	public static String INNERRADIUS = "innerRadius";
	public static String OUTERRADIUS = "outerRadius";
	public static String DHM25FILE = "dhm25File";
	public static String SRTMFILE = "srtmFile";
	public static String PATH = "path";
	public static String FILENAME = "fileName";
	public static String CREATEEVACUATIONTIMEPICTURE = "createEvacuationTimePicture";
	public static String COUNTAGENTSINEVACUATIONAREA = "countAgentsInEvacuationArea";
	public static String TRAVELTIMEPENALTYFACTOR = "travelTimePenaltyFactor";
	public static String SHARE = "share";
	public static String COMPASSPROBABILITY = "compassProbability";
	public static String TABUSEARCH = "tabuSearch";
	public static String BEHAVIOUR = "behaviour";
	public static String ENABLED = "enabled";
	public static String RAYLEIGHSIGMA = "rayleighSigma";
	public static String CAPACITYFACTOR = "capacityFactor";
	public static String SPEEDFACTOR = "speedFactor";
	public static String USELOOKUPMAP = "useLookupMap";
		
	private String path = "";
	private boolean readVehicleFleet = false;
	private boolean readEvacuationArea = false;
	
	public static void main(String[] args) throws Exception {
		log.info(new File("../../matsim/mysimulations/census2000V2/config_evacuation.xml").exists());
		new EvacuationConfigReader().readFile("../../matsim/mysimulations/census2000V2/config_evacuation.xml");
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		
		if (EVACUATIONCONFIG.equalsIgnoreCase(name)) {
		} else if (EVACUATIONTIME.equalsIgnoreCase(name)) {
			EvacuationConfig.evacuationTime = Double.parseDouble(atts.getValue(TIME));
			EvacuationConfig.evacuationDelayTime = Double.parseDouble(atts.getValue(DELAY));
		} else if (CENTERCOORD.equalsIgnoreCase(name)) {
			double x = Double.parseDouble(atts.getValue(X));
			double y = Double.parseDouble(atts.getValue(Y));
			EvacuationConfig.centerCoord = new Coord(x, y);
			EvacuationConfig.innerRadius = Double.parseDouble(atts.getValue(INNERRADIUS));
			EvacuationConfig.outerRadius = Double.parseDouble(atts.getValue(OUTERRADIUS));
		} else if (HEIGHTINFORMATION.equalsIgnoreCase(name)) {
			EvacuationConfig.dhm25File = atts.getValue(DHM25FILE);
			EvacuationConfig.srtmFile = atts.getValue(SRTMFILE);
		} else if (VEHICLEFLEET.equalsIgnoreCase(name)) {
			path = atts.getValue(PATH);
			if (!path.endsWith("/")) path = "/" + path;
			this.readVehicleFleet = true;
			EvacuationConfig.vehicleFleet.clear();
		} else if (EVACUATIONAREA.equalsIgnoreCase(name)) {
			path = atts.getValue(PATH);
			if (!path.endsWith("/")) path = "/" + path;
			this.readEvacuationArea = true;
			EvacuationConfig.evacuationArea.clear();
		} else if (HOUSEHOLDOBJECTATTRIBUTES.equalsIgnoreCase(name)) {
			EvacuationConfig.householdObjectAttributesFile = atts.getValue(FILENAME);
		} else if (FILE.equalsIgnoreCase(name)) {
			if (readEvacuationArea && !readVehicleFleet) {
				EvacuationConfig.evacuationArea.add(path + atts.getValue(FILENAME));
			} else if (readVehicleFleet && !readEvacuationArea) {
				EvacuationConfig.vehicleFleet.add(path + atts.getValue(FILENAME));
			} else log.warn("Unexpected state. Ignoring file.");
		} else if (ANALYSIS.equalsIgnoreCase(name)) {
			EvacuationConfig.createEvacuationTimePicture = Boolean.valueOf(atts.getValue(CREATEEVACUATIONTIMEPICTURE));
			EvacuationConfig.countAgentsInEvacuationArea = Boolean.valueOf(atts.getValue(COUNTAGENTSINEVACUATIONAREA));
		} else if (PT.equalsIgnoreCase(name)) {
			double value = Double.valueOf(atts.getValue(TRAVELTIMEPENALTYFACTOR));
			if (value < 1.0) log.warn("PT travel time penalty factor is < 1.0, which means that the calculated travel times " +
					"will be shorter than without penalty!");
			EvacuationConfig.ptTravelTimePenaltyFactor = value;
		} else if (PANIC.equalsIgnoreCase(name)) {
			double value;
			
			value = Double.valueOf(atts.getValue(SHARE));
			if (value < 0.0) value = 0.0;
			else if (value > 1.0) value = 1.0;
			EvacuationConfig.panicShare = value;
			
			value = Double.valueOf(atts.getValue(COMPASSPROBABILITY));
			if (value < 0.0) value = 0.0;
			else if (value > 1.0) value = 1.0;
			EvacuationConfig.compassProbability = value;
			
			EvacuationConfig.tabuSearch = Boolean.valueOf(atts.getValue(TABUSEARCH));
		} else if (PARTICIPATION.equalsIgnoreCase(name)) {
			double value = Double.valueOf(atts.getValue(SHARE));
			if (value < 0.0) value = 0.0;
			else if (value > 1.0) value = 1.0;
			EvacuationConfig.householdParticipationShare = value;
		} else if (DURINGLEGREROUTING.equalsIgnoreCase(name)) {
			double value = Double.valueOf(atts.getValue(SHARE));
			if (value < 0.0) value = 0.0;
			else if (value > 1.0) value = 1.0;
			EvacuationConfig.duringLegReroutingShare = value;
		} else if (PICKUPAGENTS.equalsIgnoreCase(name)) {
			String behaviour = atts.getValue(BEHAVIOUR);
			if (EvacuationConfig.PickupAgentBehaviour.ALWAYS.toString().equalsIgnoreCase(behaviour)) {
				EvacuationConfig.pickupAgents = EvacuationConfig.PickupAgentBehaviour.ALWAYS;
			} else if (EvacuationConfig.PickupAgentBehaviour.NEVER.toString().equalsIgnoreCase(behaviour)) {
				EvacuationConfig.pickupAgents = EvacuationConfig.PickupAgentBehaviour.NEVER;
			} else if (EvacuationConfig.PickupAgentBehaviour.MODEL.toString().equalsIgnoreCase(behaviour)) {
				EvacuationConfig.pickupAgents = EvacuationConfig.PickupAgentBehaviour.MODEL;
			} else {
				throw new RuntimeException("Unknown value for pickup agents behaviour found: " + behaviour);
			}
		} else if (FUZZYTRAVELTIMES.equalsIgnoreCase(name)) {
			EvacuationConfig.useFuzzyTravelTimes = Boolean.valueOf(atts.getValue(ENABLED));
			
			if (atts.getValue(USELOOKUPMAP) != null) {
				DistanceFuzzyFactorProviderFactory.useLookupMap = Boolean.valueOf(atts.getValue(USELOOKUPMAP));
			}
		}  else if (INFORMAGENTS.equalsIgnoreCase(name)) {
			double value = Double.valueOf(atts.getValue(RAYLEIGHSIGMA));
			if (value < 0.1) value = 0.1;
			EvacuationConfig.informAgentsRayleighSigma = value;
		} else if (ROADCONDITIONS.equalsIgnoreCase(name)) {
			double value;
			
			value = Double.valueOf(atts.getValue(CAPACITYFACTOR));
			if (value < 0.0) value = 1.0;
			else if (value > 1.0) log.warn("Network capacity factor > 1.0 was found: " + value);
			EvacuationConfig.capacityFactor = value;
			
			value = Double.valueOf(atts.getValue(SPEEDFACTOR));
			if (value < 0.0) value = 1.0;
			else if (value > 1.0) log.warn("Network speed factor > 1.0 was found: " + value);
			EvacuationConfig.speedFactor = value;			
		} else if (EVACUATIONDECISION.equalsIgnoreCase(name)) {
			String behaviour = atts.getValue(BEHAVIOUR);
			if (EvacuationConfig.EvacuationDecisionBehaviour.SHARE.toString().equalsIgnoreCase(behaviour)) {
				EvacuationConfig.evacuationDecisionBehaviour = EvacuationConfig.EvacuationDecisionBehaviour.SHARE;
			} else if (EvacuationConfig.EvacuationDecisionBehaviour.MODEL.toString().equalsIgnoreCase(behaviour)) {
				EvacuationConfig.evacuationDecisionBehaviour = EvacuationConfig.EvacuationDecisionBehaviour.MODEL;
			} else {
				throw new RuntimeException("Unknown value for evacuation decision behaviour found: " + behaviour);
			}
		} else if (TRANSITROUTER.equalsIgnoreCase(name)) {
			EvacuationConfig.transitRouterFile = atts.getValue(FILENAME);
			EvacuationConfig.useTransitRouter = Boolean.valueOf(atts.getValue(ENABLED));
		} else {
			log.warn("Ignoring startTag: " + name);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		
		if (EVACUATIONCONFIG.equalsIgnoreCase(name)) {
		} else if (EVACUATIONTIME.equalsIgnoreCase(name)) {
		} else if (CENTERCOORD.equalsIgnoreCase(name)) {
		} else if (HEIGHTINFORMATION.equalsIgnoreCase(name)) {
		} else if (VEHICLEFLEET.equalsIgnoreCase(name)) {
			this.readVehicleFleet = false;
			this.path = "";
		} else if (EVACUATIONAREA.equalsIgnoreCase(name)) {
			this.readEvacuationArea = false;
			this.path = "";
		} else if (HOUSEHOLDOBJECTATTRIBUTES.equalsIgnoreCase(name)) {
		} else if (FILE.equalsIgnoreCase(name)) {
		} else if(ANALYSIS.equalsIgnoreCase(name)) {
		} else if(PT.equalsIgnoreCase(name)) {
		} else if(PANIC.equalsIgnoreCase(name)) {
		} else if(PARTICIPATION.equalsIgnoreCase(name)) {
		} else if(DURINGLEGREROUTING.equalsIgnoreCase(name)) {
		} else if(PICKUPAGENTS.equalsIgnoreCase(name)) {
		} else if(FUZZYTRAVELTIMES.equalsIgnoreCase(name)) {
		} else if(INFORMAGENTS.equalsIgnoreCase(name)) {
		} else if(ROADCONDITIONS.equalsIgnoreCase(name)) {
		} else if(EVACUATIONDECISION.equalsIgnoreCase(name)) {
		} else if(TRANSITROUTER.equalsIgnoreCase(name)) {
		} else log.warn("Ignoring endTag: " + name);
	}
	
	public void readFile(String fileName) {
		super.parse(fileName);
	}

}