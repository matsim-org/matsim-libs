package org.matsim.application.analysis.population;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
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
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.PersonVehicles;
import picocli.CommandLine;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "population-attribute", description = "Generates statistics of the population attributes.")
@CommandSpec(requirePopulation = true, produces = {"amount_per_age_group.csv", "amount_per_sex_group.csv", "total_agents.csv", "average_income_per_age_group.csv"})
public class PopulationAttributeAnalysis implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(PopulationAttributeAnalysis.class);
	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(PopulationAttributeAnalysis.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(PopulationAttributeAnalysis.class);
	private final Int2IntMap amountPerAgeGroup = new Int2IntOpenHashMap();
	private final HashMap<String, Integer> amountPerSexGroup = new HashMap<>();
	private final HashMap<Integer, List<Double>> averageIncomeOverAge = new HashMap<>(); // <Age, <Number, Total Income>>
	private Integer totalAgents = 0;
	private final List<Double> allIncomes = new ArrayList<>();
	private final List<Integer> allAges = new ArrayList<>();

	public static void main(String[] args) {
		new PopulationAttributeAnalysis().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Population population = input.getPopulation();

		for (Person person : population.getPersons().values()) {

			// Count all algents
			totalAgents++;

//			System.out.println(System.lineSeparator() + "#############################################" + System.lineSeparator());
//			amountPerAgeGroup.computeIfAbsent()

			if (person.getAttributes().getAsMap().containsKey("age") && person.getAttributes().getAsMap().containsKey("income") && person.getAttributes().getAsMap().containsKey("sex")) {
				this.splitIncomeOverAgeAndSex((int) person.getAttributes().getAttribute("age"), (double) person.getAttributes().getAttribute("income"));
			}

			Map<String, Object> map = person.getAttributes().getAsMap();
			for (Map.Entry<String, Object> entry : map.entrySet()) {

				if (entry.getKey().equals("income")) {
					allIncomes.add(Double.valueOf(entry.getValue().toString()));
				}

				if (entry.getKey().equals("vehicles")) System.out.println(((PersonVehicles) entry.getValue()).getModeVehicles().toString());
				else System.out.println(entry.getValue().toString());

				if (entry.getKey().equals("sex")) {
					String sex = (String) entry.getValue();
					this.splitAgentsIntoSex(sex);
				}

				if (entry.getKey().equals("age")) {
					Integer age = (Integer) entry.getValue();
					this.splitAgentsIntoAgeGroup(age);
					this.allAges.add(age);
				}
			}
		}

		// Agents per age group
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("amount_per_age_group.csv").toString()), CSVFormat.DEFAULT)) {
			printer.printRecord("Age", "# Number");
			for (Int2IntMap.Entry entry : amountPerAgeGroup.int2IntEntrySet()) {
				printer.printRecord(String.valueOf(entry.getIntKey()), entry.getIntValue());
			}
		} catch (IOException ex) {
			log.error(ex);
		}

		// Total Agents
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("average_income_per_age_group.csv").toString()), CSVFormat.DEFAULT)) {
			printer.printRecord("Age", "avg. Income");
			for (Map.Entry<Integer, List<Double>> entry : averageIncomeOverAge.entrySet()) {
				printer.printRecord(entry.getKey(), this.calculateMeanFromDoubleArray(entry.getValue()));
			}
		} catch (IOException ex) {
			log.error(ex);
		}

		// Average Income Per Age
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("total_agents.csv").toString()), CSVFormat.DEFAULT)) {
			printer.printRecord("Total Agents", totalAgents, "user-group");
			printer.printRecord("Average Age", new DecimalFormat("#.0#").format(this.calculateMeanFromIntegerArray(allAges)));
			printer.printRecord("Average Income", new DecimalFormat("#.0#").format(this.calculateMeanFromDoubleArray(allIncomes)), "money-check-dollar");
		} catch (IOException ex) {
			log.error(ex);
		}

		// Agents per sex group
		try (CSVPrinter printer = new CSVPrinter(IOUtils.getBufferedWriter(output.getPath("amount_per_sex_group.csv").toString()), CSVFormat.DEFAULT)) {
			printer.printRecord(amountPerSexGroup.keySet());
			printer.printRecord(amountPerSexGroup.values());
		} catch (IOException ex) {
			log.error(ex);
		}

		return 0;
	}

	private double calculateMeanFromIntegerArray(List<Integer> allAges) {
		Double sum = 0.;
		for (Integer income : allAges) {
			sum += income;
		}
		return Math.round((sum / allAges.size()) * 100.0) / 100.0;
	}

	private double calculateMeanFromDoubleArray(List<Double> value) {
		Double sum = 0.;
		for (Double income : value) {
			sum += income;
		}
		return Math.round((sum / value.size()) * 100.0) / 100.0;
	}

	private void splitIncomeOverAgeAndSex(int age, double income) {
		// Rounded Age: 9 -> 10; 10 -> 10; 11 -> 20
		int roundedAge = Math.round(((age - 1)/10) + 1) * 10;
		double roundedIncome = (Math.round(((income - 1) / 100) + 1) * 100);

		if (!averageIncomeOverAge.containsKey(roundedAge)) {
			averageIncomeOverAge.put(roundedAge, new ArrayList<>());
		}
		averageIncomeOverAge.get(roundedAge).add(roundedIncome);
	}

	private void splitAgentsIntoSex(String sex) {
		if (sex.equals("m")) sex = "Male";
		if (sex.equals("f")) sex = "Female";
		if (amountPerSexGroup.containsKey(sex)) amountPerSexGroup.put(sex, amountPerSexGroup.get(sex) + 1);
		else amountPerSexGroup.put(sex, 1);
	}

	private void splitAgentsIntoAgeGroup(Integer age) {
		// Rounded Age: 9 -> 10; 10 -> 10; 11 -> 20
		int roundedAge = Math.round(((age - 1)/10) + 1) * 10;
		if (amountPerAgeGroup.keySet().contains(roundedAge)) amountPerAgeGroup.put(roundedAge, amountPerAgeGroup.get(roundedAge) + 1);
		else amountPerAgeGroup.put(roundedAge, 1);
	}

}
