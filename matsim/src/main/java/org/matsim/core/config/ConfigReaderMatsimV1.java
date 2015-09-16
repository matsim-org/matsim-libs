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

package org.matsim.core.config;

import java.io.File;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

/**
 * A reader for config-files of MATSim according to <code>config_v1.dtd</code>.
 *
 * @author mrieser
 */
 class ConfigReaderMatsimV1 extends MatsimXmlParser implements MatsimSomeReader {

	private final static Logger log = Logger.getLogger(ConfigReaderMatsimV1.class);
//	private final static String CONFIG = "config";
	private final static String MODULE = "module";
	private final static String INCLUDE = "include";
	private final static String PARAM = "param";

	private final Config config;
	private ConfigGroup currmodule = null;

	private String localDtd;

	public ConfigReaderMatsimV1(final Config config) {
		this.config = config;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if (PARAM.equals(name)) {
			startParam(atts);
		} else if (MODULE.equals(name)) {
			startModule(atts);
		} else if (INCLUDE.equals(name)) {
			log.warn("<include> is currently not supported.");
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if (MODULE.equals(name)) {
			this.currmodule = null;
		}
	}

	private void startModule(final Attributes atts) {
		String name = atts.getValue("name");
	  this.currmodule = this.config.getModule(name);

		if (this.currmodule == null) {
		  //if there are type safe optional modules they have to be added here
		  if (name.equals(QSimConfigGroup.GROUP_NAME)){
		    this.currmodule = this.config.qsim();
		  } else if ( name.equals(SimulationConfigGroup.GROUP_NAME) ) {
			  this.currmodule = new SimulationConfigGroup() ;
			  this.config.addModule(this.currmodule);
		  }
		  //it must be a not type safe generic module
		  else {
		    this.currmodule = this.config.createModule(atts.getValue("name"));
		  }
		}
	}

	private void startParam(final Attributes meta) {
		String value = meta.getValue("value");
		this.currmodule.addParam(meta.getValue("name"),meta.getValue("value"));
	}

	/**
	 * Parses the specified config file. This method calls {@link #parse(String)}.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
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
				log.info("Using the local DTD " + this.localDtd);
				return new InputSource(this.localDtd);
			}
			return null;
		}
		return is;
	}

}
