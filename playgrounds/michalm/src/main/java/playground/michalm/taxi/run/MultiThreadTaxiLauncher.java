/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.run;

import java.util.concurrent.*;


public class MultiThreadTaxiLauncher
{
    //-1 20 d:\michalm\2014_02\mielec-2-peaks-new-10-25\params.in
    private static final String PATH = "d:\\michalm\\2014_02\\mielec-2-peaks-new-";
    private static final String FILE = "\\params.in";

    private static final int[] DEMANDS = { 10, 15, 20, 25, 30, 35, 40 };
    private static final int[] SUPPLIES = { 25, 50 };

    private static final int RUNS = 20;
    
    private static final int THREADS = 12;


    public static void main(String[] args)
    {
        ExecutorService service = Executors.newFixedThreadPool(THREADS);

        for (int d : DEMANDS) {
            for (int s : SUPPLIES) {
                final String paramFile = PATH + d + "-" + s + FILE;

                service.execute(new Runnable() {
                    public void run()
                    {
                        MultipleTaxiLauncher.runNOS(-1, RUNS, paramFile);
                    }
                });

                service.execute(new Runnable() {
                    public void run()
                    {
                        MultipleTaxiLauncher.run(-1, RUNS, paramFile, false, false, false);
                    }
                });

                service.execute(new Runnable() {
                    public void run()
                    {
                        MultipleTaxiLauncher.run(-1, RUNS, paramFile, false, true, false);
                    }
                });

                service.execute(new Runnable() {
                    public void run()
                    {
                        MultipleTaxiLauncher.run(-1, RUNS, paramFile, false, false, true);
                    }
                });

                service.execute(new Runnable() {
                    public void run()
                    {
                        MultipleTaxiLauncher.run(-1, RUNS, paramFile, false, true, true);
                    }
                });
            }
        }

        service.shutdown();
    }
}
