package playground.mmoyo.utils;

import org.matsim.api.core.v01.Id;

import playground.mmoyo.analysis.counts.reader.CountsReader;

/**compares the simulated values of two countsComparison files*/
public class CountsComparison {

	public static void main(String[] args) {
		//count files should have the same stops
		String file1 = "../../input/juli/original/500.simCountCompareOccupancy.txt";
		String file2 = "../../input/juli/500/500.simCountCompareOccupancy.txt";
		
		CountsReader countReader1 = new CountsReader(file1);;
		CountsReader countReader2 = new CountsReader(file2);;
		String tab = "\t";
		
		for (Id id: countReader1.getStopsIds()){
			System.out.println(id);
			for (int h=0;h<24;h++){
				double val1 = countReader1.getStopSimCounts(id)[h];	
				double val2 = countReader2.getStopSimCounts(id)[h];
				System.out.println((h+1) + tab + val1 +  tab + val2 + tab +((val1-val2)/10));
			}
		}
	}
}
