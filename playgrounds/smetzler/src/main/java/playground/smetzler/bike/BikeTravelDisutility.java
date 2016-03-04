package playground.smetzler.bike;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.vehicles.Vehicle;

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


public class BikeTravelDisutility implements TravelDisutility {
	
	ObjectAttributes bikeAttributes;
	double marginalUtilityOfTime;
	double marginalUtilityOfDistance;
	double marginalUtilityOfComfort;
	
	BikeTravelDisutility(BikeConfigGroup bikeConfigGroup, PlanCalcScoreConfigGroup cnScoringGroup) {
		//get infos from ObjectAttributes
		bikeAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(bikeAttributes).parse(bikeConfigGroup.getNetworkAttFile());
		
		marginalUtilityOfDistance = Double.valueOf(cnScoringGroup.getModes().get("bike").getMarginalUtilityOfDistance());
		marginalUtilityOfTime = 	Double.valueOf(cnScoringGroup.getModes().get("bike").getMarginalUtilityOfTraveling());
		marginalUtilityOfComfort = 	Double.valueOf(bikeConfigGroup.getMarginalUtilityOfComfort()).doubleValue();
//		referenceBikeSpeed = Double.valueOf(bikeConfigGroup.getReferenceBikeSpeed()).doubleValue();

	}
// beispiel
//		this.marginalCostOfTime = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
//		this.marginalCostOfDistance = -cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney();

	
	
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		
		// Zeit
		double travelTime = link.getLength()/link.getFreespeed();
		
		// Distanz
		double distance = link.getLength();

		// Komfort
		double surfaceFactor;
		String surface= (String) bikeAttributes.getAttribute(link.getId().toString(), "surface");
		if (surface != null) {
			if      (surface.equals("paved")) 					{surfaceFactor= 100;} 
			else if (surface.equals("asphalt")) 				{surfaceFactor= 100;} 
			else if (surface.equals("cobblestone")) 			{surfaceFactor=  40;} 
			else if (surface.equals("cobblestone:flattened"))	{surfaceFactor=  50;} 
			else if (surface.equals("sett")) 					{surfaceFactor=  50;} 

			else if (surface.equals("concrete")) 				{surfaceFactor= 100;} 
			else if (surface.equals("concrete:lanes"))			{surfaceFactor=  95;}	
			else if (surface.equals("concrete:plates")) 		{surfaceFactor=  95;} 
			else if (surface.equals("paving_stones"))			{surfaceFactor=  80;} 
			else if (surface.equals("paving_stones:30")) 		{surfaceFactor=  80;} 

			else if (surface.equals("unpaved")) 				{surfaceFactor=  60;} 
			else if (surface.equals("compacted"))				{surfaceFactor=  70;} 	
			else if (surface.equals("dirt")) 					{surfaceFactor=  30;} 
			else if (surface.equals("earth")) 					{surfaceFactor=  30;} 
			else if (surface.equals("fine_gravel"))				{surfaceFactor=  95;} 

			else if (surface.equals("grass")) 					{surfaceFactor=  50;} 
			else if (surface.equals("grass_paver")) 			{surfaceFactor=  50;}	 
			else if (surface.equals("gravel")) 					{surfaceFactor=  60;} 
			else if (surface.equals("ground")) 					{surfaceFactor=  60;} 
			else if (surface.equals("ice"))						{surfaceFactor=  15;} 

			else if (surface.equals("metal")) 					{surfaceFactor=  50;} 	 
			else if (surface.equals("mud")) 					{surfaceFactor=  30;}
			else if (surface.equals("pebblestone")) 			{surfaceFactor=  30;} 
			else if (surface.equals("salt")) 					{surfaceFactor=  15;} 
			else if (surface.equals("sand"))					{surfaceFactor=  10;} 
			else if (surface.equals("wood")) 					{surfaceFactor=  30;} 
			
			else if (surface.equals("bricks")) 					{surfaceFactor=  60;}

			else {surfaceFactor= 50;
		//	log.info(surface + " surface not recognized");
			}
		}
		else {
			surfaceFactor= 50;
			//log.info("no surface info");
		}
	
	
		
	double disutility = this.marginalUtilityOfTime * travelTime + marginalUtilityOfDistance * distance + marginalUtilityOfComfort * 1/surfaceFactor;
	
	return disutility;
	//return link.getLength()/link.getFreespeed();
	
	//// beispiel aus RandomizingTimeDistanceTravelDisutility
//	double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
//	return this.marginalCostOfTime * travelTime + logNormalRnd * this.marginalCostOfDistance * link.getLength();
	}




	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}

}
