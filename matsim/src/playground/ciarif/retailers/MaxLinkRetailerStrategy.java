package playground.ciarif.retailers;




import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.population.Person;
import org.matsim.core.basic.v01.BasicLinkImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;

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
			//double rd1 = MatsimRandom.getRandom().nextDouble();
			//if (rd1 < 0.1 & f.getActivityOption("shop").getCapacity()>50 ) { 
				//First it is ensured that only a given percentage of facilities can be moved at one step,
				// then the specific shop is moved only if it exceeds a given capacity. This allows for moving 
				// only "interesting" facilities (large enough to notice differences in the customer data) 

			int rd = MatsimRandom.getRandom().nextInt(this.links.length);
			BasicLinkImpl link = (BasicLinkImpl)this.links[rd];
			log.info("The link " + link.getId() + " is proposed as new location for the facility " + f.getId());
			controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
			double[] currentlink_volumes = controler.getLinkStats().getAvgLinkVolumes(f.getLink().getId());
			double[] newlink_volumes = controler.getLinkStats().getAvgLinkVolumes(link.getId());
			double currentlink_volume =0;
			double newlink_volume =0;
			for (int j=0; j<currentlink_volumes.length;j=j+1) {
				currentlink_volume = currentlink_volume + currentlink_volumes[j];
				
			}
			for (int j=0; j<newlink_volumes.length;j=j+1) {
				newlink_volume = newlink_volume + newlink_volumes[j];
			}
			Collection<Person> persons_actual = Utils.getPersonQuadTree().get(f.getLink().getCoord().getX(),f.getLink().getCoord().getY(),150);
			Collection<Person> persons_new = Utils.getPersonQuadTree().get(link.getCoord().getX(),link.getCoord().getY(),150);
			Collection<Facility> facilities_actual = Utils.getFacilityQuadTree().get(f.getCoord().getX(),f.getCoord().getY(),150);
			Collection<Facility> facilities_new = Utils.getFacilityQuadTree().get(link.getCoord().getX(),link.getCoord().getY(),150);
			boolean move_facilities = false;
			
			for (Facility f1:facilities_new) {
				if (facilities.values().contains(f1)){
					log.info("Around the proposed new link a facility of this retailer already exists ");
					break;
				}
				else {
					move_facilities = true;
				}
			}
			if (move_facilities == true) {
				if (persons_actual.size()/facilities_actual.size()<persons_new.size()/facilities_new.size()) {
					log.info("Persons living around the actual location = " + persons_actual.size());
					log.info("Persons living around the new location = " + persons_new.size());
					log.info("Facilities in the actual area = " + facilities_actual.size());
					log.info("Facilities in the new area = " + facilities_new.size());
					log.info("Ratio Persons/Facilities in the actual area = " + persons_actual.size()/facilities_actual.size());
					log.info("Ratio Persons/Facilities in the new area = " + persons_new.size()/facilities_new.size());
					log.info("facility = " + f.getId());
					log.info ("currentlink = " + f.getLink().getId());
					log.info ("currentlink_volume = " + currentlink_volume);
					log.info ("newlink_volume = " + newlink_volume);
				}
				else {
					move_facilities = false;
				}
			}
			if (move_facilities == true) {
				if (newlink_volume >= currentlink_volume) {
				}
				else {
					move_facilities = false;
				}
			}	
			if (move_facilities == true) {
				Utils.moveFacility(f,link, controler.getWorld());
			}
			else {
				log.info ("The facility " + f.getId() + " will stay at the current link");
			}
		}
	}


	public void moveRetailersFacilities(
			Map<Id, FacilityRetailersImpl> facilities) {
		// TODO Auto-generated method stub
		
	}	
}	
