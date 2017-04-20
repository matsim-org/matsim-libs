package playground.smetzler.bike;



import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.vehicles.Vehicle;

/**
 * in this class disutility per link is calculateted for routing depending on the following parameters:
 * traveltime, distance, surface, slope/elevation, cyclewaytype, highwaytype (streets with cycleways are prefered)
 * 
 * following parameters may be added in the future
 * smoothness? (vs surface), weather/wind?, #crossings (info in nodes), on-street-parking cars?, prefere routes that are offical bike routes
 */


public class BikeTravelDisutility implements TravelDisutility {

	int linkCount=0;
	double individualDis;

	private final static Logger log = Logger.getLogger(BikeTravelTime.class);

	ObjectAttributes bikeAttributes;
	double marginalUtilityOfTime;
	double marginalUtilityOfDistance;
	//	double marginalUtilityOfComfort;
	double marginalUtilityOfStreettype;
	double marginalUtilityOfSurfacetype;



	BikeTravelDisutility(BikeConfigGroup bikeConfigGroup, PlanCalcScoreConfigGroup cnScoringGroup) {
		//get infos from ObjectAttributes
		bikeAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(bikeAttributes).readFile(bikeConfigGroup.getNetworkAttFile());

		marginalUtilityOfDistance = Double.valueOf(cnScoringGroup.getModes().get("bike").getMarginalUtilityOfDistance());
		marginalUtilityOfTime = 	Double.valueOf(cnScoringGroup.getModes().get("bike").getMarginalUtilityOfTraveling());

		marginalUtilityOfStreettype = 	Double.valueOf(bikeConfigGroup.getMarginalUtilityOfStreettype()).doubleValue();
		marginalUtilityOfSurfacetype = 	Double.valueOf(bikeConfigGroup.getMarginalUtilityOfSurfacetype()).doubleValue();

		//deprectated
		//referenceBikeSpeed = Double.valueOf(bikeConfigGroup.getReferenceBikeSpeed()).doubleValue();
		//marginalUtilityOfComfort = 	Double.valueOf(bikeConfigGroup.getMarginalUtilityOfComfort()).doubleValue();
	}
	// example
	//		this.marginalCostOfTime = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
	//		this.marginalCostOfDistance = -cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney();



	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {

		String surface= (String) bikeAttributes.getAttribute(link.getId().toString(), "surface");
		String highway= (String) bikeAttributes.getAttribute(link.getId().toString(), "highway");
		String cyclewaytype= (String) bikeAttributes.getAttribute(link.getId().toString(), "cyclewaytype");


		// time
		double travelTime = link.getLength()/link.getFreespeed();

		// distance
		double distance = link.getLength();

		//		// Junction: signal or crossing TODO?
		//		String junction = (String) bikeAttributes.getAttribute(link.getId().toString(), "junctionTag");
		//		if (junction != junction) {
		//			if (junction.equals("signal"))
		//			{			}
		//			if (junction.equals("crossing"))
		//			{			}	
		//		}

		// comfort
		// SURFACE
		double surfaceFactor = 100;
		if (surface != null) {
			switch (surface){
			case "paved": 					surfaceFactor= 100; break;
			case "asphalt": 				surfaceFactor= 100; break;
			case "cobblestone":				surfaceFactor=  40; break;
			case "cobblestone (bad)":		surfaceFactor=  30; break;
			case "sett":					surfaceFactor=  50; break;
			case "cobblestone;flattened":
			case "cobblestone:flattened": 	surfaceFactor=  50; break;

			case "concrete": 				surfaceFactor= 100; break;
			case "concrete:lanes": 			surfaceFactor=  95; break;
			case "concrete_plates":
			case "concrete:plates": 		surfaceFactor=  90; break;
			case "paving_stones": 			surfaceFactor=  80; break;
			case "paving_stones:35": 
			case "paving_stones:30": 		surfaceFactor=  80; break;

			case "unpaved": 				surfaceFactor=  60; break;
			case "compacted": 				surfaceFactor=  70; break;
			case "dirt": 					surfaceFactor=  30; break;
			case "earth": 					surfaceFactor=  30; break;
			case "fine_gravel": 			surfaceFactor=  90; break;

			case "gravel": 					surfaceFactor=  60; break;
			case "ground": 					surfaceFactor=  60; break;
			case "wood": 					surfaceFactor=  30; break;
			case "pebblestone": 			surfaceFactor=  30; break;
			case "sand": 					surfaceFactor=  30; break;

			case "bricks": 					surfaceFactor=  60; break;
			case "stone": 					surfaceFactor=  40; break;
			case "grass": 					surfaceFactor=  40; break;

			case "compressed": 				surfaceFactor=  40; break; //guter sandbelag
			case "asphalt;paving_stones:35":surfaceFactor=  60; break;
			case "paving_stones:3": 		surfaceFactor=  40; break;

			//	default: 						surfaceFactor=  70; //log.info(surface + " surface not recognized");
			default: 						surfaceFactor=  85; //log.info(surface + " surface not recognized");
			}
		}
		else {

			//for many prim/sec streets there is no surface because the deafealt is asphalt; for tert street this is not true, f.e. friesenstr in kreuzberg
			if (highway != null) {
				if (highway.equals("primary") || highway.equals("primary_link") ||highway.equals("secondary") || highway.equals("secondary_link")) 
					surfaceFactor= 100;
				else
				{surfaceFactor= 85;
				//log.info("no surface info");
				}
			}
		}


		// STREETTYPE
		//how safe and comfortable does one feel on this kind of street?
		//highway: big streets without cycleways bad, residential and footway ok
		//cyclewaytype lane and track good & highway cycleway good

		double streetFactor = 100;
		if (highway != null) {
			/////große Straßen
			if      (highway.equals("trunk")) {//lane or track or shared buslane or opposite
				if (cyclewaytype != null) 
					if  (cyclewaytype.equals("no") || cyclewaytype.equals("none")) {} //no cycleway
					else {streetFactor= 95;} //has some kind of cycleway
				else {streetFactor=   5;}}   //no cycleway tagged

			else if (highway.equals("primary") || highway.equals("primary_link")) {
				if (cyclewaytype != null) 
					if  (cyclewaytype.equals("no") || cyclewaytype.equals("none")) {} //no cycleway
					else {streetFactor= 95;} //has some kind of cycleway
				else {streetFactor=   10;}}  //no cycleway tagged

			else if (highway.equals("secondary") || highway.equals("secondary_link")) {
				if (cyclewaytype != null) 
					if  (cyclewaytype.equals("no") || cyclewaytype.equals("none")) {} //no cycleway
					else {streetFactor= 95;} //has some kind of cycleway
				else {streetFactor=   30;}}  //no cycleway tagged

			else if (highway.equals("tertiary") || highway.equals("tertiary_link")) {
				if (cyclewaytype != null) 
					if  (cyclewaytype.equals("no") || cyclewaytype.equals("none")) {} //no cycleway
					else {streetFactor= 95;} //has some kind of cycleway
				else {streetFactor=   40;}}  //no cycleway tagged

			else if (highway.equals("unclassified")) {
				if (cyclewaytype != null) 
					if  (cyclewaytype.equals("no") || cyclewaytype.equals("none")) {} //no cycleway 
					else {streetFactor= 95;} //has some kind of cycleway
				else {streetFactor=   90;}}  //no cycleway tagged

			else if (highway.equals("residential")) {
				if (cyclewaytype != null) 				
					if  (cyclewaytype.equals("no") || cyclewaytype.equals("none")) {} //no cycleway 
					else {streetFactor= 95;} //has some kind of cycleway
				else {streetFactor=   95;}}  //no cycleway tagged

			////// Wege
			else if (highway.equals("service")|| highway.equals("living_street") || highway.equals("minor")) {
				streetFactor=   95;}
			else if (highway.equals("cycleway")|| highway.equals("path")) {
				streetFactor=   100;}
			else if (highway.equals("footway") || highway.equals("track") || highway.equals("pedestrian")) {
				streetFactor=   95;}
			else if (highway.equals("steps")) {
				streetFactor=   10;}

			else {streetFactor= 85;
			//log.info(highway + " highway not recognized");
			}
		}
		else {
			streetFactor= 85;
			//log.info("no highway info");
		}

		// SLOPE
		//add disutility for slope here, makes sense for hilly cities, but left aside for berlin



		// adding a randomfactor to disutility calculation
		Random r = new Random();
		double standardDeviation = 0.2;
		int mean = 1;
		double randomfactor = r.nextGaussian() * standardDeviation + mean;
		// yyyyyy in the randomized toll disutility this is LOGnormal.  Should be made consistent, or an argument provided why in the different cases different
		// mathematical forms make sense.  kai, feb'17

		double travelTimeDisutility        = -(marginalUtilityOfTime/3600 * travelTime);
		double distanceDisutility	       = -(marginalUtilityOfDistance  *   distance);

		double surfaceDisutility_util_m    = -(marginalUtilityOfSurfacetype * (100-surfaceFactor)/100);   //     (Math.pow((1/surfaceFactor), 2) + Math.pow((1/streetFactor), 2)); //TODO vielleicht quadratisch?
		double streettypeDisutility_util_m = -(marginalUtilityOfStreettype  * (100- streetFactor)/100);        //     (Math.pow((1/surfaceFactor), 2) + Math.pow((1/streetFactor), 2)); //TODO vielleicht quadratisch?
		double surfaceDisutility 	    = surfaceDisutility_util_m    * distance;
		double streettypeDisutility 	= streettypeDisutility_util_m * distance;


		double disutility = (travelTimeDisutility + distanceDisutility + streettypeDisutility + surfaceDisutility) * randomfactor;


		//String person123 = person.getId().toString();
		//		if (person.getId().toString().equals("bike_home1_work1_0")) {
		//			individualDis = individualDis+ disutility;
		//			linkCount++;
		//			log.info(linkCount + "   "+disutility + "  " + individualDis);
		//			log.info("current link: " + "length: " + distance + "traveltime:  " + travelTime);
		//			log.info("further attributes: " + highway + "   " + cyclewaytype + "   " + surface);
		//			log.info(" ");
		//
		//		}
		//				
		//// example from RandomizingTimeDistanceTravelDisutility
		//	double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		//	return this.marginalCostOfTime * travelTime + logNormalRnd * this.marginalCostOfDistance * link.getLength();


		//		//for calibration:	
		//			System.out.println("Link "+ link.getId());
		//			System.out.println("marginalUtilityOfTime     " + marginalUtilityOfTime);
		//			System.out.println("marginalUtilityOfDistance " + marginalUtilityOfDistance);
		//			System.out.println("marginalUtilityOfComfort  " + marginalUtilityOfComfort);
		//			
		//			System.out.println("travelTime    " + travelTime);
		//			System.out.println("distance      " + distance);
		//			System.out.println("surface " + surface);
		//			System.out.println("surfaceFactor " + surfaceFactor);
		//			System.out.println("cyclewaytype " + cyclewaytype);
		//			System.out.println("highway " + highway);
		//			System.out.println("streetFactor  " + streetFactor);
		//		
		//			System.out.println("travelTimeDisutility      " + travelTimeDisutility);
		//			System.out.println("distanceDisutility        " + distanceDisutility);
		////			System.out.println("comfortDisutility_util_m  " + comfortDisutility_util_m);
		////			System.out.println("comfortDisutility         " + comfortDisutility);
		//			
		//			System.out.println("surfaceDisutility_util_m     " + surfaceDisutility_util_m);
		//			System.out.println("surfaceDisutility            " + surfaceDisutility);
		//			System.out.println("streettypeDisutility_util_m  " + streettypeDisutility_util_m);
		//			System.out.println("streettypeDisutility         " + streettypeDisutility);
		//			double sum = streettypeDisutility+surfaceDisutility;
		//			System.out.println("sum         " + sum);
		//
		//			
		//			System.out.println("disutility           " + disutility);
		//			System.out.println(" ");

		return disutility;

	}


	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}

}
