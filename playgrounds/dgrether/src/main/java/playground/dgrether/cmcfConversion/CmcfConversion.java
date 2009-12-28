/* *********************************************************************** *
 * project: org.matsim.*
 * CmcfConversion
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dgrether.cmcfConversion;

import playground.msieg.cmcf.BestFitRouter;


/**
 * @author dgrether
 *
 */
public class CmcfConversion {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String base = "/Volumes/data/work/vspSvn/studies/schweiz-ivtch/";
		String net = base + "baseCase/network/ivtch-osm.xml";
		String plans = base + "baseCase/plans/plans_miv_zrh30km_10pct.xml.gz";
		String cmcf = base + "cmcf/plans/flowG001.cmcf";
		String out = base + "cmcf/plans/plans_miv_zrh30km_10pct_cmcf_flowG001.xml.gz";
		String[] args2 = {net, plans, cmcf, out};
		BestFitRouter.main(args2);
		
	}

}
