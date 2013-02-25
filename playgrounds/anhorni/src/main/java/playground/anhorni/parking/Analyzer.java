package playground.anhorni.parking;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.analysis.Bins;

public class Analyzer {

	/**
	 * @param path to summary.txt
	 */
	public static void main(String[] args) {
		String path = args[0];
		Analyzer analyzer = new Analyzer();
		analyzer.run(path);
	}
	
	public void run(String path) {
		ArrayList<Double> searchtimes = this.readSummaryFile(path + "/summary.txt");
		this.createBins(searchtimes, path);
	}
	
	private ArrayList<Double> readSummaryFile(String file) {
		ArrayList<Double> searchtimes = new ArrayList<Double>();
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			br.readLine(); // skip header
			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				String[] entrs = curr_line.split("\t", -1);				
				double ts = Double.parseDouble(entrs[5].trim());
				
				if (ts > 0) searchtimes.add(ts);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return searchtimes;
	}
	
	private void createBins(ArrayList<Double> searchtimes, String path) {
		Bins ts = new Bins(1, 10, "searchtimes");		
		for (double t:searchtimes) {
			ts.addVal(t / 60.0, 1.0);
		}	
		ts.plotBinnedDistribution(path, "#", " min");
	}
}
