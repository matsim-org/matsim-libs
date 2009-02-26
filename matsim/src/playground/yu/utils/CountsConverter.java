/**
 * 
 */
package playground.yu.utils;

import java.util.ArrayList;
import java.util.List;

import playground.yu.utils.io.SimpleReader;

/**
 * @author yu
 * 
 */
public class CountsConverter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		------------READ .ATT COUNTSFILE----------------
		SimpleReader sr = new SimpleReader(
				"../berlin data/link_counts_PKW_hrs0-24.att");
		List<String> NOs = new ArrayList<String>();
		List<String> TONODENOs = new ArrayList<String>();
		List<List<Double>> countsValues = new ArrayList<List<Double>>();
		// filehead
		String line = sr.readLine();
		// after filehead
		while (line != null) {
			line = sr.readLine();
			if (line != null) {
				String[] cells = line.split(";");
				NOs.add(cells[0]);
				TONODENOs.add(cells[1]);
				List<Double> cvs = new ArrayList<Double>(24);
				for (int i = 2; i < 26; i++) {
					cvs.add(Double.parseDouble(cells[i]));
				}
				countsValues.add(cvs);
			}
		}
		try {
			sr.close();
		} catch (Exception e) {
			System.err.println(e);
		}
//		-----------------------------------------------------
		
	}

}
