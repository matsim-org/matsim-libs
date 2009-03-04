package playground.ciarif.retailers;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;


import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.network.NetworkLayer;

import playground.ciarif.modechoice_old.UtilityComputer2;
import playground.ciarif.retailers.RetailerStrategy;


public class LogitMaxLinkRetailerStrategy implements RetailerStrategy {
	
	public final static String CONFIG_GROUP = "Retailers";
	public final static String CONFIG_N_ALTERNATIVES = "alternatives";
	private Controler controler;
	private int alternatives;	
	
	public LogitMaxLinkRetailerStrategy (Controler controler) {
		this.controler = controler;
		String logitAlternatives = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_N_ALTERNATIVES);
		int alternatives = Integer.parseInt(logitAlternatives);
		this.alternatives = alternatives;
	}

	public void moveFacilities(Map<Id, Facility> facilities) {

		for (Facility f : facilities.values()) {
			
			double[] utils = new double[alternatives];
			Object[] links = controler.getNetwork().getLinks().values().toArray();
			controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
			double[] currentlink_volumes = controler.getLinkStats().getAvgLinkVolumes(f.getLink().getId().toString());
			ArrayList<Link> newLinks = new ArrayList<Link>(); 
			newLinks.add(f.getLink());
			double currentlink_volume =0;
			for (int j=0; j<currentlink_volumes.length;j=j+1) {
				currentlink_volume = currentlink_volume + currentlink_volumes[j];
			}
			utils [0]= Math.log(currentlink_volume); //If the utility would be defined in a more complex way than it is now here a 
			// calc_utility method might be called at this point	
			System.out.println ("Utility [0] " + utils [0]);
			for (int i=1; i<alternatives;i++) {
				int rd = MatsimRandom.random.nextInt(links.length);
				newLinks.add((Link)links[rd]);
				double[] newlink_volumes = controler.getLinkStats().getAvgLinkVolumes(newLinks.get(i).getId().toString());
				
				double newlink_volume =0;
				
				for (int j=0; j<newlink_volumes.length;j=j+1) {
					newlink_volume = newlink_volume + newlink_volumes[j];
				}
				utils [i]= Math.log(newlink_volume); // see above calc_utility
				System.out.println ("Utility [" + i + "] = " + utils [i]);
			}
			double r = MatsimRandom.random.nextDouble();
			double [] probs = calcLogitProbability(utils);
			System.out.println ("Probs [0] " + probs [0]);
			System.out.println ("Probs [1] " + probs [1]);
			for (int k=0;k<probs.length;k++) {
				if (r<=probs [k]) {
					f.moveTo(newLinks.get(k).getCenter());
				}
			}
		}
	}
	private final double[] calcLogitProbability(double[] utils) {
		double exp_sum = 0.0;
		for (int i=0; i<utils.length; i++) { exp_sum += Math.exp(utils[i]); System.out.println ("exp_sum = " + exp_sum);}
		double [] probs = new double[utils.length];
		for (int i=0; i<utils.length; i++) { probs[i] = Math.exp(utils[i])/exp_sum; System.out.println ("util [i] exp = " + Math.exp(utils[i]));}
		return probs;
	}
}
