/* *********************************************************************** *
 * project: org.matsim.*
 * KS2010SolutionTXTParser10
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;


/**
 * Deprecated since BTU changed their solution format in spring 2014. The new parser is KS2014SolutionXMLParser.java
 * 
 * @author dgrether
 * @author tthunig
 *
 */
@Deprecated
public class KS2010SolutionTXTParser10 {
	
	private static final Logger log = Logger.getLogger(KS2010SolutionTXTParser10.class);
	
	private List<KS2010CrossingSolution> solutions;
	private Map<Id<Link>, Double> streetFlow;
	
	public void readFile(String filename) {
		solutions = new ArrayList<KS2010CrossingSolution>();
		streetFlow = new HashMap<Id<Link>, Double>();
		BufferedReader br = IOUtils.getBufferedReader(filename);
		String line;
		try {
			line = br.readLine();
			while (line != null) {
				processLine(line);
				line = br.readLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void parseKreuzungLine(String line) {
		String[] s = line.split(" ");
		Id<DgCrossing> crossingId = Id.create(s[1], DgCrossing.class);
		Id<DgProgram> programId = Id.create(s[4], DgProgram.class);
		int offset = Integer.valueOf(s[7]);
		log.debug("Crossing " + crossingId + " program " + programId + " offset " + offset);
		KS2010CrossingSolution c = new KS2010CrossingSolution(crossingId);
		this.solutions.add(c);
		c.addOffset4Program(programId, offset);
	}

	private void parseStrasseLine(String line) {
		String[] s = line.split(" ");
		Id<Link> streetId = Id.create(s[1], Link.class);
		double commodityFlow = Double.valueOf(s[4]);
		log.debug("Street " + streetId + " flow of one commodity " + commodityFlow);
		if (!this.streetFlow.containsKey(streetId))
			this.streetFlow.put(streetId, 0.0);
		// add the flow of this commodity to the street flow
		this.streetFlow.put(streetId, this.streetFlow.get(streetId) + commodityFlow);
	}

	private void processLine(String line) {
		line = line.trim();
		if (line.startsWith("Kreuzung ") && line.endsWith("zugewiesen bekommen.")) {
			log.info("parsing line: " + line);
			this.parseKreuzungLine(line);
		}
		else if (line.startsWith("Strasse ")){
			log.info("parsing line: " + line);
			this.parseStrasseLine(line);
		}
		else {
			log.warn("Ignoring line : " + line);
		}
	}

	public List<KS2010CrossingSolution> getSolutionCrossings() {
		return solutions;
	}

	public Map<Id<Link>, Double> getStreetFlow() {
		return streetFlow;
	}
	
}
