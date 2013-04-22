/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
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

package air.run;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.vis.otfvis.OTFFileWriterFactory;


public class SfAirController {

	/**
	 * @param args
	 * @author fuerbas
	 */
	public static void main(String[] args) {

//		String config = "Z:\\WinHome\\shared-svn\\studies\\countries\\de\\flight\\sf_oag_flight_model\\munich\\flight_model_muc_all_flights\\air_config.xml";
//		String config = "/media/data/work/repos/shared-svn/studies/countries/de/flight/sf_oag_flight_model/air_config.xml";
//		Controler con = new Controler(config);		//args: configfile

//		args[0] = "/media/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_flight_model_2_runways_3600vph/air_config.xml";
		args[0] = "/home/dgrether/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_flight_model_2_runways_3600vph_one_line/air_config.xml";
		Controler con = new Controler(args[0]);		//args: configfile
		con.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		con.setOverwriteFiles(true);
		ControlerListener lis = new SfFlightTimeControlerListener();
		con.addControlerListener(lis);
		con.run();
	
	}
	


}
