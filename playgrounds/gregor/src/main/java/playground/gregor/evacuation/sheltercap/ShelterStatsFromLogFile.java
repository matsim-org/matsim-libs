package playground.gregor.evacuation.sheltercap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.matsim.core.utils.misc.StringUtils;

import playground.gregor.gis.referencing.TextFileReader;

public class ShelterStatsFromLogFile {
	
	public static void main(String [] args) {
//		String output = "/home/laemmel/devel/allocation/output";
		String output = "/home/laemmel/arbeit/svn/runs-svn/run1078/output";
		String logfile = output + "/logfile.log";
		TextFileReader tf = new TextFileReader(logfile, ' ' , 10);
		String [] line  = tf.readLine();
		List<HashMap<String,Integer>> list = new ArrayList<HashMap<String, Integer>>();
		HashMap<String,Integer> shelters = new LinkedHashMap<String, Integer>();
			
		
		while (line != null) {
			if (line.length >= 9) {
				if (line[8].equals("BEGINS")) {
//					System.out.println("Iteration: " + line[7]);
					if (shelters.size() > 0) {
						list.add(shelters);
						shelters = new LinkedHashMap<String, Integer>();
					}
				}
				if (line[5].equals("Shelter:")) {
//					System.out.println(line[6]);
					int val = Integer.parseInt(StringUtils.explode(line[9], ':')[1]);
					if (!line[6].equals("el1")) {
						shelters.put(line[6], val);
					}
				}
			}
			line = tf.readLine();
		}
	
		int count = 0;
		System.out.println(list.get(0).size());
		System.out.print("ITERATION,");
		HashMap<String, Integer> sc = list.get(0);
		for (Entry<String, Integer> e : sc.entrySet()) {
			System.out.print(e.getKey() + ",");
		}
		System.out.println("sum");
		
		for (HashMap<String, Integer> s : list) {
			System.out.print(count + ",");
			int sum =0;
			for (Entry<String, Integer> e : s.entrySet()) {
				System.out.print(e.getValue() + ",");
				sum += e.getValue();
			}
			System.out.println(sum);
			count++;
		}
		
		
	}

}
