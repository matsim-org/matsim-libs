/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package vwExamples.utils.drtTrajectoryAnalyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author saxer
 *
 */
public class DrtTrajectoryStatsListener implements MobsimBeforeCleanupListener {

	@Inject
	MyDynModeTrajectoryStats myDynModeTrajectoryStats;

	private final DrtConfigGroup drtgroup;
	// private boolean headerWritten = false;
	// private boolean vheaderWritten = false;
	private final String runId;
	private final DecimalFormat format = new DecimalFormat();
	private final DrtConfigGroup drtCfg;

	@Inject
	IterationCounter iterationCounter;
	@Inject
	OutputDirectoryHierarchy controlerIO;

	/**
	 * @param myDynModeTrajectoryStats
	 * @param drtCfg
	 *
	 */
	@Inject
	public DrtTrajectoryStatsListener(Config config, DrtConfigGroup drtCfg,
									  MyDynModeTrajectoryStats myDynModeTrajectoryStats) {
		drtgroup = DrtConfigGroup.getSingleModeDrtConfig(config);
		runId = config.controler().getRunId();

		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		this.myDynModeTrajectoryStats = myDynModeTrajectoryStats;
		this.drtCfg = drtCfg;
	}


	private String filename(String prefix, String extension) {
		return controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), prefix + "_" + drtCfg.getMode() + extension);
	}



	private static void writeCSVExample(String csvfilepath, List<String> trajectroyList) throws IOException {

		
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new File(csvfilepath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		StringBuilder builder = new StringBuilder();
		String ColumnNamesList = "vehicleID" + "," + "time" + "," + "occ" + "," + "dist_m" + "," + "actualTaskType" + "," + "x"  + "," + "y" + ","+ "tt_sec"+ ","+"v_meter_sec" + "," + "SOC" ;

		builder.append(ColumnNamesList + "\n");

		for (String entry : trajectroyList) {
			builder.append(entry + '\n');
		}

		pw.write(builder.toString());
		pw.close();

	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent event) {

		for (Entry<Id<Vehicle>, List<String>> entry : myDynModeTrajectoryStats.vehicleTrajectoryMap.entrySet()) {

			String csvfilepath = filename(entry.getKey().toString(), ".csv");

			try {
				writeCSVExample(csvfilepath, entry.getValue());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		//Clean myDynModeTrajectoryStats.vehicleTrajectoryMap
		myDynModeTrajectoryStats.vehicleTrajectoryMap.clear();
	}
}
