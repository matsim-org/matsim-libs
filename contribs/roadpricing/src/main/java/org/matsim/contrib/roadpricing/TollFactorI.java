package org.matsim.contrib.roadpricing;

import org.matsim.api.core.v01.Id;

public interface TollFactorI {

	public double getTollFactor(Id personId, Id vehicleId, Id linkId, double time);

}