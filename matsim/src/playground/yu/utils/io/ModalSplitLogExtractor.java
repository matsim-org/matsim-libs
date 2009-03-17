/**
 * 
 */
package playground.yu.utils.io;

import java.util.ArrayList;
import java.util.List;

import org.matsim.utils.charts.XYLineChart;

/**
 * @author yu
 * 
 */
public class ModalSplitLogExtractor {
	private List<Double> carFracs = new ArrayList<Double>();
	private List<Double> ptFracs = new ArrayList<Double>();
	private List<Double> wlkFracs = new ArrayList<Double>();
	private int maxIter;

	public int getMaxIter() {
		return maxIter;
	}

	public ModalSplitLogExtractor(int n) {
		maxIter = n;
		for (int i = 0; i < n; i++) {
			carFracs.add(i, Double.valueOf(0));
			ptFracs.add(i, Double.valueOf(0));
			wlkFracs.add(i, Double.valueOf(0));
		}
	}

	public void addCar(int idx, String carFrac) {
		carFracs.set(idx, Double.valueOf(carFrac));
	}

	public void addPt(int idx, String ptFrac) {
		ptFracs.set(idx, Double.valueOf(ptFrac));
	}

	public void addWalk(int idx, String walkFrac) {
		wlkFracs.set(idx, Double.valueOf(walkFrac));
	}

	private static String extractFrac(String line) {
		String[] words = line.split("\t");
		String frac = words[words.length - 1];
		return frac.substring(0, frac.length() - 1);
	}

	/**
	 * @param line
	 *            , which must contains "ITERATION " and " BEGINS"
	 * @return ITERATION-No.
	 */
	private static int getCount(String line) {
		String[] words = line.split(" ");
		return Integer.parseInt(words[words.length - 2]);
	}

	private static void readLog(String logFilename, ModalSplitLogExtractor msle) {
		SimpleReader sr = new SimpleReader(logFilename);
		String line = sr.readLine();
		int count = -1;
		while (line != null) {
			line = sr.readLine();
			if (line != null) {
				if (line.contains("ITERATION ") && line.contains(" BEGINS")) {
					count = getCount(line);
				} else if (line.contains("car legs")) {
					if (count > -1)
						msle.addCar(count, extractFrac(line));
				} else if (line.contains("pt legs")) {
					if (count > -1)
						msle.addPt(count, extractFrac(line));
				} else if (line.contains("walk legs")) {
					if (count > -1)
						msle.addWalk(count, extractFrac(line));
				} else if (line.contains("ITERATION ")
						&& line.contains(" ENDS")) {
					count = -1;
				}
			}
		}
		sr.close();
	}

	private static void writeMode(String chartFilename, String outputFilename,
			ModalSplitLogExtractor msle) {
		int maxIter = msle.getMaxIter();
		double xs[] = new double[maxIter];
		double carFracs[] = new double[maxIter];
		double ptFracs[] = new double[maxIter];
		double wlkFracs[] = new double[maxIter];

		for (int i = 0; i < maxIter; i++) {
			xs[i] = i;
			Double carFrac = (i < msle.carFracs.size()) ? msle.carFracs.get(i)
					: null;
			if (carFrac != null)
				carFracs[i] = carFrac;

			Double ptFrac = (i < msle.ptFracs.size()) ? msle.ptFracs.get(i)
					: null;
			if (ptFrac != null)
				ptFracs[i] = ptFrac;
			Double wlkFrac = (i < msle.wlkFracs.size()) ? msle.wlkFracs.get(i)
					: null;
			if (wlkFrac != null)
				wlkFracs[i] = wlkFrac;

		}

		XYLineChart chart = new XYLineChart("Mode Choice", "iteration",
				"leg mode fraction [%]");
		chart.addSeries("car", xs, carFracs);
		chart.addSeries("pt", xs, ptFracs);
		chart.addSeries("walk", xs, wlkFracs);
		chart.saveAsPng(chartFilename, 800, 600);

		SimpleWriter sw = new SimpleWriter(outputFilename);
		sw.writeln("iteration\tcar [%]\tpt [%]\twalk [%]");
		System.out.println("n=" + maxIter);
		for (int i = 0; i < maxIter; i++) {
			sw.writeln(i + "\t" + carFracs[i] + "\t" + ptFracs[i] + "\t"
					+ wlkFracs[i]);
			sw.flush();
		}
		sw.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String logFilename = "../runs_SVN/run711/logfile.log";
		String chartFilename = "../runs_SVN/run711/legModeChart.png";
		String outputFilename = "../runs_SVN/run711/legMode.txt";
		int maxIter = 1001;
		ModalSplitLogExtractor msle = new ModalSplitLogExtractor(maxIter);
		// reading
		readLog(logFilename, msle);
		// writing
		writeMode(chartFilename, outputFilename, msle);
	}
}
