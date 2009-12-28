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

import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;

public class PlansCreator_RandomTimes {

	/**
	 * Creates plans for the Manhattan-like network.
	 */
	public static void main(String[] args) {
		
		int networkSize 	= 10;
		double distance 	= 1000;
		int personID		= 1;
	
	
		try{
			 FileWriter fw = new FileWriter("output/plans.xml");
			 BufferedWriter out = new BufferedWriter(fw);
			 
			 out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			 out.write("<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">\n\n");
			 
			 out.write("<plans name=\"Plans_Test1\">\n\n");
			 
			 out.write("<!-- ====================================================================== -->\n\n");
			 
			 for (int i=1;i<networkSize;i++){
				 for (int j=1;j<networkSize;j++){
					 int locationIDShopping=(37+(int)(MatsimRandom.getRandom().nextDouble()*(42-37)));
					 int locationIDLeisure=(42+(int)(MatsimRandom.getRandom().nextDouble()*(47-42)));
					 int shoppingX =0, shoppingY=0, leisureX=0, leisureY=0, linkIDShopping=0, linkIDLeisure = 0;
					
					 int[]IDs = getShoppingIDs (locationIDShopping);
					 linkIDShopping=IDs[0];
					 shoppingX=IDs[1];
					 shoppingY=IDs[2];
					 IDs = getLeisureIDs (locationIDLeisure);
					 linkIDLeisure=IDs[0];
					 leisureX=IDs[1];
					 leisureY=IDs[2];
					 
					 //double ran = MatsimRandom.getRandom().nextDouble();
					 double ran = 0;
					 double time1 = MatsimRandom.getRandom().nextDouble()*86400;
					 double time2 = MatsimRandom.getRandom().nextDouble()*(86400-time1)+time1;
					 double time3 = MatsimRandom.getRandom().nextDouble()*(86400-time2)+time2;
					 double time4 = MatsimRandom.getRandom().nextDouble()*(86400-time3)+time3;
					 
					 
					 out.write("\t<person id=\""+personID+"\" age=\""+((int)(MatsimRandom.getRandom().nextDouble()*100))+"\">\n");
					 
					 out.write("\t\t<knowledge>\n");
					 out.write("\t\t\t<activity type=\"home\">\n");
					 out.write("\t\t\t\t<location id=\""+i+"\" isPrimary=\"yes\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"work\">\n");
					 if (ran<0.5){
						 out.write("\t\t\t\t<location id=\""+(networkSize-1+j)+"\" isPrimary=\"yes\"/>\n");
						 out.write("\t\t\t</activity>\n");
						 out.write("\t\t\t<activity type=\"shopping\">\n");
						 out.write("\t\t\t\t<location id=\""+locationIDShopping+"\" isPrimary=\"no\"/>\n");
					 }
					 else {
						 out.write("\t\t\t\t<location id=\""+(networkSize-1+j)+"\" isPrimary=\"no\"/>\n");
						 out.write("\t\t\t</activity>\n");
						 out.write("\t\t\t<activity type=\"shopping\">\n");
						 out.write("\t\t\t\t<location id=\""+locationIDShopping+"\" isPrimary=\"yes\"/>\n"); 
					 }
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"leisure\">\n");
					 out.write("\t\t\t\t<location id=\""+locationIDLeisure+"\" isPrimary=\"no\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t</knowledge>\n\n");
					 
					 out.write("\t\t<plan>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(90+i)+"\" facility=\""+i+"\" x=\"0.0\" y=\""+(i*distance-distance/2)+"\" start_time=\"00:00:00\" dur=\""+time1+"\" end_time=\""+time1+"\" />\n");
					 out.write("\t\t\t<leg num=\"0\" mode=\"car\" dep_time=\""+time1+"\" trav_time=\"00:00:00\" arr_time=\""+time1+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"work\" link=\""+(171+j)+"\" facility=\""+(networkSize-1+j)+"\" x=\"9000.0\" y=\""+(j*distance-distance/2)+"\" start_time=\""+time1+"\" dur=\""+(time2-time1)+"\" end_time=\""+time2+"\" />\n");
					 out.write("\t\t\t<leg num=\"1\" mode=\"car\" dep_time=\""+time2+"\" trav_time=\"00:00:00\" arr_time=\""+time2+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"shopping\" link=\""+linkIDShopping+"\" facility=\""+locationIDShopping+"\" x=\""+shoppingX+"\" y=\""+shoppingY+"\" start_time=\""+time2+"\" dur=\""+(time3-time2)+"\" end_time=\""+time3+"\" />\n");
					 out.write("\t\t\t<leg num=\"2\" mode=\"car\" dep_time=\""+time3+"\" trav_time=\"00:00:00\" arr_time=\""+time3+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"leisure\" link=\""+linkIDLeisure+"\" facility=\""+locationIDLeisure+"\" x=\""+leisureX+"\" y=\""+leisureY+"\" start_time=\""+time3+"\" dur=\""+(time4-time3)+"\" end_time=\""+time4+"\" />\n");
					 out.write("\t\t\t<leg num=\"3\" mode=\"car\" dep_time=\""+time4+"\" trav_time=\"00:00:00\" arr_time=\""+time4+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(90+i)+"\" facility=\""+i+"\" x=\"0.0\" y=\""+(i*distance-distance/2)+"\" start_time=\""+time4+"\" dur=\""+(86400-time4)+"\" end_time=\"24:00:00\" />\n");
					 out.write("\t\t</plan>\n");
					 
					 out.write("\t</person>\n\n");
					 personID++;
					 
					 out.write("<!-- ====================================================================== -->\n\n");
				 }
			 }
			 
			 for (int i=1;i<networkSize;i++){
				 for (int j=1;j<networkSize;j++){
					 int locationIDShopping=(37+(int)(MatsimRandom.getRandom().nextDouble()*(42-37)));
					 int locationIDLeisure=(42+(int)(MatsimRandom.getRandom().nextDouble()*(47-42)));
					 int shoppingX =0, shoppingY=0, leisureX=0, leisureY=0, linkIDShopping=0, linkIDLeisure = 0;
						
					 int[]IDs = getShoppingIDs (locationIDShopping);
					 linkIDShopping=IDs[0];
					 shoppingX=IDs[1];
					 shoppingY=IDs[2];
					 IDs = getLeisureIDs (locationIDLeisure);
					 linkIDLeisure=IDs[0];
					 leisureX=IDs[1];
					 leisureY=IDs[2];
					 
					//double ran = MatsimRandom.getRandom().nextDouble();
					 double ran = 0;
					 double time1 = MatsimRandom.getRandom().nextDouble()*86400;
					 double time2 = MatsimRandom.getRandom().nextDouble()*(86400-time1)+time1;
					 double time3 = MatsimRandom.getRandom().nextDouble()*(86400-time2)+time2;
					 double time4 = MatsimRandom.getRandom().nextDouble()*(86400-time3)+time3;
					 
					 out.write("\t<person id=\""+personID+"\" age=\""+((int)(MatsimRandom.getRandom().nextDouble()*100))+"\">\n");
					 
					 out.write("\t\t<knowledge>\n");
					 out.write("\t\t\t<activity type=\"home\">\n");
					 out.write("\t\t\t\t<location id=\""+(networkSize-1+i)+"\" isPrimary=\"yes\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"work\">\n");
					 if (ran<0.5){
						 out.write("\t\t\t\t<location id=\""+j+"\" isPrimary=\"yes\"/>\n");
						 out.write("\t\t\t</activity>\n");
						 out.write("\t\t\t<activity type=\"shopping\">\n");
						 out.write("\t\t\t\t<location id=\""+locationIDShopping+"\" isPrimary=\"no\"/>\n");
					 }
					 else{
						 out.write("\t\t\t\t<location id=\""+j+"\" isPrimary=\"no\"/>\n");
						 out.write("\t\t\t</activity>\n");
						 out.write("\t\t\t<activity type=\"shopping\">\n");
						 out.write("\t\t\t\t<location id=\""+locationIDShopping+"\" isPrimary=\"yes\"/>\n"); 
					 }
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"leisure\">\n");
					 out.write("\t\t\t\t<location id=\""+locationIDLeisure+"\" isPrimary=\"no\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t</knowledge>\n\n");
					 
					 out.write("\t\t<plan>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(171+i)+"\" facility=\""+(networkSize-1+i)+"\" x=\"9000.0\" y=\""+(i*distance-distance/2)+"\" start_time=\"00:00:00\" dur=\""+time1+"\" end_time=\""+time1+"\" />\n");
					 out.write("\t\t\t<leg num=\"0\" mode=\"car\" dep_time=\""+time1+"\" trav_time=\"00:00:00\" arr_time=\""+time1+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"work\" link=\""+(90+j)+"\" facility=\""+j+"\" x=\"0.0\" y=\""+(j*distance-distance/2)+"\" start_time=\""+time1+"\" dur=\""+(time2-time1)+"\" end_time=\""+time2+"\" />\n");
					 out.write("\t\t\t<leg num=\"1\" mode=\"car\" dep_time=\""+time1+"\" trav_time=\"00:00:00\" arr_time=\""+time1+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"shopping\" link=\""+linkIDShopping+"\" facility=\""+locationIDShopping+"\" x=\""+shoppingX+"\" y=\""+shoppingY+"\" start_time=\""+time2+"\" dur=\""+(time3-time2)+"\" end_time=\""+time3+"\" />\n");
					 out.write("\t\t\t<leg num=\"2\" mode=\"car\" dep_time=\""+time1+"\" trav_time=\"00:00:00\" arr_time=\""+time1+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"leisure\" link=\""+linkIDLeisure+"\" facility=\""+locationIDLeisure+"\" x=\""+leisureX+"\" y=\""+leisureY+"\" start_time=\""+time3+"\" dur=\""+(time4-time3)+"\" end_time=\""+time4+"\" />\n");
					 out.write("\t\t\t<leg num=\"3\" mode=\"car\" dep_time=\""+time1+"\" trav_time=\"00:00:00\" arr_time=\""+time1+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(171+i)+"\" facility=\""+(networkSize-1+i)+"\" x=\"9000.0\" y=\""+(i*distance-distance/2)+"\" start_time=\""+time4+"\" dur=\""+(86400-time4)+"\" end_time=\"24:00:00\" />\n");
					 out.write("\t\t</plan>\n");
					 
					 out.write("\t</person>\n\n");
					 personID++;
					 
					 out.write("<!-- ====================================================================== -->\n\n");
				 }
			 }
			 
			 for (int i=1;i<networkSize;i++){
				 for (int j=1;j<networkSize;j++){
					 int locationIDShopping=(37+(int)(MatsimRandom.getRandom().nextDouble()*(42-37)));
					 int locationIDLeisure=(42+(int)(MatsimRandom.getRandom().nextDouble()*(47-42)));
					 int shoppingX =0, shoppingY=0, leisureX=0, leisureY=0, linkIDShopping=0, linkIDLeisure = 0;
						
					 int[]IDs = getShoppingIDs (locationIDShopping);
					 linkIDShopping=IDs[0];
					 shoppingX=IDs[1];
					 shoppingY=IDs[2];
					 IDs = getLeisureIDs (locationIDLeisure);
					 linkIDLeisure=IDs[0];
					 leisureX=IDs[1];
					 leisureY=IDs[2];
					 
					//double ran = MatsimRandom.getRandom().nextDouble();
					 double ran = 0;
					 double time1 = MatsimRandom.getRandom().nextDouble()*86400;
					 double time2 = MatsimRandom.getRandom().nextDouble()*(86400-time1)+time1;
					 double time3 = MatsimRandom.getRandom().nextDouble()*(86400-time2)+time2;
					 double time4 = MatsimRandom.getRandom().nextDouble()*(86400-time3)+time3;
					 out.write("\t<person id=\""+personID+"\" age=\""+((int)(MatsimRandom.getRandom().nextDouble()*100))+"\">\n");
					 
					 out.write("\t\t<knowledge>\n");
					 out.write("\t\t\t<activity type=\"home\">\n");
					 out.write("\t\t\t\t<location id=\""+(2*(networkSize-1)+i)+"\" isPrimary=\"yes\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"work\">\n");
					 if (ran<0.5){
						 out.write("\t\t\t\t<location id=\""+(3*(networkSize-1)+j)+"\" isPrimary=\"yes\"/>\n");
						 out.write("\t\t\t</activity>\n");
						 out.write("\t\t\t<activity type=\"shopping\">\n");
						 out.write("\t\t\t\t<location id=\""+locationIDShopping+"\" isPrimary=\"no\"/>\n");
					 }
					 else{
						 out.write("\t\t\t\t<location id=\""+(3*(networkSize-1)+j)+"\" isPrimary=\"no\"/>\n");
						 out.write("\t\t\t</activity>\n");
						 out.write("\t\t\t<activity type=\"shopping\">\n");
						 out.write("\t\t\t\t<location id=\""+locationIDShopping+"\" isPrimary=\"yes\"/>\n");
						 
					 }
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"leisure\">\n");
					 out.write("\t\t\t\t<location id=\""+locationIDLeisure+"\" isPrimary=\"no\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t</knowledge>\n\n");
					 
					 out.write("\t\t<plan>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(i)+"\" facility=\""+(2*(networkSize-1)+i)+"\" x=\""+(i*distance-distance/2)+"\" y=\"0.0\" start_time=\"00:00:00\" dur=\""+time1+"\" end_time=\""+time1+"\" />\n");
					 out.write("\t\t\t<leg num=\"0\" mode=\"car\" dep_time=\""+time1+"\" trav_time=\"00:00:00\" arr_time=\""+time1+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"work\" link=\""+(9*(networkSize-1)+j)+"\" facility=\""+(3*(networkSize-1)+j)+"\" x=\""+(j*distance-distance/2)+"\" y=\"9000.0\" start_time=\""+time1+"\" dur=\""+(time2-time1)+"\" end_time=\""+time2+"\" />\n");
					 out.write("\t\t\t<leg num=\"1\" mode=\"car\" dep_time=\""+time2+"\" trav_time=\"00:00:00\" arr_time=\""+time2+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"shopping\" link=\""+linkIDShopping+"\" facility=\""+locationIDShopping+"\" x=\""+shoppingX+"\" y=\""+shoppingY+"\" start_time=\""+time2+"\" dur=\""+(time3-time2)+"\" end_time=\""+time3+"\" />\n");
					 out.write("\t\t\t<leg num=\"2\" mode=\"car\" dep_time=\""+time3+"\" trav_time=\"00:00:00\" arr_time=\""+time3+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"leisure\" link=\""+linkIDLeisure+"\" facility=\""+locationIDLeisure+"\" x=\""+leisureX+"\" y=\""+leisureY+"\" start_time=\""+time3+"\" dur=\""+(time4-time3)+"\" end_time=\""+time4+"\" />\n");
					 out.write("\t\t\t<leg num=\"3\" mode=\"car\" dep_time=\""+time4+"\" trav_time=\"00:00:00\" arr_time=\""+time4+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(i)+"\" facility=\""+(2*(networkSize-1)+i)+"\" x=\""+(i*distance-distance/2)+"\" y=\"0.0\" start_time=\""+time4+"\" dur=\""+(86400-time4)+"\" end_time=\"24:00:00\" />\n");
					 out.write("\t\t</plan>\n");
					 
					 out.write("\t</person>\n\n");
					 personID++;
					 
					 out.write("<!-- ====================================================================== -->\n\n");
				 }
			 }
				
			 for (int i=1;i<networkSize;i++){
				 for (int j=1;j<networkSize;j++){
					 int locationIDShopping=(37+(int)(MatsimRandom.getRandom().nextDouble()*(42-37)));
					 int locationIDLeisure=(42+(int)(MatsimRandom.getRandom().nextDouble()*(47-42)));
					 int shoppingX =0, shoppingY=0, leisureX=0, leisureY=0, linkIDShopping=0, linkIDLeisure = 0;
						
					 int[]IDs = getShoppingIDs (locationIDShopping);
					 linkIDShopping=IDs[0];
					 shoppingX=IDs[1];
					 shoppingY=IDs[2];
					 IDs = getLeisureIDs (locationIDLeisure);
					 linkIDLeisure=IDs[0];
					 leisureX=IDs[1];
					 leisureY=IDs[2];
					 
					//double ran = MatsimRandom.getRandom().nextDouble();
					 double ran = 0;
					 double time1 = MatsimRandom.getRandom().nextDouble()*86400;
					 double time2 = MatsimRandom.getRandom().nextDouble()*(86400-time1)+time1;
					 double time3 = MatsimRandom.getRandom().nextDouble()*(86400-time2)+time2;
					 double time4 = MatsimRandom.getRandom().nextDouble()*(86400-time3)+time3;
					 
					 out.write("\t<person id=\""+personID+"\" age=\""+((int)(MatsimRandom.getRandom().nextDouble()*100))+"\">\n");
					 
					 out.write("\t\t<knowledge>\n");
					 out.write("\t\t\t<activity type=\"home\">\n");
					 out.write("\t\t\t\t<location id=\""+(3*(networkSize-1)+i)+"\" isPrimary=\"yes\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"work\">\n");
					 if (ran<0.5){
						 out.write("\t\t\t\t<location id=\""+(2*(networkSize-1)+j)+"\" isPrimary=\"yes\"/>\n");
						 out.write("\t\t\t</activity>\n");
						 out.write("\t\t\t<activity type=\"shopping\">\n");
						 out.write("\t\t\t\t<location id=\""+locationIDShopping+"\" isPrimary=\"no\"/>\n");
					 }
					 else{
						 out.write("\t\t\t\t<location id=\""+(2*(networkSize-1)+j)+"\" isPrimary=\"no\"/>\n");
						 out.write("\t\t\t</activity>\n");
						 out.write("\t\t\t<activity type=\"shopping\">\n");
						 out.write("\t\t\t\t<location id=\""+locationIDShopping+"\" isPrimary=\"yes\"/>\n");
					 }
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t\t<activity type=\"leisure\">\n");
					 out.write("\t\t\t\t<location id=\""+locationIDLeisure+"\" isPrimary=\"no\"/>\n");
					 out.write("\t\t\t</activity>\n");
					 out.write("\t\t</knowledge>\n\n");
					 
					 out.write("\t\t<plan>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(9*(networkSize-1)+i)+"\" facility=\""+(3*(networkSize-1)+i)+"\" x=\""+(i*distance-distance/2)+"\" y=\"9000.0\" start_time=\"00:00:00\" dur=\""+time1+"\" end_time=\""+time1+"\" />\n");
					 out.write("\t\t\t<leg num=\"0\" mode=\"car\" dep_time=\""+time1+"\" trav_time=\"00:00:00\" arr_time=\""+time1+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"work\" link=\""+(j)+"\" facility=\""+(2*(networkSize-1)+j)+"\" x=\""+(j*distance-distance/2)+"\" y=\"0.0\" start_time=\""+time1+"\" dur=\""+(time2-time1)+"\" end_time=\""+time2+"\" />\n");
					 out.write("\t\t\t<leg num=\"1\" mode=\"car\" dep_time=\""+time2+"\" trav_time=\"00:00:00\" arr_time=\""+time2+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"shopping\" link=\""+linkIDShopping+"\" facility=\""+locationIDShopping+"\" x=\""+shoppingX+"\" y=\""+shoppingY+"\" start_time=\""+time2+"\" dur=\""+(time3-time2)+"\" end_time=\""+time3+"\" />\n");
					 out.write("\t\t\t<leg num=\"2\" mode=\"car\" dep_time=\""+time3+"\" trav_time=\"00:00:00\" arr_time=\""+time3+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"leisure\" link=\""+linkIDLeisure+"\" facility=\""+locationIDLeisure+"\" x=\""+leisureX+"\" y=\""+leisureY+"\" start_time=\""+time3+"\" dur=\""+(time4-time3)+"\" end_time=\""+time4+"\" />\n");
					 out.write("\t\t\t<leg num=\"3\" mode=\"car\" dep_time=\""+time4+"\" trav_time=\"00:00:00\" arr_time=\""+time4+"\">\n");
					 out.write("\t\t\t</leg>\n");
					 out.write("\t\t\t<act type=\"home\" link=\""+(9*(networkSize-1)+i)+"\" facility=\""+(3*(networkSize-1)+i)+"\" x=\""+(i*distance-distance/2)+"\" y=\"9000.0\" start_time=\""+time4+"\" dur=\""+(86400-time4)+"\" end_time=\"24:00:00\" />\n");
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
	public static int [] getShoppingIDs (int locationIDShopping){
		 if (locationIDShopping==37) return (new int[]{5, 4500, 0});
		 if (locationIDShopping==38) return (new int[]{14, 4500, 2000});
		 if (locationIDShopping==39) return (new int[]{23, 4500, 4000});
		 if (locationIDShopping==40) return (new int[]{32, 4500, 6000});
		 else return (new int[]{41, 4500, 8000});
	}
	public static int [] getLeisureIDs (int locationIDShopping){
		 if (locationIDShopping==42) return (new int[]{50, 4500, 1000});
		 if (locationIDShopping==43) return (new int[]{59, 4500, 3000});
		 if (locationIDShopping==44) return (new int[]{68, 4500, 5000});
		 if (locationIDShopping==45) return (new int[]{77, 4500, 7000});
		 else return (new int[]{86, 4500, 9000});
	}
}
