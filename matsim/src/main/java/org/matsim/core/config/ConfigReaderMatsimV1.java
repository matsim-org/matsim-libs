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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

import static org.matsim.core.config.ConfigV2XmlNames.NAME;

/**
 * A reader for config-files of MATSim according to <code>config_v1.dtd</code>.
 *
 * @author mrieser
 */
 class ConfigReaderMatsimV1 extends MatsimXmlParser {

	private final static Logger log = LogManager.getLogger(ConfigReaderMatsimV1.class);
//	private final static String CONFIG = "config";
	private final static String MODULE = "module";
	private final static String INCLUDE = "include";
	private final static String PARAM = "param";

	private static final String msg = "using deprecated config version; please switch to config v2; your output_config.xml " +
							   "will be in the correct version; v1 will fail eventually, since we want to reduce the " +
							   "workload on keeping everything between v1 and v2 consistent (look into " +
							   "ScoringConfigGroup or RoutingConfigGroup if you want to know what we mean).";


	private final Config config;
	private final ConfigAliases aliases;
	private final Deque<String> pathStack = new ArrayDeque<>();
	private ConfigGroup currmodule = null;

	private String localDtd;

	public ConfigReaderMatsimV1(final Config config) {
		super(ValidationType.DTD_ONLY);
		this.config = config;
		this.aliases = new ConfigAliases();
		log.warn(msg);
	}

	public ConfigReaderMatsimV1(final Config config, final ConfigAliases aliases) {
		super(ValidationType.DTD_ONLY);
		this.config = config;
		this.aliases = aliases;
		log.warn(msg);
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

			if (GlobalConfigGroup.GROUP_NAME.equals(name) ) {
				if (!config.global().isInsistingOnDeprecatedConfigVersion()) {
					throw new RuntimeException(msg);
				}
			}
			// the idea here was to wait until the global config group is read because only then we can
			// decide if the user is insisting on it.  However, this clearly does not work in full since the condition
			// will never be triggered when the global config group is not used at all in the file.
			// :-( kai, aug'18

			this.currmodule = null;
			this.pathStack.removeFirst();
		}
	}

	private void startModule(final Attributes atts) {
		String name = this.aliases.resolveAlias(atts.getValue(NAME), this.pathStack);
	  this.currmodule = this.config.getModule(name);

		if (this.currmodule == null) {
		  //if there are type safe optional modules they have to be added here
		  if (name.equals(QSimConfigGroup.GROUP_NAME)){
		    this.currmodule = this.config.qsim();
		  }
		  //it must be a not type safe generic module
		  else {
		    this.currmodule = this.config.createModule(atts.getValue("name"));
		  }
		}
		this.pathStack.addFirst(name);
	}

	private void startParam(final Attributes atts) {
		String name = this.aliases.resolveAlias(atts.getValue(NAME), this.pathStack);
		this.currmodule.addParam(name, atts.getValue("value"));
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

	// The following did override the inherited resolveEntity method.  But I have no idea why that may have made sense.  kai, jul'16
//	@Override
//	public InputSource resolveEntity(final String publicId, final String systemId) {
//
//		InputSource is = super.resolveEntity(publicId, systemId);
//		if (is == null && this.localDtd != null) {
//			File dtdFile = new File(this.localDtd);
//			if (dtdFile.exists() && dtdFile.isFile() && dtdFile.canRead()) {
//				log.info("Using the local DTD " + this.localDtd);
//				return new InputSource(this.localDtd);
//			}
//			return null;
//		}
//		return is;
//	}

}
