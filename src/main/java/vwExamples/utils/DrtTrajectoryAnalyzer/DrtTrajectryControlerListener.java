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

package vwExamples.utils.DrtTrajectoryAnalyzer;

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
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author saxer
 *
 */
public class DrtTrajectryControlerListener implements IterationEndsListener {

	@Inject
	MyDynModeTrajectoryStats myDynModeTrajectoryStats;

	private final DrtConfigGroup drtgroup;
	// private boolean headerWritten = false;
	// private boolean vheaderWritten = false;
	private final String runId;
	private final DecimalFormat format = new DecimalFormat();
	private final MatsimServices matsimServices;
	private final DrtConfigGroup drtCfg;

	/**
	 * @param myDynModeTrajectoryStats
	 * @param fleet
	 * @param drtCfg
	 * @param network
	 * @param matsimServices
	 * 
	 */
	@Inject
	public DrtTrajectryControlerListener(Config config, DrtConfigGroup drtCfg,
                                         MyDynModeTrajectoryStats myDynModeTrajectoryStats, MatsimServices matsimServices, Network network) {
		drtgroup = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
		runId = config.controler().getRunId();

		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);

		this.myDynModeTrajectoryStats = myDynModeTrajectoryStats;
		this.matsimServices = matsimServices;
		this.drtCfg = drtCfg;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.core.controler.listener.IterationEndsListener#notifyIterationEnds(
	 * org.matsim.core.controler.events. IterationEndsEvent)
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		

		for (Entry<Id<Vehicle>, List<String>> entry : myDynModeTrajectoryStats.vehicleTrajectoryMap.entrySet()) {

			String csvfilepath = filename(event, entry.getKey().toString(), ".csv");
			
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
	
	private String filename(IterationEndsEvent event, String prefix) {
		return filename(event, prefix, "");
	}

	private String filename(IterationEndsEvent event, String prefix, String extension) {
		return matsimServices.getControlerIO()
				.getIterationFilename(event.getIteration(), prefix + "_" + drtCfg.getMode() + extension);
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

}
