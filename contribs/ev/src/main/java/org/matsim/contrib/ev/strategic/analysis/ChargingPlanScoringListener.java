package org.matsim.contrib.ev.strategic.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.strategic.plan.ChargingPlan;
import org.matsim.contrib.ev.strategic.plan.ChargingPlans;
import org.matsim.contrib.ev.withinday.WithinDayEvEngine;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * Analysis class that writes out the minimum, maximum, mean, and selected
 * charging scores per selected agent plan.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargingPlanScoringListener implements IterationEndsListener {
	static public final String SCORING_PATH = "sevc_scores.csv";

	private final Population population;
	private final String outputPath;

	public ChargingPlanScoringListener(Population population, OutputDirectoryHierarchy outputHierarchy) {
		this.population = population;
		this.outputPath = outputHierarchy.getOutputFilename(SCORING_PATH);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			boolean writeHeader = !(new File(outputPath).exists());
			BufferedWriter writer = IOUtils.getAppendingBufferedWriter(outputPath);

			if (writeHeader) {
				List<String> header = new LinkedList<>();
				header.add("iteration");
				header.add("min_mean");
				header.add("max_mean");
				header.add("mean_mean");
				header.add("selected_mean");
				header.add("persons");

				writer.write(String.join(";", header) + "\n");
			}

			List<String> row = new LinkedList<>();
			row.add(String.valueOf(event.getIteration()));

			int count = 0;

			double min = 0.0;
			double max = 0.0;
			double mean = 0.0;
			double selected = 0.0;

			for (Person person : population.getPersons().values()) {
				if (WithinDayEvEngine.isActive(person)) {
					ChargingPlans chargingPlans = ChargingPlans.get(person.getSelectedPlan());

					if (chargingPlans.getChargingPlans().size() > 0) {
						min += chargingPlans.getChargingPlans().stream().mapToDouble(ChargingPlan::getScore).min()
								.getAsDouble();
						max += chargingPlans.getChargingPlans().stream().mapToDouble(ChargingPlan::getScore).max()
								.getAsDouble();
						mean += chargingPlans.getChargingPlans().stream().mapToDouble(ChargingPlan::getScore).average()
								.getAsDouble();
						selected += chargingPlans.getSelectedPlan().getScore();

						count++;
					}
				}
			}

			row.add(String.valueOf(min / count));
			row.add(String.valueOf(max / count));
			row.add(String.valueOf(mean / count));
			row.add(String.valueOf(selected / count));
			row.add(String.valueOf(count));

			writer.write(String.join(";", row) + "\n");

			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
