/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Carriers;
import org.matsim.freight.carriers.CarriersUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

/**
 * Some basic analysis / data collection for {@link Carriers}(files)
 * <p></p>
 * For all carriers it writes out the:
 * - score of the selected plan
 * - number of tours (= vehicles) of the selected plan
 * - number of Services (input)
 * - number of shipments (input)
 * to a tsv-file.
 * @author Kai Martins-Turner (kturner)
 */
public class CarrierPlanAnalysis  {

	private static final Logger log = LogManager.getLogger(CarrierPlanAnalysis.class);

	Carriers carriers;

	public CarrierPlanAnalysis(Carriers carriers) {
		this.carriers = carriers;
	}

	public void runAnalysisAndWriteStats(String analysisOutputDirectory) throws IOException {
		log.info("Writing out carrier analysis ...");
		//Load per vehicle
		String fileName = analysisOutputDirectory + "Carrier_stats.tsv";

		BufferedWriter bw1 = new BufferedWriter(new FileWriter(fileName));

		//Write headline:
		bw1.write("carrierId \t MATSimScoreSelectedPlan \t jSpritScoreSelectedPlan \t nuOfTours \t nuOfShipments(input) \t nuOfServices(input) \t jspritComputationTime[HH:mm:ss]");
		bw1.newLine();

		final TreeMap<Id<Carrier>, Carrier> sortedCarrierMap = new TreeMap<>(carriers.getCarriers());

		for (Carrier carrier : sortedCarrierMap.values()) {
			bw1.write(carrier.getId().toString());
			bw1.write("\t" + carrier.getSelectedPlan().getScore());
			bw1.write("\t" + carrier.getSelectedPlan().getJspritScore());
			bw1.write("\t" + carrier.getSelectedPlan().getScheduledTours().size());
			bw1.write("\t" + carrier.getShipments().size());
			bw1.write("\t" + carrier.getServices().size());
			if (CarriersUtils.getJspritComputationTime(carrier) != Integer.MIN_VALUE)
				bw1.write("\t" + Time.writeTime(CarriersUtils.getJspritComputationTime(carrier), Time.TIMEFORMAT_HHMMSS));
			else
				bw1.write("\t" + "null");

			bw1.newLine();
		}

		bw1.close();
		log.info("Output written to " + fileName);
	}
}
