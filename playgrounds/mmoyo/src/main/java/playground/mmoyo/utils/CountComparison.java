package playground.mmoyo.utils;

import org.matsim.api.core.v01.Id;
import playground.mmoyo.analysis.counts.reader.CountsReader;

public class CountComparison {
	public static void main(String[] args) {
		String countComparisonFile = "../../input/juli/500/500.simBseCountCompareOccupancy.txt";
		CountsReader countReader = new CountsReader(countComparisonFile);

		String tab= "\t";
		for (Id id : countReader.getStopsIds()){
			System.out.println(id);
			for (int h=0; h<24; h++ ){
				double real = countReader.getStopCounts(id)[h];
				double sim = countReader.getStopSimCounts(id)[h];
				System.out.println((h+1) + tab + real + tab + sim + tab + ((real-sim)/10));
			}
		}
	}

}
