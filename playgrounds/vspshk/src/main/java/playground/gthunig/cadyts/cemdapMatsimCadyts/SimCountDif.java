package playground.gthunig.cadyts.cemdapMatsimCadyts;

import org.matsim.contrib.util.CSVReaders;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gabriel Thunig on 05.10.2016.
 */
public class SimCountDif {

	public static void main(String[] args) {
		analyze("C:/Users/GabrielT/Desktop/run2/ITERS/it.0/0.configurablePTcountsCompare.txt");
		analyze("C:/Users/GabrielT/Desktop/run3/ITERS/it.0/0.configurablePTcountsCompare.txt");
	}

	public static void analyze(String filename) {
		String s = "\t";
		List<String[]> content = CSVReaders.readFile(filename, s.charAt(0));
		Long count = 0L;
		Long sim = 0L;

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
		int positiveDeviations = 0;
		int negativeDeviations = 0;
		int noDeviationCount = 0;
		int avaragePositiveDeviation = 0;
		int avarageNegativeDeviation = 0;
		int avarageDeviation = 0;
		int positiveDeviationCountValues = 0;
		int negativeDeviationCountValues = 0;
		int noDeviationCountValues = 0;
		int deviationCount = deviations.size();
		for(Integer[] deviation : deviations) {
			avarageDeviation += Math.abs(deviation[0]) * deviation[1];
			if (deviation[0] > 0) {
				positiveDeviations++;
				avaragePositiveDeviation += deviation[0] * deviation[1];
				positiveDeviationCountValues += deviation[1];
			} else if (deviation[0] < 0){
				negativeDeviations++;
				avarageNegativeDeviation += deviation[0] * deviation[1];
				negativeDeviationCountValues += deviation[1];
			} else {
				noDeviationCount++;
				noDeviationCountValues += deviation[1];
			}
		}

		System.out.println("deviationCount = " + deviationCount);
		avarageDeviation /= count;
		System.out.println("avarageDeviation = " + avarageDeviation);

		System.out.println("noDeviationCount = " + noDeviationCount);
		System.out.println("noDeviationsWeight = " + (double)noDeviationCountValues/(double)count);

		System.out.println("positiveDeviations = " + positiveDeviations);
		avaragePositiveDeviation /= positiveDeviationCountValues;
		System.out.println("avaragePositiveDeviation = " + avaragePositiveDeviation);
		System.out.println("positiveDeviationsWeight = " + (double)positiveDeviationCountValues/(double)count);

		System.out.println("negativeDeviations = " + negativeDeviations);
		avarageNegativeDeviation /= negativeDeviationCountValues;
		System.out.println("avarageNegativeDeviation = " + avarageNegativeDeviation);
		System.out.println("negativeDeviationsWeight = " + (double)negativeDeviationCountValues/(double)count);

		System.out.println();
		System.out.println("sim =\t" + sim);
		System.out.println("count =\t" + count);
	}

}
