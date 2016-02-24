/* *********************************************************************** *
 * project: org.matsim.*
 * MATSimConfigObject.java
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

/**
 * 
 */
package org.matsim.contrib.matsim4urbansim.config;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.Matsim4UrbansimConfigType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.Matsim4UrbansimType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.MatsimConfigType;
import org.matsim.core.config.Config;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;

/**
 * @author thomas
 * 
 * improvements dec'11:
 * - adjusting flow- and storage capacities to population sample rate. The
 * storage capacity includes a fetch factor to avoid backlogs and network breakdown
 * for small sample rates.
 * 
 * improvements jan'12:
 * - initGlobalSettings sets the number of available processors in the 
 * 	GlobalConfigGroup to speed up MATSim computations. Before that only
 * 	2 processors were used even if there are more.
 * 
 * improvements feb'12:
 * - setting mutationrange = 2h for TimeAllocationMutator (this shifts the departure times)
 * 
 * improvements march'12:
 * - extended the MATSim4UrbanSim configuration, e.g. a standard MATSim config can be loaded
 * 
 * improvements aug'12
 * - extended the MATSim4UrbanSim configuration: 
 *   - added a switch to select between between radius or shape-file distribution of locations within a zone
 *   - added a field "project_name" of the UrbanSim scenario as an identifier
 *   - added a time-of-a-day parameter
 *   - added beta parameter for mode bike
 *   
 * improvements/changes oct'12
 * - switched to qsim
 * 
 * changes dec'12
 * - switched matsim4urbansim config v2 parameters from v3 are out sourced into external matsim config
 * - introducing pseudo pt (configurable via external MATSim config)
 * - introducing new strategy module "changeSingeLegMode" (configurable via external MATSim config)
 * 
 * changes jan'13
 * - added attributes for pt stop, travel times and distances input files 
 *
 * changes march'13
 * - now, the external config overwrites all parameter settings that overlap with the settings 
 *   made in the MATSim4UrbanSim config (see also test cases in "ConfigLoadingTest")
 */
public class M4UConfigurationConverterV4 {
	// logger
	static final Logger log = Logger.getLogger(M4UConfigurationConverterV4.class);
	
	// MATSim config
	private Config config = null;

	// JAXB representation of matsim4urbansim config
	// private final MatsimConfigType matsim4urbansimConfig ;
	private final Matsim4UrbansimConfigType matsim4urbansimConfig;

	/**
	 * constructor
	 * 
	 * @param config stores MATSim parameters
	 * @param matsim4urbansimConfigFilename path to matsim config file
	 */
	public M4UConfigurationConverterV4(final String matsim4urbansimConfigFilename){
		this.matsim4urbansimConfig = M4UConfigUtils.unmarschal(matsim4urbansimConfigFilename); // loading and initializing MATSim config	
		Assert.assertTrue( this.matsim4urbansimConfig!= null ) ;
	}
	
	/**
	 * Transferring all parameter from matsim4urbansim config to internal MATSim config/scenario
	 * @return boolean true if initialization successful
	 */
	public boolean init(){

		// creates an empty config to be filled by settings from the MATSim4UrbanSim and external config files
		this.config = M4UConfigUtils.createEmptyConfigWithSomeDefaults();
		Assert.assertTrue( this.config != null ) ;

		// get root elements from JAXB matsim4urbansim config object
		MatsimConfigType matsim4urbansimConfigPart1 = this.matsim4urbansimConfig.getMatsimConfig();
		Assert.assertTrue( matsim4urbansimConfigPart1 != null ) ;
		
		Matsim4UrbansimType matsim4urbansimConfigPart2 = this.matsim4urbansimConfig.getMatsim4Urbansim();
		Assert.assertTrue( matsim4urbansimConfigPart2 != null ) ;

		// int MATSimConfigType parameters
		M4UConfigUtils.initAccessibilityConfigGroupParameters(matsim4urbansimConfigPart1, config);
		M4UConfigUtils.initM4UControlerConfigModuleV3Parameters(matsim4urbansimConfigPart1, config);
		M4UConfigUtils.initUrbanSimParameterConfigModuleV3Parameters(matsim4urbansimConfigPart2, config);
		
		M4UConfigUtils.initNetwork(matsim4urbansimConfigPart1, config);
		M4UConfigUtils.initControler(matsim4urbansimConfigPart1, config);
		M4UConfigUtils.initPlanCalcScore(matsim4urbansimConfigPart1, config);
		M4UConfigUtils.initQSim(matsim4urbansimConfigPart2, config);

		M4UConfigUtils.initStrategy(config) ;
		// note: ending innovation after 80% of iterations is now switched on in "createEmptyConfigWithSomeDefaults" above.

//		MatrixBasedPtRouterConfigUtils.initMatrixBasedPtRouterParameters(matsim4urbansimConfigPart3, config);
		// (was, in my view, not doing anything besides consistency checking, which is now in the config group consistency
		// checker. kai, jul'13)

		// loading the external MATSim config in to the initialized config
		M4UConfigUtils.loadExternalConfigAndOverwriteMATSim4UrbanSimSettings(matsim4urbansimConfigPart1.getExternalMatsimConfig().getInputFile(), config);
		// (by the design, the matsim xml config reader over-writes only entries which are explicitly mentioned in the external config)

		// show final settings
		M4UConfigUtils.printUrbanSimParameterSettings( M4UConfigUtils.getUrbanSimParameterConfigAndPossiblyConvert(config) );
		M4UConfigUtils.printMATSim4UrbanSimControlerSettings( M4UConfigUtils.getMATSim4UrbaSimControlerConfigAndPossiblyConvert(config) );

		config.addConfigConsistencyChecker( new VspConfigConsistencyCheckerImpl() ) ;
		config.addConfigConsistencyChecker( new M4UConfigConsistencyChecker() ) ;

		M4UConfigUtils.checkConfigConsistencyAndWriteToLog(config, "at the end of the matsim4urbansim config converter") ;

		return true;
	}

	public Config getConfig(){
			return this.config;
	}
	
}

