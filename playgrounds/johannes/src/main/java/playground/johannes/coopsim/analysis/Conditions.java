/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.population.Leg;
import playground.johannes.coopsim.pysical.Trajectory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class Conditions {

	public static Map<String, PlanElementConditionComposite<Leg>> getLegConditions(Collection<Trajectory> trajectories) {
		Set<String> modes = TrajectoryUtils.getModes(trajectories);
		Set<String> purposes = TrajectoryUtils.getTypes(trajectories);

		modes.add(null);
		purposes.add(null);

		Map<String, PlanElementConditionComposite<Leg>> conditions = new HashMap<String, PlanElementConditionComposite<Leg>>();

		for (String mode : modes) {
			for (String purpose : purposes) {
				PlanElementConditionComposite<Leg> composite = new PlanElementConditionComposite<Leg>();
				if (mode == null)
					composite.addComponent(DefaultCondition.getInstance());
				else
					composite.addComponent(new LegModeCondition(mode));

				if (purpose == null)
					composite.addComponent(DefaultCondition.getInstance());
				else
					composite.addComponent(new LegPurposeCondition(purpose));

				String modeStr = mode;
				if (modeStr == null)
					modeStr = "all";

				String purposeStr = purpose;
				if (purposeStr == null)
					purposeStr = "all";

				conditions.put(String.format("%s.%s", modeStr, purposeStr), composite);
			}
		}

		return conditions;
	}
	
	public static Map<String, PlanElementConditionComposite<Leg>> getLegPurposeConditions(Collection<Trajectory> trajectories) {
		Set<String> purposes = TrajectoryUtils.getTypes(trajectories);

		purposes.add(null);

		Map<String, PlanElementConditionComposite<Leg>> conditions = new HashMap<String, PlanElementConditionComposite<Leg>>();

			for (String purpose : purposes) {
				PlanElementConditionComposite<Leg> composite = new PlanElementConditionComposite<Leg>();
				
				if (purpose == null)
					composite.addComponent(DefaultCondition.getInstance());
				else
					composite.addComponent(new LegPurposeCondition(purpose));

				String purposeStr = purpose;
				if (purposeStr == null)
					purposeStr = "all";

				conditions.put(purposeStr, composite);
			}
		
		return conditions;
	}
}
