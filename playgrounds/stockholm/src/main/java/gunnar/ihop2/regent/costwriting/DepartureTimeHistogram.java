package gunnar.ihop2.regent.costwriting;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import floetteroed.utilities.math.Histogram;

/**
 * TODO Extracted from HalfTourCostMatrices; use this there as well!
 * 
 * @author Gunnar Flötteröd
 *
 */
public class DepartureTimeHistogram {

	// -------------------- MEMBERS --------------------

	private final int startTime_s;

	private final int binSize_s;

	private final int binCnt;

	private final Map<String, Histogram> actType2tourStartTimeHist = new LinkedHashMap<>();

	private final Map<String, Histogram> actType2returnTripStartTimeHist = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public DepartureTimeHistogram(final int startTime_s, final int binSize_s,
			final int binCnt) {
		this.startTime_s = startTime_s;
		this.binSize_s = binSize_s;
		this.binCnt = binCnt;
	}

	// -------------------- IMPLEMENTATION --------------------

	private void addTimeToHistogram(
			final Map<String, Histogram> actType2timeHist,
			final String actType, final double time_s) {
		Histogram histogram = actType2timeHist.get(actType);
		if (histogram == null) {
			histogram = Histogram.newHistogramWithUniformBins(this.startTime_s,
					this.binSize_s, this.binCnt);
			actType2timeHist.put(actType, histogram);
		}
		histogram.add(time_s);
	}

	private void addTourStartTime(final String actType, final double time_s) {
		this.addTimeToHistogram(this.actType2tourStartTimeHist, actType, time_s);
	}

	private void addReturnTripStartTime(final String actType,
			final double time_s) {
		this.addTimeToHistogram(this.actType2returnTripStartTimeHist, actType,
				time_s);
	}

	public void addPerson(final Person person) {
		final Plan plan = person.getSelectedPlan();
		if (plan == null) {
			Logger.getLogger(this.getClass().getName()).warning(
					"person " + person + " has no selected plan");
		} else {
			Activity prevAct = null;
			Double prevDptTime_s = null;
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					final Activity currentAct = (Activity) planElement;
					// TODO CHECK, NEW:
					if (!currentAct.getType().toUpperCase().startsWith("H")) {
					// if (!HOME.equals(currentAct.getType())) {
						// Prev. trip was from home to activity location.
						this.addTourStartTime(currentAct.getType(),
								prevDptTime_s);
					} else if (prevAct != null) {
						// Prev. trip was from activity location to home.
						this.addReturnTripStartTime(prevAct.getType(),
								prevDptTime_s);
					}
					prevAct = currentAct;
					prevDptTime_s = currentAct.getEndTime();
				}
			}
		}
	}

	public void writeHistogramsToFile(final String histogramFileName) {

		try {
			Logger.getLogger(this.getClass().getName()).info(
					"writing histogram file " + histogramFileName);
			final PrintWriter writer = new PrintWriter(histogramFileName);

			writer.print("TIMEBIN");
			for (String actType : this.actType2tourStartTimeHist.keySet()) {
				writer.print("\t");
				writer.print("TO-TRIPS-RATE(" + actType + ")");
				writer.print("\t");
				writer.print("BACK-TRIPS-RATE(" + actType + ")");
				writer.print("\t");
				writer.print("TO-TRIPS-TOTAL(" + actType + ")");
				writer.print("\t");
				writer.print("BACK-TRIPS-TOTAL(" + actType + ")");
			}
			writer.println();

			for (int bin = 0; bin < this.actType2tourStartTimeHist.values()
					.iterator().next().binCnt(); bin++) {
				writer.print(bin - 1);
				for (String actType : this.actType2tourStartTimeHist.keySet()) {
					writer.print("\t");
					writer.print(this.actType2tourStartTimeHist.get(actType)
							.freq(bin));
					writer.print("\t");
					writer.print(this.actType2returnTripStartTimeHist.get(
							actType).freq(bin));
					writer.print("\t");
					writer.print(this.actType2tourStartTimeHist.get(actType)
							.cnt(bin));
					writer.print("\t");
					writer.print(this.actType2returnTripStartTimeHist.get(
							actType).cnt(bin));
				}
				writer.println();
			}

			writer.flush();
			writer.close();
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).severe(
					"Failed to write histograms to file: " + e.getMessage());
		}
	}
}
