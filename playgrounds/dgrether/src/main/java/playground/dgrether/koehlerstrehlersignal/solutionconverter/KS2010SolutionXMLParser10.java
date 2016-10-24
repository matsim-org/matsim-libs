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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;


/**
 * @author dgrether
 *
 */
public class KS2010SolutionXMLParser10 extends MatsimXmlParser {
	
	private static final Logger log = Logger.getLogger(KS2010SolutionXMLParser10.class);
	
	/** A constant for the exactness when comparing doubles. */
	private static final double EPSILON = 1e-10;

	
//	private final static String VARIABLES = "variables";
	private final static String VARIABLE = "variable";
	private final static String NAME = "name";
	private final static String VALUE = "value";
	
	private List<KS2010CrossingSolution> solutionCrossingByIdMap = new ArrayList<KS2010CrossingSolution>();
	
	public KS2010SolutionXMLParser10() {
		this.setValidating(false);
	}
	
	public List<KS2010CrossingSolution> getSolutionCrossingByIdMap(){
		return this.solutionCrossingByIdMap;
	}
	
//	public void readFile(final String filename) {
//		this.setValidating(false);
//		readFile(filename);
//		log.info("Read " + solutionCrossingByIdMap.size() + " solutions");
//	}
	
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
					Id<DgCrossing> crossingString = Id.create(nameParts[1], DgCrossing.class);
					Id<DgProgram> programString = Id.create(nameParts[2], DgProgram.class);
					int offsetSeconds = Integer.parseInt(nameParts[3]);
					KS2010CrossingSolution crossing = new KS2010CrossingSolution(crossingString);
					crossing.addOffset4Program(programString, offsetSeconds);
					this.solutionCrossingByIdMap.add(crossing);
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
