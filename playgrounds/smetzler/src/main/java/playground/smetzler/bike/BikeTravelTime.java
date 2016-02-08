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

public class BikeTravelTime implements TravelTime {
	
	
	/**
	 * in this class traveltime is calculated depending on the following parameters:
	 * surface, smoothness, slope/elevation, #crossings (info in nodes), cyclewaytype, size of street aside, weather/wind, parkende autos?
	 * 
	 */

	private final static Logger log = Logger.getLogger(BikeTravelTime.class);
	ObjectAttributes bikeAttributes;
	final double referenceBikeSpeed;	
	
	@Inject
	@Singleton
	BikeTravelTime(BikeConfigGroup bikeConfigGroup) {
		//get infos from ObjectAttributes
		bikeAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(bikeAttributes).parse(bikeConfigGroup.getSurfaceInformationFile());
		//get speed
		referenceBikeSpeed = Double.valueOf(bikeConfigGroup.getReferenceBikeSpeed()).doubleValue();
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		double speedfactor;
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
			if (surface.equals("asphalt")) {
				speedfactor= 1;
			} else if (surface.equals("paved")) {
				speedfactor= 1;		
			} else if (surface.equals("paving_stones")) {
				speedfactor= 0.9;
			} else if (surface.equals("cobblestone")) {
				speedfactor= 0.6;
			} else if (surface.equals("gravel")) {
				speedfactor= 0.5;		
			} else if (surface.equals("unpaved")) {
				speedfactor= 0.5;
			} else if (surface.equals("sand")) {
				speedfactor= 0.1;
			} else {
				speedfactor= 0.8;
				log.info(surface + " surface not recognized");
			}
		}
		else {
			speedfactor= 0.9;
			//log.info("no surface info");
		}
		double reducedSpeed= referenceBikeSpeed*speedfactor;
		double biketravelTime= (link.getLength() / reducedSpeed);			
		return biketravelTime;	
	}

}
