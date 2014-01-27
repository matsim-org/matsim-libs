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


public class VehicleGenerator
{
    public static void generateVehicles(String vehiclesFilename, int count, int t1)
        throws IOException
    {
        String[] startLinks = { "385", "499", "277", "52", "449" };

        PrintWriter pw = new PrintWriter(new File(vehiclesFilename));
        pw.println("<?xml version=\"1.0\" ?>");
        pw.println("<!DOCTYPE vehicles SYSTEM \"http://matsim.org/files/dtd/vehicles_v1.dtd\">");
        pw.println();

        pw.println("<vehicles>");

        for (int i = 0; i < 5; i++) {
            String startLinkId = startLinks[i];

            for (int ii = 1; ii <= 400; ii++) {
                pw.println("\t<vehicle id=\"" + startLinkId + "." + ii + "\" start_link=\""
                        + startLinkId + "\" t_0=\"0\" t_1=\"" + t1 + "\"/>");
            }
        }

        pw.println("</vehicles>");
        pw.close();
    }


    public static void main(String[] args)
        throws IOException
    {
        int count = 1000;
        int t1 = 24 * 60 * 60;

        String dir = "C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\";
        String vehiclesFilename = dir + "vehicles-" + count + ".xml";

        generateVehicles(vehiclesFilename, count, t1);
    }
}
