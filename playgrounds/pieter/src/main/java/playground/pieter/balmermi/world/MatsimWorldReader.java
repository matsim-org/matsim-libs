/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimWorldReader.java
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

package playground.pieter.balmermi.world;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.Attributes;

/**
 * A reader for world-files of MATSim. This reader recognizes the format of the world-file and uses
 * the correct reader for the specific version, without manual setting.
 *
 * @author mrieser
 */
class MatsimWorldReader extends MatsimXmlParser {

	private final static Logger log = Logger.getLogger(MatsimWorldReader.class);

	private final static String WORLD_V0 = "world_v0.dtd";
	private final static String WORLD_V1 = "world_v1.dtd";
	private final static String WORLD_V2 = "world_v2.dtd";

	private final MutableScenario scenario;
	private MatsimXmlParser delegate = null;

	private final World world;

	/**
	 * Creates a new reader for MATSim world files.
	 *
	 * @param scenario The Scenario-object to store the world in.
	 */
    private MatsimWorldReader(final MutableScenario scenario, final World world) {
		this.scenario = scenario;
		this.world = world;
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
        switch (doctype) {
            case WORLD_V2:
                this.delegate = new WorldReaderMatsimV2(this.scenario, world);
                log.info("using world_v2-reader.");
                break;
            case WORLD_V0:
                throw new IllegalArgumentException("world_v0.dtd is no longer supported..");
            case WORLD_V1:
                throw new IllegalArgumentException("world_v1.dtd is no longer supported..");
            default:
                throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
        }
	}

}
