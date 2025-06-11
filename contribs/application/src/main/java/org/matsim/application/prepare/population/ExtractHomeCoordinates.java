package org.matsim.application.prepare.population;

import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@CommandLine.Command(
	name = "extract-home-coordinates",
	description = "Extract the home coordinates of a person"
)
public final class ExtractHomeCoordinates implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(ExtractHomeCoordinates.class);

	@CommandLine.Parameters(paramLabel = "INPUT", arity = "1", description = "Path to input population")
	private Path input;

	@CommandLine.Option(names = "--csv", description = "Write coordinates to csv file")
	private Path csv;

	@CommandLine.Option(names = "--output", description = "Write population with home coordinates as attributes")
	private Path output;

	@CommandLine.Mixin
	private CsvOptions options = new CsvOptions();

	/**
	 * Set and return home coordinate of this person. Can be null if no home activity is known.
	 */
	public static Coord setHomeCoordinate(Person person) {
		for (Plan plan : person.getPlans()) {
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					String actType = ((Activity) planElement).getType();
					if (actType.startsWith("home")) {
						Coord homeCoord = CoordUtils.round(((Activity) planElement).getCoord());

						person.getAttributes().putAttribute("home_x", homeCoord.getX());
						person.getAttributes().putAttribute("home_y", homeCoord.getY());

						return homeCoord;
					}
				}
			}
		}

		return null;
	}

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());

		Map<Person, Coord> coords = new LinkedHashMap<>();

		for (Person person : population.getPersons().values()) {
			Coord coord = setHomeCoordinate(person);
			if (coord != null)
				coords.put(person, coord);
		}

		if (csv != null) {

			log.info("Writing csv to {}", csv);

			try (CSVPrinter printer = options.createPrinter(csv)) {

				printer.printRecord("person", "home_x", "home_y");

				for (Map.Entry<Person, Coord> e : coords.entrySet()) {
					printer.printRecord(e.getKey().getId().toString(), e.getValue().getX(), e.getValue().getY());
				}
			}
		}

		if (output != null) {
			PopulationUtils.writePopulation(population, output.toString());
		}

		return 0;
	}
}
