/* *********************************************************************** *
 * project: org.matsim.*
 * SignalsMorningEveningMergeTool
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
package playground.dgrether.koehlerstrehlersignal.run;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalPlan;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;


/**
 * @author dgrether
 *
 */
public class SignalsMorningEveningMergeTool {
	
	private static final Logger log = Logger.getLogger(SignalsMorningEveningMergeTool.class);
	private String basefilename = null;
	
	private void merge(String morningFolder, String eveningFolder, String outputDirectory) {
		SignalControlData morningControlData = this.loadSignalControlData(morningFolder);
		SignalControlData eveningControlData = this.loadSignalControlData(eveningFolder);
		SignalControlData merged = mergeData(morningControlData, eveningControlData);
		writeSignalControl(outputDirectory, merged);
	}

	
	private void writeSignalControl(String outputDirectory, SignalControlData merged) {
		IOUtils.createDirectory(outputDirectory);
		SignalsScenarioWriter writer = new SignalsScenarioWriter();
		writer.setSignalControlOutputFilename(outputDirectory + "merged_signal_control_" + basefilename + ".xml");
		writer.writeSignalControlData(merged);
	}


	/**
	 * merges evening control into morning control and returns morning control
	 */
	private SignalControlData mergeData(SignalControlData morningControl, SignalControlData eveningControl){
		for (SignalSystemControllerData morningController : morningControl.getSignalSystemControllerDataBySystemId().values()) {
			List<SignalPlanData> morningPlans = new ArrayList<SignalPlanData>(morningController.getSignalPlanData().values());
			morningController.getSignalPlanData().clear();
			for (SignalPlanData morningPlan : morningPlans) {
				Id<SignalPlan> newId = Id.create(morningPlan.getId().toString() + "_m", SignalPlan.class);
				SignalPlanData newPlan = DgSignalsUtils.copySignalPlanData(morningPlan, newId, morningControl.getFactory());
				newPlan.setStartTime(0.0);
				newPlan.setEndTime(12.0 * 3600.0);
				morningController.addSignalPlanData(newPlan);
			}
			SignalSystemControllerData ecd = eveningControl.getSignalSystemControllerDataBySystemId().get(morningController.getSignalSystemId());
			for (SignalPlanData eplan : ecd.getSignalPlanData().values()) {
				Id<SignalPlan> newId = Id.create(eplan.getId().toString() + "_e", SignalPlan.class);
				SignalPlanData newPlan = DgSignalsUtils.copySignalPlanData(eplan, newId, morningControl.getFactory());
				newPlan.setStartTime(12.0 * 3600.0);
				newPlan.setEndTime((23.0 * 3600.0) + (59.0 * 60.0) + 59.0);
				morningController.addSignalPlanData(newPlan);
			}
		}
		return morningControl;
	}
	
	
	private SignalControlData loadSignalControlData(String morningFolder) {
		File dir = new File(morningFolder);
		if (! dir.isDirectory()) throw new RuntimeException();
		File infile = null;
		String prefix = "optimized_signal_control_";
		for (File f :  dir.listFiles()) {
			if (f.getName().startsWith(prefix)) {
				log.info("Loading " + f.getAbsolutePath());
				infile = f;
				String basename = f.getName().substring(prefix.length());
				basename = basename.substring(0, basename.lastIndexOf("."));
				if (this.basefilename == null) {
					this.basefilename = basename + "_";
				}
				else {
					this.basefilename += basename;
				}
				break;
			}
		}
		Config config = ConfigUtils.createConfig();
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalControlFile(infile.getAbsolutePath());
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class));
		SignalsData signals = signalsLoader.loadSignalsData();
		return signals.getSignalControlData();
	}




	public static void main(String[] args) {
//		String morningFolder =DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_morning_peak/";
//		String eveningFolder = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10_evening_peak/";
//		String outputDirectory = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_10/";
		String morningFolder =DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-08-12_minflow_10_morning_peak/";
		String eveningFolder = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-08-12_minflow_10_evening_peak/";
		String outputDirectory = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-08-12_minflow_10/";

		new SignalsMorningEveningMergeTool().merge(morningFolder, eveningFolder, outputDirectory);
	}


}
