package playground.tnicolai.matsim4opus.utils.io;

import java.io.BufferedWriter;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;

public class AccessibilityCSVWriter {

	public static void dumpAccessibilityMeasures2CVS(String file,
			Iterator<Node> nodes, Map<Id, Double> travelTimeAccessibilityMap,
			Map<Id, Double> travelCostAccessibilityMap,
			Map<Id, Double> travelDistanceAccessibilityMap) {
		
		BufferedWriter accessibilityIndicatorWriter = IOUtils.getBufferedWriter(file);
		try {
			
			
			// create header
			accessibilityIndicatorWriter.write(Constants.ERSA_ZONE_ID + ","
					+ Constants.ERSA_X_COORDNIATE + ","
					+ Constants.ERSA_Y_COORDINATE + ","
					+ Constants.ERSA_TRAVEL_TIME_ACCESSIBILITY + ","
					+ Constants.ERSA_TRAVEL_COST_ACCESSIBILITY + ","
					+ Constants.ERSA_TRAVEL_DISTANCE_ACCESSIBILITY);
			accessibilityIndicatorWriter.newLine();
			
			
			while( nodes.hasNext() ){
				Node node = nodes.next();
				assert(node != null);

				
			}
			
			
			accessibilityIndicatorWriter.flush();
			accessibilityIndicatorWriter.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
