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

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimExtensionPoint;
import org.matsim.core.config.ConfigWriter.Verbosity;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

/**
 * @author mrieser
 */
/**
 * @author nagel
 *
 */
public abstract class ConfigUtils implements MatsimExtensionPoint {

	public static Config createConfig(final String context) {
		URL url = IOUtils.getUrlFromFileOrResource(context) ;
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
			// if the config does not exist yet, we interpret the file name as is:
			config.addCoreModules();
			new ConfigReader(config).readFile(filename);
		} else {
			// if the config does already exist, this can be used to read a second config file to override
			// settings from before, e.g. using a base config and then several case study configs.
			// In this case, using the above syntax generates inconsistent behavior between
			// gui and command line: command line takes the config file from the java root,
			// while the gui takes it from the config file root.  The following syntax should
			// now also take it from the config file root when it is called from the command line
			// (same as in other places: we are making the command line behavior
			// and GUI behavior consistent).
			// kai, jan'18
//			URL url = ConfigGroup.getInputFileURL(config.getContext(), filename);;
//			new ConfigReader(config).parse(url) ;
			// yyyyyy the above probably works, but has ramifications across many test
			// cases.  Need to discuss first (and then find some time again).
			// See MATSIM-776 and MATSIM-777.  kai, feb'18
			
			new ConfigReader(config).readFile(filename);
		}
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
	 * Convenience method to all addOrGetModule with only two arguments.
	 * <br/>
	 * Notes:<ul>
	 * <li>Seems to be really slow, so don't use in inner loop.</li>
	 * </ul>
	 * @param config
	 * @param moduleClass
	 * @return instance of moduleClass inside config
	 */
	public static <T extends ConfigGroup> T addOrGetModule( Config config, Class<T> moduleClass ) {
		String groupName;
		try {
			groupName = moduleClass.newInstance().getName();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e) ;
		}
		return addOrGetModule( config, groupName, moduleClass ) ;
	}

	/**
	 * This is a refactoring device to remove former core config groups from the core config.
	 * Instructions: If you want to remove e.g. Config.vspExperimental(), replace that
	 * method with
	 * ConfigUtils.addOrGetModule(this, VspExperimentalConfigGroup.GROUP_NAME, VspExperimentalConfigGroup.class)
	 * and then hit Refactor/Inline.
	 * <br/>
	 * Notes:<ul>
	 * <li>Seems to be really slow, so don't use in inner loop.</li>
	 * </ul>
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

	public static void setVspDefaults(final Config config) {
		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration);
		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
	}
	
	public static void writeConfig( final Config config, String filename ) {
		new ConfigWriter(config).write(filename);
	}
	public static void writeMinimalConfig( final Config config, String filename ) {
		new ConfigWriter(config,Verbosity.minimal).write(filename);
	}
}
