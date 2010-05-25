/* *********************************************************************** *
 * project: org.matsim.*
 * DgAnalysisReaderFilter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.benjamin.analysis.filter;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.dgrether.analysis.io.DgAnalysisReaderFilter;

/**
 * @author benjamin
 *
 */
public class OnlyInnerZurichFilter implements DgAnalysisReaderFilter {

	private RoadPricingScheme tollLinks ;

	public OnlyInnerZurichFilter(RoadPricingScheme tollLinks) {
		this.tollLinks = tollLinks;
	}

	/* (non-Javadoc)
	 * @see playground.dgrether.analysis.io.DgAnalysisReaderFilter#doAcceptPerson(org.matsim.api.core.v01.population.Person)
	 */
	@Override
	public boolean doAcceptPerson(Person person) {
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()){
            if (pe instanceof Activity) {
                    Activity act = (Activity) pe;
			if (this.tollLinks.getLinkIdSet().contains( act.getLinkId())) {
				return true;
            }
            }
    }
		return false;
	}

}
