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

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.ExternalMobimConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * A reader for config-files of MATSim according to <code>config_v1.dtd</code>.
 *
 * @author mrieser
 */
 class ConfigReaderMatsimV1 extends MatsimXmlParser {

	private final static Logger log = Logger.getLogger(ConfigReaderMatsimV1.class);
//	private final static String CONFIG = "config";
	private final static String MODULE = "module";
	private final static String INCLUDE = "include";
	private final static String PARAM = "param";
	
	private static final String msg = "using deprecated config version; please switch to config v2; your output_config.xml " +
							   "will be in the correct version; v1 will fail eventually, since we want to reduce the " +
							   "workload on keeping everything between v1 and v2 consistent (look into " +
							   "PlanCalcScoreConfigGroup or PlanCalcRouteConfigGroup if you want to know what we mean).";
	
	
	private final Config config;
	private ConfigGroup currmodule = null;

	private String localDtd;

	public ConfigReaderMatsimV1(final Config config) {
		this.config = config;
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
		  } else if ( name.equals(ExternalMobimConfigGroup.GROUP_NAME) ) {
			  this.currmodule = new ExternalMobimConfigGroup() ;
			  this.config.addModule(this.currmodule);
		  }
		  //it must be a not type safe generic module
		  else {
		    this.currmodule = this.config.createModule(atts.getValue("name"));
		  }
		}
	}

	private void startParam(final Attributes meta) {
		this.currmodule.addParam(meta.getValue("name"), meta.getValue("value"));
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
