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

/**
 * @author michalm
 */
public interface Request
{
    public enum RequestStatus
    {
        INACTIVE("I"), // invisible to the dispatcher (ARTIFICIAL STATE!)
        UNPLANNED("U"), // submitted by the CUSTOMER and received by the DISPATCHER
        PLANNED("P"), // planned - included into one of the routes
        STARTED("S"), // vehicle starts serving
        PERFORMED("PE"), //
        REJECTED("R"), // rejected by the DISPATCHER
        CANCELLED("C");// canceled by the CUSTOMER

        public final String shortName;


        private RequestStatus(String shortName)
        {
            this.shortName = shortName;
        }
    };


    int getId();


    RequestStatus getStatus();// based on: serveTask.getStatus();


    Customer getCustomer();


    int getQuantity();


    int getT0();// earliest start time


    int getT1();// latest start time


    int getSubmissionTime();


    void setStatus(RequestStatus status);
}
