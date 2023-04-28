package org.matsim.contrib.freight.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;

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
		bw1.write("carrierId \t scoreOfSelectedPlan \t nuOfTours \t nuOfShipments(input) \t nuOfServices(input) ");
		bw1.newLine();

		final TreeMap<Id<Carrier>, Carrier> sortedCarrierMap = new TreeMap<>(carriers.getCarriers());

		for (Carrier carrier : sortedCarrierMap.values()) {
			bw1.write(carrier.getId().toString());
			bw1.write("\t" + carrier.getSelectedPlan().getScore());
			bw1.write("\t" + carrier.getSelectedPlan().getScheduledTours().size());
			bw1.write("\t" + carrier.getShipments().size());
			bw1.write("\t" + carrier.getServices().size());
			bw1.newLine();
		}

		bw1.close();
		log.info("Output written to " + fileName);
	}
}
