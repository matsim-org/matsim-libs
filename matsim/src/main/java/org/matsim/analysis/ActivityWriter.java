package org.matsim.analysis;

import jakarta.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActivityWriter {

	@Inject
	private Config config;

	@Inject
	private ExperiencedPlansService experiencedPlansService;

	@Inject
	private OutputDirectoryHierarchy outputDirectoryHierarchy;

	private static final Logger log = LogManager.getLogger(ActivityWriter.class);

	void writeCsv(int iteration) {
		log.info("Writing all Activities to " + Controler.DefaultFiles.activitiescsv);

		List<String> attributes = prepareAttributes();
		String[] header = prepareHeader(attributes);

		try{
			BufferedWriter bufferedWriter = IOUtils.getBufferedWriter(outputDirectoryHierarchy.getIterationFilename(iteration, Controler.DefaultFiles.activitiescsv));
			CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.Builder.create()
																					.setDelimiter(config.global().getDefaultDelimiter().charAt(0))
																					.setHeader(header).build());

			for (Map.Entry<Id<Person>, Plan> e : experiencedPlansService.getExperiencedPlans().entrySet()) {
				writeActivitiesPerPerson(e.getKey(), e.getValue(), attributes, csvPrinter);
			}

			csvPrinter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("...done");
	}

	private static void writeActivitiesPerPerson(Id<Person> personId, Plan plan, List<String> attributes, CSVPrinter csvPrinter) throws IOException {
		int i = 0;
		for (Activity act : TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities)) {

			List<Object> line = new ArrayList<>();
			int id = ++i;
			line.add(personId);
			line.add(id);
			line.add(personId.toString() + "_" + id);
			line.add(act.getType());

			line.add(act.getStartTime().isDefined() ? act.getStartTime().seconds() : "");
			line.add(act.getEndTime().isDefined() ? act.getEndTime().seconds() : "");
			line.add(act.getMaximumDuration().isDefined() ? act.getMaximumDuration().seconds() : "");
			line.add(act.getLinkId() != null ? act.getLinkId() : "");
			line.add(act.getFacilityId() != null ? act.getFacilityId(): "");

			if (act.getCoord() != null) {
				line.add(act.getCoord().getX());
				line.add(act.getCoord().getY());
			} else  {
				line.add("");
				line.add("");
			}

			for (String attribute : attributes) {
				Object value = plan.getAttributes().getAttribute(attribute);
				String result = value != null ? String.valueOf(value) : "";
				line.add(result);
			}

			csvPrinter.printRecord(line);
		}
	}

	private List<String> prepareAttributes() {
		return experiencedPlansService.getExperiencedPlans().values().stream()
                                      .flatMap(p -> TripStructureUtils.getActivities(p, TripStructureUtils.StageActivityHandling.ExcludeStageActivities).stream())
                                      .flatMap(act -> act.getAttributes().getAsMap().keySet().stream())
                                      .sorted().distinct().toList();
	}

	private static String[] prepareHeader(List<String> attributes) {
		List<String> header = new ArrayList<>();
		header.add("person");
		header.add("activity_number");
		header.add("activity_id");
		header.add("activity_type");
		header.add("start_time");
		header.add("end_time");
		header.add("maximum_duration");
		header.add("link_id");
		header.add("facility_id");
		header.add("coord_x");
		header.add("coord_y");
		header.addAll(attributes);
		return header.toArray(String[]::new);
	}
}
