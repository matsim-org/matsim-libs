package playground.johannes.gsv.synPop;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import playground.johannes.synpop.data.Element;

public class SortLegsTimeTask implements ProxyPlanTask {

	private static final Logger logger = Logger.getLogger(SortLegsTimeTask.class);
	
	@Override
	public void apply(ProxyPlan plan) {
		SortedMap<Double, Element> map = new TreeMap<Double, Element>();
		/*
		 * Insert leg according to start time or end time, respectively.
		 */
		for(Element leg : plan.getLegs()) {
			String val = leg.getAttribute(CommonKeys.LEG_START_TIME);
			Double time = null;
			if(val == null) {
				val = leg.getAttribute(CommonKeys.LEG_END_TIME);
			}
			
			if(val != null) {
				time = new Double(val);
				map.put(time, leg);
				
			} else {
				logger.debug("Missing leg end and start time. Cannot sort legs.");
				return;
			}
		}
		/*
		 * Check for overlapping legs.
		 */
		double prevEnd = 0;
		for(Entry<Double, Element> entry : map.entrySet()) {
			String startStr = entry.getValue().getAttribute(CommonKeys.LEG_START_TIME);
			if(startStr != null) {
				double start = Double.parseDouble(startStr);
				if(start < prevEnd) {
					logger.debug("Overlapping legs. Cannot sort legs.");
					return;
				}
			}
			String endStr = entry.getValue().getAttribute(CommonKeys.LEG_END_TIME);
			if(endStr != null) {
				prevEnd = Double.parseDouble(endStr);
			} else {
				prevEnd = Double.parseDouble(startStr) + 1;
			}	
		}
		/*
		 * Clear old legs an insert sorted legs.
		 */
		plan.getLegs().clear();
		for(Entry<Double, Element> entry : map.entrySet()) {
			plan.getLegs().add(entry.getValue());
		}

	}

}
