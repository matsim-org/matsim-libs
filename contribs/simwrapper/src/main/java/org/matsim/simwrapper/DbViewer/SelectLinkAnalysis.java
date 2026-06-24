package org.matsim.simwrapper.DbViewer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.dashboard.SelectLinkAnalysisDashboard;
import picocli.CommandLine;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;

import java.nio.file.Path;

@CommandLine.Command(name = "select-link-analysis", description = "Compute Necessary sqlite, csv and parquet files for Select-Link-Analysis")
public class SelectLinkAnalysis  implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(SelectLinkAnalysis.class);

	@CommandLine.Option(names = "--events", description = "Input event file", required = true)
	private Path eventsPath;

	@CommandLine.Option(names = "--population", description = "Input persons file", required = true)
	private Path populationPath;

	@CommandLine.Option(names = "--output", description = "Output directory", required = true)
	private Path outputPath;

	@CommandLine.Mixin
	private CsvOptions csv = new CsvOptions();

	public static void main(String[] args) {
		new SelectLinkAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		EventsManager eventsManager = EventsUtils.createEventsManager();

		DbEventHandler DbEventHandler = new DbEventHandler(outputPath.toString());

//		1. Initialize a config instance
		Config config = ConfigUtils.createConfig();
		config.controller().setOutputDirectory(outputPath.toString());

//		2. Create an empty population container
		Population population = PopulationUtils.createPopulation(config);

//		 3. Populate the container by reading the file
		PopulationUtils.readPopulation(population, populationPath.toString());

//
////		Population population = PopulationUtils.readPopulation();
//
		AgentTable agentTable = new AgentTable(population, outputPath.toString());
		agentTable.run();

		eventsManager.addHandler(DbEventHandler);

		eventsManager.initProcessing();

		EventsUtils.readEvents(eventsManager, eventsPath.toString());

		log.info("Writing sla files to {}", outputPath);

		eventsManager.finishProcessing();

		DbEventHandler.finish();

		SimWrapper sw = SimWrapper.create();
		sw.addDashboard(new SelectLinkAnalysisDashboard());
		sw.generate(outputPath, true);

		return null;
	}


}
