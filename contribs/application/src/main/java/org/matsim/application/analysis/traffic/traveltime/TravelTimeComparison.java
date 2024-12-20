package org.matsim.application.analysis.traffic.traveltime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.BufferedReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static tech.tablesaw.aggregate.AggregateFunctions.mean;

/**
 * See {@link SampleValidationRoutes}, which produces the required --input-ref file.
 */
@CommandLine.Command(
	name = "travel-time-comparison",
	description = "Compare travel time with routes from API."
)
@CommandSpec(
	requireEvents = true,
	requireNetwork = true,
	produces = {"travel_time_comparison_by_hour.csv", "travel_time_comparison_by_route.csv"}
)
public class TravelTimeComparison implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(TravelTimeComparison.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(TravelTimeComparison.class);

	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(TravelTimeComparison.class);

	@CommandLine.Option(names = "--input-ref", description = "File with reference data", required = true)
	private String apiFile;

	@CommandLine.Option(names = "--modes", description = "Network modes to analyze", defaultValue = TransportMode.car, split = ",")
	private Set<String> networkModes;

	public static void main(String[] args) {
		new TravelTimeComparison().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Table data;
		try (BufferedReader reader = IOUtils.getBufferedReader(apiFile)) {
			data = Table.read().csv(CsvReadOptions.builder(reader).columnTypesPartial(Map.of(
				"from_node", ColumnType.STRING,
				"to_node", ColumnType.STRING
			)).build());
		}

		Network network = input.getNetwork();
		TravelTime tt = collectTravelTimes(network).getLinkTravelTimes();
		TravelTime fs = new FreeSpeedTravelTime();

		OnlyTimeDependentTravelDisutility util = new OnlyTimeDependentTravelDisutility(tt);

		LeastCostPathCalculator congestedRouter = new SpeedyALTFactory().createPathCalculator(network, util, tt);
		LeastCostPathCalculator freeflowRouter = new SpeedyALTFactory().createPathCalculator(network, new OnlyTimeDependentTravelDisutility(fs), fs);

		data.addColumns(
			DoubleColumn.create("simulated", data.rowCount()),
			DoubleColumn.create("free_flow", data.rowCount())
		);

		for (Row row : data) {
			LeastCostPathCalculator.Path congested = computePath(network, congestedRouter, row);

			// Skip if path is not found
			if (congested == null) {
				row.setDouble("simulated", Double.NaN);
				continue;
			}

			double dist = congested.links.stream().mapToDouble(Link::getLength).sum();
			double speed = 3.6 * dist / congested.travelTime;

			row.setDouble("simulated", speed);

			LeastCostPathCalculator.Path freeflow = computePath(network, freeflowRouter, row);
			dist = freeflow.links.stream().mapToDouble(Link::getLength).sum();
			speed = 3.6 * dist / freeflow.travelTime;

			row.setDouble("free_flow", speed);
		}

		data = data.dropWhere(data.doubleColumn("simulated").isMissing());

		data.addColumns(
			data.doubleColumn("simulated").subtract(data.doubleColumn("mean")).setName("bias")
		);

		data.addColumns(data.doubleColumn("bias").abs().setName("abs_error"));

		data.write().csv(output.getPath("travel_time_comparison_by_route.csv").toFile());

		List<String> columns = List.of("min", "max", "mean", "std", "simulated", "free_flow", "bias", "abs_error");

		Table aggr = data.summarize(columns, mean).by("hour");

		for (Column<?> column : aggr.columns()) {
			String name = column.name();
			if (name.startsWith("Mean"))
				column.setName(name.substring(6, name.length() - 1));
		}

		aggr.write().csv(output.getPath("travel_time_comparison_by_hour.csv").toFile());

		return 0;
	}

	private LeastCostPathCalculator.Path computePath(Network network, LeastCostPathCalculator router, Row row) {
		Node fromNode = network.getNodes().get(Id.createNodeId(row.getString("from_node")));
		Node toNode = network.getNodes().get(Id.createNodeId(row.getString("to_node")));

		if (fromNode == null) {
			log.error("Node {} not found in network", row.getString("from_node"));
			return null;
		}

		if (toNode == null) {
			log.error("Node {} not found in network", row.getString("to_node"));
			return null;
		}

		return router.calcLeastCostPath(fromNode, toNode, row.getInt("hour") * 3600, null, null);
	}

	private TravelTimeCalculator collectTravelTimes(Network network) {
		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
		builder.setCalculateLinkTravelTimes(true);
		builder.setMaxTime(86400);
		builder.setTimeslice(900);
		builder.setAnalyzedModes(networkModes);
		builder.setFilterModes(true);

		TravelTimeCalculator travelTimes = builder.build();

		EventsManager manager = EventsUtils.createEventsManager();

		manager.addHandler(travelTimes);

		manager.initProcessing();
		EventsUtils.readEvents(manager, input.getEventsPath());
		manager.finishProcessing();

		return travelTimes;
	}

}
