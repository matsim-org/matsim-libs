/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.io;

import java.util.Set;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.mid.LegDistanceHandler;
import playground.johannes.gsv.synPop.mid.MIDKeys;

/**
 * @author johannes
 *
 */
public class ParserTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		XMLParser parser = new XMLParser();
		parser.setValidating(false);
		parser.addSerializer(MIDKeys.LEG_DISTANCE, new LegDistanceHandler());
		parser.parse("/home/johannes/gsv/mid2008/pop.xml");
		
		Set<ProxyPerson> persons = parser.getPersons();
		
		XMLWriter writer = new XMLWriter();
		writer.addSerializer(MIDKeys.LEG_DISTANCE, new LegDistanceHandler());
		writer.write("/home/johannes/gsv/mid2008/pop2.xml", persons);

	}

}
