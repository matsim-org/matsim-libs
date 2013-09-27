/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.demand;

import java.io.*;
import java.util.Set;

import org.matsim.api.core.v01.*;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import pl.poznan.put.util.random.*;
import playground.michalm.vrp.run.VrpConfigUtils;


public class DepotGenerator
{
    public static void generateVehicles(String depotsFilename, int count,
            int t1)
        throws IOException
    {
    	
    	String[] depotlinks =  {"385","499","277","52","449"};
    	

    	
        PrintWriter pw = new PrintWriter(new File(depotsFilename));
        pw.println("<?xml version=\"1.0\" ?>");
        pw.println("<!DOCTYPE depots SYSTEM \"http://www.man.poznan.pl/~michalm/matsim/depots_v1.dtd\">");
        pw.println();

        pw.println("<depots>");

        for (int i = 0; i < 5; i++) {
            pw.println("\t<depot linkId=\""+depotlinks[i]+"\">");
            pw.println();
            pw.println("\t\t<vehicles>");
        	for (int ii = 1; ii<=400;ii++ ){

            pw.println("\t\t\t<vehicle name=\""+depotlinks[i]+"."+ii +"\" t0=\"0\" t1=\""+t1+"\"/>");
        	}
            pw.println("\t\t</vehicles>");
            pw.println("\t</depot>");
        }

        pw.println("</depots>");
        pw.close();
    }


    public static void main(String[] args)
        throws IOException
    {
        int count = 1000;
        int t1 = 24 * 60 * 60;

        String dir = "C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\";
        String depotsFilenam = dir + "depots-taxis-" + count + ".xml";

        generateVehicles(depotsFilenam, count, t1);
    }
}
