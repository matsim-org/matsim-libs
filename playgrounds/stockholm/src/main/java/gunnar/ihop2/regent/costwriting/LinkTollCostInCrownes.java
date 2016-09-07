package gunnar.ihop2.regent.costwriting;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.vehicles.Vehicle;

/**
 * This would be much nicer if I could directly access the roadpricing contrib
 * somehow.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinkTollCostInCrownes implements TravelDisutility {

	private final RoadPricingSchemeImpl roadPricingScheme;

	public LinkTollCostInCrownes(final String tollFileName) {
		this.roadPricingScheme = new RoadPricingSchemeImpl();
		final RoadPricingReaderXMLv1 reader = new RoadPricingReaderXMLv1(
				this.roadPricingScheme);
		reader.readFile(tollFileName);
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time,
			Person person, Vehicle vehicle) {
		final RoadPricingSchemeImpl.Cost cost = this.roadPricingScheme
				.getLinkCostInfo(link.getId(), time,
						(person != null) ? person.getId() : null,
						(vehicle != null) ? vehicle.getId() : null);
		if (cost == null) {
			return 0.0;
		} else {
			return cost.amount;
		}
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0.0;
	}

}
