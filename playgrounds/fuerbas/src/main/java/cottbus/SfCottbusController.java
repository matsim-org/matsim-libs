/* *********************************************************************** *
 * project: org.matsim.*
 * SfCottbusController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package cottbus;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.pt.TransitControlerListener;

/**
 * @author fuerbas
 *
 */

public class SfCottbusController {

	public static void main(String[] args) {
		Controler con = new Controler("E:\\Cottbus\\Cottbus_pt\\Cottbus-pt\\config_1.xml");		//args: configfile
		con.setOverwriteFiles(true);
		ControlerListener lis = new TransitControlerListener();
		con.addControlerListener(lis);
		con.run();

	}

}

