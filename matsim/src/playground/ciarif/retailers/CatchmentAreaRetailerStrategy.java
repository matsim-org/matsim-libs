package playground.ciarif.retailers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.collections.QuadTree;

public class CatchmentAreaRetailerStrategy implements RetailerStrategy {
	public static final String NAME = "catchmentAreaRetailerStrategy";
	public final static String CONFIG_GROUP = "Retailers";
	public final static String CONFIG_N_ALTERNATIVES = "alternatives";
	public final static String CONFIG_RAD_CATCHMENT = "radius_catchment";
	private Controler controler;
	private int alternatives;	
		
	public CatchmentAreaRetailerStrategy (Controler controler, Object[] links) {
		this.controler = controler;
		String logitAlternatives = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_N_ALTERNATIVES);
		int alternatives = Integer.parseInt(logitAlternatives);
		this.alternatives = alternatives;
	}

		public void moveFacilities(Map<Id, ActivityFacility> facilities) {

			for (ActivityFacility f : facilities.values()) {
				
				// example of the use of a bad code style, but works anyway....
				QuadTree<PersonImpl> personQuadTree = Utils.getPersonQuadTree();
				if (personQuadTree == null) { throw new RuntimeException("QuadTree not set!"); }
				Collection<PersonImpl> persons = personQuadTree.get(f.getCoord().getX(),f.getCoord().getY(),100);
				System.out.println(" Persons living around the facility " + f.getId() + " are: " + persons.toArray().length);
				
				
				double[] utils = new double[alternatives];
				Object[] links = controler.getNetwork().getLinks().values().toArray();
				
				controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
				double[] currentlink_volumes = controler.getLinkStats().getAvgLinkVolumes(f.getLink().getId());
				ArrayList<Link> newLinks = new ArrayList<Link>(); 
				newLinks.add(f.getLink());
				double currentlink_volume =0;
				for (int j=0; j<currentlink_volumes.length;j=j+1) {
					currentlink_volume = currentlink_volume + currentlink_volumes[j];
				}
				utils [0]= Math.log(currentlink_volume); //If the utility would be defined in a more complex way than it is now here a 
				// calc_utility method might be called at this point
				for (int i=1; i<alternatives;i++) {
					int rd = MatsimRandom.getRandom().nextInt(links.length);
					newLinks.add((Link)links[rd]);
					double[] newlink_volumes = controler.getLinkStats().getAvgLinkVolumes(newLinks.get(i).getId());
					
					double newlink_volume =0;
					
					for (int j=0; j<newlink_volumes.length;j=j+1) {
						newlink_volume = newlink_volume + newlink_volumes[j];
					}
					utils [i]= Math.log(newlink_volume); // see above calc_utility
				}
				double r = MatsimRandom.getRandom().nextDouble();
				double [] probs = calcLogitProbability(utils);
				for (int k=0;k<probs.length;k++) {
					if (r<=probs [k]) {
						f.moveTo(newLinks.get(k).getCoord());
					}
				}
			}
		}
		private final double[] calcLogitProbability(double[] utils) {
			double exp_sum = 0.0;
			for (int i=0; i<utils.length; i++) { exp_sum += Math.exp(utils[i]);}
			double [] probs = new double[utils.length];
			for (int i=0; i<utils.length; i++) { probs[i] = Math.exp(utils[i])/exp_sum;}
			return probs;
		}

		public void moveRetailersFacilities(
				Map<Id, FacilityRetailersImpl> facilities) {
			// TODO Auto-generated method stub
			
		}

}
