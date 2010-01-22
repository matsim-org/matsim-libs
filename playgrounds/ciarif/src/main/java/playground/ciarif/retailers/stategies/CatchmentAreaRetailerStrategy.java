package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.QuadTree;

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
	private Map<Id,ActivityFacilityImpl> movedFacilities = new TreeMap<Id,ActivityFacilityImpl>();
		
	public CatchmentAreaRetailerStrategy (Controler controler) {
		this.controler = controler;
		String logitAlternatives = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_N_ALTERNATIVES);
		int alternatives = Integer.parseInt(logitAlternatives);
		this.alternatives = alternatives;
	}

		public Map<Id, ActivityFacilityImpl> moveFacilities(Map<Id, ActivityFacilityImpl> facilities, ArrayList<LinkRetailersImpl> allowedLinks) {

			for (ActivityFacilityImpl f : facilities.values()) {
				
				log.info("allowed links are = " + allowedLinks );
				// example of the use of a bad code style, but works anyway....
				QuadTree<Person> personQuadTree = Utils.getPersonQuadTree();
				if (personQuadTree == null) { throw new RuntimeException("QuadTree not set!"); }
				Collection<? extends Person> persons = personQuadTree.get(f.getCoord().getX(),f.getCoord().getY(),200);
				log.info(" Persons living around the facility " + f.getId() + " are: " + persons.size());
//				Object[] links = controler.getNetwork().getLinks().values().toArray();
				controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
					double[] currentlink_volumes = controler.getLinkStats().getAvgLinkVolumes(f.getLinkId());
					ArrayList<Id> newLinks = new ArrayList<Id>(); 
					newLinks.add(f.getLinkId());
					double currentlink_volume =0;
					for (int j=0; j<currentlink_volumes.length;j=j+1) {
						currentlink_volume = currentlink_volume + currentlink_volumes[j];
					}
					 
					for (int i=1; i<alternatives;i++) {
						int rd = MatsimRandom.getRandom().nextInt(allowedLinks.size());
						newLinks.add(allowedLinks.get(rd).getId());
						double[] newlink_volumes = controler.getLinkStats().getAvgLinkVolumes(newLinks.get(i));
						
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

	

		public ArrayList<LinkRetailersImpl> findAvailableLinks() {
			// TODO Auto-generated method stub
			return null;
		}

		public Map<Id, ActivityFacility> moveFacilities(
				Map<Id, ActivityFacility> facilities,
				Map<Id, LinkRetailersImpl> links) {
			// TODO Auto-generated method stub
			return null;
		}

		public Map<Id, ActivityFacilityImpl> moveFacilities(
				Map<Id, ActivityFacilityImpl> facilities,
				TreeMap<Id, LinkRetailersImpl> links) {
			// TODO Auto-generated method stub
			return null;
		}	
}
