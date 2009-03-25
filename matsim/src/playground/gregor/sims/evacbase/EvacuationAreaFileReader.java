/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationAreaFileReader.java
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

package playground.gregor.sims.evacbase;

import java.io.IOException;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Reads the evacuation area links from an xml file and puts them
 * into a Map (preferably a HashMap).
 *
 * @author glaemmel
 */
public class EvacuationAreaFileReader extends MatsimXmlParser {

	public static final String XML_ROOT = "evacuationarea";
	public static final String XML_LINK = "link";
	public static final String XML_ID = "id";
	public static final String XML_DEADLINE = "deadline";

	Map<Id, EvacuationAreaLink> links;

	public EvacuationAreaFileReader(final Map<Id, EvacuationAreaLink> links) {
		super();
		this.links = links;
	}

	public void readFile(final String filename) throws SAXException, ParserConfigurationException, IOException {
		this.parse(filename);
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (XML_LINK.equals(name)) {
			handleLink(atts.getValue(XML_ID),atts.getValue(XML_DEADLINE));

		} else if (!XML_ROOT.equals(name)) {
			Gbl.errorMsg(this + "[tag=" + name + " not known]");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
	}

	private void handleLink(final String id, final String deadline){
		EvacuationAreaLink link = new EvacuationAreaLink(id,Time.parseTime(deadline));
		this.links.put(new IdImpl(id),link);
	}

}
