package playground.dziemke.ikea;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;



public class ResidentialAreaTravelDisutilityCalculator implements TravelDisutility{
	
	private TravelTime timeCalculator;
	private double marginalUtlOfMoney;
	private double distanceCostRateCar;
	private double marginalUtlOfTravelTime;
	
	
	public ResidentialAreaTravelDisutilityCalculator(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		this.timeCalculator = timeCalculator;
		this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		this.distanceCostRateCar = cnScoringGroup.getMonetaryDistanceCostRateCar();
		this.marginalUtlOfTravelTime = (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
	
	}
	
	
	@Override
	public double getLinkTravelDisutility(Link link, double time,
			Person person, Vehicle vehicle) {
		double linkTravelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		double linkTravelTimeDisutility = this.marginalUtlOfTravelTime * linkTravelTime ;

		double distance = link.getLength();
		double distanceCost = - this.distanceCostRateCar * distance;
		double linkDistanceDisutility = this.marginalUtlOfMoney * distanceCost;
		//System.out.println("getLinkTravelDisutility called!!!! linkTravelTime: " +linkTravelTime+", linkTravelTimeDisutility: "+linkTravelTimeDisutility);

		double linkTravelDisutility = linkTravelTimeDisutility + linkDistanceDisutility;
						
		//if(person.getSelectedPlan().getPlanElements().toString().contains("dailyShopping")){
		if(link.toString().contains("id=82039")||link.toString().contains("id=82040")){
		linkTravelDisutility=linkTravelDisutility*10000;
		//}
		}
		
		
	//	double linkExpectedSlagboomDisutility = calculateExpectedTollDisutility(link.getId(), time, person.getId());
		
		
		return linkTravelDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}

}
