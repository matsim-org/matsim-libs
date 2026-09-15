/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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


package org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner;


import com.google.common.base.Verify;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;

import java.util.*;
/**
 * @author Steffen Axer
 */
public class RequestData {
    private final DrtRequest drtRequest;
	private InsertionRecord solution;

    public RequestData(DrtRequest drtRequest) {
        this.drtRequest = drtRequest;
	}

    public DrtRequest getDrtRequest() {
        return drtRequest;
    }

	public InsertionRecord getSolution() {
		return solution;
	}

	public void setSolution(InsertionRecord solution) {
		Verify.verify(this.solution==null);
		this.solution = solution;
	}

	public record InsertionRecord(Optional<InsertionWithDetourData> insertion) {}

}
