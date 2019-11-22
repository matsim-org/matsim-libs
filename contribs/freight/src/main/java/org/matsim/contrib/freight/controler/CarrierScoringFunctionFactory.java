package org.matsim.contrib.freight.controler;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.core.scoring.ScoringFunction;

public interface CarrierScoringFunctionFactory {
	
	public ScoringFunction createScoringFunction(Carrier carrier);
	
}
