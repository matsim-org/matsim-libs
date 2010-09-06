/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.misc;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public abstract class ConfigUtils {

	public static Config loadConfig(final String filename) throws IOException {
		Config config = new Config();
		config.addCoreModules();

		try {
			new MatsimConfigReader(config).parse(filename);
		} catch (SAXException e) {
			throw new IOException(e);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		}

		return config;
	}

	public static void loadConfig(final Config config, final String filename) throws IOException {
		if (config.global() == null) {
			config.addCoreModules();
		}
		try {
			new MatsimConfigReader(config).parse(filename);
		} catch (SAXException e) {
			throw new IOException(e);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Adds the <code>pathPrefix</code> in front of all supported file-paths
	 * in core-config groups in the configuration, except when the paths are
	 * specified as absolute paths. This is useful e.g. if the configuration
	 * contains file-paths relative to the location of the config-file.
	 * The currently supported file-paths are:
	 * <ul>
	 *  <li>global.localDtdBase</li>
	 *  <li>controler.outputDirectory</li>
	 *  <li>network.inputNetworkFile</li>
	 *  <li>plans.inputPlansFile</li>
	 *  <li>facilities.inputFacilitiesFile</li>
	 *  <li>counts.inputCountsFile</li>
	 *  <li>world.inputWorldFile</li>
	 *  <li>households.inputFile</li>
	 *  <li>roadpricing.tollLinksFile</li>
	 * </ul>
	 *
	 * @param config
	 * @param pathPrefix
	 */
	public static void modifyFilePaths(final Config config, final String pathPrefix) {
		String prefix = pathPrefix;
		if (!prefix.endsWith("/") && !prefix.endsWith(File.separator)) {
			prefix = prefix + File.separator;
		}
		config.controler().setOutputDirectory(getAbsolutePath(prefix, config.controler().getOutputDirectory()));
		config.network().setInputFile(getAbsolutePath(prefix, config.network().getInputFile()));
		config.plans().setInputFile(getAbsolutePath(prefix, config.plans().getInputFile()));
		config.facilities().setInputFile(getAbsolutePath(prefix, config.facilities().getInputFile()));
		config.counts().setCountsFileName(getAbsolutePath(prefix, config.counts().getCountsFileName()));
		config.households().setInputFile(getAbsolutePath(prefix, config.households().getInputFile()));
		config.roadpricing().setTollLinksFile(getAbsolutePath(prefix, config.roadpricing().getTollLinksFile()));
	}

	private static String getAbsolutePath(final String prefix, final String path) {
		if (path == null) {
			return null;
		}
		File file = new File(path);
		if (file.exists() && path.equals(file.getAbsolutePath())) {
			return path;
		}
		/* even if the file exists at the given location, its path
		 * seems not to be specified as absolute path, thus we have to
		 * interpret it as a relative path and add the prefix to it.
		 */
		String absolutePath = prefix + path;
		return absolutePath;
	}
}
