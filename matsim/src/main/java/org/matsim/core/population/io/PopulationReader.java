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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.population.io.StreamingPopulationReader.StreamingPopulation;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.AttributeConverter;
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
	private final static String POPULATION_V6 = "population_v6.dtd";

	private final CoordinateTransformation coordinateTransformation;

	private MatsimXmlParser delegate = null;
	private final Scenario scenario;

	private Map<Class<?>, AttributeConverter<?>> attributeConverters = new HashMap<>();

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

	public void putAttributeConverter( final Class<?> clazz , AttributeConverter<?> converter ) {
		attributeConverters.put( clazz , converter );
	}

	public void putAttributeConverters( final Map<Class<?>, AttributeConverter<?>> converters ) {
		attributeConverters.putAll( converters );
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
		switch ( doctype ) {
			case POPULATION_V6:
				this.delegate =
						new PopulationReaderMatsimV6(
								coordinateTransformation,
								this.scenario);
				((PopulationReaderMatsimV6) delegate).putAttributeConverters( attributeConverters );
				log.info("using population_v6-reader.");
				break;
			case POPULATION_V5:
				this.delegate =
						new PopulationReaderMatsimV5(
								coordinateTransformation,
								this.scenario);
				log.info("using population_v5-reader.");
				break;
			case PLANS_V4:
				// Replaced non-parallel reader with parallel implementation. cdobler, mar'12.
				this.delegate =
						new ParallelPopulationReaderMatsimV4(
								coordinateTransformation,
								this.scenario);
				log.info("using plans_v4-reader.");
				break;
			case PLANS_V1:
				this.delegate =
						new PopulationReaderMatsimV1(
								coordinateTransformation,
								this.scenario);
				log.info("using plans_v1-reader.");
				break;
			case PLANS_V0:
			case PLANS:
				this.delegate =
						new PopulationReaderMatsimV0(
								coordinateTransformation,
								this.scenario);
				log.info("using plans_v0-reader.");
				break;
			default:
				throw new IllegalArgumentException("No population reader available for doctype \"" + doctype + "\".");
		}
	}

}
