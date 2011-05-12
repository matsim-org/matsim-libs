/* *********************************************************************** *
 * project: org.matsim.*
 * DgSolutionParser
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
package playground.dgrether.koehlerstrehlersignal.solutionconverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;


/**
 * @author dgrether
 *
 */
public class DgSolutionParser extends MatsimXmlParser {
	
	/** A constant for the exactness when comparing doubles. */
	private static final double EPSILON = 1e-10;

	
//	private final static String VARIABLES = "variables";
	private final static String VARIABLE = "variable";
	private final static String NAME = "name";
	private final static String VALUE = "value";
	
	private Map<Id, DgSolutionCrossing> solutionCrossingByIdMap = new HashMap<Id, DgSolutionCrossing>();
	
	public Map<Id, DgSolutionCrossing> getSolutionCrossingByIdMap(){
		return this.solutionCrossingByIdMap;
	}
	
	public void readFile(final String filename) {
		this.setValidating(false);
		parse(filename);
	}
	
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		
	}

	@Override
	public void startTag(String elementName, Attributes atts, Stack<String> context) {
		if (elementName.equals(VARIABLE)){
			String name = atts.getValue(NAME);
			if (name.startsWith("B")){
				double value = Double.parseDouble(atts.getValue(VALUE));
				if (this.compareDouble(1.0, value, EPSILON)){
					String[] nameParts = name.split("#");
					String crossingString = nameParts[1];
					String programString = nameParts[2];
					int offsetSeconds = Integer.parseInt(nameParts[3]);
					Id crossingId = new IdImpl(crossingString);
					DgSolutionCrossing crossing = new DgSolutionCrossing(crossingId);
					crossing.addOffset4Program(new IdImpl(programString), offsetSeconds);
					this.solutionCrossingByIdMap.put(crossingId, crossing);
				}
			}
		}
	}

	private boolean compareDouble(double reference, double compare, double delta){
		if (Double.compare(reference, compare) == 0)
			return true;
		if (!(Math.abs(reference-compare) <= delta)){
			return false;
		}
		return true;
	}
	
}
