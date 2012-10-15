/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.singapore.fcl.parkopedia;

import java.util.Stack;

import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class ParkopediaReaderV1 extends MatsimXmlParser implements MatsimSomeReader {

	boolean priceFound=false;
	
	public ParkopediaReaderV1() {
		super(false);
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		
		/*
		if (name.equalsIgnoreCase("price")) {
			String hours = atts.getValue("hours");
			System.out.println(hours + "->" + atts.getValue("amount") + "->" + atts.getValue("duration"));
		
			if (hours.contains("Mon-Fri 8:00-18:00") || hours.contains("Mon-Fri 8:00-17:00")){
				
			}
		
		}
		
		*/
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		/*
		if (name.equalsIgnoreCase("id")) {
			System.out.println("id:" + content);
		}
		
		if (name.equalsIgnoreCase("space")) {
			System.out.println("=============");
		}
		*/
	}

}
