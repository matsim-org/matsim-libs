package playground.ciarif.retailers;

import java.util.Map;

import org.matsim.controler.Controler;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Link;

public class MaxLinkRetailerStrategy implements RetailerStrategy {
	private Controler controler;
	// TODO balmermi: do the same speed optimization here

	public MaxLinkRetailerStrategy(Controler controler) {
		this.controler = controler;
	}

	public void moveFacilities(Map<Id, Facility> facilities) {
		
		for (Facility f : facilities.values()) {
			Object[] links = controler.getNetwork().getLinks().values().toArray();
			int rd = MatsimRandom.random.nextInt(links.length);
			Link link = (Link)links[rd];
			controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
			double[] currentlink_volumes = controler.getLinkStats().getAvgLinkVolumes(f.getLink().getId().toString());
			double[] newlink_volumes = controler.getLinkStats().getAvgLinkVolumes(link.getId().toString());
			System.out.println ("currentlink_volumes = " + currentlink_volumes);
			double currentlink_volume =0;
			double newlink_volume =0;
			for (int j=0; j<currentlink_volumes.length;j=j+1) {
				currentlink_volume = currentlink_volume + currentlink_volumes[j];
				
			}
			for (int j=0; j<newlink_volumes.length;j=j+1) {
				newlink_volume = newlink_volume + newlink_volumes[j];
				
			}
			System.out.println ("currentlink_volume = " + currentlink_volume);
			System.out.println ("newlink_volume = " + newlink_volume);
			if (newlink_volume >= currentlink_volume) {
				System.out.println ("newlink = " + link);
				System.out.println ("facility = " + f.getId());
				Utils.moveFacility(f,link);
			}
		}
	}
}
