package org.matsim.application.analysis.population;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(
		name = "activity-length-analysis",
		description = "Analyze the length of activity"
)
public class ActivityLengthAnalysis implements MATSimAppCommand {
	@CommandLine.Option(names = "--population", description = "Path to input population", required = true)
	private String populationPath;

	@CommandLine.Option(names = "--reference-population", description = "Path to reference population", defaultValue = "")
	private String referencePopulationPath;

	@CommandLine.Option(names = "--output-folder", description = "Path to analysis output folder", required = true)
	private Path outputFolder;

	public static void main(String[] args) {
		new ActivityLengthAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		List<Double> activityDurations = new ArrayList<>();
		List<Double> referenceActivityDurations = new ArrayList<>();

		Population population = PopulationUtils.readPopulation(populationPath);
		for (Person person : population.getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					double startTime = ((Activity) planElement).getStartTime().orElse(0);
					double endTime = ((Activity) planElement).getEndTime().orElse(30 * 3600);
					double duration = endTime - startTime;
					activityDurations.add(duration);
				}
			}
		}

		CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputFolder + "/activity-length.csv"), CSVFormat.TDF);
		csvWriter.printRecord("activity_duration_in_seconds");
		for (double activityDuration : activityDurations) {
			csvWriter.printRecord(Double.toString(activityDuration));
		}
		csvWriter.close();

		if (!referencePopulationPath.equals("")) {
			Population referencePopulation = PopulationUtils.readPopulation(referencePopulationPath);
			for (Person person : referencePopulation.getPersons().values()) {
				for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
					if (planElement instanceof Activity) {
						double startTime = ((Activity) planElement).getStartTime().orElse(0);
						double endTime = ((Activity) planElement).getEndTime().orElse(30 * 3600);
						double duration = endTime - startTime;
						referenceActivityDurations.add(duration);
					}
				}
			}

			CSVPrinter csvWriter2 = new CSVPrinter(new FileWriter(outputFolder + "/reference-activity-length.csv"), CSVFormat.TDF);
			csvWriter2.printRecord("activity_duration_in_seconds");
			for (double activityDuration : referenceActivityDurations) {
				csvWriter2.printRecord(Double.toString(activityDuration));
			}
			csvWriter2.close();
		}
		return 0;
	}
}
