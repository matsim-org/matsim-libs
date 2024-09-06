package org.matsim.application.analysis.population;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import tech.tablesaw.api.*;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.util.Map;

@CommandLine.Command(name = "trip-filter", description = "Extracts trips and separate them by mode.")
@CommandSpec(
	requires = {"trips.csv"},
	produces = {"trips_per_mode_%s.csv"}
)
public class FilterTripModes implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(FilterTripModes.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(FilterTripModes.class);

	@CommandLine.Mixin
	private ShpOptions shp;

	public static void main(String[] args) {
		new FilterTripModes().execute(args);
	}


	@Override
	public Integer call() throws Exception {

		Table trips = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(input.getPath()))
			.columnTypesPartial(Map.of("person", ColumnType.TEXT, "main_mode", ColumnType.STRING))
			.sample(false)
			.separator(';').build());

		for (String mainMode : trips.stringColumn("main_mode").unique()) {

			Table table = trips.where(trips.stringColumn("main_mode").isEqualTo(mainMode));

			table.write().csv(output.getPath("trips_per_mode_%s.csv", mainMode).toFile());
		}

		return 0;
	}
}
