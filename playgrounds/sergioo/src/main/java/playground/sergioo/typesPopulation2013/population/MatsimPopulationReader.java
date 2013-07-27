/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimPopulationReader.java
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

package playground.sergioo.typesPopulation2013.population;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.Attributes;

/**
 * A reader for plans-files of MATSim. This reader recognizes the format of the plans-file and uses
 * the correct reader for the specific plans-version, without manual setting.
 *
 * @author mrieser
 */
public class MatsimPopulationReader extends MatsimXmlParser implements PopulationReader {

	private final static String POPULATION_V5 = "population_v5.dtd";
	private final static String POPULATION_POPS = "population_pops.dtd";

	private MatsimXmlParser delegate = null;
	private final Scenario scenario;

	private static final Logger log = Logger.getLogger(MatsimPopulationReader.class);

	public MatsimPopulationReader(final Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		this.delegate.startTag(name, atts, context);
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		this.delegate.endTag(name, content, context);
	}

	/**
	 * Parses the specified plans file. This method calls {@link #parse(String)}, but handles all
	 * possible exceptions on its own.
	 *
	 * @param filename The name of the file to parse.
	 * @throws UncheckedIOException
	 */
	@Override
	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
	}

	@Override
	protected void setDoctype(final String doctype) {
		if(doctype.equals(POPULATION_V5))
			super.setDoctype(POPULATION_POPS);
		else
			super.setDoctype(doctype);
		if (POPULATION_POPS.equals(getDoctype())) {
			this.delegate = new PopulationReaderMatsimPops(this.scenario);
			log.info("using population_pops-reader.");
		}else {
			throw new IllegalArgumentException("Doctype \"" + getDoctype() + "\" not known.");
		}
	}

}
