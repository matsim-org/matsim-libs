package org.matsim.application.analysis.population;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;
import scala.util.parsing.combinator.testing.Str;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@CommandLine.Command(name = "population-attribute", description = "Generates statistics of the population attributes.")
@CommandSpec(requirePopulation = true, produces = {"amount_per_age_group.csv"})
public class PopulationAttributeAnalysis implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(PopulationAttributeAnalysis.class);
	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(PopulationAttributeAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(PopulationAttributeAnalysis.class);
	private final Int2IntMap amountPerAgeGroup = new Int2IntOpenHashMap();

	public static void main(String[] args) {
		new PopulationAttributeAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = input.getPopulation();

		for (Person person : population.getPersons().values()) {

//			person.getAttributes()
//			amountPerAgeGroup.computeIfAbsent()

			Map<String, Object> map = person.getAttributes().getAsMap();

			for (Map.Entry<String, Object> entry : map.entrySet()) {

				if (entry.getKey().equals("age")) {

					Integer age = (Integer) entry.getValue();

					if (age < 10) {
						if (amountPerAgeGroup.keySet().contains(10)) amountPerAgeGroup.put(10, amountPerAgeGroup.get(10) + 1);
						else amountPerAgeGroup.put(10, 1);
					} else if (age < 20) {
						if (amountPerAgeGroup.keySet().contains(20)) amountPerAgeGroup.put(20, amountPerAgeGroup.get(20) + 1);
						else amountPerAgeGroup.put(20, 1);
					} else if (age < 30) {
						if (amountPerAgeGroup.keySet().contains(30)) amountPerAgeGroup.put(30, amountPerAgeGroup.get(30) + 1);
						else amountPerAgeGroup.put(30, 1);
					} else if (age < 40) {
						if (amountPerAgeGroup.keySet().contains(40)) amountPerAgeGroup.put(40, amountPerAgeGroup.get(40) + 1);
						else amountPerAgeGroup.put(40, 1);
					} else if (age < 50) {
						if (amountPerAgeGroup.keySet().contains(50)) amountPerAgeGroup.put(50, amountPerAgeGroup.get(50) + 1);
						else amountPerAgeGroup.put(50, 1);
					} else if (age < 70) {
						if (amountPerAgeGroup.keySet().contains(70)) amountPerAgeGroup.put(70, amountPerAgeGroup.get(70) + 1);
						else amountPerAgeGroup.put(70, 1);
					} else if (age < 80) {
						if (amountPerAgeGroup.keySet().contains(80)) amountPerAgeGroup.put(80, amountPerAgeGroup.get(80) + 1);
						else amountPerAgeGroup.put(80, 1);
					} else if (age < 90) {
						if (amountPerAgeGroup.keySet().contains(90)) amountPerAgeGroup.put(90, amountPerAgeGroup.get(90) + 1);
						else amountPerAgeGroup.put(90, 1);
					} else {
						if (amountPerAgeGroup.keySet().contains(9999)) amountPerAgeGroup.put(9999, amountPerAgeGroup.get(9999) + 1);
						else amountPerAgeGroup.put(9999, 1);
					}


				}

			}



		}

		System.out.println(amountPerAgeGroup.keySet());
		System.out.println(amountPerAgeGroup.values().toString());
		System.out.println("ERGEBNIS");

		// Stuck agents per mode
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("amount_per_age_group.csv").toString()), CSVFormat.DEFAULT)) {
			printer.printRecord("Age", "# Number");
			for (Int2IntMap.Entry entry : amountPerAgeGroup.int2IntEntrySet()) {
				String age = String.valueOf(entry.getIntKey());
				if (age.equals("9999")) age = "100";
				printer.printRecord(age, entry.getIntValue());
			}
		} catch (IOException ex) {
			log.error(ex);
		}

		return 0;
	}

}
