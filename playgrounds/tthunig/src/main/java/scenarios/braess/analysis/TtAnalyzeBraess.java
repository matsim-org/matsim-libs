/**
 * 
 */
package scenarios.braess.analysis;

import org.matsim.api.core.v01.events.LinkEnterEvent;

import scenarios.analysis.TtAbstractAnalysisTool;

/**
 * This class extends the abstract analysis tool for the specific scenario of
 * Braess' example.
 * 
 * @see scenarios.analysis.TtAbstractAnalysisTool
 * 
 * @author tthunig
 * 
 */
public class TtAnalyzeBraess extends TtAbstractAnalysisTool {
	
	@Override
	protected int determineRoute(LinkEnterEvent linkEnterEvent) {
		// in the braess scenario the route is unique if one gets a link enter
		// event of link 2_4, 3_4 or 3_5.
		int route = -1;
		switch (linkEnterEvent.getLinkId().toString()) {
		case "2_4":
		case "2_24":
			// the person uses the lower route
			route = 2;
			break;
		case "3_4": // the person uses the middle route
			route = 1;
			break;
		case "3_5": // the person uses the upper route
			route = 0;
			break;
		default:
			break;
		}
		return route;
	}

	@Override
	protected void defineNumberOfRoutes() {
		setNumberOfRoutes(3);
	}

}
