/**
 * 
 */
package playground.yu.tests;

import playground.yu.utils.charts.XYScatterLineChart;
import playground.yu.utils.io.SimpleReader;

/**
 * mades plot from the biasErrorGraphData.txt
 * 
 * @author yu
 * 
 */
public class MREtext2Graph {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputTxt = "D:/fromNB04/wm/matsim-bse/raise_it.1000/biasErrorGraphData.txt";
		String outputPlot = "D:/fromNB04/wm/matsim-bse/raise_it.1000/biasErrorGraph.png";

		SimpleReader reader = new SimpleReader(inputTxt);
		reader.readLine();
		String line = reader.readLine();

		double[] mre = new double[24];
		while (line != null) {
			String[] words = line.split("\t");
			mre[Integer.parseInt(words[0]) - 1] = Double.parseDouble(words[1]);
			// System.out.println((Integer.parseInt(words[0]) - 1) + "\t"
			// + mre[Integer.parseInt(words[0]) - 1]);
			line = reader.readLine();
		}

		reader.close();

		XYScatterLineChart chart = new XYScatterLineChart("", "Hour",
				"Mean rel. error [%]");
		double[] xs = new double[24];
		for (int i = 0; i < 24; i++)
			xs[i] = (double) (i + 1);
		for (int i = 0; i < 24; i++)
			System.out.println(xs[i] + "\t" + mre[i]);
		chart.addSeries("Mean rel. error", xs, mre);
		chart.saveAsPng(outputPlot, 640, 480);
	}

}
