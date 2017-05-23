package cba.toynet2;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class PopulationAnalyzer {

	// -------------------- MEMBERS --------------------

	private final String logFileName;

	private final Map<Id<Person>, PlanForResampling> personId2chosenPlan = new LinkedHashMap<>();

	private final Map<Id<Person>, Double> personId2experiencedMatsimScore = new LinkedHashMap<>();

	// private Double resamplingStatistic = null;

	private Double estimatedMaxUtil = null;

	private Double realizedMaxUtil = null;

	// -------------------- CONSTRUCTION --------------------

	PopulationAnalyzer(final String logFileName) {
		this.logFileName = logFileName;
		try {
			final PrintWriter writer = new PrintWriter(logFileName);
			for (TourSequence.Type tourSeqType : TourSequence.Type.values()) {
				writer.print("cnt(" + tourSeqType + ")\t");
			}
			for (TourSequence.Type tourSeqType : TourSequence.Type.values()) {
				writer.print("freq(" + tourSeqType + ")\t");
			}
			for (TourSequence.Type tourSeqType : TourSequence.Type.values()) {
				writer.print("U_Sampers(" + tourSeqType + ")\t");
			}
			for (TourSequence.Type tourSeqType : TourSequence.Type.values()) {
				writer.print("U_MATSim_pred(" + tourSeqType + ")\t");
			}
			for (TourSequence.Type tourSeqType : TourSequence.Type.values()) {
				writer.print("U_MATSim_real(" + tourSeqType + ")\t");
			}
			writer.print(
					"U_Sampers\tU_MATSim_pred\tU_MATSim_real\tU_Sampers_time\tU_MATSim_pred_time\tU_MATSim_real_time");
			writer.println("\testimatedMaxUtil\trealizedMaxUtil");
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

	}

	// -------------------- CONTENT ACCESS --------------------

	void registerChosenPlan(final PlanForResampling planForResampling) {
		this.personId2chosenPlan.put(planForResampling.getMATSimPlan().getPerson().getId(), planForResampling);
	}

	void registerExperiencedScore(final Person person) {
		this.personId2experiencedMatsimScore.put(person.getId(), person.getSelectedPlan().getScore());
	}

	void registerEstimatedMaximumUtility(final double estimatedMaxUtil) {
		this.estimatedMaxUtil = estimatedMaxUtil;
	}

	void registerRealizedMaximumUtility(final double realizedMaxUtil) {
		this.realizedMaxUtil = realizedMaxUtil;
	}

	// -------------------- ANALYSIS --------------------

	private <K> void add(final K key, final double addend, final Map<K, Double> key2cnt) {
		final Double oldCnt = key2cnt.get(key);
		if (oldCnt == null) {
			key2cnt.put(key, addend);
		} else {
			key2cnt.put(key, oldCnt + addend);
		}

	}

	private double null2zero(Double val) {
		return ((val == null) ? 0.0 : val);
	}

	void clear() {
		this.personId2chosenPlan.clear();
		this.personId2experiencedMatsimScore.clear();
	}

	void dumpAnalysis() {

		// COMPUTE ANALYSIS RESULTS

		final Map<TourSequence.Type, Double> tourSeqType2cnt = new LinkedHashMap<>();
		final Map<TourSequence.Type, Double> tourSeqType2sampersUtl = new LinkedHashMap<>();
		final Map<TourSequence.Type, Double> tourSeqType2PredictedMatsimUtl = new LinkedHashMap<>();
		final Map<TourSequence.Type, Double> tourSeqType2ExperiencedMatsimUtl = new LinkedHashMap<>();
		double totalSampersUtl = 0.0;
		double totalPredMATSimUtl = 0.0;
		double totalRealMATSimUtl = 0.0;

		double totalSampersTimeUtl = 0.0;
		double totalPredMATSimTimeUtl = 0.0;
		double totalRealMATSimTimeUtl = 0.0;

		final Set<Id<Person>> allPersonIds = new LinkedHashSet<>(this.personId2chosenPlan.keySet());
		// allPersonIds.addAll(this.personId2experiencedMatsimScore.keySet());
		for (Id<Person> personId : allPersonIds) {
			final PlanForResampling planForResampling = this.personId2chosenPlan.get(personId);
			final TourSequence tourSequence = planForResampling.getTourSequence();
			assert tourSequence != null;
			this.add(tourSequence.type, 1.0, tourSeqType2cnt);

			final double locationModeUtility = planForResampling.getSampersOnlyScore();
			// TODO NEW
			// + planForResampling.getSampersEpsilonRealization();
			final double sampersUtl = locationModeUtility + planForResampling.getSampersTimeScore();
			final double predMATSimUtl = locationModeUtility + planForResampling.getMATSimTimeScore();
			final double realMATSimUtl = locationModeUtility + this.personId2experiencedMatsimScore.get(personId);

			this.add(tourSequence.type, sampersUtl, tourSeqType2sampersUtl);
			this.add(tourSequence.type, predMATSimUtl, tourSeqType2PredictedMatsimUtl);
			this.add(tourSequence.type, realMATSimUtl, tourSeqType2ExperiencedMatsimUtl);
			totalSampersUtl += sampersUtl;
			totalPredMATSimUtl += predMATSimUtl;
			totalRealMATSimUtl += realMATSimUtl;

			totalSampersTimeUtl += planForResampling.getSampersTimeScore();
			totalPredMATSimTimeUtl += planForResampling.getMATSimTimeScore();
			totalRealMATSimTimeUtl += this.personId2experiencedMatsimScore.get(personId);
		}

		// CREATE STRING

		final StringBuffer result = new StringBuffer();
		for (TourSequence.Type tourSeqType : TourSequence.Type.values()) {
			result.append(this.null2zero(tourSeqType2cnt.get(tourSeqType)));
			result.append("\t");
		}
		for (TourSequence.Type tourSeqType : TourSequence.Type.values()) {
			result.append(this.null2zero(tourSeqType2cnt.get(tourSeqType)) / allPersonIds.size());
			result.append("\t");
		}
		for (TourSequence.Type tourSeqType : TourSequence.Type.values()) {
			result.append(this.null2zero(tourSeqType2sampersUtl.get(tourSeqType))
					/ this.null2zero(tourSeqType2cnt.get(tourSeqType)));
			result.append("\t");
		}
		for (TourSequence.Type tourSeqType : TourSequence.Type.values()) {
			result.append(this.null2zero(tourSeqType2PredictedMatsimUtl.get(tourSeqType))
					/ this.null2zero(tourSeqType2cnt.get(tourSeqType)));
			result.append("\t");
		}
		for (TourSequence.Type tourSeqType : TourSequence.Type.values()) {
			result.append(this.null2zero(tourSeqType2ExperiencedMatsimUtl.get(tourSeqType))
					/ this.null2zero(tourSeqType2cnt.get(tourSeqType)));
			result.append("\t");
		}
		result.append(totalSampersUtl / allPersonIds.size());
		result.append("\t");
		result.append(totalPredMATSimUtl / allPersonIds.size());
		result.append("\t");
		result.append(totalRealMATSimUtl / allPersonIds.size());
		result.append("\t");
		result.append(totalSampersTimeUtl / allPersonIds.size());
		result.append("\t");
		result.append(totalPredMATSimTimeUtl / allPersonIds.size());
		result.append("\t");
		result.append(totalRealMATSimTimeUtl / allPersonIds.size());
		result.append("\t");
		result.append(this.estimatedMaxUtil);
		result.append("\t");
		result.append(this.realizedMaxUtil);
		// result.append(this.resamplingStatistic);
		result.append("\n");

		// WRITE STRING

		try {
			Files.write(Paths.get(this.logFileName), result.toString().getBytes(), StandardOpenOption.APPEND);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
