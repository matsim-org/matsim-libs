/* *********************************************************************** *
 * project: org.matsim.*
 * RunDirectoryLoader
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v20.LaneDefinitionsReader;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;

import playground.dgrether.DgPaths;


/**
 * 
 * @author dgrether
 *
 */
public class RunResultsLoader {
	
	private String directory;
	private String runId;
	private OutputDirectoryHierarchy outputDir;
	private Network network;
	private Population population;
	private Lanes lanes;
	private SignalsData signals;
	
	public RunResultsLoader(String path, String runId) {
		this.directory = path;
		this.runId = runId;
		initialize();
	}
	
	private void initialize(){
		File dir = new File(this.directory);
		if (! (dir.exists() && dir.isDirectory())) {
			throw new IllegalArgumentException("Run directory " + this.directory + " can not be found");
		}
		this.outputDir = new OutputDirectoryHierarchy(
				this.directory,
				this.runId,
						false ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists,
				false);
		String configFilename = outputDir.getOutputFilename(Controler.FILENAME_CONFIG);
	}

	public String getEventsFilename(Integer iteration){
		return this.outputDir.getIterationFilename(iteration, Controler.FILENAME_EVENTS_XML);
	}
	
	public Network getNetwork(){
		if (this.network == null) {
			String nf = this.outputDir.getOutputFilename(Controler.FILENAME_NETWORK);
			this.network = loadNetwork(nf);
		}
		return this.network;
	}

	private Network loadNetwork(String path) {
		//Why we have to do all this, we simply want to read a file
		Config c = ConfigUtils.createConfig(); 
		c.network().setInputFile(path);
		Scenario sc = ScenarioUtils.createScenario(c);
		MatsimNetworkReader nr = new MatsimNetworkReader(sc);
		nr.readFile(path);
		return sc.getNetwork();
	}
	
	public Population getPopulation(){
		if (this.population == null) {
			String pf = this.outputDir.getOutputFilename(Controler.FILENAME_POPULATION);
			this.population = this.loadPopulation(pf);
		}
		return this.population;
	}

	private Population loadPopulation(String path) {
		//Why we have to do all this, we simply want to read a file
		Config c = ConfigUtils.createConfig(); 
		c.plans().setInputFile(path);
		ScenarioUtils.ScenarioBuilder builder = new ScenarioUtils.ScenarioBuilder(c) ;
		builder.setNetwork(this.network) ;
		Scenario sc = builder.build() ;
		MatsimPopulationReader pr= new MatsimPopulationReader(sc);
		pr.readFile(path);
		return sc.getPopulation();
	}
	
	//untested
	public Lanes getLanes() {
		if (this.lanes == null){
			String lf = this.outputDir.getOutputFilename(Controler.FILENAME_LANES);
			this.lanes = this.loadLanes(lf);
		}
		return this.lanes;
	}
	
	private Lanes loadLanes(String path) {
		Config c = ConfigUtils.createConfig();
		c.qsim().setUseLanes(true);
		Scenario sc = ScenarioUtils.createScenario(c);
		LaneDefinitionsReader reader = new LaneDefinitionsReader(sc);
		reader.readFile(path);
		return (Lanes) sc.getScenarioElement(Lanes.ELEMENT_NAME);
	}
	
	public SignalsData getSignals() {
		if (this.signals == null) {
			//The next 3 lines should work with recent matsim revisions
//			String systemsfile = this.outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_SYSTEMS );
//			String groupsfile = this.outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_GROUPS);
//			String controlfile = this.outputDir.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_CONTROL);
			//The next 3 lines are only required by old matsim revisions that do not write the full scenario to the output folder
			String systemsfile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_systems_no_13.xml";
			String groupsfile =  DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_groups_no_13.xml";
			String controlfile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_control_no_13.xml";
			this.signals = loadSignals(systemsfile, groupsfile, controlfile);
		}
		return this.signals;
	}

	private SignalsData loadSignals(String systemspath, String groupspath, String controlpath) {
		Config c = ConfigUtils.createConfig();
		ConfigUtils.addOrGetModule(c, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalSystemFile(systemspath);
		ConfigUtils.addOrGetModule(c, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalGroupsFile(groupspath);
		ConfigUtils.addOrGetModule(c, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalControlFile(controlpath);
		SignalsScenarioLoader loader = new SignalsScenarioLoader(ConfigUtils.addOrGetModule(c, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class));
		return loader.loadSignalsData();
	}

	public final String getIterationPath(int iteration) {
		return outputDir.getIterationPath(iteration);
	}

	public final String getIterationFilename(int iteration, String filename) {
		return outputDir.getIterationFilename(iteration, filename);
	}

	public final String getOutputFilename(String filename) {
		return outputDir.getOutputFilename(filename);
	}

	public String getOutputPath() {
		return outputDir.getOutputPath();
	}
	
	
	
}
