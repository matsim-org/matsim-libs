/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.coopers;

import org.matsim.run.OTFVis;



/**
 * @author dgrether
 *
 */
public class CoopersVisStarter {

	private static final String network = "../studies/arvidDaniel/input/berlinData/wip_net.xml";

	private static final String runbase = "../studies/arvidDaniel/output/berlinApril/";

//	private static final String veh = "100.T.veh.gz";

//	private static final String veh = "300.T.veh.gz";

	private static final String snapshot = "snapshot.mvi";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		OTFStarter.main(new String[] { runbase + "run415/" + veh, network});
//		OTFStarter.main(new String[] { runbase + "normalCase/" + snapshot});
//		OTFStarter.main(new String[] { runbase + "normalCaseExtendedCapacities/" + snapshot});

//		OTFVis.main(new String[] { runbase + "accidentNoControl/" + snapshot});

		OTFVis.main(new String[] { runbase + "disturbance/" + snapshot});

	}

}
