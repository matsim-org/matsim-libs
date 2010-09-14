package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.ciarif.retailers.data.FacilityRetailersImpl;
import playground.ciarif.retailers.data.LinkRetailersImpl;


public class LogitMaxLinkRetailerStrategy implements RetailerStrategy {
	
	public static final String NAME = "logitMaxLinkRetailerStrategy";
	public final static String CONFIG_GROUP = "Retailers";
	public final static String CONFIG_N_ALTERNATIVES = "alternatives";
	private Controler controler;
	private int alternatives;	
	private Map<Id,ActivityFacility> movedFacilities = new TreeMap<Id,ActivityFacility>();
	
	public LogitMaxLinkRetailerStrategy (Controler controler) {
		this.controler = controler;
		String logitAlternatives = this.controler.getConfig().findParam(CONFIG_GROUP,CONFIG_N_ALTERNATIVES);
		int alternatives = Integer.parseInt(logitAlternatives);
		this.alternatives = alternatives;
	}

	public Map<Id, ActivityFacility> moveFacilities(Map<Id, ActivityFacility> facilities, ArrayList<LinkRetailersImpl> Allowedlinks) {
		
		// example to get the facilities (locations) of a link 
//		controler.getNetwork().getLink("").getUpMapping();
		

		for (ActivityFacility f : facilities.values()) { //francesco: TODO check again this loop (or one of the internal one), it seems that too many 
			// facility relocations are performed, if this might not influence the results it is certainly a waste of memory
			
			double[] utils = new double[alternatives];
			Link[] links = controler.getNetwork().getLinks().values().toArray(new Link[controler.getNetwork().getLinks().size()]);
			controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
			double[] currentlink_volumes = controler.getLinkStats().getAvgLinkVolumes(f.getLinkId());
			ArrayList<Id> newLinkIds = new ArrayList<Id>(); 
			newLinkIds.add(f.getLinkId());
			double currentlink_volume =0;
			for (int j=0; j<currentlink_volumes.length;j=j+1) {
				currentlink_volume = currentlink_volume + currentlink_volumes[j];
			}
			utils [0]= Math.log(currentlink_volume); //If the utility would be defined in a more complex way than it is now, a 
			// calc_utility method might be called at this point
			for (int i=1; i<alternatives;i++) {
				int rd = MatsimRandom.getRandom().nextInt(links.length);
				newLinkIds.add(links[rd].getId());
				double[] newlink_volumes = controler.getLinkStats().getAvgLinkVolumes(newLinkIds.get(i));
				
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
					Link l = this.controler.getNetwork().getLinks().get(newLinkIds.get(k));
					((ActivityFacilityImpl) f).setCoord(l.getCoord());
					this.movedFacilities.put(f.getId(),f);
				}
			}
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
