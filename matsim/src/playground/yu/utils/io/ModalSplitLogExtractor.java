/**
 * 
 */
package playground.yu.utils.io;

import java.util.ArrayList;
import java.util.List;

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
	private int length(){
		return Math.min(Math.min(carFracs.size(), ptFracs.size()),walkFracs.size());
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String logFilename = "../runs_SVN/run684/logfile.log";
		SimpleReader sr = new SimpleReader(logFilename);
		String line = sr.readLine();
		ModalSplitLogExtractor msle = new ModalSplitLogExtractor();
		int carCount = 0, ptCount = 0, walkCount = 0;
		while (line != null) {
			line = sr.readLine();
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
		int n=msle.length();
		double xs[]=new double[n];
		for(int i=0;i<n;i++){
			xs[i]=i;
		}
		
	}

}
