/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystems11To20Converter
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
package playground.dgrether.signalsystems.data;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.jaxb.signalgroups20.XMLSignalGroupType;
import org.matsim.jaxb.signalgroups20.XMLSignalGroups;
import org.matsim.jaxb.signalgroups20.XMLSignalSystemSignalGroupType;
import org.matsim.jaxb.signalsystems11.XMLIdRefType;
import org.matsim.jaxb.signalsystems11.XMLSignalGroupDefinitionType;
import org.matsim.jaxb.signalsystems11.XMLSignalSystemDefinitionType;
import org.matsim.jaxb.signalsystems11.XMLSignalSystems;
import org.matsim.jaxb.signalsystems20.XMLSignalSystemType;
import org.matsim.jaxb.signalsystems20.XMLSignalType;
import org.matsim.jaxb.signalsystems20.XMLSignalSystemType.XMLSignals;
import org.matsim.jaxb.signalsystems20.XMLSignalType.XMLLane;
import org.matsim.lanes.Lane;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.lanes.LaneDefinitionsImpl;
import org.matsim.lanes.LaneDefinitionsReader11;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.MatsimLaneDefinitionsReader;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
import org.matsim.signalsystems.SignalSystemsReader11;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsWriter20;
import org.xml.sax.SAXException;




/**
 * @author dgrether
 *
 */
public class SignalSystems11To20Converter {
	
	private static final Logger log = Logger.getLogger(SignalSystems11To20Converter.class);

	public void convert(String laneDefinitions11, String signalSystems, String signalSystems20, String signalGroups20) {
		LaneDefinitions laneDefs = this.readLaneDefinitions11(laneDefinitions11);
		XMLSignalSystems xmlSignals = this.readSignals11(signalSystems);
		Tuple<org.matsim.jaxb.signalsystems20.XMLSignalSystems, XMLSignalGroups> signals20Groups20 = this.createSignalSystems20(laneDefs, xmlSignals);
		this.writeSignals20(signals20Groups20.getFirst(), signalSystems20);
		this.writeSignalGroups20(signals20Groups20.getSecond(), signalGroups20);
		log.info("conversion done!");
	}
	
	private LaneDefinitions readLaneDefinitions11(String laneDefinitions11) {
		LaneDefinitions laneDefs = new LaneDefinitionsImpl();
		LaneDefinitionsReader11 reader = new LaneDefinitionsReader11(laneDefs, MatsimLaneDefinitionsReader.SCHEMALOCATIONV11);
		try {
			reader.readFile(laneDefinitions11);
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return laneDefs;
	}

	private void writeSignals20(org.matsim.jaxb.signalsystems20.XMLSignalSystems signals20,
			String filename) {
		SignalSystemsWriter20  writer = new SignalSystemsWriter20(null);
		writer.write(filename, signals20);
	}

	private void writeSignalGroups20(XMLSignalGroups xmlSignalGroups, String filename) {
		SignalGroupsWriter20 writer = new SignalGroupsWriter20(null);
		writer.write(filename, xmlSignalGroups);
	}

	private XMLSignalSystems readSignals11(String filename) {
		SignalSystemsReader11 reader = new SignalSystemsReader11(null, MatsimSignalSystemsReader.SIGNALSYSTEMS11);
		XMLSignalSystems xmlSignals = null;
		try {
			xmlSignals = reader.readSignalSystems11File(filename);
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return xmlSignals;
	}
	
	
	private Tuple<org.matsim.jaxb.signalsystems20.XMLSignalSystems, XMLSignalGroups> createSignalSystems20(LaneDefinitions laneDefs, XMLSignalSystems xmlSignals11){
		org.matsim.jaxb.signalsystems20.ObjectFactory signals20ObjectFactory = new org.matsim.jaxb.signalsystems20.ObjectFactory();
		org.matsim.jaxb.signalsystems20.XMLSignalSystems signals20 = signals20ObjectFactory.createXMLSignalSystems();
		org.matsim.jaxb.signalgroups20.ObjectFactory signalGroups20ObjectFactory = new org.matsim.jaxb.signalgroups20.ObjectFactory();
		org.matsim.jaxb.signalgroups20.XMLSignalGroups signalGroups20 = signalGroups20ObjectFactory.createXMLSignalGroups();
		
		//going through signalSystemDefinitions 
		for (XMLSignalSystemDefinitionType signaldefinition11 : xmlSignals11.getSignalSystemDefinition()) {
			XMLSignalSystemType xmlsstye20 = signals20ObjectFactory.createXMLSignalSystemType();
			xmlsstye20.setId(signaldefinition11.getId());
			signals20.getSignalSystem().add(xmlsstye20);
			log.warn("Defaults for cycle time, intergreen time and synchronization offset are no longer available in the 2.0 data model.");
		}
		//loop throug signalGroupDefinitions

		for (XMLSignalGroupDefinitionType signalgroupdef11 : xmlSignals11.getSignalGroupDefinition()){
			//search for the system to that the signals should be added
			String signalSystemId = signalgroupdef11.getSignalSystemDefinition().getRefId();
			XMLSignalSystemType signalSystem20 = this.findXMLSignalType(signals20, signalSystemId);
			XMLSignals signalsList20 = null;
			if (signalSystem20.getSignals() == null){
				signalsList20 = signals20ObjectFactory.createXMLSignalSystemTypeXMLSignals();
				signalSystem20.setSignals(signalsList20);
			}
			else {
				signalsList20 = signalSystem20.getSignals();
			}
			//create the signal
			XMLSignalType signal20 = signals20ObjectFactory.createXMLSignalType();
			signalsList20.getSignal().add(signal20);
			signal20.setId(signalgroupdef11.getId());
			signal20.setLinkIdRef(signalgroupdef11.getLinkIdRef());
			//create a signalGroup per signal as in singalSystems11 a 2.0 signal was a signalGroup
			XMLSignalSystemSignalGroupType signalGroupSystem20 = findSignalGroupSystem(signalGroups20.getSignalSystem(), signalSystemId);
			if (signalGroupSystem20 == null) {
				signalGroupSystem20 = signalGroups20ObjectFactory.createXMLSignalSystemSignalGroupType();
				signalGroups20.getSignalSystem().add(signalGroupSystem20);
				signalGroupSystem20.setRefId(signalSystemId);
			}
			
			XMLSignalGroupType signalGroup20 = signalGroups20ObjectFactory.createXMLSignalGroupType();
			signalGroupSystem20.getSignalGroup().add(signalGroup20);
			signalGroup20.setId(signalgroupdef11.getId());
			org.matsim.jaxb.signalgroups20.XMLIdRefType signalGroup20Id = signalGroups20ObjectFactory.createXMLIdRefType();
			signalGroup20.getSignal().add(signalGroup20Id);
			signalGroup20Id.setRefId(signalgroupdef11.getId());
			
			//set the lanes
			if (signalgroupdef11.getLane() != null) {
				for (XMLIdRefType laneId : signalgroupdef11.getLane()){
					XMLLane lane = signals20ObjectFactory.createXMLSignalTypeXMLLane();
					lane.setRefId(laneId.getRefId());
					signal20.getLane().add(lane);
				}
			}
			//set the turning move restrictions
			if (signalgroupdef11.getToLink() != null){
				LanesToLinkAssignment l2link = laneDefs.getLanesToLinkAssignments().get(new IdImpl(signal20.getLinkIdRef()));
				if (!this.checkSignalGroupToLinkEqualLaneToLink(signalgroupdef11, l2link)){
					signal20.setTurningMoveRestrictions(signals20ObjectFactory.createXMLSignalTypeXMLTurningMoveRestrictions());
					for (XMLIdRefType toLinkId : signalgroupdef11.getToLink()){
						org.matsim.jaxb.signalsystems20.XMLIdRefType toLinkId20 = signals20ObjectFactory.createXMLIdRefType();
						toLinkId20.setRefId(toLinkId.getRefId());
						signal20.getTurningMoveRestrictions().getToLink().add(toLinkId20);
					}
				}
			}
		}//end loop signalGroupDefinitions
		return new Tuple<org.matsim.jaxb.signalsystems20.XMLSignalSystems, XMLSignalGroups>(signals20, signalGroups20);
	}
	
	private XMLSignalSystemSignalGroupType findSignalGroupSystem(
			List<XMLSignalSystemSignalGroupType> signalSystem, String signalSystemId) {
		for (XMLSignalSystemSignalGroupType sgt : signalSystem){
			if (sgt.getRefId().equals(signalSystemId)){
				return sgt;
			}
		}
		return null;
	}

	private boolean checkSignalGroupToLinkEqualLaneToLink(
			XMLSignalGroupDefinitionType signalGroup, LanesToLinkAssignment l2link) {
		List<XMLIdRefType> sgToLinkIdRefTypes = signalGroup.getToLink();
		Set<Id> sgToLinkIds = new HashSet<Id>();
		for (XMLIdRefType idreftype : sgToLinkIdRefTypes){
			sgToLinkIds.add(new IdImpl(idreftype.getRefId()));
		}
		
		Set<Id> laneToLinksIds = new HashSet<Id>();
		for (XMLIdRefType sgLaneId : signalGroup.getLane()){
			Lane lane = l2link.getLanes().get(new IdImpl(sgLaneId.getRefId()));
			laneToLinksIds.addAll(lane.getToLinkIds());
		}
		
		return sgToLinkIds.containsAll(laneToLinksIds) && laneToLinksIds.containsAll(sgToLinkIds);
	}

	private XMLSignalSystemType findXMLSignalType(org.matsim.jaxb.signalsystems20.XMLSignalSystems xmlSignals20, String signalSystemId){
		for (XMLSignalSystemType xmlsignal : xmlSignals20.getSignalSystem()){
			if (xmlsignal.getId().equals(signalSystemId)){
				return xmlsignal;
			}
		}
		return null;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String base = "./test/input/org/matsim/";
		//one agent test
		String inputDir = base + "signalsystems/SignalSystemsOneAgentTest/";
		String lanedefinitions11 = inputDir + "testLaneDefinitions_v1.1.xml";
		String signalSystems = inputDir + "testSignalSystems_v1.1.xml";
		String signalSystems20 = inputDir + "testSignalSystems_v2.0.xml";
		String signalGroups20 = inputDir + "testSignalGroups_v2.0.xml";
		new SignalSystems11To20Converter().convert(lanedefinitions11, signalSystems, signalSystems20, signalGroups20);
		
		//travel time one way test
		inputDir = base + "signalsystems/TravelTimeOneWayTest/";
		lanedefinitions11 = inputDir + "testLaneDefinitions_v1.1.xml";
		signalSystems = inputDir + "testSignalSystems_v1.1.xml";
		signalSystems20 = inputDir + "testSignalSystems_v2.0.xml";
		signalGroups20 = inputDir + "testSignalGroups_v2.0.xml";
		new SignalSystems11To20Converter().convert(lanedefinitions11, signalSystems, signalSystems20, signalGroups20);
		
		//travel time four ways test
		inputDir = base + "signalsystems/TravelTimeFourWaysTest/";
		lanedefinitions11 = inputDir + "testLaneDefinitions_v1.1.xml";
		signalSystems = inputDir + "testSignalSystems_v1.1.xml";
		signalSystems20 = inputDir + "testSignalSystems_v2.0.xml";
		signalGroups20 = inputDir + "testSignalGroups_v2.0.xml";
		new SignalSystems11To20Converter().convert(lanedefinitions11, signalSystems, signalSystems20, signalGroups20);
		
		//signalsystems integration test
		inputDir = base + "integration/signalsystems/SignalSystemsIntegrationTest/";
		lanedefinitions11 = inputDir + "testLaneDefinitions_v1.1.xml";
		signalSystems = inputDir + "testSignalSystems_v1.1.xml";
		signalSystems20 = inputDir + "testSignalSystems_v2.0.xml";
		signalGroups20 = inputDir + "testSignalGroups_v2.0.xml";
		new SignalSystems11To20Converter().convert(lanedefinitions11, signalSystems, signalSystems20, signalGroups20);

		
	}


}
