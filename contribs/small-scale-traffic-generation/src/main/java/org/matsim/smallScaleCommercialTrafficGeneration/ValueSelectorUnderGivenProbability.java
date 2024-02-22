package org.matsim.smallScaleCommercialTrafficGeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** This class creates a distribution under given probabilities for the possible values.
 *
 * @author: Ricardo Ewert
 */
public class ValueSelectorUnderGivenProbability {

	private int anIntAsSum;
	private final List<ProbabilityForValue> ProbabilityDistribution;
	private final Random rnd;

	public ValueSelectorUnderGivenProbability(List<ProbabilityForValue> ProbabilityDistribution, Random rnd) {
		this.anIntAsSum = 0;
		this.ProbabilityDistribution = ProbabilityDistribution;
		this.rnd = rnd;
	}


	public ProbabilityForValue getNextValueUnderGivenProbability() {
        anIntAsSum++;
        weightedProbability();
        ProbabilityForValue selectedValue = null;
        while (selectedValue == null) {
			ProbabilityForValue probabilityForValue = ProbabilityDistribution.get(rnd.nextInt(ProbabilityDistribution.size()));
            if (probabilityForValue.getRealizedValues() < probabilityForValue.getExpectedCount()) {
                selectedValue = probabilityForValue;
				selectedValue.increaseRealizedValue(1);
            }
        }
        return selectedValue;
    }

	private void weightedProbability() {

		//List to hold the cumulative probabilities of each value
		List<Double> cumulativeProbabilities = new ArrayList<>();
		double sum = 0;
		//Calculate cumulative probabilities for each value
		for (ProbabilityForValue l : ProbabilityDistribution) {
			sum += l.getProbability();
			cumulativeProbabilities.add(sum);
		}
		//Generate a random number between 0 and sum
		double r = rnd.nextDouble(0.0, sum);
		//Select a value based on the cumulative probabilities
		String selectedLetter = ProbabilityDistribution.stream()
			//Find the first value whose cumulative probability is greater than the random number
			.filter(l -> r < cumulativeProbabilities.get(ProbabilityDistribution.indexOf(l)))
			.findFirst().get().getValue();

		//Increment the expectedCount for the selected value
		ProbabilityDistribution.stream()
			.filter(a -> a.getValue().equals(selectedLetter))
			.findFirst()
			.ifPresent(l -> l.setExpectedCount(l.getExpectedCount() + 1));
	}
	public void writeResults(){
		for (ProbabilityForValue probabilityForValue : ProbabilityDistribution) {
			System.out.println(probabilityForValue.getValue()
				+ " -> expected: " + probabilityForValue.getExpectedCount()
				+ "(" + String.format("%.2f", (probabilityForValue.getExpectedCount() * Math.pow(anIntAsSum,
				-1)) * 100) + " %); prob: " + ((double) Math.round(probabilityForValue.getProbability() * 1000) / 10) + "%");
		}
	}

	public static class ProbabilityForValue {

		private String value;
		private String upperBound;
		private int expectedCount;
		private double probability;
		private int realizedValues;

		public ProbabilityForValue(String value, double probability) {
			this.value = value;
			this.probability = probability;
			this.expectedCount = 0;
			this.realizedValues = 0;
		}


		public ProbabilityForValue(String lowerBound, String upperBound, double probability) {
			this.value = lowerBound;
			this.upperBound = upperBound;
			this.probability = probability;
			this.expectedCount = 0;
			this.realizedValues = 0;
		}

		public double getProbability() {
			return probability;
		}

		public void setProbability(double probability) {
			this.probability = probability;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public int getExpectedCount() {
			return expectedCount;
		}

		public void setExpectedCount(int expectedCount) {
			this.expectedCount = expectedCount;
		}

		public int getRealizedValues() {
			return realizedValues;
		}

		public void setRealizedValues(int realizedValues) {
			this.realizedValues = realizedValues;
		}

		public void increaseRealizedValue(int i) {
			setRealizedValues(this.getRealizedValues() + i);
		}

		public String getUpperBound() {
			return upperBound;
		}

		public void setUpperBound(String upperBound) {
			this.upperBound = upperBound;
		}
	}
}
