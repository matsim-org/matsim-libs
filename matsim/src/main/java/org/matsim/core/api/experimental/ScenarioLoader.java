package org.matsim.core.api.experimental;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimSomeReader;

public interface ScenarioLoader extends MatsimSomeReader {
	// yy one can debate if this is really a "reader", but since the plan is to eventually be able to read
	// the toplevel containers independently, this belongs into the same design "box". kai, apr'10

	public Scenario loadScenario();
	
	public Scenario getScenario() ;

}