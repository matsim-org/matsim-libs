package playground.yu.counts;

import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;

public class CountsFragmentTests {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String originalCountsFilename = "../matsimTests/Calibration/test/counts100.xml";
		// String networkFilename = "examples/equil/network.xml";
		String fragmentsCountsFilenameBase = "../matsimTests/Calibration/test/counts100.";

		// NetworkLayer network = new NetworkLayer();
		// new MatsimNetworkReader(network).readFile(networkFilename);

		Counts originalCounts = new Counts();
		new MatsimCountsReader(originalCounts).readFile(originalCountsFilename);

		System.out.println(originalCounts.toString());
		for (Count count : originalCounts.getCounts().values()) {
			System.out.println(count.toString());
			for (Volume volume : count.getVolumes().values())
				System.out.println(volume.toString());
		}

		Counts fragmentsCounts = new Counts();
		for (int i = 0; i < 4; i++)
			new MatsimCountsReader(fragmentsCounts)
					.readFile(fragmentsCountsFilenameBase + i + ".xml");

		System.out.println(fragmentsCounts.toString());
		for (Count count : fragmentsCounts.getCounts().values()) {
			System.out.println(count.toString());
			for (Volume volume : count.getVolumes().values())
				System.out.println(volume.toString());
		}
	}

}
