/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCreator.java
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
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;

public class PlansCreator {

	/**
	 * Creates plans for the Manhattan-like network.
	 */
	public static void main(String[] args) {
		
		int networkSize 	= 10;
		double distance 	= 1000;
	
		
		int personID		= 1;
	
	
		try{
			 FileWriter fw = new FileWriter("output/Plans_Test1.xml");
			 BufferedWriter out = new BufferedWriter(fw);
			 
			 out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			 out.write("<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">\n\n");
			 
			 out.write("<plans name=\"Plans_Test1\">\n\n");
			 
			 out.write("<!-- ====================================================================== -->\n\n");
			 
			 for (int i=1;i<networkSize;i++){
				 for (int j=1;j<networkSize;j++){
					 int locationIDShopping=(37+(int)(MatsimRandom.random.nextDouble()*(42-37)));
					 int locationIDLeisure=(42+(int)(MatsimRandom.random.nextDouble()*(47-42)));
					 int linkIDShopping=0;
					 if (locationIDShopping==37) linkIDShopping=5;
					 if (locationIDShopping==38) linkIDShopping=14;
					 if (locationIDShopping==39) linkIDShopping=23;
					 if (locationIDShopping==40) linkIDShopping=32;
					 if (locationIDShopping==41) linkIDShopping=41;
					 int linkIDLeisure=0;
					 if (locationIDLeisure==42) linkIDLeisure=50;
					 if (locationIDLeisure==43) linkIDLeisure=59;
					 if (locationIDLeisure==44) linkIDLeisure=68;
					 if (locationIDLeisure==45) linkIDLeisure=77;
					 if (locationIDLeisure==46) linkIDLeisure=86;
					 
					 out.write("\t<person id=\""+personID+"\">\n");
					 
					 out.write("\t\t<knowledge>\n");
					 out.write("\t\t\t<activity type=\"home\">\n");
					 out.write("\t\t\t\t<location id=\""+i+"\" isPrimary=\"yes\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"work\">\n");
					 out.write("\t\t\t\t<location id=\""+(networkSize-1+j)+"\" isPrimary=\"yes\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"shopping\">\n");
					 out.write("\t\t\t\t<location id=\""+locationIDShopping+"\" isPrimary=\"no\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"leisure\">\n");
					 out.write("\t\t\t\t<location id=\""+locationIDLeisure+"\" isPrimary=\"no\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t</knowledge>\n\n");
					 
					 out.write("\t\t<plan>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(90+i)+"\" facility=\""+i+"\" x=\"0.0\" y=\""+(i*distance-distance/2)+"\" start_time=\"00:00:00\" dur=\"08:00:00\" end_time=\"08:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"0\" mode=\"car\" dep_time=\"08:00:00\" trav_time=\"00:00:00\" arr_time=\"08:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"work\" link=\""+(171+j)+"\" facility=\""+(networkSize-1+j)+"\" x=\"9000.0\" y=\""+(j*distance-distance/2)+"\" start_time=\"00:8:00\" dur=\"08:00:00\" end_time=\"16:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"1\" mode=\"car\" dep_time=\"16:00:00\" trav_time=\"00:00:00\" arr_time=\"16:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"shopping\" link=\""+linkIDShopping+"\" facility=\""+locationIDShopping+"\" start_time=\"00:16:00\" dur=\"02:00:00\" end_time=\"18:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"2\" mode=\"car\" dep_time=\"18:00:00\" trav_time=\"00:00:00\" arr_time=\"18:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"leisure\" link=\""+linkIDLeisure+"\" facility=\""+locationIDLeisure+"\" start_time=\"00:18:00\" dur=\"02:00:00\" end_time=\"20:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"3\" mode=\"car\" dep_time=\"20:00:00\" trav_time=\"00:00:00\" arr_time=\"20:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(90+i)+"\" facility=\""+i+"\" x=\"0.0\" y=\""+(i*distance-distance/2)+"\" start_time=\"20:00:00\" dur=\"04:00:00\" end_time=\"24:00:00\" />\n");
					 out.write("\t\t</plan>\n");
					 
					 out.write("\t</person>\n\n");
					 personID++;
					 
					 out.write("<!-- ====================================================================== -->\n\n");
				 }
			 }
			 
			 for (int i=1;i<networkSize;i++){
				 for (int j=1;j<networkSize;j++){
					 int locationIDShopping=(37+(int)(MatsimRandom.random.nextDouble()*(42-37)));
					 int locationIDLeisure=(42+(int)(MatsimRandom.random.nextDouble()*(47-42)));
					 int linkIDShopping=0;
					 if (locationIDShopping==37) linkIDShopping=5;
					 if (locationIDShopping==38) linkIDShopping=14;
					 if (locationIDShopping==39) linkIDShopping=23;
					 if (locationIDShopping==40) linkIDShopping=32;
					 if (locationIDShopping==41) linkIDShopping=41;
					 int linkIDLeisure=0;
					 if (locationIDLeisure==42) linkIDLeisure=50;
					 if (locationIDLeisure==43) linkIDLeisure=59;
					 if (locationIDLeisure==44) linkIDLeisure=68;
					 if (locationIDLeisure==45) linkIDLeisure=77;
					 if (locationIDLeisure==46) linkIDLeisure=86;
					 
					 out.write("\t<person id=\""+personID+"\">\n");
					 
					 out.write("\t\t<knowledge>\n");
					 out.write("\t\t\t<activity type=\"home\">\n");
					 out.write("\t\t\t\t<location id=\""+(networkSize-1+i)+"\" isPrimary=\"yes\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"work\">\n");
					 out.write("\t\t\t\t<location id=\""+j+"\" isPrimary=\"yes\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"shopping\">\n");
					 out.write("\t\t\t\t<location id=\""+locationIDShopping+"\" isPrimary=\"no\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"leisure\">\n");
					 out.write("\t\t\t\t<location id=\""+locationIDLeisure+"\" isPrimary=\"no\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t</knowledge>\n\n");
					 
					 out.write("\t\t<plan>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(171+i)+"\" facility=\""+(networkSize-1+i)+"\" x=\"9000.0\" y=\""+(i*distance-distance/2)+"\" start_time=\"00:00:00\" dur=\"08:00:00\" end_time=\"08:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"0\" mode=\"car\" dep_time=\"08:00:00\" trav_time=\"00:00:00\" arr_time=\"08:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"work\" link=\""+(90+j)+"\" facility=\""+j+"\" x=\"0.0\" y=\""+(j*distance-distance/2)+"\" start_time=\"00:8:00\" dur=\"08:00:00\" end_time=\"16:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"1\" mode=\"car\" dep_time=\"16:00:00\" trav_time=\"00:00:00\" arr_time=\"16:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"shopping\" link=\""+linkIDShopping+"\" facility=\""+locationIDShopping+"\" start_time=\"00:16:00\" dur=\"02:00:00\" end_time=\"18:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"2\" mode=\"car\" dep_time=\"18:00:00\" trav_time=\"00:00:00\" arr_time=\"18:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"leisure\" link=\""+linkIDLeisure+"\" facility=\""+locationIDLeisure+"\" start_time=\"00:18:00\" dur=\"02:00:00\" end_time=\"20:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"3\" mode=\"car\" dep_time=\"20:00:00\" trav_time=\"00:00:00\" arr_time=\"20:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(171+i)+"\" facility=\""+(networkSize-1+i)+"\" x=\"9000.0\" y=\""+(i*distance-distance/2)+"\" start_time=\"20:00:00\" dur=\"04:00:00\" end_time=\"24:00:00\" />\n");
					 out.write("\t\t</plan>\n");
					 
					 out.write("\t</person>\n\n");
					 personID++;
					 
					 out.write("<!-- ====================================================================== -->\n\n");
				 }
			 }
			 
			 for (int i=1;i<networkSize;i++){
				 for (int j=1;j<networkSize;j++){
					 int locationIDShopping=(37+(int)(MatsimRandom.random.nextDouble()*(42-37)));
					 int locationIDLeisure=(42+(int)(MatsimRandom.random.nextDouble()*(47-42)));
					 int linkIDShopping=0;
					 if (locationIDShopping==37) linkIDShopping=5;
					 if (locationIDShopping==38) linkIDShopping=14;
					 if (locationIDShopping==39) linkIDShopping=23;
					 if (locationIDShopping==40) linkIDShopping=32;
					 if (locationIDShopping==41) linkIDShopping=41;
					 int linkIDLeisure=0;
					 if (locationIDLeisure==42) linkIDLeisure=50;
					 if (locationIDLeisure==43) linkIDLeisure=59;
					 if (locationIDLeisure==44) linkIDLeisure=68;
					 if (locationIDLeisure==45) linkIDLeisure=77;
					 if (locationIDLeisure==46) linkIDLeisure=86;
					 
					 out.write("\t<person id=\""+personID+"\">\n");
					 
					 out.write("\t\t<knowledge>\n");
					 out.write("\t\t\t<activity type=\"home\">\n");
					 out.write("\t\t\t\t<location id=\""+(2*(networkSize-1)+i)+"\" isPrimary=\"yes\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"work\">\n");
					 out.write("\t\t\t\t<location id=\""+(3*(networkSize-1)+j)+"\" isPrimary=\"yes\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"shopping\">\n");
					 out.write("\t\t\t\t<location id=\""+locationIDShopping+"\" isPrimary=\"no\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"leisure\">\n");
					 out.write("\t\t\t\t<location id=\""+locationIDLeisure+"\" isPrimary=\"no\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t</knowledge>\n\n");
					 
					 out.write("\t\t<plan>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(i)+"\" facility=\""+(2*(networkSize-1)+i)+"\" x=\""+(i*distance-distance/2)+"\" y=\"0.0\" start_time=\"00:00:00\" dur=\"08:00:00\" end_time=\"08:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"0\" mode=\"car\" dep_time=\"08:00:00\" trav_time=\"00:00:00\" arr_time=\"08:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"work\" link=\""+(9*(networkSize-1)+j)+"\" facility=\""+(3*(networkSize-1)+j)+"\" x=\""+(j*distance-distance/2)+"\" y=\"9000.0\" start_time=\"00:8:00\" dur=\"08:00:00\" end_time=\"16:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"1\" mode=\"car\" dep_time=\"16:00:00\" trav_time=\"00:00:00\" arr_time=\"16:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"shopping\" link=\""+linkIDShopping+"\" facility=\""+locationIDShopping+"\" start_time=\"00:16:00\" dur=\"02:00:00\" end_time=\"18:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"2\" mode=\"car\" dep_time=\"18:00:00\" trav_time=\"00:00:00\" arr_time=\"18:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"leisure\" link=\""+linkIDLeisure+"\" facility=\""+locationIDLeisure+"\" start_time=\"00:18:00\" dur=\"02:00:00\" end_time=\"20:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"3\" mode=\"car\" dep_time=\"20:00:00\" trav_time=\"00:00:00\" arr_time=\"20:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(i)+"\" facility=\""+(2*(networkSize-1)+i)+"\" x=\""+(i*distance-distance/2)+"\" y=\"0.0\" start_time=\"20:00:00\" dur=\"04:00:00\" end_time=\"24:00:00\" />\n");
					 out.write("\t\t</plan>\n");
					 
					 out.write("\t</person>\n\n");
					 personID++;
					 
					 out.write("<!-- ====================================================================== -->\n\n");
				 }
			 }
				
			 for (int i=1;i<networkSize;i++){
				 for (int j=1;j<networkSize;j++){
					 int locationIDShopping=(37+(int)(MatsimRandom.random.nextDouble()*(42-37)));
					 int locationIDLeisure=(42+(int)(MatsimRandom.random.nextDouble()*(47-42)));
					 int linkIDShopping=0;
					 if (locationIDShopping==37) linkIDShopping=5;
					 if (locationIDShopping==38) linkIDShopping=14;
					 if (locationIDShopping==39) linkIDShopping=23;
					 if (locationIDShopping==40) linkIDShopping=32;
					 if (locationIDShopping==41) linkIDShopping=41;
					 int linkIDLeisure=0;
					 if (locationIDLeisure==42) linkIDLeisure=50;
					 if (locationIDLeisure==43) linkIDLeisure=59;
					 if (locationIDLeisure==44) linkIDLeisure=68;
					 if (locationIDLeisure==45) linkIDLeisure=77;
					 if (locationIDLeisure==46) linkIDLeisure=86;
					 
					 out.write("\t<person id=\""+personID+"\">\n");
					 
					 out.write("\t\t<knowledge>\n");
					 out.write("\t\t\t<activity type=\"home\">\n");
					 out.write("\t\t\t\t<location id=\""+(3*(networkSize-1)+i)+"\" isPrimary=\"yes\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"work\">\n");
					 out.write("\t\t\t\t<location id=\""+(2*(networkSize-1)+j)+"\" isPrimary=\"yes\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"shopping\">\n");
					 out.write("\t\t\t\t<location id=\""+locationIDShopping+"\" isPrimary=\"no\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"leisure\">\n");
					 out.write("\t\t\t\t<location id=\""+locationIDLeisure+"\" isPrimary=\"no\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t</knowledge>\n\n");
					 
					 out.write("\t\t<plan>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(9*(networkSize-1)+i)+"\" facility=\""+(3*(networkSize-1)+i)+"\" x=\""+(i*distance-distance/2)+"\" y=\"9000.0\" start_time=\"00:00:00\" dur=\"08:00:00\" end_time=\"08:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"0\" mode=\"car\" dep_time=\"08:00:00\" trav_time=\"00:00:00\" arr_time=\"08:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"work\" link=\""+(j)+"\" facility=\""+(2*(networkSize-1)+j)+"\" x=\""+(j*distance-distance/2)+"\" y=\"0.0\" start_time=\"00:8:00\" dur=\"08:00:00\" end_time=\"16:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"1\" mode=\"car\" dep_time=\"16:00:00\" trav_time=\"00:00:00\" arr_time=\"16:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"shopping\" link=\""+linkIDShopping+"\" facility=\""+locationIDShopping+"\" start_time=\"00:16:00\" dur=\"02:00:00\" end_time=\"18:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"2\" mode=\"car\" dep_time=\"18:00:00\" trav_time=\"00:00:00\" arr_time=\"18:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"leisure\" link=\""+linkIDLeisure+"\" facility=\""+locationIDLeisure+"\" start_time=\"00:18:00\" dur=\"02:00:00\" end_time=\"20:00:00\" />\n");
					 out.write("\t\t\t<leg num=\"3\" mode=\"car\" dep_time=\"20:00:00\" trav_time=\"00:00:00\" arr_time=\"20:00:00\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(4*(networkSize-1)+i)+"\" facility=\""+(3*(networkSize-1)+i)+"\" x=\""+(i*distance-distance/2)+"\" y=\"9000.0\" start_time=\"20:00:00\" dur=\"04:00:00\" end_time=\"24:00:00\" />\n");
					 out.write("\t\t</plan>\n");
					 
					 out.write("\t</person>\n\n");
					 personID++;
					 
					 out.write("<!-- ====================================================================== -->\n\n");
				 }
			 }
			 out.write("</plans>");
			 
			 out.flush();
			 out.close();
			 fw.close();
			 
		}catch (IOException e) {
			Gbl.errorMsg(e);
		}

		
		
	}

}
