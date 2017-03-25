package cba.toynet2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class UtilityChoiceProbaLogger extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- INNER CLASS --------------------

	private class Entry {

		private Map<TourSequence.Type, Double> utils = new LinkedHashMap<>();
		private Map<TourSequence.Type, Double> probs = new LinkedHashMap<>();

		private Entry() {
		}

		private Entry(final Map<PlanForResampling, Double> alternative2choiceProba) {
			for (Map.Entry<PlanForResampling, Double> entry : alternative2choiceProba.entrySet()) {
				final PlanForResampling alternative = entry.getKey();
				this.set(alternative.getTourSequence().type,
						alternative.getSampersOnlyScore() + alternative.getMATSimTimeScore(), entry.getValue());
			}
		}

		private void set(final TourSequence.Type type, final Double util, final Double prob) {
			this.utils.put(type, util == null ? 0.0 : util);
			this.probs.put(type, prob == null ? 0.0 : prob);
		}

		private double getExpUtil() {
			double result = 0.0;
			for (TourSequence.Type type : TourSequence.Type.values()) {
				if (this.utils.containsKey(type)) {
					result += this.probs.get(type) * this.utils.get(type);
				}
			}
			return result;
		}
	}

	// -------------------- MEMBERS --------------------

	private Map<Id<Person>, Entry> personId2entry = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	UtilityChoiceProbaLogger() {
	}

	UtilityChoiceProbaLogger(final String fileName) {
		this.readFromFile(fileName);
	}

	// -------------------- IMPLEMENTATION --------------------

	void register(final Id<Person> personId, final Map<PlanForResampling, Double> alternative2choiceProba) {
		this.personId2entry.put(personId, new Entry(alternative2choiceProba));
	}

	public double getExpUtil() {
		double result = 0.0;
		for (Entry entry : this.personId2entry.values()) {
			result += entry.getExpUtil();
		}
		return result;
	}

	public static double getConsumerSurplus(final UtilityChoiceProbaLogger baseCase,
			final UtilityChoiceProbaLogger policyCase) {

		double result = 0.0;

		assert (baseCase.personId2entry.size() == policyCase.personId2entry.size());

		for (Id<Person> personId : baseCase.personId2entry.keySet()) {
			final Entry baseEntry = baseCase.personId2entry.get(personId);
			final Entry policyEntry = policyCase.personId2entry.get(personId);

			for (TourSequence.Type type : TourSequence.Type.values()) {
				final double baseP = baseEntry.probs.get(type);
				final double baseV = baseEntry.utils.get(type);
				final double policyP = policyEntry.probs.get(type);
				final double policyV = policyEntry.utils.get(type);
				result += 0.5 * (policyV - baseV) * (policyP + baseP);
			}
		}

		return result;
	}

	// -------------------- FILE IO ----------

	void writeToFile(final String fileName) {
		try {
			final PrintWriter writer = new PrintWriter(fileName);
			writer.println("person\talternative\tV\tP");
			for (Map.Entry<Id<Person>, Entry> personId2entryEntry : this.personId2entry.entrySet()) {
				final Id<Person> personId = personId2entryEntry.getKey();
				final Entry entry = personId2entryEntry.getValue();
				for (TourSequence.Type type : TourSequence.Type.values()) {
					writer.print(personId);
					writer.print("\t");
					writer.print(type);
					writer.print("\t");
					writer.print(entry.utils.get(type));
					writer.print("\t");
					writer.println(entry.probs.get(type));
				}
			}
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	void readFromFile(final String fileName) {
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "\t" });
		try {
			parser.parse(fileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// ---------- IMPLEMENTATION OF TabularFileHandler ----------

	@Override
	public void startDocument() {
		this.personId2entry.clear();
		this.label2index.put("person", 0);
		this.label2index.put("alternative", 1);
		this.label2index.put("V", 2);
		this.label2index.put("P", 3);
	}

	@Override
	public void startCurrentDataRow() {

		final Id<Person> personId = Id.createPersonId(this.getStringValue("person"));
		final TourSequence.Type type = TourSequence.Type.valueOf(this.getStringValue("alternative"));
		final Double util = this.getDoubleValue("V");
		final Double prob = this.getDoubleValue("P");

		Entry entry = this.personId2entry.get(personId);
		if (entry == null) {
			entry = new Entry();
			this.personId2entry.put(personId, entry);
		}
		entry.set(type, util, prob);
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<Id<Person>, Entry> entry : this.personId2entry.entrySet()) {
			for (TourSequence.Type type : entry.getValue().probs.keySet()) {
				result.append(entry.getKey() + "\t");
				result.append(type + "\t");
				result.append(entry.getValue().utils.get(type) + "\t");
				result.append(entry.getValue().probs.get(type) + "\n");
			}
		}
		return result.toString();
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		final String path = "./output/cba/toynet-analysis/";
		final String basePrefix = path + "base-100/utilsProbasLog.";
		final String poliPrefix = path + "policy-100/utilsProbasLog.";

		System.out.println("E{base}\tE{policy}\tE{policy}-E{base}\tconsumer-Surplus");
		for (int k = 1; k <= 50; k++) {
			UtilityChoiceProbaLogger baseLogger = new UtilityChoiceProbaLogger(basePrefix + k + ".txt");
			UtilityChoiceProbaLogger poliLogger = new UtilityChoiceProbaLogger(poliPrefix + k + ".txt");

			final double baseExp = baseLogger.getExpUtil();
			final double poliExp = poliLogger.getExpUtil();
			final double surplus = getConsumerSurplus(baseLogger, poliLogger);

			System.out.println(baseExp + "\t" + poliExp + "\t" + (poliExp - baseExp) + "\t" + surplus);
		}
	}
}
