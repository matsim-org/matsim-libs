/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.dgrether.signalsystems.cottbus.pt;

/**
 *@author jbischoff
 *
 */

public class CottbusPTNetworkAndScheduleGenerator {


	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		
		String cottbusBaseDirectory = "\\\\vsp-nas\\jbischoff\\WinHome\\Docs\\cottbus\\cottbus_feb_fix\\";
//		String cottbusBaseDirectoy = "/shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/";
		
		String[] argu = new String[1];
		argu[0] = cottbusBaseDirectory;
		
		
		CottbusTramLinkCreator.main(argu);
		
		TransitRouteCreator.main(argu);
		SfCottbusPtSchedule.main(argu);
		
	}

}
