package floetteroed.opdyts.example.roadpricing;

import java.util.ArrayList;
import java.util.List;

import opdytsintegration.TimeDiscretization;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Scenario;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TollLevels implements DecisionVariable {

	// -------------------- MEMBERS --------------------

	final double level1start_s;
	final double morningLevel2start_s;
	final double morningLevel3start_s;
	final double morningLevel3end_s;
	final double morningLevel2end_s;
	final double eveningLevel2start_s;
	final double eveningLevel3start_s;
	final double eveningLevel3end_s;
	final double eveningLevel2end_s;
	final double level1end_s;
	final double level1cost_money;
	final double level2cost_money;
	final double level3cost_money;

	final Scenario scenario;

	// -------------------- CONSTRUCTION --------------------

	public TollLevels(final List<Double> timeStructure,
			final List<Double> costStructure, final Scenario scenario) {
		this(timeStructure.get(0), timeStructure.get(1), timeStructure.get(2),
				timeStructure.get(3), timeStructure.get(4), timeStructure
						.get(5), timeStructure.get(6), timeStructure.get(7),
				timeStructure.get(8), timeStructure.get(9), costStructure
						.get(0), costStructure.get(1), costStructure.get(2),
				scenario);
	}

	public TollLevels(final double level1start_s,
			final double morningLevel2start_s,
			final double morningLevel3start_s, final double morningLevel3end_s,
			final double morningLevel2end_s, final double eveningLevel2start_s,
			final double eveningLevel3start_s, final double eveningLevel3end_s,
			final double eveningLevel2end_s, final double level1end_s,
			final double level1cost_money, final double level2cost_money,
			final double level3cost_money, final Scenario scenario) {
		this.level1start_s = level1start_s;
		this.morningLevel2start_s = morningLevel2start_s;
		this.morningLevel3start_s = morningLevel3start_s;
		this.morningLevel3end_s = morningLevel3end_s;
		this.morningLevel2end_s = morningLevel2end_s;
		this.eveningLevel2start_s = eveningLevel2start_s;
		this.eveningLevel3start_s = eveningLevel3start_s;
		this.eveningLevel3end_s = eveningLevel3end_s;
		this.eveningLevel2end_s = eveningLevel2end_s;
		this.level1end_s = level1end_s;
		this.level1cost_money = level1cost_money;
		this.level2cost_money = level2cost_money;
		this.level3cost_money = level3cost_money;

		this.scenario = scenario;
	}

	// -------------------- GETTERS --------------------

	public List<Double> getTimeStructure() {
		final List<Double> timeStructure = new ArrayList<>(10);
		timeStructure.add(this.level1start_s);
		timeStructure.add(this.morningLevel2start_s);
		timeStructure.add(this.morningLevel3start_s);
		timeStructure.add(this.morningLevel3end_s);
		timeStructure.add(this.morningLevel2end_s);
		timeStructure.add(this.eveningLevel2start_s);
		timeStructure.add(this.eveningLevel3start_s);
		timeStructure.add(this.eveningLevel3end_s);
		timeStructure.add(this.eveningLevel2end_s);
		timeStructure.add(this.level1end_s);
		return timeStructure;
	}

	public List<Double> getCostStructure() {
		final List<Double> costStructure = new ArrayList<>(3);
		costStructure.add(this.level1cost_money);
		costStructure.add(this.level2cost_money);
		costStructure.add(this.level3cost_money);
		return costStructure;
	}

	// --------------- IMPLEMENTATION OF DecisionVariable ---------------

	@Override
	public void implementInSimulation() {
		final RoadPricingScheme roadPricingScheme = (RoadPricingScheme) this.scenario
				.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
		for (List<RoadPricingSchemeImpl.Cost> costList : roadPricingScheme
				.getTypicalCostsForLink().values()) {
			costList.clear();
			costList.add(new RoadPricingSchemeImpl.Cost(this.level1start_s,
					this.morningLevel2start_s, this.level1cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.morningLevel2start_s, this.morningLevel3start_s,
					this.level2cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.morningLevel3start_s, this.morningLevel3end_s,
					this.level3cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.morningLevel3end_s, this.morningLevel2end_s,
					this.level2cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.morningLevel2end_s, this.eveningLevel2start_s,
					this.level1cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.eveningLevel2start_s, this.eveningLevel3start_s,
					this.level2cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.eveningLevel3start_s, this.eveningLevel3end_s,
					this.level3cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.eveningLevel3end_s, this.eveningLevel2end_s,
					this.level2cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.eveningLevel2end_s, this.level1end_s,
					this.level1cost_money));
		}
	}

	public double getTollValue(final int time_s) {
		if (time_s < this.level1start_s) {
			return 0.0;
		} else if (time_s < this.morningLevel2start_s) {
			return this.level1cost_money;
		} else if (time_s < this.morningLevel3start_s) {
			return this.level2cost_money;
		} else if (time_s < this.morningLevel3end_s) {
			return this.level3cost_money;
		} else if (time_s < this.morningLevel2end_s) {
			return this.level2cost_money;
		} else if (time_s < this.eveningLevel2start_s) {
			return this.level1cost_money;
		} else if (time_s < this.eveningLevel3start_s) {
			return this.level2cost_money;
		} else if (time_s < this.eveningLevel3end_s) {
			return this.level3cost_money;
		} else if (time_s < this.eveningLevel2end_s) {
			return this.level2cost_money;
		} else if (time_s < this.level1end_s) {
			return this.level1cost_money;
		} else {
			return 0.0;
		}
	}

	public Vector toVector(final TimeDiscretization timeDiscretization) {
		final Vector result = new Vector(timeDiscretization.getBinCnt());
		for (int bin = 0; bin < timeDiscretization.getBinCnt(); bin++) {
			final int time_s = timeDiscretization.getBinCenterTime_s(bin);
			result.set(bin, this.getTollValue(time_s));
		}
		return result;
	}

	public String toPSTricks(final double deltaX, final double deltaY) {

		final StringBuffer result = new StringBuffer();

		result.append("\\psline(0.0)");
		result.append("(" + (deltaX * this.level1start_s) + ",0.0)");
		result.append("(" + (deltaX * this.level1start_s) + ","
				+ (deltaY * this.level1cost_money) + ")");
		result.append("(" + (deltaX * this.morningLevel2start_s) + ","
				+ (deltaY * this.level1cost_money) + ")");
		result.append("(" + (deltaX * this.morningLevel2start_s) + ","
				+ (deltaY * this.level2cost_money) + ")");
		result.append("(" + (deltaX * this.morningLevel3start_s) + ","
				+ (deltaY * this.level2cost_money) + ")");
		result.append("(" + (deltaX * this.morningLevel3start_s) + ","
				+ (deltaY * this.level3cost_money) + ")");
		result.append("(" + (deltaX * this.morningLevel3end_s) + ","
				+ (deltaY * this.level3cost_money) + ")");
		result.append("(" + (deltaX * this.morningLevel3end_s) + ","
				+ (deltaY * this.level2cost_money) + ")");
		result.append("(" + (deltaX * this.morningLevel2end_s) + ","
				+ (deltaY * this.level2cost_money) + ")");
		result.append("(" + (deltaX * this.morningLevel2end_s) + ","
				+ (deltaY * this.level1cost_money) + ")");
		result.append("(" + (deltaX * this.eveningLevel2start_s) + ","
				+ (deltaY * this.level1cost_money) + ")");
		result.append("(" + (deltaX * this.eveningLevel2start_s) + ","
				+ (deltaY * this.level2cost_money) + ")");
		result.append("(" + (deltaX * this.eveningLevel3start_s) + ","
				+ (deltaY * this.level2cost_money) + ")");
		result.append("(" + (deltaX * this.eveningLevel3start_s) + ","
				+ (deltaY * this.level3cost_money) + ")");
		result.append("(" + (deltaX * this.eveningLevel3end_s) + ","
				+ (deltaY * this.level3cost_money) + ")");
		result.append("(" + (deltaX * this.eveningLevel3end_s) + ","
				+ (deltaY * this.level2cost_money) + ")");
		result.append("(" + (deltaX * this.eveningLevel2end_s) + ","
				+ (deltaY * this.level2cost_money) + ")");
		result.append("(" + (deltaX * this.eveningLevel2end_s) + ","
				+ (deltaY * this.level1cost_money) + ")");
		result.append("(" + (deltaX * this.level1end_s) + ","
				+ (deltaY * this.level1cost_money) + ")");
		result.append("(" + (deltaX * this.level1end_s) + ",0.0)");
		result.append("(" + (deltaX * 24 * 3600) + ",0.0)");

		return result.toString();
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public int hashCode() {
		return this.getTimeStructure().hashCode()
				+ this.getCostStructure().hashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof TollLevels) {
			final TollLevels otherTollLevels = (TollLevels) other;
			return ((this.getTimeStructure().equals(otherTollLevels
					.getTimeStructure())) && (this.getCostStructure()
					.equals(otherTollLevels.getCostStructure())));
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return this.toVector(new TimeDiscretization(0, 1800, 48)).toString();
		// return "Time Structure = " + this.getTimeStructure()
		// + "; Cost Structure = " + this.getCostStructure();
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	static void print128results() {
		final String[] allResults = new String[] {
				"0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 1.0 1.0 1.5 1.5 1.5 1.5 1.5 1.5 1.5 1.5 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.5 1.5 1.5 1.5 1.0 1.0 1.0 1.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0",
				"0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0",
				"0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.5 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.5 1.5 1.25 1.25 1.25 1.25 1.25 1.25 1.25 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0",
				"0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 1.0 1.5 1.5 1.5 1.5 1.5 1.5 1.5 1.5 1.0 1.0 1.0 1.0 1.0 1.0 1.5 2.0 2.0 2.0 2.0 2.0 1.5 1.5 1.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0",
				"0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 1.0 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.0 1.0 1.25 1.25 1.25 1.25 1.25 1.0 1.0 1.0 1.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0",
				"0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 0.0 0.0 0.0 0.0 0.0 0.0 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0",
				"0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.5 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.5 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0",
				"0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.25 1.25 1.25 1.5 1.5 1.5 1.5 1.5 1.5 1.5 1.5 1.25 1.25 1.25 1.5 1.5 1.5 1.5 1.5 1.5 1.5 1.25 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0",
				"0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.0 1.0 1.0 1.0 1.0 1.0 1.5 1.5 1.5 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.5 1.5 1.5 1.0 1.0 1.0 1.0 1.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0",
				"0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.5 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 1.25 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0" };

		final List<List<Double>> allData = new ArrayList<>();
		for (final String result : allResults) {
			final List<Double> data = new ArrayList<>();
			allData.add(data);
			for (String element : result.split("\\s")) {
				element = element.trim();
				if (element.length() > 0) {
					final double val = Double.parseDouble(element);
					data.add(val);
				}
			}
			System.out.println();
			System.out.println();
		}

		for (int k = 0; k < allData.get(0).size(); k++) {

			final DescriptiveStatistics stat = new DescriptiveStatistics();
			for (List<Double> data : allData) {
				stat.addValue(data.get(k));
			}

			final double x = k * 0.5;
			final double dx = 0.2;

			final double min = stat.getMin();
			final double max = stat.getMax();
			final double p25 = stat.getPercentile(25);
			final double p50 = stat.getPercentile(50);
			final double p75 = stat.getPercentile(75);

			// the box
			System.out.println("\\psline(" + (x - dx) + "," + p50 + ")("
					+ (x + dx) + "," + p50 + ")\n");
			System.out.println("\\psframe(" + (x - dx) + "," + p25 + ")("
					+ (x + dx) + "," + p75 + ")\n");

			// upper vertical line
			System.out.println("\\psline(" + x + "," + p75 + ")(" + x + ","
					+ max + ")\n");
			// upper horizontal line
			System.out.println("\\psline(" + (x - 0.5 * dx) + "," + max
					+ ")(" + (x + 0.5 * dx) + "," + max + ")\n");

			// lower vertical line
			System.out.println("\\psline(" + x + "," + min + ")(" + x + ","
					+ p25 + ")\n");
			// upper horizontal line
			System.out.println("\\psline(" + (x - 0.5 * dx) + "," + min
					+ ")(" + (x + 0.5 * dx) + "," + min + ")\n");
			
			System.out.println();

		}

	}

	public static void main(String[] args) {
		print128results();
		// final TollLevels originalTollLevels = new TollLevels(6 * 3600 + 1800,
		// 7 * 3600, 7 * 3600 + 1800, 8 * 3600 + 1800, 9 * 3600,
		// 15 * 3600 + 1800, 16 * 3600, 17 * 3600 + 1800, 18 * 3600,
		// 18 * 3600 + 1800, 10.0, 15.0, 20.0, null);
		// System.out.println(originalTollLevels.toPSTricks(1.0 / 3600, 0.25));

	}

}
