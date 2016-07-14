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
	 * surface, slope/elevation
	 * 
	 * following parameters are supposed to be implemented
	 * cyclewaytype, smoothness? (vs surface), weather/wind?, #crossings (info in nodes)
	 * 
	 * 
	 * following parameters are supposed to be implemented to the disutility
	 * traveltime, distance, surface, smoothness, slope/elevation, #crossings (info in nodes), cyclewaytype, 
	 * size of street aside, weather/wind, parkende autos?
	 * 
	 */

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {

		double travelTime = link.getLength()/link.getFreespeed();
		return travelTime;	
	}

}






///////////////////////////////////////////////// ALTER CODE ////////////////////////////////////////////////
//private final static Logger log = Logger.getLogger(BikeTravelTime.class);
//@Inject
//@Singleton
//BikeTravelTime(BikeConfigGroup bikeConfigGroup) {
//	//get infos from ObjectAttributes
//	bikeAttributes = new ObjectAttributes();
//	new ObjectAttributesXmlReader(bikeAttributes).parse(bikeConfigGroup.getNetworkAttFile());
//	//get speed
//	referenceBikeSpeed = Double.valueOf(bikeConfigGroup.getReferenceBikeSpeed()).doubleValue();
//}
//
//@Override
//public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
//
////
//////	///////////// SURFACE ///////////////////////////////////
////	double surfaceSpeedFactor;
////	String surface= (String) bikeAttributes.getAttribute(link.getId().toString(), "surface");
////	// surface eigenschaften : https://wiki.openstreetmap.org/wiki/DE:Key:surface
////	//		asphalt
////	//		paved: versiegelte Fläche
////	//		paving_stones: Plasterstein
////	//		cobblestone: Kopfsteinpflaster
////	//		gravel: Schotter	
////	//		unpaved: unversiegelte Fläche
////	//		sand
////	if (surface != null) {
////		if (surface.equals("asphalt")) {
////			surfaceSpeedFactor= 1;
////		} else if (surface.equals("paved")) {
////			surfaceSpeedFactor= 1;		
////		} else if (surface.equals("paving_stones")) {
////			surfaceSpeedFactor= 0.9;
////		} else if (surface.equals("cobblestone")) {
////			surfaceSpeedFactor= 0.6;
////		} else if (surface.equals("gravel")) {
////			surfaceSpeedFactor= 0.5;		
////		} else if (surface.equals("unpaved")) {
////			surfaceSpeedFactor= 0.5;
////		} else if (surface.equals("sand")) {
////			surfaceSpeedFactor= 0.1;
////		} else {
////			surfaceSpeedFactor= 0.8;
////			log.info(surface + " surface not recognized");
////		}
////	}
////	else {
////		surfaceSpeedFactor= 0.9;
////		//log.info("no surface info");
////	}
////
////
//////	///////////// SLOPE  ///////////////////////////////////
//////	String slopeStr= (String) bikeAttributes.getAttribute(link.getId().toString(), "slope");
//////	double slope = 0;
//////	if (slopeStr != null) {
//////		slope = Double.parseDouble(slopeStr);}
////	
////	Double slope= 0.;
////	if (bikeAttributes.getAttribute(link.getId().toString(), "slope") != null) {
////	slope= (Double) bikeAttributes.getAttribute(link.getId().toString(), "slope");
////	}
////	
////	
////	//von multimodal
////	//		private final double downhillFactor = 0.2379;	// 0%..-15%
////	//		private final double uphillFactor = -0.4002;	// 0%..12%
////
////	double slopeSpeedFactor = 0; 
////
////	if (slope > 0.10) {								//// bergauf
////		slopeSpeedFactor= 0.1;
////	} else if (slope <= 0.10 && slope > 0.05) {		
////		slopeSpeedFactor= 0.3;		
////	} else if (slope <= 0.05 && slope > 0.03) {
////		slopeSpeedFactor= 0.5;	
////	} else if (slope <= 0.03 && slope > 0.01) {
////		slopeSpeedFactor= 0.8;
////	} else if (slope <= 0.01 && slope > -0.01) { 	//// eben
////		slopeSpeedFactor= 1;
////	} else if (slope <= -0.01 && slope > -0.03) {	//// bergab
////		slopeSpeedFactor= 1.2;
////	} else if (slope <= -0.03 && slope > -0.05) {	
////		slopeSpeedFactor= 1.5;
////	} else if (slope <= -0.05 && slope > -0.10) {	
////		slopeSpeedFactor= 1.7;
////	} else if (slope <= -0.10) {	
////		slopeSpeedFactor= 2;
////	}
////		
////	/// only surface
//////	double reducedSpeed= referenceBikeSpeed*surfaceSpeedFactor ;
////	
////	/// only slope
//////	double reducedSpeed= referenceBikeSpeed*slopeSpeedFactor ;
////
////	double reducedSpeed= referenceBikeSpeed*surfaceSpeedFactor*slopeSpeedFactor ;
////	double biketravelTime= (link.getLength() / reducedSpeed);			
//
//	return 2;	
////diese zeit wird dem router gegeben -> plans!
//	
//}

