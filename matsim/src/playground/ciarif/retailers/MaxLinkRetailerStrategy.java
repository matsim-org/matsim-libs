package playground.ciarif.retailers;


import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.basic.v01.BasicLinkImpl;
import org.matsim.controler.Controler;
import org.matsim.core.api.facilities.Facility;
import org.matsim.gbl.MatsimRandom;

public class MaxLinkRetailerStrategy implements RetailerStrategy {
	
	private final static Logger log = Logger.getLogger(MaxLinkRetailerStrategy.class);
	public static final String NAME = "maxLinkRetailerStrategy";
	
	private Controler controler;

	private Object[] links;
	// TODO balmermi: do the same speed optimization here

	public MaxLinkRetailerStrategy(Controler controler, Object [] links) {
		this.controler = controler;
		this.links = links;
	}
	
	
	public void moveFacilities(Map<Id, Facility> facilities) {
		
		for (Facility f : facilities.values()) {
			//Object[] links = controler.getNetwork().getLinks().values().toArray();
			int rd = MatsimRandom.random.nextInt(this.links.length);
			BasicLinkImpl link = (BasicLinkImpl)this.links[rd];
			controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
			double[] currentlink_volumes = controler.getLinkStats().getAvgLinkVolumes(f.getLink().getId().toString());
			double[] newlink_volumes = controler.getLinkStats().getAvgLinkVolumes(link.getId().toString());
			double currentlink_volume =0;
			double newlink_volume =0;
			for (int j=0; j<currentlink_volumes.length;j=j+1) {
				currentlink_volume = currentlink_volume + currentlink_volumes[j];
				
			}
			for (int j=0; j<newlink_volumes.length;j=j+1) {
				newlink_volume = newlink_volume + newlink_volumes[j];
			}
			
			log.info("facility = " + f.getId());
			log.info ("currentlink = " + f.getLink().getId());
			log.info ("current link coord= " + f.getLink().getCoord());
			log.info ("currentlink_volume = " + currentlink_volume);
			log.info ("newlink_volume = " + newlink_volume);
			if (newlink_volume >= currentlink_volume) {
				log.info ("newlink Id= " + link.getId());
				log.info ("newlink coord= " + link.getCoord());
				Utils.moveFacility(f,link);
			}
		}
	}
}
