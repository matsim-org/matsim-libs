package playground.anhorni.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.utils.Utils;


public class Bins {
	
	protected double interval;
	protected int numberOfBins;
	protected int [] bins;
	protected double maxVal;
	protected String desc;
	protected List<Double> values = new Vector<Double>();
	
	private final static Logger log = Logger.getLogger(Bins.class);
	
	public Bins(double interval, double maxVal, String desc) {
		this.interval = interval;
		this.maxVal = maxVal;
		this.numberOfBins = (int)Math.ceil(maxVal / interval);
		this.desc = desc;
		
		bins = new int[numberOfBins];		
		for (int i = 0; i < numberOfBins; i++) {
			bins[i] = 0;
		}
	}	
	public void addValues(double[] values) {
		for (double value : values) {
			this.addVal(value);
		}
	}
	
	public void addVal(double value) {
		// values > maximum value are assigned to the last bin
		if (value >= maxVal) value = maxVal - 0.00000000001; 
		
		// values < 0.0 value are assigned to the first bin
		if (value < 0.0) {
			log.error("Value < 0.0 received");
			value = 0.0;
		}	
		int index = (int)Math.floor(value / interval);
		this.bins[index]++;
		
		this.values.add(value);
	}
	
	
	public int [] getBins() {	
		return this.bins;
	}
	
	
	public void plotBinnedDistribution(String path, String xLabel, String xUnit) {				
		String [] categories  = new String[this.numberOfBins];
		for (int i = 0; i < this.numberOfBins; i++) {
			categories[i] = Integer.toString(i);
		}		
		
		DecimalFormat formatter = new DecimalFormat("0.00");
		String s = xLabel + " " + 
		"[interval = " + formatter.format(this.interval) + xUnit + "]" +
		"[mean = " + formatter.format(Utils.mean(values)) + xUnit + "]" +
		"[sigma = " + formatter.format(Utils.getStdDev(values)) + xUnit + "]";
		
		BarChart chart = 
			new BarChart(desc, s , "#", categories);
		chart.addSeries("Bin size", Utils.convert2double(this.bins));
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
}
