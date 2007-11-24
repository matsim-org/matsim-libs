/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationNetFileReader.java
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

package playground.gregor.evacuation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author glaemmel
 *
 */

//////////////////////////////////////////////////////////////////////
//EvacuationNetFileReader reads the desaster area links from xml and
//put them into a HashMap
//////////////////////////////////////////////////////////////////////
public class EvacuationNetFileReader extends MatsimXmlParser {

	public static final String XML_ROOT = "desasterarea";
	public static final String XML_LINK = "link";
	public static final String XML_ID = "id";
	public static final String XML_DEADLINE = "deadline";

	HashMap<IdI, EvacuationAreaLink> links;

	public EvacuationNetFileReader(HashMap<IdI, EvacuationAreaLink> links) {
		super();
		this.links = links;
	}

	public void readFile(String filename) throws SAXException, ParserConfigurationException, IOException {
		this.parse(filename);
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (XML_LINK.equals(name)) {
			handleLink(atts.getValue(XML_ID),atts.getValue(XML_DEADLINE));

		} else if (XML_ROOT.equals(name)) {
		} else {
			Gbl.errorMsg(this + "[tag=" + name + " not known]");
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {

	}

	private void handleLink(String id, String deadline){
		EvacuationAreaLink link = new EvacuationAreaLink(id,Gbl.parseTime(deadline));
		links.put(new Id(id),link);

	}

}
