package playground.anhorni.locationchoice.preprocess.helper;

import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.io.IOUtils;


public class Bins {
	
	private int interval;
	private int numberOfBins;
	private int [] bins;
	private double maxVal;
	
	private final static Logger log = Logger.getLogger(Bins.class);
	
	public Bins(int interval, double maxVal) {
		this.interval = interval;
		this.maxVal = maxVal;
		this.numberOfBins = (int)(maxVal / interval);
		
		bins = new int[numberOfBins];
		
		for (int i = 0; i < numberOfBins; i++) {
			bins[i] = 0;
		}
	}
	
	public void addVal(double value) {
		// values > maximum value are assigned to the last bin
		if (value > maxVal) value = maxVal; 
		
		// values < 0.0 value are assigned to the first bin
		if (value < 0.0) {
			log.error("Value < 0.0 received");
			value = 0.0;
		}	
		int index = (int)Math.floor(Math.min((maxVal / interval), 0.0));
		this.bins[index]++;
	}
	
	
	public int [] getBins() {	
		return this.bins;
	}
	
	
	public void plotBinnedDistribution(String filename, String path, String title, String xLabel, String xUnit) {	
		
		String [] categories  = new String[this.numberOfBins];
		for (int i = 0; i < this.numberOfBins; i++) {
			categories[i] = Integer.toString(i);
		}	
		BarChart chart = 
			new BarChart(title, xLabel + " " + xUnit, "#", categories);
		chart.addSeries("Bin size", Utils.convert2double(this.bins));
		chart.saveAsPng(path + filename + ".png", 1600, 800);
		
		try {			
			BufferedWriter out = IOUtils.getBufferedWriter(filename + ".txt");
			out.write("Bin [intervall = " + this.interval + " " + xUnit  + "\t" + "#" + "\n");
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
