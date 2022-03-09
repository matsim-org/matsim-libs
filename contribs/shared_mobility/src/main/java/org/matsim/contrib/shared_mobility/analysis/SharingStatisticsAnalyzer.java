package org.matsim.contrib.shared_mobility.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;
import com.google.inject.Inject;

/**
 * @author steffenaxer
 */
public class SharingStatisticsAnalyzer implements IterationEndsListener {
	public static final String SHARING_LEGS_NAME = "sharingLegs";

	Scenario scenario;
	SharingLegCollector sharingLegCollector;
	MatsimServices matsimServices;

	@Inject
	SharingStatisticsAnalyzer(Scenario scenario, SharingLegCollector sharingLegCollector,
			MatsimServices matsimServices) {
		this.scenario = scenario;
		this.sharingLegCollector = sharingLegCollector;
		this.matsimServices = matsimServices;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String sharingLegFile = getFilename(event, "all", SHARING_LEGS_NAME+".csv");
		writeAllLeg(sharingLegFile);
	}

	void writeAllLeg(String filename) {
	
		String defaultDelimiter = scenario.getConfig().global().getDefaultDelimiter();
		String header = String.join(defaultDelimiter, //
				"personId", //
				"departureTime", //
				"arrivalTime", //
				"sharingServiceId", //
				"distance", //
				"vehicleId", //
				"from_x", //
				"from_y", //
				"to_x", //
				"to_y");

		Map<Id<SharingService>, Collection<SharingLeg>> legsPerService = sharingLegCollector.getSharingLegs();

		List<String> exportedLeg = new ArrayList<>();

		for (Collection<SharingLeg> sharingLegs : legsPerService.values()) {
			for (SharingLeg sharingLeg : sharingLegs) {

				exportedLeg.add(String.join(defaultDelimiter,
						sharingLeg.getPersonId().toString(),
						Double.toString(sharingLeg.getDepartureTime()),
						Double.toString(sharingLeg.getArrivalTime()),
						sharingLeg.getSharingServiceId().toString(),
						Double.toString(sharingLeg.getDistance()),
						sharingLeg.getVehicleId().toString(),
						Double.toString(sharingLeg.getFromCoord().getX()),
						Double.toString(sharingLeg.getFromCoord().getY()),
						Double.toString(sharingLeg.getToCoord().getX()),
						Double.toString(sharingLeg.getToCoord().getY())));
			}
		}
		
		writeStringCollection(exportedLeg, header, filename);

	}

	private String getFilename(IterationEndsEvent event, String prefix, String extension) {
		return matsimServices.getControlerIO().getIterationFilename(event.getIteration(), prefix + "_" + extension);
	}

	private void writeStringCollection(Collection<String> data, String header, String filename)
	{
		
		try (BufferedWriter bw = IOUtils.getBufferedWriter(filename)) {
			bw.write(header);
			bw.newLine();
			for (String row : data) {
				bw.write(row);
				bw.newLine();

			}
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
