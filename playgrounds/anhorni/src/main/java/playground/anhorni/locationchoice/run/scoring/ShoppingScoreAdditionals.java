package playground.anhorni.locationchoice.run.scoring;

import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;

public class ShoppingScoreAdditionals {
	
	private final static double scoreGreater2500 = 18.0;
	private final static double score1000_2500 = 9.0;
	private final static double score400_1000 = 3.0;
	private QuadTree<ActivityFacilityImpl> shopQuadTree;
	private final ActivityFacilities facilities;
	
	public ShoppingScoreAdditionals(final ActivityFacilities facilities) {
		this.facilities = facilities;
	}
								
	public double getScoreElementForSize(Plan plan) {
		
		double additionalScore = 0.0;
		
		List<? extends PlanElement> actslegs = plan.getPlanElements();			
		for (int j = 0; j < actslegs.size(); j=j+2) {			
			final ActivityImpl act = (ActivityImpl)actslegs.get(j);
			
			if (act.getType().startsWith("shop")) {
			//if (act.getType().equals("shop_grocery")) {
				ActivityFacilityImpl facility = (ActivityFacilityImpl) this.facilities.getFacilities().get(act.getFacilityId());
				
				// Verbrauchermaerkte 	(> 2500 m2)
				if (facility.getActivityOptions().containsKey("B015211A")) {
					additionalScore += scoreGreater2500;
				}
				// Grosse Supermaerkte 	(1000-2499 m2)
				else if (facility.getActivityOptions().containsKey("B015211B")) {
					additionalScore += score1000_2500;
				}
				// Kleine Supermaerkte 	(400-999 m2)
				else if (facility.getActivityOptions().containsKey("B015211C")) {
					additionalScore += score400_1000;
				}
			}
		}
		return additionalScore;
	}
	
	public double getScoreElementForStoreDensity(Plan plan) {
		double additionalScore = 0.0;
		
		List<? extends PlanElement> actslegs = plan.getPlanElements();			
		for (int j = 0; j < actslegs.size(); j=j+2) {			
			final ActivityImpl act = (ActivityImpl)actslegs.get(j);
			
			if (act.getType().startsWith("shop")) {
				int numberOfCloseByShopLocations = 
					this.shopQuadTree.get(act.getCoord().getX(), act.getCoord().getY(), 750.0).size();
					additionalScore += Math.min(20.0, numberOfCloseByShopLocations);
			}
		}	
		return additionalScore;
	}
	
	public double getScoreElementForShoppingCenters(Plan plan) {
		double scoreElement = 0.0;
		return scoreElement;
	}

	public QuadTree<ActivityFacilityImpl> getShopQuadTree() {
		return shopQuadTree;
	}

	public void setShopQuadTree(QuadTree<ActivityFacilityImpl> shopQuadTree) {
		this.shopQuadTree = shopQuadTree;
	}
}
