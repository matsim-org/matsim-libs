package playground.yu.test;

import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

public class CountsTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MatsimCountsReader mcr = new MatsimCountsReader(new Counts());
		mcr.readFile("../berlin data/link_counts_PKW_hrs0-24.xml");
	}
}
