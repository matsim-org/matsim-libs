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

package playground.balmermi.world;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader for world-files of MATSim. This reader recognizes the format of the world-file and uses
 * the correct reader for the specific version, without manual setting.
 *
 * @author mrieser
 */
public class MatsimWorldReader extends MatsimXmlParser {

	private final static Logger log = Logger.getLogger(MatsimWorldReader.class);

	private final static String WORLD_V0 = "world_v0.dtd";
	private final static String WORLD_V1 = "world_v1.dtd";
	private final static String WORLD_V2 = "world_v2.dtd";

	private final ScenarioImpl scenario;
	private MatsimXmlParser delegate = null;

	private World world;

	/**
	 * Creates a new reader for MATSim world files.
	 *
	 * @param scenario The Scenario-object to store the world in.
	 */
	public MatsimWorldReader(final ScenarioImpl scenario, final World world) {
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

	/**
	 * Parses the specified matrices file. This method calls {@link #parse(String)}, but handles all
	 * possible exceptions on its own.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) {
		try {
			parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		if (WORLD_V2.equals(doctype)) {
			this.delegate = new WorldReaderMatsimV2(this.scenario, world);
			log.info("using world_v2-reader.");
		} else if (WORLD_V0.equals(doctype)) {
			throw new IllegalArgumentException("world_v0.dtd is no longer supported..");
		} else if (WORLD_V1.equals(doctype)) {
			throw new IllegalArgumentException("world_v1.dtd is no longer supported..");
		} else {
			throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
		}
	}

}
