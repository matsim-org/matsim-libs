package playground.smetzler.bike;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.vehicles.Vehicle;

import playground.smetzler.bikeNetwork.BikeCustomizedOsmNetworkReader;

public class BikeTravelTime implements TravelTime {

	private final static Logger log = Logger.getLogger(BikeTravelTime.class);
	ObjectAttributes bikeAttributes;
	
	@Inject
	@Singleton
	BikeTravelTime(BikeConfigGroup bikeConfigGroup) {
		bikeAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(bikeAttributes).parse(bikeConfigGroup.getSurfaceInformationFile());
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		double factor= 0;
		final double referenceBikeSpeed = 6.01;		// 6.01 according to Prakin and Rotheram
		double biketravelTime = 0;
		
		//greife auf surface daten zu
		String surface= (String) bikeAttributes.getAttribute(link.getId().toString(), "surface");
		
		// surface eigenschaften : https://wiki.openstreetmap.org/wiki/DE:Key:surface
//		asphalt
//		paved: versiegelte Fläche
//		paving_stones: Plasterstein
//		cobblestone: Kopfsteinpflaster
//		gravel: Schotter	
//		unpaved: unversiegelte Fläche
//		sand
		if (surface != null) {
		if (surface.equals("asphalt"))
			factor= 1;
		else if (surface.equals("paved"))
			factor= 1;		
		else if (surface.equals("paving_stones"))
			factor= 0.9;
		else if (surface.equals("cobblestone"))
			factor= 0.6;
		else if (surface.equals("gravel"))
			factor= 0.5;		
		else if (surface.equals("unpaved"))
			factor= 0.5;
		else if (surface.equals("sand"))
			factor= 0.3;
		else {
			factor=0.8;
			log.info(surface + " not recognized");}
		
		biketravelTime= (link.getLength() / (referenceBikeSpeed*factor));
		}
		
		return biketravelTime;
		
	}

}
