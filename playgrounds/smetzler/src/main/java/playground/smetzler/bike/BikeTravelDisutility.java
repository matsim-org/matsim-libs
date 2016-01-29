package playground.smetzler.bike;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.vehicles.Vehicle;

public class BikeTravelDisutility implements TravelDisutility {
	
	ObjectAttributes bikeAttributes;
	
//	@Inject
//	@Singleton
//	BikeTravelDisutility(BikeConfigGroup bikeConfigGroup) {
//		bikeAttributes = new ObjectAttributes();
//		new ObjectAttributesXmlReader(bikeAttributes).parse(bikeConfigGroup.getSurfaceInformationFile());
//	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		//greife auf surface daten zu
	//	bikeAttributes.getAttribute(link.getId().toString(), );
		return 5;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}

}
