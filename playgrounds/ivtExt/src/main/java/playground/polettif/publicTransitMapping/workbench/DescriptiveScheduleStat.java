package playground.polettif.publicTransitMapping.workbench;

import com.sun.media.jfxmedia.logging.Logger;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.polettif.publicTransitMapping.tools.ScheduleTools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author polettif
 */
public class DescriptiveScheduleStat {

	public static void main(String[] args) {
		TransitSchedule schedule = ScheduleTools.readTransitSchedule(args[0]);

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
		int nTransportModes = scheduleTransportModes.size();

		System.out.println("Statistics for " + args[0]);
		System.out.println("   Stop Facilities   "+nStopFacilities);
		System.out.println("   Transit Lines     "+nTransitLines);
		System.out.println("   Transit Routes    "+nTransitRoutes);
		System.out.println("   Transport Modes   "+CollectionUtils.setToString(scheduleTransportModes) + " ("+nTransportModes+")");
	}
}
