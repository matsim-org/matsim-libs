package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.collections.QuadTree;
import playground.ciarif.retailers.data.FacilityRetailersImpl;
import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.retailers.utils.Utils;

public class CatchmentAreaRetailerStrategy implements RetailerStrategy {
	
	private final static Logger log = Logger.getLogger(CatchmentAreaRetailerStrategy.class);
	
	public static final String NAME = "catchmentAreaRetailerStrategy";
	public final static String CONFIG_GROUP = "Retailers";
	public final static String CONFIG_N_ALTERNATIVES = "alternatives";
	public final static String CONFIG_RAD_CATCHMENT = "radius_catchment";
	private Controler controler;
	private int alternatives;
	private Map<Id,ActivityFacility> movedFacilities = new TreeMap<Id,ActivityFacility>();
		
	public CatchmentAreaRetailerStrategy (Controler controler) {
		this.controler = controler;
		String logitAlternatives = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_N_ALTERNATIVES);
		int alternatives = Integer.parseInt(logitAlternatives);
		this.alternatives = alternatives;
	}

		public Map<Id, ActivityFacility> moveFacilities(Map<Id, ActivityFacility> facilities, ArrayList<LinkRetailersImpl> allowedLinks) {

			for (ActivityFacility f : facilities.values()) {
				
				log.info("allowed links are = " + allowedLinks );
				// example of the use of a bad code style, but works anyway....
				QuadTree<PersonImpl> personQuadTree = Utils.getPersonQuadTree();
				if (personQuadTree == null) { throw new RuntimeException("QuadTree not set!"); }
				Collection<PersonImpl> persons = personQuadTree.get(f.getCoord().getX(),f.getCoord().getY(),200);
				log.info(" Persons living around the facility " + f.getId() + " are: " + persons.toArray().length);
				Object[] links = controler.getNetwork().getLinks().values().toArray();
				controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
					double[] currentlink_volumes = controler.getLinkStats().getAvgLinkVolumes(f.getLink().getId());
					ArrayList<Link> newLinks = new ArrayList<Link>(); 
					newLinks.add(f.getLink());
					double currentlink_volume =0;
					for (int j=0; j<currentlink_volumes.length;j=j+1) {
						currentlink_volume = currentlink_volume + currentlink_volumes[j];
					}
					 
					for (int i=1; i<alternatives;i++) {
						int rd = MatsimRandom.getRandom().nextInt(allowedLinks.size());
						newLinks.add((LinkRetailersImpl)allowedLinks.get(rd));
						double[] newlink_volumes = controler.getLinkStats().getAvgLinkVolumes(newLinks.get(i).getId());
						
						double newlink_volume =0;
						
						for (int j=0; j<newlink_volumes.length;j=j+1) {
							newlink_volume = newlink_volume + newlink_volumes[j];
						}
					}

				
						//f.moveTo(newLinks.get(k).getCoord());
						this.movedFacilities.put(f.getId(),f);
						
					}
			
			return this.movedFacilities;
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

		public ArrayList<LinkRetailersImpl> findAvailableLinks() {
			// TODO Auto-generated method stub
			return null;
		}	
}
