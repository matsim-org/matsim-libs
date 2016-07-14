/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.johannes.coopsim.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class PkmRouteTask extends TrajectoryAnalyzerTask {
	
	private static final Logger logger = Logger.getLogger(PkmRouteTask.class);

	public static final String KEY = "pkm.route";

	private final Network network;

	private final double startEndFactor;
	
	public PkmRouteTask(Network network, double factor) {
		this.network = network;
		this.startEndFactor = factor;
	}

	@Override
	public void analyze(Set<Trajectory> trajectories, Map<String, DescriptiveStatistics> results) {
		Map<String, PlanElementConditionComposite<Leg>> conditions = Conditions.getLegConditions(trajectories);

		for (Entry<String, PlanElementConditionComposite<Leg>> entry : conditions.entrySet()) {
			double sum = 0;
			int cnt = 0;
			for (Trajectory t : trajectories) {
				for (int i = 1; i < t.getElements().size(); i += 2) {
					Leg leg = (Leg) t.getElements().get(i);
					if (entry.getValue().test(t, leg, i)) {
						if (leg.getRoute() instanceof NetworkRoute) {
							NetworkRoute route = (NetworkRoute) leg.getRoute();
							sum += RouteUtils.calcDistanceExcludingStartEndLink(route, network);

							Link startLink = network.getLinks().get(route.getStartLinkId());
							sum += startLink.getLength() * startEndFactor;

							Link endLink = network.getLinks().get(route.getEndLinkId());
							sum += endLink.getLength() * startEndFactor;
						} else {
							sum += leg.getRoute().getDistance();
							
							if(leg.getMode().equals("car")) {
								cnt++;
							}
						}
					}
				}
				
				
			}

			DescriptiveStatistics stats = new DescriptiveStatistics();
			stats.addValue(sum);

			results.put(String.format("%s.%s.%.2f", KEY, entry.getKey(), startEndFactor), stats);

			if(cnt > 0) {
				logger.warn(String.format("mode=car, type=%s: %s not NetworkRoute", entry.getKey(), cnt));
			}
		}

	}

}
