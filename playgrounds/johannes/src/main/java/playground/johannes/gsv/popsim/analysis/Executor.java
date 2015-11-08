/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.gsv.popsim.analysis;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author jillenberger
 */
public class Executor {

    private static java.util.concurrent.ExecutorService service;

    private static void init() {
        if (service == null) {
            service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
    }

    public static Future<?> submit(Runnable task) {
        return service.submit(task);
    }
}
