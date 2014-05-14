/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ExperimentResource.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.populationsize;

import playground.mzilske.cdranalysis.FileIO;
import playground.mzilske.cdranalysis.Reading;
import playground.mzilske.cdranalysis.StreamingOutput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


class ExperimentResource {

	private final String wd;

	public ExperimentResource(String wd) {
		this.wd = wd;
	}

	public Collection<String> getRegimes() {
		final Set<String> REGIMES = new HashSet<String>();
		REGIMES.add("uncongested");
		REGIMES.add("congested");
		return REGIMES;
	}

	public RegimeResource getRegime(String regime) {
		return new RegimeResource(wd + "regimes/" + regime, regime);
	}

	public void personKilometers() {	
		FileIO.writeToFile(wd + "person-kilometers.txt", new StreamingOutput() {
			@Override
			public void write(final PrintWriter pw) throws IOException {
				boolean first = true;
				for (final String regime : getRegimes()) {
					final boolean first2 = first;
					FileIO.readFromResponse(getRegime(regime).getMultiRateRun("regular").getPersonKilometers(), new Reading() {
						@Override
						public void read(BufferedReader br) throws IOException {
							String header = br.readLine();
							if (first2) {
								pw.println(header);
							}
							String line = br.readLine();
							while (line != null) {
								pw.println(line);
								line = br.readLine();
							}
						}
					});
					first = false;
				}			
			}
		});
	}
}
