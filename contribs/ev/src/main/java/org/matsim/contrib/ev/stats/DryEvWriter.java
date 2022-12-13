package org.matsim.contrib.ev.stats;

import com.google.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.discharging.DriveDischargingHandler;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.utils.misc.Time;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class DryEvWriter implements MobsimBeforeCleanupListener {
	@Inject
	DriveDischargingHandler driveDischargingHandler;
	@Inject
	ChargerPowerCollector chargerPowerCollector;
	@Inject
	OutputDirectoryHierarchy controlerIO;
	@Inject
	IterationCounter iterationCounter;
	@Inject
	Network network;
	Logger logger = LogManager.getLogger(DryEvWriter.class);
	@Inject
	DryEvHandler dryEvHandler;

	/**
	 * Adapted from {@link EvMobsimListener} to produce output collected by {@link DryEvHandler}
	 * @param event
	 */
	@Override
	public void notifyMobsimBeforeCleanup( MobsimBeforeCleanupEvent event ) {

		logger.warn( "About to write out dry EVs to file..." );
		Map<String, List<String>> dryEVs = dryEvHandler.getDryEVs();
		for ( List<String> value : dryEVs.values() ) {
			logger.warn( value );
		}
		try {
			CSVPrinter csvPrinter = new CSVPrinter( Files.newBufferedWriter( Paths.get(controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), "dryEVsDuringMobsim.csv"))), CSVFormat.DEFAULT.withDelimiter(';').
					withHeader("VehicleId","SoC","xCoord","yCoord","LinkId","NearestChargerId","DistanceToNearestCharger_m"));
			for ( Map.Entry<String, List<String>> entry : dryEVs.entrySet() ) {
				csvPrinter.printRecord( entry.getKey(),
						entry.getValue().get( 0 ),
						entry.getValue().get( 1 ),
						entry.getValue().get( 2 ),
						entry.getValue().get( 3 ),
						entry.getValue().get( 4 ),
						entry.getValue().get( 5 ));
			}
			csvPrinter.close();
		} catch ( IOException e) {
			throw new RuntimeException(e);
		}
	}
}
