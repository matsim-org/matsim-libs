package playground.singapore.springcalibration.run.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

import playground.singapore.springcalibration.run.SingaporeControlerListener;

public class ModesHistoryPlotter {
	
	private TreeMap<Integer, Iteration> iterationmodes = new TreeMap<Integer, Iteration>();
	private DecimalFormat dfpercent = new DecimalFormat("0.0");
	
	public void addModeShare(int iteration, String mode, double share) {
		if (this.iterationmodes.get(iteration) == null) this.iterationmodes.put(iteration, new Iteration(iteration));
		Iteration iterationvalues = this.iterationmodes.get(iteration);
		iterationvalues.addShare(mode, share);
	}
	
	private TreeMap<String, Double> getModeShares(int iteration) {
		Iteration iterationvalues = this.iterationmodes.get(iteration);
		return iterationvalues.getShares();
	}
	
	private double[] getModeSharesUpToIteration(String mode, int iteration) {
		double shares [] = new double[iteration + 1];
		
		for (int i = 0; i <= iteration; i++) {
			shares[i] = this.getModeShares(i).get(mode);
		}
		return shares;		
	}
	
	
	public void writeModesHistory(String outdir, int iteration) {	
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outdir + "/modeSharesHistory.txt");
			
			
			for (int i = 0; i <= iteration; i++) {
				TreeMap<String, Double> iterationvalues = this.getModeShares(i);
				
				if (i == 0) {
					StringBuffer stringBuffer = new StringBuffer();
					stringBuffer.append("iteration\t");
					for (String mode : iterationvalues.keySet()) {					
						stringBuffer.append(mode + "\t");					
					}
					writer.write(stringBuffer.toString());
					writer.newLine();				
				}
				
				StringBuffer stringBuffer = new StringBuffer();
				stringBuffer.append(i + "\t");
				for (double share : iterationvalues.values()) {
					stringBuffer.append(dfpercent.format(100.0 * share) + "\t");
				}
				writer.write(stringBuffer.toString());
				writer.newLine();	
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//plot data here...
		XYLineChart chart = new XYLineChart("Modal Share", "iteration", "share");

		// x-series
		double[] iterations = new double[iteration + 1];
		for (int i = 0; i <= iteration; i++) {
			iterations[i] = i;
		}
		
		//y series
		for(String mode : SingaporeControlerListener.modes){
			chart.addSeries(mode, iterations, this.getModeSharesUpToIteration(mode, iteration));
		}
		chart.addMatsimLogo();
        chart.saveAsPng(outdir + "/modeSharesHistory.png", 800, 600);
	}
	
	
	private class Iteration {
		private int iteration;
		private TreeMap<String, Double> shares = new TreeMap<String, Double>(); 
		
		public Iteration(int iteration) {
			this.iteration = iteration;
		}
		
		public void addShare(String mode, double share) {
			this.shares.put(mode, share);
		}

		public TreeMap<String, Double> getShares() {
			return shares;
		}		
	}
}
