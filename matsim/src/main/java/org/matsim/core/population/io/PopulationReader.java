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

package org.matsim.core.population.io;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.population.io.StreamingPopulationReader.StreamingPopulation;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.Attributes;

/**
 * A population reader that reads the MATSim format. This reader recognizes the format of the plans-file and uses
 * the correct reader for the specific plans-version, without manual setting.
 *
 * @author mrieser
 */
public class PopulationReader extends MatsimXmlParser implements MatsimReader {

	private final static String PLANS    = "plans.dtd"; // a special, inofficial case, handle it like plans_v0
	private final static String PLANS_V0 = "plans_v0.dtd";
	private final static String PLANS_V1 = "plans_v1.dtd";
	private final static String PLANS_V4 = "plans_v4.dtd";
	private final static String POPULATION_V5 = "population_v5.dtd";

	private final CoordinateTransformation coordinateTransformation;

	private MatsimXmlParser delegate = null;
	private final Scenario scenario;

	private static final Logger log = Logger.getLogger(PopulationReader.class);

	public PopulationReader(final Scenario scenario) {
		this( new IdentityTransformation() , scenario );
	}

	public PopulationReader(
			final CoordinateTransformation coordinateTransformation,
			final Scenario scenario ) {
		this( coordinateTransformation, scenario, false ) ;
	}
	
	/*deliberately package*/ PopulationReader(
				final CoordinateTransformation coordinateTransformation,
				final Scenario scenario, boolean streaming ) {
		if ( !streaming && scenario.getPopulation() instanceof StreamingPopulation ) {
			throw new RuntimeException("MatsimPopulationReader called directly with an instance of StreamingPopulation "
					+ "in scenario.  Call via StreamingPopulationReader or ask for help.  kai, jul'16") ;
		}
		this.coordinateTransformation = coordinateTransformation;
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

	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		if (PLANS_V4.equals(doctype)) {
			// Replaced non-parallel reader with parallel implementation. cdobler, mar'12.
//			this.delegate = new PopulationReaderMatsimV4(this.scenario);
			this.delegate =
					new ParallelPopulationReaderMatsimV4(
							coordinateTransformation,
							this.scenario);
			log.info("using plans_v4-reader.");
		} else if (POPULATION_V5.equals(doctype)) {
			this.delegate =
					new PopulationReaderMatsimV5(
							coordinateTransformation,
							this.scenario);
			log.info("using population_v5-reader.");
		} else if (PLANS_V1.equals(doctype)) {
			this.delegate =
					new PopulationReaderMatsimV1(
							coordinateTransformation,
							this.scenario);
			log.info("using plans_v1-reader.");
		} else if (PLANS_V0.equals(doctype) || PLANS.equals(doctype)) {
			this.delegate =
					new PopulationReaderMatsimV0(
							coordinateTransformation,
							this.scenario);
			log.info("using plans_v0-reader.");
		} else {
			throw new IllegalArgumentException("No population reader available for doctype \"" + doctype + "\".");
		}
	}

}
