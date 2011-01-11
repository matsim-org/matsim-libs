/* *********************************************************************** *
 * project: org.matsim.*
 * 
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
package playground.benjamin;

import org.matsim.run.OTFVis;


/**
 *
 */
public class BkVis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

//===========================================================================================================		
//		Für den mvi-modus:
//			String otffile = "../detailedEval/teststrecke/sim/output/20090707/ITERS/it.0/0.otfvis.mvi";
//			String otffile = "../detailedEval/testRuns/output/ITERS/it.0/1.0.otfvis.mvi";
//			String otffile = "../matsim/output/singleIteration/ITERS/it.0/0.otfvis.mvi";
//			String otffile = BkPaths.RUNSSVN + "run749/it.2000/749.2000.Zurich.otfvis.mvi";
//			String otffile = BkPaths.RUNSSVN + "run953/it.1000/953.1000.events.mvi";

//===========================================================================================================		
/*	Für den interactiven Modus:
		Entweder DgOTFVisReplayLastIteration.java ausführen oder manuell unten die config übergeben und vorher dort folgendes ändern:
		
		1. QSim-Modul einschalten ("qsim" statt "simulation"):
		<module name="qsim">
			<param name="startTime" value="00:00:00" />
			<param name="endTime" value="24:00:00" />
	
			<param name="flowCapacityFactor" value="0.1" />
			<param name="storageCapacityFactor" value="0.30" />
	
			<param name="stuckTime" value="10" />
			<param name="removeStuckVehicles" value="no" />
		</module>
		
		2. Pfade zu output_network und zu den output_plans anpassen!*/
			
		
//		String otffile = "../matsim/examples/tutorial/singleIteration.xml";
//		String otffile = BkPaths.RUNSSVN + "run749/TestOTFVis_interactive/749.output_config.xml";
//		String otffile = "../detailedEval/pop/140k-synthetische-personen/config-for-visualisation.xml";
//		String otffile = "../detailedEval/teststrecke/sim/input/liveConfig_benjamin.xml";
		String otffile = "../detailedEval/testRuns/input/config.xml";
		
//		String otffile = BkPaths.RUNSSVN + "run950/950.output_config.xml";

		
		OTFVis.main(new String[] {otffile});
	}

}
