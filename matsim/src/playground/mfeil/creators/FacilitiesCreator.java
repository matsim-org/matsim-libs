/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesCreator.java
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

package playground.mfeil.creators;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.gbl.Gbl;

public class FacilitiesCreator {

	/**
	 * Creates facilities for the Manhattan-like network.
	 */
	public static void main(String[] args) {
		
		int networkSize 	= 10;
		double distance 	= 1000;
	
		
		int facilityID	= 1;
	
	
		try{
			 FileWriter fw = new FileWriter("output/facilities.xml");
			 BufferedWriter out = new BufferedWriter(fw);
			 
			 out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			 out.write("<!DOCTYPE facilities SYSTEM \"http://www.matsim.org/files/dtd/facilities_v1.dtd\">\n\n");
			 
			 out.write("<facilities name=\"Facilities_Test1\">\n\n");
			 
			 out.write("<!-- ====================================================================== -->\n\n");
			 
			 for (int i=1;i<networkSize;i++){
				 out.write("\t<facility id=\""+facilityID+"\" x=\"0.0\" y=\""+(i*distance-distance/2)+"\">\n");
				 out.write("\t\t<activity type=\"home\"/>\n");
				 out.write("\t\t<activity type=\"work\"/>\n");
				 out.write("\t</facility>\n\n");
				 facilityID++;
				 out.write("<!-- ====================================================================== -->\n\n");
			 }
			 for (int i=1;i<networkSize;i++){
				 out.write("\t<facility id=\""+facilityID+"\" x=\""+(networkSize-1)*distance+"\" y=\""+(i*distance-distance/2)+"\">\n");
				 out.write("\t\t<activity type=\"home\"/>\n");
				 out.write("\t\t<activity type=\"work\"/>\n");
				 out.write("\t</facility>\n\n");
				 facilityID++;
				 out.write("<!-- ====================================================================== -->\n\n");
			 }
			 for (int i=1;i<networkSize;i++){
				 out.write("\t<facility id=\""+facilityID+"\" x=\""+(i*distance-distance/2)+"\" y=\"0.0\">\n");
				 out.write("\t\t<activity type=\"home\"/>\n");
				 out.write("\t\t<activity type=\"work\"/>\n");
				 out.write("\t</facility>\n\n");
				 facilityID++;
				 out.write("<!-- ====================================================================== -->\n\n");
			 }
			 for (int i=1;i<networkSize;i++){
				 out.write("\t<facility id=\""+facilityID+"\" x=\""+(i*distance-distance/2)+"\" y=\""+distance*(networkSize-1)+"\">\n");
				 out.write("\t\t<activity type=\"home\"/>\n");
				 out.write("\t\t<activity type=\"work\"/>\n");
				 out.write("\t</facility>\n\n");
				 facilityID++;
				 out.write("<!-- ====================================================================== -->\n\n");
			 }
			 for (int i=0;i<networkSize;i+=2){
				 out.write("\t<facility id=\""+facilityID+"\" x=\""+(networkSize/2*distance-distance/2)+"\" y=\""+(i*distance)+"\">\n");
				 out.write("\t\t<activity type=\"shopping\"/>\n");
				 out.write("\t</facility>\n\n");
				 facilityID++;
				 out.write("<!-- ====================================================================== -->\n\n");
			 }
			 for (int i=1;i<networkSize;i+=2){
				 out.write("\t<facility id=\""+facilityID+"\" x=\""+(networkSize/2*distance-distance/2)+"\" y=\""+(i*distance)+"\">\n");
				 out.write("\t\t<activity type=\"leisure\"/>\n");
				 out.write("\t</facility>\n\n");
				 facilityID++;
				 out.write("<!-- ====================================================================== -->\n\n");
			 }
			 out.write("</facilities>");
			 
			 out.flush();
			 out.close();
			 fw.close();
			 
		}catch (IOException e) {
			Gbl.errorMsg(e);
		}

		
		
	}

}
