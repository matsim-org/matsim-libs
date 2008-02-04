/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigReaderMatsimV1.java
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

package org.matsim.config;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A reader for config-files of MATSim according to <code>config_v1.dtd</code>.
 *
 * @author mrieser
 */
public class ConfigReaderMatsimV1 extends MatsimXmlParser {

	private final static String CONFIG = "config";
	private final static String MODULE = "module";
	private final static String INCLUDE = "include";
	private final static String PARAM = "param";

	private final Config config;
	private Module currmodule = null;

	private String localDtd;

	/**
	 * @param config
	 */
	public ConfigReaderMatsimV1(final Config config) {
		this.config = config;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (PARAM.equals(name)) {
			startParam(atts);
		} else if (MODULE.equals(name)) {
			startModule(atts);
		} else if (CONFIG.equals(name)) {
			// do nothing
		} else if (INCLUDE.equals(name)) {
			Logger.getLogger(this.getClass()).warn("<incude> is currently not supported.");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (MODULE.equals(name)) {
			this.currmodule = null;
		}
	}

	private void startModule(final Attributes atts) {
		this.currmodule = this.config.getModule(atts.getValue("name"));
		if (this.currmodule == null) {
			this.currmodule = this.config.createModule(atts.getValue("name"));
		}
	}

	private void startParam(final Attributes meta) {
		String value = meta.getValue("value");
		if (!"null".equalsIgnoreCase(value)) {
			// only set the param if it is not "null"
			this.currmodule.addParam(meta.getValue("name"),meta.getValue("value"));
		}
	}

	/**
	 * Parses the specified config file. This method calls {@link #parse(String)}, but handles all
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

	/**
	 * Parses the specified config file, and uses the given dtd file as a local copy to use as dtd
	 * if the one specified in the config file cannot be found.
	 *
	 * @param filename The name of the file to parse.
	 * @param dtdFilename The name of a (local) dtd-file to be used for validating the config file.
	 */
	public void readFile(final String filename, final String dtdFilename) {
		this.localDtd = dtdFilename;
		readFile(filename);
		this.localDtd = null;
	}

	@Override
	public InputSource resolveEntity(final String publicId, final String systemId) {

		InputSource is = super.resolveEntity(publicId, systemId);
		if (is == null && this.localDtd != null) {
			File dtdFile = new File(this.localDtd);
			if (dtdFile.exists() && dtdFile.isFile() && dtdFile.canRead()) {
				Logger.getLogger(this.getClass()).info("Using the local DTD " + this.localDtd);
				return new InputSource(this.localDtd);
			}
			return null;
		}
		return is;
	}

}
