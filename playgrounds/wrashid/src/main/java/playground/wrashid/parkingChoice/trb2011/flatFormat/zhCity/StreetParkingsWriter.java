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

package playground.wrashid.parkingChoice.trb2011.flatFormat.zhCity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;


public class StreetParkingsWriter extends MatsimXmlWriter {

	public void writeFile(final String filename, String source, ActivityFacilities streetParkingFacilities, String idPrefix) {
		String dtd = "./test/input/playground/wrashid/parkingChoice/infrastructure/flatParkingFormat_v1.dtd";

		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("flatParkings", dtd);

			this.writer.write("<!-- data source: "+ source +" -->\n\n");
			
			this.writer.write("<flatParkings>\n");
			
			writeFacilities(streetParkingFacilities, this.writer, idPrefix);
			
			this.writer.write("</flatParkings>\n");
			
			this.writer.close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void writeFacilities(ActivityFacilities streetParkingFacilities, BufferedWriter writer, String idPrefix) throws IOException {
		for (Id facilityId:streetParkingFacilities.getFacilities().keySet()){
			ActivityFacilityImpl facilityImpl=(ActivityFacilityImpl) streetParkingFacilities.getFacilities().get(facilityId);
			
			Map<String, ActivityOption> activityOptions = facilityImpl.getActivityOptions();
			
			writer.write("\t<parking type=\"public\"");
			writer.write(" id=\""+ idPrefix + facilityId +"\"");
			writer.write(" x=\""+ facilityImpl.getCoord().getX() +"\"");
			writer.write(" y=\""+ facilityImpl.getCoord().getY() +"\"");
			writer.write(" capacity=\""+ Math.round(activityOptions.get("parking").getCapacity()) +"\"");
			writer.write("/>\n");
		}
		
		
	}

	public static void main(String[] args) {
		String sourcePath = "ETH/static data/parking/zürich city/Strassenparkplaetze/street parkings old - 2007 - aufbereitet/streetpark_facilities.xml";
		ActivityFacilities streetParkingFacilities = GeneralLib.readActivityFacilities("C:/data/My Dropbox/" + sourcePath);
		
		StreetParkingsWriter streetParkingWriter=new StreetParkingsWriter();
		streetParkingWriter.writeFile("C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/flat/streetParkings.xml", sourcePath,streetParkingFacilities, "stp-");

	}

}
