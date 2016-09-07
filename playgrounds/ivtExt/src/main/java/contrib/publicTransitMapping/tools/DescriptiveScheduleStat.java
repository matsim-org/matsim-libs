package contrib.publicTransitMapping.tools;

import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import contrib.publicTransitMapping.tools.ScheduleTools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Writes basic schedule statistics to the console
 *
 * @author polettif
 */
public class DescriptiveScheduleStat {

	public static void main(String[] args) {
		run(args[0]);
	}

	public static void run(String scheduleFile) {
		TransitSchedule schedule = ScheduleTools.readTransitSchedule(scheduleFile);

		int nTransitRoutes = 0;
		Map<TransitLine, Integer> statTransitLine = new HashMap<>();
		Set<String> scheduleTransportModes = new HashSet<>();
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				scheduleTransportModes.add(transitRoute.getTransportMode());
				MapUtils.addToInteger(transitLine, statTransitLine, transitRoute.getStops().size(), transitRoute.getStops().size());
				nTransitRoutes++;
			}
		}

		int nStopFacilities = schedule.getFacilities().size();
		int nTransitLines = schedule.getTransitLines().size();

		System.out.println("Statistics for " + scheduleFile);
		System.out.println("   Stop Facilities   "+nStopFacilities);
		System.out.println("   Transit Lines     "+nTransitLines);
		System.out.println("   Transit Routes    "+nTransitRoutes);
		System.out.println("   Transport Modes   "+CollectionUtils.setToString(scheduleTransportModes) + " ("+scheduleTransportModes.size()+")");
	}
}
