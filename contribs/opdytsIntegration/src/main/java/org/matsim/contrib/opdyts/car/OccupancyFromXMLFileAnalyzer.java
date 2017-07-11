package org.matsim.contrib.opdyts.car;

import floetteroed.utilities.Time;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.opdyts.utils.TimeDiscretization;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class OccupancyFromXMLFileAnalyzer {

	private final TimeDiscretization timeDiscr;

	public OccupancyFromXMLFileAnalyzer(final TimeDiscretization timeDiscr) {
		this.timeDiscr = timeDiscr;
	}

	public void run(final String eventsFileName) {

		final LinkOccupancyAnalyzer analyzer = new LinkOccupancyAnalyzer(this.timeDiscr, null);

		final EventsManager events = EventsUtils.createEventsManager(ConfigUtils.createConfig());
		events.addHandler(analyzer);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFileName);

		System.out.print("time");
		for (Id<Link> linkId : analyzer.observedLinkSetView()) {
			System.out.print("\t");
			System.out.print(linkId);
		}
		System.out.println();

		for (int bin = 0; bin < this.timeDiscr.getBinCnt(); bin++) {
			System.out.print("[" + Time.strFromSec(this.timeDiscr.getBinStartTime_s(bin)) + ","
					+ Time.strFromSec(this.timeDiscr.getBinEndTime_s(bin)) + ")");
			for (Id<Link> link : analyzer.observedLinkSetView()) {
				System.out.print("\t");
				System.out.print(analyzer.getCount(link, bin));
			}
			System.out.println();
		}
	}

	public static void main(String[] args) {
		final int startTime_s = 0;
		final int binSize_s = 3600;
		final int binCnt = 24;
		final OccupancyFromXMLFileAnalyzer analyzer = new OccupancyFromXMLFileAnalyzer(
				new TimeDiscretization(startTime_s, binSize_s, binCnt));
		analyzer.run("./100.events.xml.gz");
//		analyzer.run("/Nobackup/Profilen/git/matsim/playgrounds/gunnar/output/"
//				+ "cba/toynet/output/ITERS/it.100/100.events.xml.gz");
	}
}
