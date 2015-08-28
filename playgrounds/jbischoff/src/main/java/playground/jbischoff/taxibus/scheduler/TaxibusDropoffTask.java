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

package playground.jbischoff.taxibus.scheduler;

import java.util.TreeSet;

import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

import playground.jbischoff.taxibus.passenger.TaxibusRequest;


public class TaxibusDropoffTask
    extends StayTaskImpl
    implements TaxibusTaskWithRequests
{
    private final TaxibusRequest request;


    public TaxibusDropoffTask(double beginTime, double endTime, TaxibusRequest request)
    {
        super(beginTime, endTime, request.getFromLink());

        this.request = request;
        request.setDropoffTask(this);
    }

    @Override
    public TaxibusTaskType getTaxibusTaskType()
    {
        return TaxibusTaskType.DROPOFF;
    }


    public TaxibusRequest getRequest()
    {
        return request;
    }


    @Override
    protected String commonToString()
    {
        return "[" + getTaxibusTaskType().name() + "]" + super.commonToString();
    }


	@Override
	public TreeSet<TaxibusRequest> getRequests() {
		TreeSet<TaxibusRequest> t = new TreeSet<>();
		t.add(request);
		return t;
	}


	@Override
	public void removeFromRequest(TaxibusRequest request) {
		if (request!=this.request) {
			throw new IllegalStateException();
		}
		request.setDropoffTask(null);
		
	}
	@Override
	public void removeFromAllRequests() {
		removeFromRequest(this.request);
	}
}
