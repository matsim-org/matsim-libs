package org.matsim.application.analysis.accessibility;

import com.opencsv.CSVWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;

import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import picocli.CommandLine;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.*;

import java.util.zip.GZIPInputStream;

@CommandLine.Command(
	name = "prepare-pois", description = "Prepare POIs.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"persons.csv"
	}
)




public class PrepareHouseholds implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(PrepareHouseholds.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(PrepareHouseholds.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(PrepareHouseholds.class);
	public SimpleFeatureBuilder builder;


	public static void main(String[] args) {
		new PrepareHouseholds().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		String fileName = ApplicationUtils.matchInput("output_persons.csv.gz", input.getRunDirectory()).toString();

		try (
			InputStream fileStream = new FileInputStream(fileName);
			InputStream gzipStream = new GZIPInputStream(fileStream);
			Reader reader = new InputStreamReader(gzipStream)
		) {
			CsvReadOptions options = CsvReadOptions.builder(reader)
				.separator(';')     // Semicolon-separated
				.header(true)       // Assumes first row is header
				.build();

			Table table = Table.read().usingOptions(options);

//			System.out.println(table.structure());
//			System.out.println(table.print());

			Table filtered = table.where(table.doubleColumn("home_x").isNotMissing().and(table.doubleColumn("home_y").isNotMissing()));

			Table result = filtered.selectColumns("home_x", "home_y");

			result.write().csv(input.getRunDirectory().resolve("analysis/accessibility/persons.csv").toString());
		}

		return 0;
	}


}
