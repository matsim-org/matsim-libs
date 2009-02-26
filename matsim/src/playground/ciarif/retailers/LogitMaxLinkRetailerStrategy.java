package playground.ciarif.retailers;

import java.util.Map;


import org.matsim.controler.Controler;
import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.network.NetworkLayer;

import playground.ciarif.modechoice_old.UtilityComputer2;
import playground.ciarif.retailers.RetailerStrategy;


public class LogitMaxLinkRetailerStrategy implements RetailerStrategy {
	
	public final static String CONFIG_GROUP = "alternatives";
	private Controler controler;
	private int alternatives;
	// TODO balmermi: remove all params for the alt-size and replace it here by a config parameter
	
	public LogitMaxLinkRetailerStrategy (Controler controler, int alternatives) {
		this.controler = controler;
		this.alternatives = alternatives;
	}

	public void moveFacilities(Map<Id, Facility> facilities) {

		for (Facility f : facilities.values()) {
//				
//				boolean changeLocation = false;
//				changelocation = 
//				if (changeLocation){
//					Coord coord = newLink.getCenter();
//					f.moveTo(coord);
//				}
		}
	}	
		
	public void logitUtilityAlgo() {
			
//			Object[] links = controler.getNetwork().getLinks().values().toArray();
//			int rd = MatsimRandom.random.nextInt(links.length);
//			Link newLink = (Link)links[rd];
//			
//			
//			controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
//			double[] currentlink_volumes = controler.getLinkStats().getAvgLinkVolumes(f.getLink().getId().toString());
//			double[] newlink_volumes = controler.getLinkStats().getAvgLinkVolumes(newLink.getId().toString());
//			double currentlink_volume =0;
//			double newlink_volume =0;
//			for (int j=0; j<currentlink_volumes.length;j=j+1) {
//				currentlink_volume = currentlink_volume + currentlink_volumes[j];
//				
//			}
//			for (int j=0; j<newlink_volumes.length;j=j+1) {
//				newlink_volume = newlink_volume + newlink_volumes[j];
//			}
//			
//			
//			UtilityComputer[] linkUtilityComputers;
//
//			final int alternativeCount = volumes.length;
//
//			linkUtilityComputers = new UtilityComputer[alternativeCount];
//
//			double[] alternativeProbability = new double[alternativeCount];
//			double sumOfProbabilities = 0;
//
//			linkUtilityComputers[0] = ;
//			linkUtilityComputers[1] = ;
//			
//
//			double[] utilities = new double[alternativeCount];
//			
//			for (int i = 0; i < alternativeCount; i++) {
//					utilities[i] = linkUtilityComputers[i].computeUtility(volume);
//				}
//
//				for (int j = 0; j < alternativeCount; j++) {
//					alternativeProbability[j] = getLogitProbability(utilities[j], utilities);
//				}
//
//
//			double r = MatsimRandom.random.nextDouble();
//			int index = 0;
//			sumOfProbabilities = alternativeProbability[index];
//			while (r >= sumOfProbabilities) {
//				index++;
//				sumOfProbabilities += alternativeProbability[index];
//			}
//		}
//		
//		class LogitUtility implements UtilityComputer {
//
//			public double computeUtility(Link link) {
//				
//				return 0;
//			}
			
	}
	private double getLogitProbability(double referenceUtility,double[] utilities) {
		double expSumOfAlternatives = 0.0;
		for (double utility : utilities) {
			expSumOfAlternatives += Math.exp(utility);
		}
		return Math.exp(referenceUtility) / expSumOfAlternatives;
	}


}
