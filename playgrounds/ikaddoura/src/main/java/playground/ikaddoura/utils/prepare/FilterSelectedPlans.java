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

package playground.ikaddoura.utils.prepare;

import playground.agarwalamit.utils.plans.SelectedPlansFilter;

/**
* @author ikaddoura
*/

public class FilterSelectedPlans {

	public static void main(String[] args) {
		SelectedPlansFilter spf = new SelectedPlansFilter();
		spf.run("/Users/ihab/Documents/workspace/runs-svn/cemdapMatsimCadyts/run_194c/run_194c.output_plans.xml.gz");
		spf.writePlans("/Users/ihab/Documents/workspace/runs-svn/cemdapMatsimCadyts/run_194c/run_194c.150.plans_selected.xml.gz");
	}

}

