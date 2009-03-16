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
	private List<Double> walkFracs = new ArrayList<Double>();

	public void addCar(String carFrac) {
		carFracs.add(Double.valueOf(carFrac));
	}

	public void addPt(String ptFrac) {
		ptFracs.add(Double.valueOf(ptFrac));
	}

	public void addWalk(String walkFrac) {
		walkFracs.add(Double.valueOf(walkFrac));
	}

	private static String extractFrac(String line) {
		String[] words = line.split("\t");
		String frac = words[words.length - 1];
		return frac.substring(0, frac.length() - 1);
	}

	private int getModeFracsLength() {
		return Math.min(Math.min(carFracs.size(), ptFracs.size()), walkFracs
				.size());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String logFilename = "../runs_SVN/run684/logfile.log";
		String chartFilename = "../runs_SVN/run684/legModeChart.png";
		String outputFilename = "../runs_SVN/run684/legMode.txt";
		SimpleReader sr = new SimpleReader(logFilename);
		String line = sr.readLine();
		ModalSplitLogExtractor msle = new ModalSplitLogExtractor();
		int carCount = 0, ptCount = 0, walkCount = 0;
		while (line != null) {
			line = sr.readLine();
			if (line != null)
				if (line.contains("car legs")) {
					carCount++;
					msle.addCar(extractFrac(line));
				} else if (line.contains("pt legs")) {
					ptCount++;
					msle.addPt(extractFrac(line));
				} else if (line.contains("walk legs")) {
					walkCount++;
					msle.addWalk(extractFrac(line));
				}
		}
		int n = msle.getModeFracsLength();
		double xs[] = new double[n];
		double carFracs[] = new double[n];
		double ptFracs[] = new double[n];
		double wlkFracs[] = new double[n];
		for (int i = 0; i < n; i++) {
			xs[i] = i;
			carFracs[i] = msle.carFracs.get(i);
			ptFracs[i] = msle.ptFracs.get(i);
			wlkFracs[i] = msle.walkFracs.get(i);
		}

		XYLineChart chart = new XYLineChart("Mode Choice", "iteration",
				"leg mode fraction [%]");
		chart.addSeries("car", xs, carFracs);
		chart.addSeries("pt", xs, ptFracs);
		chart.addSeries("walk", xs, wlkFracs);
		chart.saveAsPng(chartFilename, 800, 600);

		SimpleWriter sw = new SimpleWriter(outputFilename);
		sw.writeln("iteration\tcar [%]\tpt [%]\twalk [%]");
		for (int i = 0; i < n; i++) {
			sw.writeln(i + "\t" + carFracs[i] + "\t" + ptFracs[i] + "\t"
					+ wlkFracs[i]);
		}
	}
}
