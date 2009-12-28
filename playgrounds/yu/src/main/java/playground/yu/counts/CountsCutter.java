/**
 * 
 */
package playground.yu.counts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;

/**
 * @author yu
 * 
 */
public class CountsCutter {
	private static int fragmentsNo;
	private static Coord distanceFilterCenterNodeCoord;
	private static double distanceFilter;

	public static boolean isInRange(final Id linkid, final NetworkLayer net) {
		Link l = net.getLink(linkid);
		if (l == null) {
			System.out.println("Cannot find requested link: "
					+ linkid.toString());
			return false;
		}
		return ((LinkImpl) l).calcDistance(distanceFilterCenterNodeCoord) < distanceFilter;
	}

	/**
	 * with this class, a countsfiles fragements into N small countsfiles.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		fragmentsNo = 10;
		String distanceFilterCenterNodeId = "2531";
		distanceFilter = 30000.0;
		String originalCountsFilename = "../matsimTests/Calibration/counts/countsIVTCH.xml";
		String networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";

		int validCountsstationNo = 0;

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(networkFilename);
		distanceFilterCenterNodeCoord = net.getNode(distanceFilterCenterNodeId)
				.getCoord();

		Counts originalCounts = new Counts();
		new MatsimCountsReader(originalCounts).readFile(originalCountsFilename);

		int year = originalCounts.getYear();
		String name = originalCounts.getName();
		String layer = originalCounts.getLayer();
		String description = originalCounts.getDescription();
		// ---------------PREPARE COUNTS------------
		Counts[] countsArray = new Counts[fragmentsNo];
		int[] countsCounter = new int[fragmentsNo];
		for (int i = 0; i < fragmentsNo; i++) {
			Counts counts = new Counts();
			counts.setYear(year);
			counts.setName(name);
			counts.setLayer(layer);
			counts.setDescription(description + "||" + i + ". 10%-sample!");
			countsArray[i] = counts;
			countsCounter[i] = 0;
		}

		// ----------RANDOMLY DISTRIBUTION----------
		List<Entry<Id, Count>> countEntrys = new ArrayList<Entry<Id, Count>>();
		for (Entry<Id, Count> entry : originalCounts.getCounts().entrySet())
			if (isInRange(entry.getKey(), net))
				countEntrys.add(entry);
		Collections.shuffle(countEntrys);

		for (int i = 0; i < countEntrys.size(); i++) {
			Entry<Id, Count> entry = countEntrys.get(i);
			Id linkId = entry.getKey();
			Count originalCount = entry.getValue();
			int idx = i % fragmentsNo;
			Count count = countsArray[idx].createCount(linkId, originalCount
					.getCsId());
			countsCounter[idx]++;
			count.setCoord(net.getLink(linkId).getCoord());
			for (Volume vol : originalCount.getVolumes().values())
				count.createVolume(vol.getHour(), vol.getValue());
			validCountsstationNo++;
		}

		// ------------------WRITE COUNTSFILE----------------
		for (int i = 0; i < fragmentsNo; i++) {
			new CountsWriter(countsArray[i])
					.writeFile("../matsimTests/Calibration/test/countsIVTCH." + i + ".xml");
			System.out.println("countsIVTCH." + i + ".xml contains\t"
					+ countsCounter[i] + "\tcountstations.");
		}
		System.out.println("There are all\t" + validCountsstationNo
				+ "\tcountstations in the 30 km circle.");
	}

}
