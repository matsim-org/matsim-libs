package utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.io.IOUtils;


public class Bins {

	protected double interval;
	protected int numberOfBins;
	protected double maxVal;
	protected String desc;
	protected List<BinEntry> entries = new Vector<BinEntry>();
	protected double [] bins;

	private final static Logger log = Logger.getLogger(Bins.class);

	public Bins(double interval, double maxVal, String desc) {
		this.interval = interval;
		this.maxVal = maxVal;
		this.numberOfBins = (int)Math.ceil(maxVal / interval);
		this.desc = desc;
		this.bins = new double[this.numberOfBins];
	}
	public void addValues(double[] values, double[] weights) {
		for (int index = 0; index < values.length; index++) {
			this.addVal(values[index], weights[index]);
		}
	}

	public void addVal(double value, double weight) {
		int index = (int)Math.floor(value / interval);
		// values > maximum value are assigned to the last bin
		if (value >= maxVal) {
			index = this.numberOfBins -1; 
		}

		// values < 0.0 value are assigned to the first bin
		if (value < 0.0) {
			log.error("Value < 0.0 received");
			index = 0;
		}
		
		this.bins[index] += weight;
		this.entries.add(new BinEntry(value, weight));
	}

	public void clear() {
		this.entries.clear();
		this.bins = new double[this.numberOfBins];
	}

	public void plotBinnedDistribution(String path, String xLabel, String xUnit) {
		String [] categories  = new String[this.numberOfBins];
		for (int i = 0; i < this.numberOfBins; i++) {
			categories[i] = Integer.toString(i);
		}
		Double[] values = new Double[this.entries.size()];
		Double[] weights = new Double[this.entries.size()];

		for (int index = 0; index < this.entries.size(); index++) {
			values[index] = this.entries.get(index).getValue();
			weights[index] = this.entries.get(index).getWeight();
		}

		DecimalFormat formatter = new DecimalFormat("0.0000");
		String s = xLabel + " " +
		"[interval = " + formatter.format(this.interval) + xUnit + "]" +
		"[number of entries = " + this.entries.size() + "]" +
		"[mean = " + formatter.format(Utils.weightedMean(values, weights)) + xUnit + "]" +
		"[median = " + formatter.format(Utils.median(values)) + xUnit + "]" +
		"[max = " + formatter.format(Utils.getMax(values)) + xUnit + "]";

		BarChart chart =
			new BarChart(desc, s , "#", categories);
		chart.addSeries("Bin size", this.bins);
		chart.saveAsPng(path + desc + ".png", 1600, 800);

		try {
			BufferedWriter out = IOUtils.getBufferedWriter(path + desc + ".txt");
			out.write("Bin [interval = " + this.interval + " " + xUnit  + "]\t" + "#" + "\n");
			for (int j = 0; j < bins.length;  j++) {
				out.write(j + "\t" + bins[j] + "\n");
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public double[] getBins() {
		return bins;
	}
	public void setBins(double[] bins) {
		this.bins = bins;
	}
}
