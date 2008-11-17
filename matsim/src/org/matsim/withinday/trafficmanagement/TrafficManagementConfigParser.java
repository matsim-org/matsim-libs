/* *********************************************************************** *
 * project: org.matsim.*
 * TrafficManagementConfigParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmanagement;
import java.util.ArrayList;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.config.groups.SimulationConfigGroup;
import org.matsim.events.Events;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.population.RouteImpl;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.withinday.trafficmanagement.controlinput.ControlInputImpl1;
import org.matsim.withinday.trafficmanagement.controlinput.ControlInputMB;
import org.matsim.withinday.trafficmanagement.controlinput.ControlInputSB;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.BangBangControler;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.FeedbackControler;
import org.matsim.withinday.trafficmanagement.feedbackcontroler.NoControl;
import org.xml.sax.Attributes;



public class TrafficManagementConfigParser extends MatsimXmlParser {

	private final static Logger log = Logger.getLogger(TrafficManagementConfigParser.class);

	private final static String TRAFFICMANAGEMENT = "trafficmanagement";

	private final static String GUIDANCEDEVICE = "guidanceDevice";

	private final static String CONTROLINPUTCLASS = "controlInputClass";

	private final static String MESSAGEHOLDTIME = "messageHoldTime";

	private final static String CONTROLEVENTS = "controlEvents";

	private final static String NOMINALSPLITTING = "nominalSplitting";

	private final static String DEADZONESYSOUT = "deadZoneSystemOutput";

	private final static String DEADZONESYSIN = "deadZoneSystemInput";

	private final static String SIGNLINK = "signLink";

	private final static String DIRECTIONLINKS = "directionLink";

	private final static String COMPLIANCE = "compliance";

	private final static String BENEFITCONTROL = "benefitControl";

	private final static String MAINROUTE = "mainRoute";

	private final static String ALTERNATIVEROUTE = "alternativeRoute";

	private final static String NODE = "node";

	private final static String ID = "id";

	private final static String FEEDBACKCONTROLER = "feedBackControler";

	private static final String BANGBANGCONTROLER = "BangBangControler";

//	private static final String CONSTANTCONTROLER = "ConstantControler";

	private static final String NOCONTROL = "NoControl";

//	private static final String PCONTROLER = "PControler";

//	private static final String PIDCONTROLER = "PIDControler";


	private static final String CONTROLINPUT1 = "ControlInputImpl1";

	private static final String CONTROLINPUTSB = "ControlInputSB";

	private static final String CONTROLINPUTMB = "ControlInputMB";

	private final static String CONTROLINPUT = "controlInput";

	private final static String OUTPUT = "output";

	private final static String SPREADSHEETFILE = "spreadsheetfile";

	private final static String IGNOREDQUEUINGTIME = "ignoredQueuingTime";

	private static final String DISTRIBUTIONCHECKACTIVATED = "distributionCheckActivated";

	private static final String BACKGROUNDNOISECOMPENSATIONACTIVATED = "backgroundNoiseCompensationActivated";

	private static final String INCIDENTDETECTEIONACTIVATED = "incidentDetectionActivated";

	private static final String UPDATETIMEINOUTFLOW = "updateTimeInOutFlow";

	private static final String RESETBOTTLENECKINTERVALL = "resetBottleNeckIntervall";

	private static final String NUMBEROFEVENTSDETECTION = "numberOfEventsDetection";

	private TrafficManagement trafficManagement;

	private VDSSign vdsSign;

	private ArrayList<Node> currentRouteNodes;

	private NetworkLayer network;

	private ControlInput controlInput;

	private Events events;

	private VDSSignOutput vdsSignOutput;

	private SimulationConfigGroup simulationConfig;


	public TrafficManagementConfigParser(final NetworkLayer network,
			final Events events, SimulationConfigGroup simulationConfigGroup) {
		this.network = network;
		this.events = events;
		this.simulationConfig = simulationConfigGroup;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (name.equalsIgnoreCase(TRAFFICMANAGEMENT)) {
			this.trafficManagement = new TrafficManagement();
		}
		else if (name.equalsIgnoreCase(GUIDANCEDEVICE)) {
			this.vdsSign = new VDSSign();
		}
		else if (name.equalsIgnoreCase(MAINROUTE) || name.equalsIgnoreCase(ALTERNATIVEROUTE)) {
			this.currentRouteNodes = new ArrayList<Node>();
		}
		else if (name.equalsIgnoreCase(NODE)) {
			String id = atts.getValue(ID);
			Node node = this.network.getNode(id);
			if (node != null) {
				this.currentRouteNodes.add(node);
			}
			else {
				throw new RuntimeException("The Node with the id: " + id
						+ " could not be found in the network");
			}
		}
		else if (name.equalsIgnoreCase(OUTPUT)) {
			this.vdsSignOutput = new VDSSignOutput();
		}
		else if (name.equalsIgnoreCase(IGNOREDQUEUINGTIME)) {
			double time = Double.parseDouble(atts.getValue("sec"));
			if (this.controlInput instanceof ControlInputSB) {
				((ControlInputSB)this.controlInput).setIgnoredQueuingTime(time);
			}
			else if (this.controlInput instanceof ControlInputMB) {
				((ControlInputMB)this.controlInput).setIgnoredQueuingTime(time);
			}
			else {
				throw new IllegalArgumentException(name + " can not be used with set ControlInput class!");
			}
		}
		else if (name.equalsIgnoreCase(DISTRIBUTIONCHECKACTIVATED)) {
			boolean b = Boolean.parseBoolean(atts.getValue("value"));
			if (this.controlInput instanceof ControlInputSB) {
				((ControlInputSB)this.controlInput).setDistributionCheckActive(b);
			}
			else {
				throw new IllegalArgumentException(name + " can not be used with set ControlInput class!");
			}
		}
		else if (name.equalsIgnoreCase(BACKGROUNDNOISECOMPENSATIONACTIVATED)) {
			boolean b = Boolean.parseBoolean(atts.getValue("value"));
			if (this.controlInput instanceof ControlInputSB) {
				((ControlInputSB)this.controlInput).setBackgroundnoiseCompensationActive(b);
			}
			else {
				throw new IllegalArgumentException(name + " can not be used with set ControlInput class!");
			}
		}
		else if (name.equalsIgnoreCase(INCIDENTDETECTEIONACTIVATED)) {
			boolean b = Boolean.parseBoolean(atts.getValue("value"));
			if (this.controlInput instanceof ControlInputSB) {
				((ControlInputSB)this.controlInput).setIncidentDetectionActive(b);
			}
			else {
				throw new IllegalArgumentException(name + " can not be used with set ControlInput class!");
			}
		}
		else if (UPDATETIMEINOUTFLOW.equalsIgnoreCase(name)) {
			double time = Double.parseDouble(atts.getValue("sec".intern()));
			if (this.controlInput instanceof ControlInputMB) {
				((ControlInputMB)this.controlInput).setUpdateTimeInOutFlow(time);
			}
		}
		else if (RESETBOTTLENECKINTERVALL.equalsIgnoreCase(name)) {
			double time = Double.parseDouble(atts.getValue("sec".intern()));
			if (this.controlInput instanceof ControlInputMB) {
				((ControlInputMB)this.controlInput).setResetBottleNeckIntervall(time);
			}
		}
		else if (NUMBEROFEVENTSDETECTION.equalsIgnoreCase(name)) {
			int events = Integer.parseInt(atts.getValue("value".intern()));
			if (this.controlInput instanceof ControlInputMB) {
				((ControlInputMB)this.controlInput).setNumberOfEventsDetection(events);
			}

		}

	}
	@Override
	public void endTag(final String name, String content, final Stack<String> context) {
		content = content.trim();
		if (name.equalsIgnoreCase(MAINROUTE)) {
			Route route = new RouteImpl();
			route.setRoute(this.currentRouteNodes);
			this.controlInput.setMainRoute(route);
		} else if (name.equalsIgnoreCase(ALTERNATIVEROUTE)) {
			Route route = new RouteImpl();
			route.setRoute(this.currentRouteNodes);
			this.controlInput.setAlternativeRoute(route);
		}
		else if (name.equalsIgnoreCase(CONTROLINPUTCLASS)) {
			this.controlInput = createControlInput(content);
			this.vdsSign.setControlInput(this.controlInput);
		}
		else if (name.equalsIgnoreCase(FEEDBACKCONTROLER)) {
			FeedbackControler controler = createControlTheoryControler(content);
			this.vdsSign.setControler(controler);
		}
		else if (name.equalsIgnoreCase(MESSAGEHOLDTIME)) {
			this.vdsSign.setMessageHoldTime(Integer.valueOf(content));
		}
		else if (name.equalsIgnoreCase(CONTROLEVENTS)) {
			this.vdsSign.setControlEvents(Integer.valueOf(content));
		}
		else if (name.equalsIgnoreCase(NOMINALSPLITTING)) {
			this.vdsSign.setNominalSplitting(Double.parseDouble(content));
		}
		else if (name.equalsIgnoreCase(DEADZONESYSIN)) {
			this.vdsSign.setDeadZoneSystemInput(Double.parseDouble(content));
		}
		else if (name.equalsIgnoreCase(DEADZONESYSOUT)) {
			this.vdsSign.setDeadZoneSystemOutput(Double.parseDouble(content));
		}
		else if (name.equalsIgnoreCase(SIGNLINK)) {
			Link l = this.network.getLink(content);
			if (l != null) {
				this.vdsSign.setSignLink(l);
			}
			else {
				throw new IllegalArgumentException("the sign link of the sign could not be found in the network!");
			}
		}
		else if (name.equalsIgnoreCase(DIRECTIONLINKS)) {
			Link l = this.network.getLink(content);
			if (l != null) {
				this.vdsSign.setDirectionLink(l);
			}
			else {
				throw new IllegalArgumentException("the direction link of the sign could not be found in the network!");
			}
		}
		else if (name.equalsIgnoreCase(COMPLIANCE)) {
			this.vdsSign.setCompliance(Double.parseDouble(content));
		}
		else if (name.equalsIgnoreCase(BENEFITCONTROL)) {
			this.vdsSign.setBenefitControl(Boolean.parseBoolean(content));
		}
		else if (name.equalsIgnoreCase(GUIDANCEDEVICE)) {
			this.trafficManagement.addVDSSign(this.vdsSign);
		}
		else if (name.equalsIgnoreCase(CONTROLINPUT)) {
			this.controlInput.init();
		}
		else if (name.equalsIgnoreCase(SPREADSHEETFILE)) {
			this.vdsSignOutput.setSpreadsheetFile(content);
		}
		else if (name.equalsIgnoreCase(OUTPUT)) {
			this.vdsSign.setOutput(this.vdsSignOutput);
		}
	}

	private FeedbackControler createControlTheoryControler(final String content) {
		if (content.compareTo(BANGBANGCONTROLER) == 0) {
			return new BangBangControler();
		}
		else if (content.compareTo(NOCONTROL) == 0) {
			return new NoControl();
		}
		throw new IllegalArgumentException("ControlTheoryControler of xml is not known in the TrafficManagementConfiguration.");
	}

	private ControlInput createControlInput(final String content) {
		if (content.trim().compareTo(CONTROLINPUT1) == 0) {
			ControlInputImpl1 controlInput = new ControlInputImpl1();
			this.events.addHandler(controlInput);
			return controlInput;
		}
		else if (content.trim().compareTo(CONTROLINPUTSB) == 0) {
			ControlInputSB controlInput = new ControlInputSB(this.simulationConfig);
			controlInput.setNetworkChangeEvents(this.network.getNetworkChangeEvents());
			this.events.addHandler(controlInput);
			return controlInput;
		}
		else if (content.trim().compareTo(CONTROLINPUTMB) == 0) {
			ControlInputMB controlInput = new ControlInputMB(this.simulationConfig);
			controlInput.setNetworkChangeEvents(this.network.getNetworkChangeEvents());
			this.events.addHandler(controlInput);
			return controlInput;
		}

		throw new IllegalArgumentException("The ControlInput of xml is not known in the TrafficManagementConfiguration.");
	}

	public TrafficManagement getTrafficManagement() {
		if (this.trafficManagement == null) {
			throw new RuntimeException("call parse(...) first!");
		}
		return this.trafficManagement;
	}
}
