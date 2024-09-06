package org.matsim.analysis;

import jakarta.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PersonWriter {

	@Inject
	Config config;

	@Inject
	Scenario scenario;

	@Inject
	OutputDirectoryHierarchy outputDirectoryHierarchy;

	private static final Logger log = LogManager.getLogger(PersonWriter.class);

	void writeCsv() {
		log.info("Writing all Persons to " + Controler.DefaultFiles.personscsv);

		List<String> attributes = prepareAttributes();
		String[] header = prepareHeader(attributes);

		try {
			BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(outputDirectoryHierarchy.getOutputFilename(Controler.DefaultFiles.personscsv));
			CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.Builder.create()
																					.setDelimiter(config.global().getDefaultDelimiter().charAt(0))
																					.setHeader(header).build());
			for (Person p : scenario.getPopulation().getPersons().values()) {
				writePerson(p, attributes, csvPrinter);
			}

			csvPrinter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("...done");
	}

	private void writePerson(Person p, List<String> attributes, CSVPrinter csvPrinter) throws IOException {
		if (p.getSelectedPlan() == null) {
			log.error("Found person without a selected plan: " + p.getId().toString() + " will not be added to output_persons.csv");
			return;
		}
		List<String> line = new ArrayList<>();
		line.add(p.getId().toString());
		line.add(p.getSelectedPlan().getScore() == null ? "null" : p.getSelectedPlan().getScore().toString());
		String x = "";
		String y = "";
		String actType = "";
		if (!p.getSelectedPlan().getPlanElements().isEmpty()) {
			Activity firstAct = (Activity) p.getSelectedPlan().getPlanElements().get(0);
			if (firstAct.getCoord() != null) {
				x = Double.toString(firstAct.getCoord().getX());
				y = Double.toString(firstAct.getCoord().getY());
			}
			actType = firstAct.getType();
		}
		line.add(x);
		line.add(y);
		line.add(actType);
		for (String attribute : attributes) {
			Object value = p.getAttributes().getAttribute(attribute);
			String result = value != null ? String.valueOf(value) : "";
			line.add(result);
		}
		csvPrinter.printRecord(line);
	}

	private String[] prepareHeader(List<String> attributes) {
		List<String> header = new ArrayList<>();
		header.add("person");
		header.add("executed_score");
		header.add("first_act_x");
		header.add("first_act_y");
		header.add("first_act_type");
		header.addAll(attributes);
		return header.toArray(String[]::new);
	}

	private List<String> prepareAttributes() {
		List<String> attributes = scenario.getPopulation().getPersons().values().parallelStream()
										  .flatMap(p -> p.getAttributes().getAsMap().keySet().stream()).distinct()
										  .collect(Collectors.toList());
		attributes.remove("vehicles");
		return attributes;
	}
}
