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

package playground.michalm.demand.mielec.taxi;

import java.io.*;
import java.util.Scanner;

import pl.poznan.put.util.random.*;


public class TaxiDemandReducer
{
    public static void main(String[] args)
        throws IOException
    {
        UniformRandom uniform = RandomUtils.getGlobalUniform();

        Scanner sc = new Scanner(new File(
                "d:\\michalm\\2013_07\\mielec-2-peaks-new-03-100\\taxiCustomers_03_pc.txt"));
        BufferedWriter bw = new BufferedWriter(new FileWriter(
                "d:\\michalm\\2013_07\\mielec-2-peaks-new-03-100\\taxiCustomers_01_pc.txt"));

        while (sc.hasNext()) {
            String id = sc.next();

            if (uniform.nextDouble(0, 1) < 1. / 3) {
                bw.write(id);
                bw.newLine();
            }
        }

        sc.close();
        bw.close();
    }
}
