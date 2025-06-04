package org.matsim.freightDemandGeneration;

import com.google.common.base.Joiner;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controller;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.matsim.freightDemandGeneration.DemandReaderFromCSV.getHomeCoord;

/**
 * This class can be used to generate demand for parcel delivery shipments by using the tool FreightDemandGeneration.
 *
 * @see FreightDemandGeneration
 * @Author Ricardo Ewert (based on the work of the master thesis of Ana Ueberhorst)
 */
public class DemandGenerationSpecificationForParcelDelivery extends DefaultDemandGenerationSpecification {
	//	private static double PACKAGES_PER_PERSON = 0.0686; //default value
	private static double PACKAGES_PER_PERSON = 0.14; //default value

	private static double PACKAGES_PER_RECIPIENT = 1.6; //default value
	private static final Logger log = LogManager.getLogger(DemandGenerationSpecificationForParcelDelivery.class);
	private static final RandomGenerator rand = new MersenneTwister(4711);
	private static final Random random = new Random(4711);
	private static final Joiner JOIN = Joiner.on("\t");

	private static DemandDistributionOption demandDistributionOption;
	protected static HashMap<Person, Integer> parcelsPerPerson = new HashMap<>();

	/**
	 * Gives the share of each age group for all online shoppers in Germany.
	 * E.g., 20.7 percent of online shoppers in Germany were aged 30 to 39 years old.
	 */
	private static final Map<AgeGroup, Double> demandDistributionPerAgeGroup = new TreeMap<>();

	/**
	 * Gives the share of the persons in each age group who have a remainingDemand.
	 * E.g., over 78 percent of 25- to 45-year-olds in Germany had ordered and purchased products online in the past three months.
	 */
	private static final Map<AgeGroup, EnumeratedDistribution<String>> ageGroupDemandShare = new TreeMap<>();

	private RouletteWheel rouletteWheel = null;

	protected enum DemandDistributionOption {
		toRandomPersons, toPersonsByAge
	}

	public DemandGenerationSpecificationForParcelDelivery(Double packagesPerPerson, Double packagesPerRecipient, boolean useAgeGroups) {
		if (packagesPerPerson != null) {
			PACKAGES_PER_PERSON = packagesPerPerson;
		}
		if (packagesPerRecipient != null) {
			PACKAGES_PER_RECIPIENT = packagesPerRecipient;
		}
		if (useAgeGroups) {
			demandDistributionOption = DemandDistributionOption.toPersonsByAge;
			createAgeGroupDemandShare();
		} else {
			demandDistributionOption = DemandDistributionOption.toRandomPersons;
		}
	}

	private static AgeGroup getAgeGroup(Set<AgeGroup> ageGroupDemandShare, int age) {
		for (AgeGroup ageGroup : ageGroupDemandShare) {
			if (ageGroup.getLowerBound() <= age && age <= ageGroup.getUpperBound()) {
				return ageGroup;
			}
		}
		return null;
	}

	/**
	 * Gives the share of the persons in each age group who have a remainingDemand.
	 * E.g., over 78 percent of 25- to 45-year-olds in Germany had ordered and purchased products online in the past three months.
	 */
	private void createAgeGroupDemandShare() {
		List<Pair<String, Double>> ageBoundsList;
		ageBoundsList = List.of(
			new Pair<>("NoDemand", 1.0),
			new Pair<>("WithDemand", 0.0)
		);
		ageGroupDemandShare.put(new AgeGroup(0, 15), new EnumeratedDistribution<>(rand, ageBoundsList));
		ageGroupDemandShare.put(new AgeGroup(76, 1000), new EnumeratedDistribution<>(rand, ageBoundsList));
		ageBoundsList = List.of(
			new Pair<>("NoDemand", 0.285),
			new Pair<>("WithDemand", 0.715)
		);
		ageGroupDemandShare.put(new AgeGroup(16, 25), new EnumeratedDistribution<>(rand, ageBoundsList));
		ageBoundsList = List.of(
			new Pair<>("NoDemand", 0.218),
			new Pair<>("WithDemand", 0.782)
		);
		ageGroupDemandShare.put(new AgeGroup(26, 45), new EnumeratedDistribution<>(rand, ageBoundsList));

		ageBoundsList = List.of(
			new Pair<>("NoDemand", 0.338),
			new Pair<>("WithDemand", 0.662)
		);
		ageGroupDemandShare.put(new AgeGroup(46, 65), new EnumeratedDistribution<>(rand, ageBoundsList));
		ageBoundsList = List.of(
			new Pair<>("NoDemand", 0.567),
			new Pair<>("WithDemand", 0.433)
		);
		ageGroupDemandShare.put(new AgeGroup(66, 75), new EnumeratedDistribution<>(rand, ageBoundsList));
	}

	/**
	 * Gives the share of each age group for all online shoppers in Germany.
	 * E.g., 20.7 percent of online shoppers in Germany were aged 30 to 39 years old.
	 */
	private static void createDemandDistributionPerAgeGroup() {
		demandDistributionPerAgeGroup.put(new AgeGroup(0, 13), 0.0);
		demandDistributionPerAgeGroup.put(new AgeGroup(14, 19), 0.074);
		demandDistributionPerAgeGroup.put(new AgeGroup(20, 29), 0.185);
		demandDistributionPerAgeGroup.put(new AgeGroup(30, 39), 0.207);
		demandDistributionPerAgeGroup.put(new AgeGroup(40, 49), 0.171);
		demandDistributionPerAgeGroup.put(new AgeGroup(50, 59), 0.189);
		demandDistributionPerAgeGroup.put(new AgeGroup(60, 69), 0.115);
		demandDistributionPerAgeGroup.put(new AgeGroup(70, 1000), 0.059);
	}

	private void createRouletteWheel(int demandToDistributed) {
		PoissonDistribution poisson = new PoissonDistribution(PACKAGES_PER_RECIPIENT - 1);
		Frequency frequency = new Frequency();
		Map<Integer, Double> probabilities = new HashMap<>();
		int upsamplingFactor = 1;
		int totalRounded = 0;
		// Step 1: Calculate raw probabilities and rounded frequencies
		for (int j = 0; j < 11; j++) {
			int value = (j + 1) * upsamplingFactor;
			double probability = poisson.probability(j) * demandToDistributed;
			probabilities.put(value, probability); // Store for later adjustment
			int roundedFrequency = (int) Math.round(probability);
			frequency.incrementValue(value, roundedFrequency);
			totalRounded += roundedFrequency;
		}

		// Step 2: Adjust frequencies to match the exact demandToDistributed
		int difference = demandToDistributed - totalRounded;
		if (difference != 0) {
			// Sort probabilities by their fractional part to prioritize adjustment
			List<Map.Entry<Integer, Double>> sorted = probabilities.entrySet().stream()
				.sorted((a, b) -> Double.compare(
					Math.abs(b.getValue() - Math.round(b.getValue())), // Descending order
					Math.abs(a.getValue() - Math.round(a.getValue()))
				))
				.toList();

			for (Map.Entry<Integer, Double> entry : sorted) {
				if (difference == 0) break;
				int value = entry.getKey();
				int adjustment = difference > 0 ? 1 : -1;
				frequency.incrementValue(value, adjustment);
				difference -= adjustment;
			}
		}
		// Step 3: Build the roulette wheel
		rouletteWheel = RouletteWheel.Builder.newInstance(frequency).setRandom(random).build();
	}

	@Override
	public int getDemandToDistribute(DemandReaderFromCSV.DemandInformationElement demandInformationElement,
									 HashMap<Id<Person>, Person> possiblePersonsFirstJobElement,
									 HashMap<Id<Person>, Person> possiblePersonsSecondJobElement) {

		int demandToDistribute = (int) Math.round(PACKAGES_PER_PERSON * possiblePersonsSecondJobElement.size());
		log.info("Demand for this carrier is set to {} with {} Demand units per person (Possible persons: {}).", demandToDistribute,
			PACKAGES_PER_PERSON, possiblePersonsSecondJobElement.size());
		if (demandDistributionOption == DemandDistributionOption.toPersonsByAge) {
			getAgeDistribution(possiblePersonsSecondJobElement);
			getDemandAndPersonsPerAgeGroup(demandToDistribute, possiblePersonsSecondJobElement);
		}
		return demandToDistribute;
	}

	@Override
	public int calculateDemandForThisLinkWithFixNumberOfJobs(int demandToDistribute, Integer numberOfJobs, int distributedDemand,
															 DemandReaderFromCSV.LinkPersonPair selectedNewLinkPersonPairForFirstJobElement,
															 DemandReaderFromCSV.LinkPersonPair selectedNewLinkPersonPairForSecondJobElement,
															 int i) {
		if (rouletteWheel == null) {
			createRouletteWheel(demandToDistribute);
		}
		int demandForThisLink = 0;

		if (demandDistributionOption == DemandDistributionOption.toPersonsByAge) { //based on age
			int age = (int) selectedNewLinkPersonPairForSecondJobElement.getPerson().getAttributes().getAttribute("age");

			AgeGroup ageGroup = getAgeGroup(ageGroupDemandShare.keySet(), age);
			assert ageGroup != null;

			// based on the share of online shoppers in each age group a person is selected as shopper or not
			String demandInformation = ageGroupDemandShare.get(ageGroup).sample();
			if (demandInformation.equals("NoDemand")) {
				return 0;
			}
			ageGroup = getAgeGroup(demandDistributionPerAgeGroup.keySet(), age);

			assert ageGroup != null;
			double error = ageGroup.getError();

			int restOfDemandForThisAge = ageGroup.getRemainingDemand();
			if (restOfDemandForThisAge != 0) {

				//poisson remainingDemand
				demandForThisLink = rouletteWheel.nextLong().intValue();
				if (demandForThisLink > restOfDemandForThisAge)
					demandForThisLink = restOfDemandForThisAge;

				ageGroup.setRemainingDemand(restOfDemandForThisAge - demandForThisLink);
				ageGroup.setPersonsWithDemandInThisAgeGroup(ageGroup.getPersonsWithDemandInThisAgeGroup() + 1);
				ageGroup.setError(error);
			}
		} else if (demandDistributionOption == DemandDistributionOption.toRandomPersons) {

			demandForThisLink = rouletteWheel.nextLong().intValue();
			if (demandForThisLink > (demandToDistribute - distributedDemand))
				demandForThisLink = (demandToDistribute - distributedDemand);

		}

		//add remainingDemand to list
		if (demandForThisLink > 0)
			parcelsPerPerson.put(selectedNewLinkPersonPairForSecondJobElement.getPerson(), demandForThisLink);
		return demandForThisLink;
	}

	/**
	 * Reduces the possiblePersons based on the given distribution of the shares for each age group of persons having ordered and purchased products online in the past three months.
	 *
	 * @param possiblePersons Population (possibly reduced to shape)
	 */

	private static void getAgeDistribution(HashMap<Id<Person>, Person> possiblePersons) {

		//each person's age is evaluated to determine the number of persons in each age group
		for (Id<Person> personId : possiblePersons.keySet()) {

			int agePerson = (int) possiblePersons.get(personId).getAttributes().getAttribute("age");
			AgeGroup ageGroup = getAgeGroup(ageGroupDemandShare.keySet(), agePerson);
			//add person to age group
			assert ageGroup != null;
			ageGroup.setTotalPersonsInAgeGroup(ageGroup.getTotalPersonsInAgeGroup() + 1);
		}

		// determine persons in the age group with share of people within each age group who have a remainingDemand
		for (AgeGroup ageGroup : ageGroupDemandShare.keySet()) {
			double share = ageGroupDemandShare.get(ageGroup).getPmf().stream().filter(
				e -> e.getFirst().equals("WithDemand")).findFirst().get().getSecond();
			int personsWithDemandInThisAgeGroup = (int) Math.round(ageGroup.getTotalPersonsInAgeGroup() * share);
			ageGroup.setPersonsWithDemandInThisAgeGroup(personsWithDemandInThisAgeGroup);
		}
	}


	/**
	 * Determination of the age distribution of given population \\NEW METHOD
	 *
	 * @param possiblePersons    Population (possibly reduced to shape)
	 * @param demandToDistribute Total amount of remainingDemand
	 */
	private static void getDemandAndPersonsPerAgeGroup(int demandToDistribute, HashMap<Id<Person>, Person> possiblePersons) {

		log.info("Splitting the remainingDemand per age group...");
		createDemandDistributionPerAgeGroup();
		//the remainingDemand volume is divided between the individual age groups and added to "demandDistributionPerAgeGroup"
		int totalDistributedDemand = 0;
		List<AgeGroup> ageGroups = new ArrayList<>(demandDistributionPerAgeGroup.keySet());

		// Step 1: Calculate initial demands and total distributed demand
		Map<AgeGroup, Double> fractionalDemands = new HashMap<>();
		for (AgeGroup ageGroup : ageGroups) {
			double demandForAgeGroupAsDouble = demandToDistribute * demandDistributionPerAgeGroup.get(ageGroup);
			int demandForAgeGroupAsInt = (int) Math.round(demandForAgeGroupAsDouble);

			ageGroup.setRemainingDemand(demandForAgeGroupAsInt);
			ageGroup.setTotalAmountParcels(demandForAgeGroupAsInt);
			fractionalDemands.put(ageGroup, demandForAgeGroupAsDouble);
			totalDistributedDemand += demandForAgeGroupAsInt;
		}

		// Step 2: Adjust demands to match demandToDistribute
		int difference = demandToDistribute - totalDistributedDemand;
		if (difference != 0) {
			// Sort age groups by largest fractional difference
			List<Map.Entry<AgeGroup, Double>> sortedByFraction = fractionalDemands.entrySet().stream()
				.sorted((a, b) -> Double.compare(
					Math.abs(b.getValue() - Math.round(b.getValue())),
					Math.abs(a.getValue() - Math.round(a.getValue()))
				))
				.toList();

			// Adjust demands iteratively
			for (Map.Entry<AgeGroup, Double> entry : sortedByFraction) {
				if (difference == 0) break;

				AgeGroup ageGroup = entry.getKey();
				int adjustment = difference > 0 ? 1 : -1;
				ageGroup.setRemainingDemand(ageGroup.getRemainingDemand() + adjustment);
				ageGroup.setTotalAmountParcels(ageGroup.getTotalAmountParcels() + adjustment);
				totalDistributedDemand += adjustment;
				difference -= adjustment;
			}
		}

		//add number of persons per age
		for (Person person : possiblePersons.values()) {
			int agePerson = (int) person.getAttributes().getAttribute("age");
			AgeGroup ageGroup = getAgeGroup(demandDistributionPerAgeGroup.keySet(), agePerson);
			assert ageGroup != null;
			ageGroup.setTotalPersonsInAgeGroup(ageGroup.getTotalPersonsInAgeGroup() + 1);
		}
		log.info("Finished with the remainingDemand per age group...");
	}

	@Override
	public void writeAdditionalOutputFiles(Controller controller) {
		super.writeAdditionalOutputFiles(controller);
		createDemandDistributionFile(controller);
		createAgeDistributionFile(controller);
	}

	/**
	 * Creates a tsv file with the age distribution.
	 *
	 * @param controller The controller to get the network from
	 */
	private static void createAgeDistributionFile(Controller controller) {

		BufferedWriter writer = IOUtils.getBufferedWriter(
			IOUtils.getFileUrl(controller.getConfig().controller().getOutputDirectory() + "/outputDemandDistrPerAgeGroup.tsv"),
			StandardCharsets.UTF_8, true);
		try {
			// Write the header
			String[] header = new String[]{"ageGroup", "shareOfShoppers (planed)", "shareOfShoppers (model)", "totalParcels", "personsInAgeGroup", "personsWithDemand", "personsWithoutDemand"};
			JOIN.appendTo(writer, header);
			int sumOfPersonsWithDemand = demandDistributionPerAgeGroup.keySet().stream().mapToInt(AgeGroup::getPersonsWithDemandInThisAgeGroup).sum();
			// Iterate through the demandDistributionPerAgeGroup map
			for (AgeGroup ageGroup : demandDistributionPerAgeGroup.keySet()) {
				writer.newLine();
				List<String> row = new ArrayList<>();
				row.add(ageGroup.boundsToString());
				row.add(String.valueOf(demandDistributionPerAgeGroup.get(ageGroup)));
				row.add(String.format("%.3f", ageGroup.getPersonsWithDemandInThisAgeGroup() / (double) sumOfPersonsWithDemand));
				row.add(String.valueOf(ageGroup.getTotalAmountParcels()));
				row.add(String.valueOf(ageGroup.getTotalPersonsInAgeGroup()));
				row.add(String.valueOf(ageGroup.getPersonsWithDemandInThisAgeGroup()));
				row.add(String.valueOf(ageGroup.getTotalPersonsInAgeGroup() - ageGroup.getPersonsWithDemandInThisAgeGroup()));
				JOIN.appendTo(writer, row);
			}
			// Flush the writer to ensure all data is written to the file
			writer.flush();
			writer.close();

		} catch (IOException e) {
			log.error("An error occurred while processing the file: {}", e.getMessage(), e);
		}
		log.info("Wrote age distribution file under " + "/outputDemandDistrPerAgeGroupFile.xml.gz");

		writer = IOUtils.getBufferedWriter(
			IOUtils.getFileUrl(controller.getConfig().controller().getOutputDirectory() + "/outputAgeGroupDemandShareFile.tsv"),
			StandardCharsets.UTF_8, true);
		try {
			// Write the header
			String[] header = new String[]{"ageGroup", "share", "totalPersonsInAgeGroup", "possiblePersonsWithDemand", "personsWithDemand", "parcels"};
			JOIN.appendTo(writer, header);
			calculateGeneratedDemandForAgeGroup();

			// Write the age group data to the file
			for (AgeGroup ageGroup : ageGroupDemandShare.keySet()) {

				writer.newLine();
				List<String> row = new ArrayList<>();
				row.add(ageGroup.boundsToString());
				row.add(String.valueOf(ageGroupDemandShare.get(ageGroup).getPmf().stream().filter(
					e -> e.getFirst().equals("WithDemand")).findFirst().get().getSecond()));
				row.add(String.valueOf(ageGroup.getTotalPersonsInAgeGroup()));
				row.add(String.valueOf(ageGroup.getPossiblePersonsWithDemandInThisAgeGroup()));
				row.add(String.valueOf(ageGroup.getPersonsWithDemandInThisAgeGroup()));
				row.add(String.valueOf(ageGroup.getTotalAmountParcels()));
				JOIN.appendTo(writer, row);
			}

			// Flush the writer to ensure all data is written to the file
			writer.flush();
			writer.close();
		} catch (IOException e) {
			log.error("An error occurred while processing the file: {}", e.getMessage(), e);
		}
		log.info("Wrote remainingDemand share file under " + "/outputAgeGroupDemandShareFile.xml.gz");
	}

	/**
	 * Calculates the generated demand for each age group of the ageGroupDemandShare map.
	 */
	private static void calculateGeneratedDemandForAgeGroup() {
		for (Person person : parcelsPerPerson.keySet()) {
			int age = (int) person.getAttributes().getAttribute("age");
			AgeGroup ageGroup = getAgeGroup(DemandGenerationSpecificationForParcelDelivery.ageGroupDemandShare.keySet(), age);
			assert ageGroup != null;
			ageGroup.setPossiblePersonsWithDemandInThisAgeGroup(ageGroup.getPossiblePersonsWithDemandInThisAgeGroup() + 1);
			ageGroup.setTotalAmountParcels(ageGroup.getTotalAmountParcels() + parcelsPerPerson.get(person));
		}
	}

	/**
	 * Creates a tsv file with the remainingDemand distribution.
	 *
	 * @param controller The controller to get the network from
	 */
	private static void createDemandDistributionFile(Controller controller) {

		BufferedWriter writer = IOUtils.getBufferedWriter(
			IOUtils.getFileUrl(controller.getConfig().controller().getOutputDirectory() + "/outputDemandPerPersonFile.tsv"),
			StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[]{"personId", "age", "amountOfParcels", "xCoord", "yCoord"};
			JOIN.appendTo(writer, header);
			for (Person person : parcelsPerPerson.keySet()) {
				writer.newLine();
				List<String> row = new ArrayList<>();
				row.add(person.getId().toString());
				row.add(person.getAttributes().getAttribute("age").toString());
				row.add(parcelsPerPerson.get(person).toString());
				row.add(String.valueOf(getHomeCoord(person).getX()));
				row.add(String.valueOf(getHomeCoord(person).getY()));
				JOIN.appendTo(writer, row);
			}

			writer.flush();
			writer.close();

		} catch (IOException e) {
			log.error("An error occurred while processing the file: {}", e.getMessage(), e);
		}
		log.info("Wrote remainingDemand distribution file under " + "/outputDemandDistributionFile.xml.gz");
	}


	private static class AgeGroup implements Comparable<AgeGroup> {
		private final int lowerBound;
		private final int upperBound;
		private int totalPersonsInAgeGroup;
		private int personsWithDemandInThisAgeGroup;
		private int possiblePersonsWithDemandInThisAgeGroup;
		private int remainingDemand;
		private int totalAmountParcels;
		private double error;

		public AgeGroup(int lowerBound, int upperBound) {
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
			this.totalAmountParcels = 0;
			this.totalPersonsInAgeGroup = 0;
			this.possiblePersonsWithDemandInThisAgeGroup = 0;
			this.personsWithDemandInThisAgeGroup = 0;
			this.remainingDemand = 0;
			this.error = 0;
		}

		public int getTotalPersonsInAgeGroup() {
			return totalPersonsInAgeGroup;
		}

		public int getLowerBound() {
			return lowerBound;
		}

		public int getUpperBound() {
			return upperBound;
		}

		public void setTotalPersonsInAgeGroup(int totalPersonsInAgeGroup) {
			this.totalPersonsInAgeGroup = totalPersonsInAgeGroup;
		}

		public int getPersonsWithDemandInThisAgeGroup() {
			return personsWithDemandInThisAgeGroup;
		}

		public void setPersonsWithDemandInThisAgeGroup(int personsWithDemandInThisAgeGroup) {
			this.personsWithDemandInThisAgeGroup = personsWithDemandInThisAgeGroup;
		}

		public String boundsToString() {
			return lowerBound + "-" + upperBound;
		}

		public double getError() {
			return error;
		}

		public void setError(double error) {
			this.error = error;
		}

		public int getRemainingDemand() {
			return remainingDemand;
		}

		public void setRemainingDemand(int remainingDemand) {
			this.remainingDemand = remainingDemand;
		}

		public void setTotalAmountParcels(int totalAmountParcels) {
			this.totalAmountParcels = totalAmountParcels;
		}

		public int getTotalAmountParcels() {
			return totalAmountParcels;
		}

		public int getPossiblePersonsWithDemandInThisAgeGroup() {
			return possiblePersonsWithDemandInThisAgeGroup;
		}

		public void setPossiblePersonsWithDemandInThisAgeGroup(int possiblePersonsWithDemandInThisAgeGroup) {
			this.possiblePersonsWithDemandInThisAgeGroup = possiblePersonsWithDemandInThisAgeGroup;
		}

		@Override
		public int compareTo(AgeGroup other) {
			return Integer.compare(this.getLowerBound(), other.getLowerBound()); // Sorting by minimum age
		}
	}

	private static class RouletteWheel {

		static class Builder {


			private Random random = new Random(Long.MAX_VALUE);

			private final Frequency frequency;

			public static Builder newInstance(Frequency frequency) {
				return new Builder(frequency);
			}

			private Builder(Frequency frequency) {
				this.frequency = frequency;
			}

			public Builder setRandom(Random random) {
				this.random = random;
				return this;
			}

			public RouletteWheel build() {
				return new RouletteWheel(this);
			}
		}


		private final Random random;

		private final Frequency frequency;

		private RouletteWheel(Builder builder) {
			this.frequency = builder.frequency;
			this.random = builder.random;
		}


		/*package-private*/ Long nextLong() {
			double randomNumber = random.nextDouble();
			double sum = 0;
			Iterator<Comparable<?>> iterator = frequency.valuesIterator();
			while (iterator.hasNext()) {
				Long value = (Long) iterator.next();
				sum += frequency.getPct(value);
				if (randomNumber < sum) {
					return value;
				}
			}
			throw new IllegalStateException("no item found. this must not be.");
		}
	}
}
