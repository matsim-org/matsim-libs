package org.matsim.application.analysis.traffic.traveltime;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.analysis.traffic.traveltime.api.*;

import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Task to fetch routes from api service.
 */
final class FetchRoutesTask implements Runnable {

	private static final Logger log = LogManager.getLogger(FetchRoutesTask.class);

	private final RouteAPI api;
	private final String apiKey;

	private final List<SampleValidationRoutes.Route> routes;
	private final List<Integer> hours;
	private final Path out;

	FetchRoutesTask(RouteAPI api, String apiKey, List<SampleValidationRoutes.Route> routes, List<Integer> hours, Path out) {
		this.api = api;
		this.apiKey = apiKey;
		this.routes = routes;
		this.hours = hours;
		this.out = out;
	}


	private void fetch() throws Exception {

		// Collect existing entries to support resuming
		Set<Entry> entries = new HashSet<>();
		OpenOption open;
		if (Files.exists(out)) {
			open = StandardOpenOption.APPEND;
			try (CSVParser csv = new CSVParser(Files.newBufferedReader(out), CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
				for (CSVRecord r : csv) {
					entries.add(new Entry(
						Id.createNodeId(r.get("from_node")),
						Id.createNodeId(r.get("to_node")),
						r.get("api"),
						Integer.parseInt(r.get("hour")))
					);
				}
			}

		} else
			open = StandardOpenOption.CREATE_NEW;

		try (RouteValidator val = switch (api) {
			case google -> new GoogleRouteValidator(apiKey);
			case woosmap -> new WoosMapRouteValidator(apiKey);
			case mapbox -> new MapboxRouteValidator(apiKey);
			case here -> new HereRouteValidator(apiKey);
			case tomtom -> new TomTomRouteValidator(apiKey);
		}) {

			try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(out, open), CSVFormat.DEFAULT)) {

				if (open == StandardOpenOption.CREATE_NEW) {
					csv.printRecord("from_node", "to_node", "api", "hour", "dist", "travel_time");
					csv.flush();
				}

				int i = 0;
				int errors = 0;
				for (SampleValidationRoutes.Route route : routes) {
					for (int h : hours) {

						// Skip entries already present
						Entry e = new Entry(route.fromNode(), route.toNode(), val.name(), h);
						if (entries.contains(e))
							continue;

						try {
							RouteValidator.Result res = fetchWithBackoff(val, route, h, 0);
							csv.printRecord(route.fromNode(), route.toNode(), val.name(), h, res.dist(), res.travelTime());

							// reset errors when one request was successful
							errors = 0;
						} catch (RouteValidator.Forbidden ex1) {
							csv.flush();
							log.error("{}: stopping because API indicated key is not valid or quota exhausted", api);
							return;

						} catch (Exception ex) {
							log.warn("Could not retrieve result for route {} {}", api, route, ex);
							errors++;
						}

					}

					csv.flush();

					if (i++ % 50 == 0 && i - 1 > 0) {
						log.info("{}: processed {} routes", api, i - 1);
					}

					if (errors > hours.size()) {
						log.error("{}: stopping because of too many errors", api);
						break;
					}
				}
			}
		}
	}

	/**
	 * Fetch route with increasing delay.
	 */
	private RouteValidator.Result fetchWithBackoff(RouteValidator val, SampleValidationRoutes.Route route, int h, int i) throws InterruptedException {
		try {
			return val.retrieve(route.from(), route.to(), h);
		} catch (RouteValidator.Forbidden fb) {
			// No retry here
			throw fb;
		} catch (Exception e) {
			if (i < 3) {
				long backoff = (long) (10000d * Math.pow(2, i));
				log.warn("Failed to fetch result for {} {}: {} (retrying after {}s)", api, route, e.getMessage(), backoff / 1000);

				Thread.sleep(backoff);
				return fetchWithBackoff(val, route, h, ++i);
			}

			throw e;
		}
	}

	@Override
	public void run() {
		try {
			fetch();
			log.info("API {} finished.", api);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	record Entry(Id<Node> fromNode, Id<Node> toNode, String api, int hour) {
	}

}
