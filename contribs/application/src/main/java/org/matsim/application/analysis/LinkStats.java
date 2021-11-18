package org.matsim.application.analysis;

import com.beust.jcommander.internal.Lists;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@CommandLine.Command(name = "link-stats", description = "Compute aggregated link statistics, like volume and travel time")
public class LinkStats implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(LinkStats.class);

	@CommandLine.Option(names = "--events", description = "Input event file", required = true)
	private Path events;

	@CommandLine.Option(names = "--network", description = "Input network file", required = true)
	private Path network;

	@CommandLine.Option(names = "--output", description = "Path for output csv", required = true)
	private Path output;

	//@CommandLine.Option(names = "--link-to-link", description = "Also calculate link to link travel times", defaultValue = "false")
	//private boolean l2l;

	@CommandLine.Option(names = "--max-time", description = "Maximum time used in aggregation", defaultValue = "86399")
	private int maxTime;

	@CommandLine.Option(names = "--time-slice", description = "Number of seconds per time slice", defaultValue = "900")
	private int timeSlice;

	@CommandLine.Mixin
	private ShpOptions shp;

	@Override
	public Integer call() throws Exception {

		EventsManager eventsManager = EventsUtils.createEventsManager();

		Network network = NetworkUtils.readTimeInvariantNetwork(this.network.toString());

		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
		builder.setTimeslice(timeSlice);
		builder.setMaxTime(maxTime);
		builder.setCalculateLinkTravelTimes(true);
		builder.setCalculateLinkToLinkTravelTimes(false);

		TravelTimeCalculator calculator = builder.build();
		eventsManager.addHandler(calculator);

		VolumesAnalyzer volume = new VolumesAnalyzer(timeSlice, maxTime, network, true);
		eventsManager.addHandler(volume);

		eventsManager.initProcessing();

		EventsUtils.readEvents(eventsManager, events.toString());

		eventsManager.finishProcessing();

		log.info("Writing stats to {}", output);

		List<String> header = Lists.newArrayList("linkId", "time", "avgTravelTime");

		Set<String> modes = volume.getModes();

		for (String mode : modes) {
			header.add("vol_" + mode);
		}

		TravelTime tt = calculator.getLinkTravelTimes();

		ShpOptions.Index index = shp.getShapeFile() != null ? shp.createIndex(ProjectionUtils.getCRS(network), "_") : null;

		int n = volume.getVolumesArraySize();

		try (CSVPrinter writer = new CSVPrinter(IOUtils.getBufferedWriter(output.toString()), CSVFormat.DEFAULT)) {

			writer.printRecord(header);

			for (Link link : network.getLinks().values()) {

				if (index != null && !index.contains(link.getCoord()))
					continue;

				for (int i = 0; i < n; i++) {

					int time = i * timeSlice;
					double avgTt = tt.getLinkTravelTime(link, time, null, null);

					List<Object> row = Lists.newArrayList(link.getId(), time, avgTt);

					for (String mode : modes) {
						int[] vol = volume.getVolumesForLink(link.getId(), mode);

						// can be null (if no vehicle uses this link?)
						if (vol == null)
							row.add(0);
						else
							row.add(vol[i]);
					}

					writer.printRecord(row);
				}
			}
		}

		return 0;
	}
}
