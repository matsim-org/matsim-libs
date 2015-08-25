/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.southafrica.sandboxes.qvanheerden.freight;

/**
 * Class to call MyCarrierSimulation with paths for kai.  I can't put this into my own playground, because
 * once your playground depends on mine (as it currently does), I cannot (in maven) make mine depend on yours.
 * 
 * @author nagel
 *
 */
public class KNRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String ROOT = "/Users/nagel/southafrica/" ;
		String[] str = {
				ROOT + "MATSim-SA/sandbox/qvanheerden/input/freight/myGridSim/config.xml"  ,
				ROOT + "MATSim-SA/data/areas/nmbm/network/NMBM_Network_FullV7.xml.gz"  ,
				ROOT + "MATSim-SA/sandbox/qvanheerden/input/freight/myGridSim/carrier.xml" ,
				ROOT + "MATSim-SA/sandbox/qvanheerden/input/freight/myGridSim/vehicleTypes.xml" ,
				ROOT + "MATSim-SA/sandbox/qvanheerden/input/freight/myGridSim/initialPlanAlgorithm.xml" ,
				ROOT + "MATSim-SA/sandbox/qvanheerden/input/freight/myGridSim/algorithm.xml" } ;
		
		MyCarrierSimulation.main( str ) ;
		
//		String configFile = args[0];
//		String networkFile = args[1];
//		String carrierPlanFile = args[2];
//		String vehicleTypesFile = args[3];
//		String initialPlanAlgorithm = args[4];
//		String algorithm = args[5];
		
		

	}

}
