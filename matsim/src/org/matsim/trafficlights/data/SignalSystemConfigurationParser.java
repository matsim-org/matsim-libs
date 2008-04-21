/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.trafficlights.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;


/**
 * Parser for xml files of schema signalSystemConfiguration.xsd
 * @author dgrether
 *
 */
public class SignalSystemConfigurationParser extends MatsimXmlParser {

	private static final Logger log = Logger.getLogger(SignalSystemConfigurationParser.class);

	private static final String SIGNALSYSTEMConfiguration = "signalSystemConfiguration";

	private static final String SIGNALSYSTEMCONTROLInfo = "signalSystemControlInfo";

	private static final String SIGNALSYSTEMPLAN = "signalSystemPlan";

	private static final String START = "start";

	private static final String STOP = "stop";

	private static final String CIRCULATIONTIME = "circulationTime";

	private static final String SYNCTIME = "syncTime";

	private static final String ID = "id";

	private static final String REFID = "refId";

	private static final String POWERONTIME = "powerOnTime";

	private static final String POWEROFFTIME = "powerOffTime";

	private static final String SIGNALGROUPSETTINGS = "signalGroupSettings";

	private static final String ROUGHCAST = "roughCast";

	private static final String DROPPING = "dropping";

	private static final String INTERIMTIMEROUGHCAST = "interimTimeRoughcast";

	private static final String INTERIMTIMEDROPPING = "interimTimeDropping";

	private static final String DAYTIME = "daytime";

	private static final String SEC = "sec";

	private static final String SCHEMANS = "http://www.w3.org/2001/XMLSchema-instance";

	private static final String TYPE = "type";

	private static final String PLANBASEDCONTROL = "planbasedSignalSystemControlInfoType";

	private static final String ADAPTIVECONTROL = "adaptiveSignalSystemControlInfoType";

	private static final String ADAPTIVEPLANBASEDCONTROL = "adaptivePlanbasedSignalSystemControlInfoType";

	private SignalSystemConfiguration currentSignalSystem;

	private SignalSystemPlan currentSignalPlan;

	private SignalGroupSettings currentSignalGroupSettings;

	private Map<Id, SignalGroupDefinition> signalGroupDefinitions;

	private Map<Id, SignalSystemConfiguration> signalSystemConfigurations;

	private SignalSystemControlInfo currentSignalSystemControler;

	public SignalSystemConfigurationParser(List<SignalGroupDefinition> signalGroups) {
		this.signalGroupDefinitions = new HashMap<Id, SignalGroupDefinition>(signalGroups.size());
		for (SignalGroupDefinition s : signalGroups) {
			this.signalGroupDefinitions.put(s.getId(), s);
		}
		this.signalSystemConfigurations = new HashMap<Id, SignalSystemConfiguration>();
	}


	/**
	 * @see org.matsim.utils.io.MatsimXmlParser#endTag(java.lang.String, java.lang.String, java.util.Stack)
	 */
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (SIGNALGROUPSETTINGS.equalsIgnoreCase(name)) {
			this.currentSignalPlan.addSignalGroupSettings(this.currentSignalGroupSettings);
			this.currentSignalGroupSettings = null;
		}
		else if (SIGNALSYSTEMPLAN.equalsIgnoreCase(name)) {
			((PlanbasedSignalSystemControlInfo)this.currentSignalSystemControler).addSignalSystemPlan(this.currentSignalPlan);
			this.currentSignalPlan = null;
		}
		else if (SIGNALSYSTEMConfiguration.equalsIgnoreCase(name)) {
			this.signalSystemConfigurations.put(this.currentSignalSystem.getId(), this.currentSignalSystem);
			this.currentSignalSystem = null;
		}
		else if (SIGNALSYSTEMCONTROLInfo.equalsIgnoreCase(name)) {
			this.currentSignalSystem.setSignalSystemControler(this.currentSignalSystemControler);
			this.currentSignalSystemControler = null;
		}

	}

	/**
	 * @see org.matsim.utils.io.MatsimXmlParser#startTag(java.lang.String, org.xml.sax.Attributes, java.util.Stack)
	 */
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (SIGNALSYSTEMConfiguration.equalsIgnoreCase(name)) {
			this.currentSignalSystem = new SignalSystemConfiguration(new IdImpl(atts.getValue(ID)));
		}
		else if (SIGNALSYSTEMCONTROLInfo.equalsIgnoreCase(name)) {
			if (PLANBASEDCONTROL.equalsIgnoreCase(atts.getValue(SCHEMANS, TYPE))) {
				this.currentSignalSystemControler = new PlanbasedSignalSystemControlInfoImpl();
			}
			else if (ADAPTIVECONTROL.equalsIgnoreCase(atts.getValue(SCHEMANS, TYPE))) {
				throw new UnsupportedOperationException("Adaptive traffic light system control has to be implemented!");
			}
			else if (ADAPTIVEPLANBASEDCONTROL.equalsIgnoreCase(atts.getValue(SCHEMANS, TYPE))) {
				throw new UnsupportedOperationException("Adaptive traffic light system control has to be implemented!");
			}
		}
		else if (SIGNALSYSTEMPLAN.equalsIgnoreCase(name)) {
			this.currentSignalPlan = new SignalSystemPlan(new IdImpl(atts.getValue(ID)));
		}
		else if (START.equalsIgnoreCase(name)) {
			this.currentSignalPlan.setStartTime(Time.parseTime(atts.getValue(DAYTIME)));
		}
		else if (STOP.equalsIgnoreCase(name)) {
			this.currentSignalPlan.setStopTime(Time.parseTime(atts.getValue(DAYTIME)));
		}
		else if (CIRCULATIONTIME.equalsIgnoreCase(name)) {
			this.currentSignalPlan.setCirculationTime(Integer.parseInt(atts.getValue(SEC)));
		}
		else if (SYNCTIME.equalsIgnoreCase(name)) {
			this.currentSignalPlan.setSyncTime(Integer.parseInt(atts.getValue(SEC)));
		}
		else if (POWERONTIME.equalsIgnoreCase(name)) {
			this.currentSignalPlan.setPowerOnTime(Integer.parseInt(atts.getValue(SEC)));
		}
		else if (POWEROFFTIME.equalsIgnoreCase(name)) {
			this.currentSignalPlan.setPowerOffTime(Integer.parseInt(atts.getValue(SEC)));
		}
		else if (SIGNALGROUPSETTINGS.equalsIgnoreCase(name)) {
			SignalGroupDefinition def = this.signalGroupDefinitions.get(new IdImpl(atts.getValue(REFID)));
			if (def != null) {
				this.currentSignalGroupSettings = new SignalGroupSettings(def);
			}
			else {
				this.currentSignalGroupSettings = null;
				log.error("SignalGroupDefinition " +
						"with Id: " + atts.getValue(REFID) + " could not be found! Plans can only reference signal group settings, which are defined in the signal group definition file.");
				throw new RuntimeException("SignalGroupDefinition " +
						"with Id: " + atts.getValue(REFID) + " could not be found! Plans can only reference signal group settings, which are defined in the signal group definition file.");			}
		}
		else if (ROUGHCAST.equalsIgnoreCase(name)) {
			this.currentSignalGroupSettings.setRoughCast(Integer.parseInt(atts.getValue(SEC)));
		}
		else if (DROPPING.equalsIgnoreCase(name)) {
			this.currentSignalGroupSettings.setDropping(Integer.parseInt(atts.getValue(SEC)));
		}
		else if (INTERIMTIMEDROPPING.equalsIgnoreCase(name)) {
			this.currentSignalGroupSettings.setInterimTimeDropping(Integer.parseInt(atts.getValue(SEC)));
		}
		else if (INTERIMTIMEROUGHCAST.equalsIgnoreCase(name)) {
			this.currentSignalGroupSettings.setInterimTimeRoughcast(Integer.parseInt(atts.getValue(SEC)));
		}
	}

	public Map<Id, SignalSystemConfiguration> getSignalSystemConfigurations() {
		return this.signalSystemConfigurations;
	}

}
