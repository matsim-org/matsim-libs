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

package org.matsim.core.config;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimExtensionPoint;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author mrieser
 */
public abstract class ConfigUtils implements MatsimExtensionPoint {

	public static Config createConfig(final String filename) {
		// are there systematic arguments against such a method?  otherwise, users will return to new Controler( filename ), since that
		// is easier to memorize. kai, jul'16
		
		URL url = IOUtils.getUrlFromFileOrResource(filename) ;
		return createConfig( url ) ;
	}

	public static Config createConfig(URL context) {
		Config config = createConfig();
		config.setContext(context);
		return config;
	}

	public static Config createConfig() {
		Config config = new Config();
		config.addCoreModules();
		return config;
	}

	public static Config createConfig(ConfigGroup... customModules) {
		Config config = createConfig();
        for (ConfigGroup customModule : customModules) {
            config.addModule(customModule);
        }
		return config;
	}

	public static Config loadConfig(final String filename, ConfigGroup... customModules) throws UncheckedIOException {
		return loadConfig(IOUtils.getUrlFromFileOrResource(filename), customModules);
	}

	public static Config loadConfig(final URL url, ConfigGroup... customModules) throws UncheckedIOException {
		Gbl.assertNotNull(url);
		
		Config config = new Config();
		config.addCoreModules();

		for (ConfigGroup customModule : customModules) {
			config.addModule(customModule);
		}
		
		new ConfigReader(config).parse(url);
		config.setContext(url);
		return config;
	}


	/**
	 * This does (hopefully) overwrite config settings if they are defined in the file.  So you can do
	 * <pre>
	 * Config config = ConfigUtils.createConfig() ;
	 * config.xxx().setYyy() ; // set some defaults for your application.
	 * ConfigUtils.loadConfig( config, filename ) ; // read user-defined options
	 * config.aaa().bbb() ; // set config options which you don't want the user to potentially overwrite.
	 * ...
	 * </pre>  
	 */
	public static void loadConfig(final Config config, final String filename) throws UncheckedIOException {
		if (config.global() == null) {
			config.addCoreModules();
		}
		new ConfigReader(config).readFile(filename);
	}

	public static void loadConfig(final Config config, final URL url) throws UncheckedIOException {
		if (config.global() == null) {
			config.addCoreModules();
		}
		new ConfigReader(config).parse(url);
	}


	public static Config loadConfig(URL url) {
		Config config = new Config();
		config.addCoreModules();
		new ConfigReader(config).parse(url);
		config.setContext(url);
		return config;

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
		config.counts().setInputFile(getAbsolutePath(prefix, config.counts().getCountsFileName()));
		config.households().setInputFile(getAbsolutePath(prefix, config.households().getInputFile()));
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

	public static Id<StrategySettings> createAvailableStrategyId(Config config) {
		long maxStrategyId = 0;
		Iterator<StrategySettings> iterator = config.strategy().getStrategySettings().iterator();
		while(iterator.hasNext()){
			maxStrategyId = Math.max(maxStrategyId, Long.parseLong(iterator.next().getId().toString()));
		}
		return Id.create(maxStrategyId + 1, StrategySettings.class);
	}

	/**
	 * This is a refactoring device to remove former core config groups from the core config.
	 * Instructions: If you want to remove e.g. Config.vspExperimental(), replace that
	 * method with
	 * ConfigUtils.addOrGetModule(this, VspExperimentalConfigGroup.GROUP_NAME, VspExperimentalConfigGroup.class)
	 * and then hit Refactor/Inline.
	 */
	public static <T extends ConfigGroup> T addOrGetModule(Config config, String groupName, Class<T> moduleClass) {
		ConfigGroup module = config.getModule(groupName);
		if (module == null || module.getClass() == ConfigGroup.class) {
			try {
				module = moduleClass.newInstance();
				config.addModule(module);
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
        }
		return moduleClass.cast(module);
	}
}
