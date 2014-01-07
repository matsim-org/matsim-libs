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

package org.matsim.contrib.dvrp.data;

import java.util.List;

import org.matsim.contrib.dvrp.data.model.*;
import org.matsim.contrib.dvrp.data.network.VrpPathCalculator;


public interface VrpData

{
    List<Depot> getDepots();


    List<Customer> getCustomers();


    List<Vehicle> getVehicles();


    List<Request> getRequests();


    int getTime();


    VrpPathCalculator getPathCalculator();


    VrpDataParameters getParameters();


    void addDepot(Depot depot);


    void addCustomer(Customer customer);


    void addVehicle(Vehicle vehicle);


    void addRequest(Request request);


    void setTime(int time);


    void setPathCalculator(VrpPathCalculator calculator);


    void setParameters(VrpDataParameters parameters);
}