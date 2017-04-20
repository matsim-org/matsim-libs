package playground.singapore.springcalibration.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

public class SubpopTravelDisutilityFactory implements TravelDisutilityFactory {
	
	private ScoringParametersForPerson params;
	private String mode = TransportMode.car;
	
	public SubpopTravelDisutilityFactory(ScoringParametersForPerson params, String mode) {
		this.params = params;
		this.mode = mode;
	}
		
	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		return new SubpopTravelDisutility(timeCalculator, this.params, this.mode);
	}
}