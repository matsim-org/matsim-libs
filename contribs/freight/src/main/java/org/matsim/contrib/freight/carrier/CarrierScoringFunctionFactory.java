package org.matsim.contrib.freight.carrier;

import org.matsim.core.scoring.ScoringFunction;

public interface CarrierScoringFunctionFactory {
	
	public ScoringFunction createScoringFunction(Carrier carrier);
	
}
