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

package pl.poznan.put.vrp.dynamic.data.model;

import java.util.EnumSet;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.*;


/**
 * @author michalm
 */
public interface Request
{
    public enum ReqStatus
    {
        INACTIVE("I"), // invisible to the dispatcher (ARTIFICIAL STATE!)
        UNPLANNED("U"), // submitted by the CUSTOMER and received by the DISPATCHER
        PLANNED("P"), // planned - included into one of the routes
        STARTED("S"), // vehicle starts serving
        PERFORMED("PE"), //
        REJECTED("R"), // rejected by the DISPATCHER
        CANCELLED("C");// canceled by the CUSTOMER

        public final String shortName;

        // this means that Request is scheduled (by means of ServeTask)
        public static final EnumSet<ReqStatus> SCHEDULED = EnumSet.of(PLANNED, STARTED, PERFORMED);
        public static final EnumSet<ReqStatus> REGULAR = EnumSet.of(UNPLANNED, PLANNED, STARTED,
                PERFORMED);


        private ReqStatus(String shortName)
        {
            this.shortName = shortName;
        }
    };


    int getId();


    ReqStatus getStatus();// based on: serveTask.getStatus();


    Customer getCustomer();


    Vertex getFromVertex();


    Vertex getToVertex();


    int getQuantity();


    int getT0();// earliest start time


    int getT1();// latest start time


    int getSubmissionTime();


    ServeTask getServeTask();


    Schedule getSchedule();


    void notifyScheduled(ServeTask serveTask);


    void notifyUnscheduled();


    /////////////////////////
    //TODO the following 3 methods need some refactoring in the future
    void submit();


    void reject();


    void cancel();
    /////////////////////////
}
