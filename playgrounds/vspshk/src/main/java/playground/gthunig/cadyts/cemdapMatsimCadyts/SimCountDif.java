package playground.gthunig.cadyts.cemdapMatsimCadyts;

import org.matsim.contrib.util.CSVReaders;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gabriel Thunig on 05.10.2016.
 */
public class SimCountDif {

	public static void main(String[] args) {
		final String runX = "run_1";
		final int itX = 0;
		final String runY = "run_1";
		final int itY = 500;

//		final String x = "C:/Users/GabrielT/Desktop/BerlinScenario/" + runX + "/ITERS/it." + itX + "/" + itX + ".configurablePTcountsCompare.txt";
//		final String y = "C:/Users/GabrielT/Desktop/BerlinScenario/" + runY + "/ITERS/it." + itY + "/" + itY + ".configurablePTcountsCompare.txt";
//		analyzeConfigurablePTcountsCompare(x);
//		analyzeConfigurablePTcountsCompare(y);

		final String x = "C:/Users/GabrielT/Desktop/BerlinScenario/" + runX + "/ITERS/it." + itX + "/" + itX + ".simCountCompareOccupancy.txt";
		final String y = "C:/Users/GabrielT/Desktop/BerlinScenario/" + runY + "/ITERS/it." + itY + "/" + itY + ".simCountCompareOccupancy.txt";
		analyzeSimCountCompareOccupancy(x);
		analyzeSimCountCompareOccupancy(y);
	}

	private static void analyzeConfigurablePTcountsCompare(String filename) {
		String s = "\t";
		List<String[]> content = CSVReaders.readFile(filename, s.charAt(0));
		long count = 0L;
		long sim = 0L;

		//System.out.println(Arrays.asList(file.get(2)));

		List<Integer[]> deviations = new ArrayList<>();

		for (int i = 2; i < content.size(); i += 3) {
			String[] line = content.get(i);
			Double simVal = Double.parseDouble(line[1]);
			Double countVal = Double.parseDouble(line[3]);
			double ratio = (countVal-simVal)/countVal;
			Double deviation = ratio * 100;
//			Double absDeviation = Math.abs(deviation);
			if (countVal >= 0) {
				Integer[] dev = new Integer[2];
				dev[0] = deviation.intValue();
				dev[1] = countVal.intValue();
				deviations.add(dev);
			}

			sim += simVal.longValue();
			count += countVal.longValue();
		}

		System.out.println();
		int belowCountDeviations = 0;
		int aboveCountDeviations = 0;
		int exactMatches = 0;
		int avarageBelowCountDeviation = 0;
		int avarageAboveCountDeviation = 0;
		long avarageDeviation = 0;
		int belowCountDeviationCountValues = 0;
		int aboveCountDeviationCountValues = 0;
		int exactMatchesCountValues = 0;
		int deviationCount = deviations.size();
		for(Integer[] deviation : deviations) {
			avarageDeviation += Math.abs(deviation[0]) * deviation[1];
			if (deviation[0] > 0) {
				belowCountDeviations++;
				avarageBelowCountDeviation += deviation[0] * deviation[1];
				belowCountDeviationCountValues += deviation[1];
			} else if (deviation[0] < 0){
				aboveCountDeviations++;
				avarageAboveCountDeviation += deviation[0] * deviation[1];
				aboveCountDeviationCountValues += deviation[1];
			} else {
				exactMatches++;
				exactMatchesCountValues += deviation[1];
			}
		}

		System.out.println("deviationCount = " + deviationCount);
		avarageDeviation /= count;
		System.out.println("avarageDeviation = " + avarageDeviation);

		System.out.println("exactMatches = " + exactMatches);
		System.out.println("exactMatchesWeight = " + (double)exactMatchesCountValues/(double)count);

		System.out.println("belowCountDeviations = " + belowCountDeviations);
		avarageBelowCountDeviation /= belowCountDeviationCountValues;
		System.out.println("avarageBelowCountDeviation = " + avarageBelowCountDeviation);
		System.out.println("belowCountDeviationsWeight = " + (double)belowCountDeviationCountValues/(double)count);

		System.out.println("aboveCountDeviations = " + aboveCountDeviations);
		avarageAboveCountDeviation /= aboveCountDeviationCountValues;
		System.out.println("avarageAboveCountDeviation = " + avarageAboveCountDeviation);
		System.out.println("aboveCountDeviationsWeight = " + (double)aboveCountDeviationCountValues/(double)count);

		System.out.println();
		System.out.println("sim =\t" + sim);
		System.out.println("count =\t" + count);
	}

	private static void analyzeSimCountCompareOccupancy(String filename) {
		String s = "\t";
		List<String[]> content = CSVReaders.readFile(filename, s.charAt(0));
		long count = 0L;
		long sim = 0L;

		//System.out.println(Arrays.asList(file.get(2)));

		List<Integer[]> deviations = new ArrayList<>();

		for (int i = 2; i < content.size(); i++) {
			int mod = i % 26;
			if (mod < 2) {
				i += 2;
			} else {
				String[] line = content.get(i);
				Double simVal = Double.parseDouble(line[1]);
				Double countVal = Double.parseDouble(line[3]);
				double ratio = (countVal-simVal)/countVal;
				Double deviation = ratio * 100;
//			Double absDeviation = Math.abs(deviation);
				if (countVal >= 0) {
					Integer[] dev = new Integer[2];
					dev[0] = deviation.intValue();
					dev[1] = countVal.intValue();
					deviations.add(dev);
				}

				sim += simVal.longValue();
				count += countVal.longValue();
			}
		}

		System.out.println();
		int belowCountDeviations = 0;
		int aboveCountDeviations = 0;
		int exactMatches = 0;
		int avarageBelowCountDeviation = 0;
		int avarageAboveCountDeviation = 0;
		long avarageDeviation = 0;
		int belowCountDeviationCountValues = 0;
		int aboveCountDeviationCountValues = 0;
		int exactMatchesCountValues = 0;
		int deviationCount = deviations.size();
		for(Integer[] deviation : deviations) {
			avarageDeviation += Math.abs(deviation[0]) * deviation[1];
			if (deviation[0] > 0) {
				belowCountDeviations++;
				avarageBelowCountDeviation += deviation[0] * deviation[1];
				belowCountDeviationCountValues += deviation[1];
			} else if (deviation[0] < 0){
				aboveCountDeviations++;
				avarageAboveCountDeviation += deviation[0] * deviation[1];
				aboveCountDeviationCountValues += deviation[1];
			} else {
				exactMatches++;
				exactMatchesCountValues += deviation[1];
			}
		}

		System.out.println("deviationCount = " + deviationCount);
		avarageDeviation /= count;
		System.out.println("avarageDeviation = " + avarageDeviation);

		System.out.println("exactMatches = " + exactMatches);
		System.out.println("exactMatchesWeight = " + (double)exactMatchesCountValues/(double)count);

		System.out.println("belowCountDeviations = " + belowCountDeviations);
		avarageBelowCountDeviation /= belowCountDeviationCountValues;
		System.out.println("avarageBelowCountDeviation = " + avarageBelowCountDeviation);
		System.out.println("belowCountDeviationsWeight = " + (double)belowCountDeviationCountValues/(double)count);

		System.out.println("aboveCountDeviations = " + aboveCountDeviations);
		avarageAboveCountDeviation /= aboveCountDeviationCountValues;
		System.out.println("avarageAboveCountDeviation = " + avarageAboveCountDeviation);
		System.out.println("aboveCountDeviationsWeight = " + (double)aboveCountDeviationCountValues/(double)count);

		System.out.println();
		System.out.println("sim =\t" + sim);
		System.out.println("count =\t" + count);
	}

}
