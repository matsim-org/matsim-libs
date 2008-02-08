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
package org.matsim.trafficlights;

import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;


/**
 * @author dgrether
 *
 */
public class SignalGroupDefinitionParser extends MatsimXmlParser {

	private static final Logger log = Logger.getLogger(SignalGroupDefinitionParser.class);

	private static final String SIGNALGROUP = "signalGroup";

	private static final String  FROMLINK = "fromLink";

	private static final String TOLINK = "toLink";

	private static final String TURNIFRED = "turnIfRed";

	private static final String PASSINGCLEARINGTIME = "passingClearingTime";

	private static final String ID = "id";

	private static final String REFID = "refId";

	private List<SignalGroupDefinition> signalGroups;

	private SignalGroupDefinition currentSignalGroup;

	/**
	 *
	 */
	public SignalGroupDefinitionParser(List<SignalGroupDefinition> signalGroups) {
		this.signalGroups = signalGroups;
		this.setNamespaceAware(true);
	}

	/**
	 * @see org.matsim.utils.io.MatsimXmlParser#endTag(java.lang.String, java.lang.String, java.util.Stack)
	 */
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (TURNIFRED.equalsIgnoreCase(name)) {
			this.currentSignalGroup.setTurnIfRed("true".compareToIgnoreCase(content.trim()) == 0);
		}
		else if (PASSINGCLEARINGTIME.equalsIgnoreCase(name)) {
			this.currentSignalGroup.setPassingClearingTime(Integer.valueOf(content.trim()));
		}
		else if (SIGNALGROUP.equalsIgnoreCase(name)) {
			this.signalGroups.add(this.currentSignalGroup);
		}

	}

	/**
	 * @see org.matsim.utils.io.MatsimXmlParser#startTag(java.lang.String, org.xml.sax.Attributes, java.util.Stack)
	 */
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (SIGNALGROUP.equalsIgnoreCase(name)) {
			this.currentSignalGroup = new SignalGroupDefinition(new Id(atts.getValue(ID)));
		}
		else if (FROMLINK.equalsIgnoreCase(name)) {
			this.currentSignalGroup.setFromLink(new Id(atts.getValue(REFID)));
		}
		else if (TOLINK.equalsIgnoreCase(name)) {
			this.currentSignalGroup.addToLink(new Id(atts.getValue(REFID)));
		}


	}

}
