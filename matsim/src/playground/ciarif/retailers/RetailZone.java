package playground.ciarif.retailers;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.collections.QuadTree;

public class RetailZone {
	
	private final static Logger log = Logger.getLogger(MaxLinkRetailerStrategy.class);
	
	private Id id;
	private QuadTree<Person> personsQuadTree; // TODO Check if it does make more sense private, or if static does make sense at all  
	private QuadTree<ActivityFacility> shopsQuadTree;
	
	public RetailZone(Id id,final Double minx,final Double miny,final Double maxx,final Double maxy) { 
		this.id = id;
		this.personsQuadTree = new QuadTree<Person>(minx, miny, maxx, maxy);
		this.shopsQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
	}

	public Id getId() {
		Id id= this.id;
		return id;
	}

	public void addPersons(Collection<Person> persons) {
		for (Person p : persons ) {
			Coord c = p.getSelectedPlan().getFirstActivity().getFacility().getCoord();
			double x1 = this.personsQuadTree.getMaxEasting();
			double x2 = this.personsQuadTree.getMinEasting();
			double y1 = this.personsQuadTree.getMaxNorthing();
			double y2 = this.personsQuadTree.getMinNorthing();
//			log.info("x1 = " + x1 );
//			log.info("x2 = " + x2 );
//			log.info("y1 = " + y1 );
//			log.info("y2 = " + y2 );
//			log.info("Person's X home coord = " + c.getX() );
//			log.info("Person's Y home coord = " + c.getY() );
//			log.info("Person's Id = " + p.getId() );
			if (c.getX()< x1 && c.getX()>=x2 && c.getY()<y1 && c.getY()>=y2) {
				this.personsQuadTree.put(c.getX(),c.getY(),p);
//				log.info("the person " + p.getId() + " has been added" );
//				log.info(this.personsQuadTree.values().size() + " persons are living in this zone ");
			}		
		}		
	}

	public void addFacilities(Collection<ActivityFacility> shops) {
		for (ActivityFacility af : shops) {
			Coord c = af.getCoord();
			double x1 = this.shopsQuadTree.getMaxEasting();
			double x2 = this.shopsQuadTree.getMinEasting();
			double y1 = this.shopsQuadTree.getMaxNorthing();
			double y2 = this.shopsQuadTree.getMinNorthing();
			if (c.getX()< x1 & c.getX()>=x2 & c.getY()<y1 & c.getY()>=y2) {
				this.shopsQuadTree.put(c.getX(),c.getY(),af);
			}	
		}	
	}
	
	public QuadTree<Person> getPersonsQuadTree (){
		return this.personsQuadTree;
	}
	
	public QuadTree<ActivityFacility> getShopsQuadTree (){
		return this.shopsQuadTree;
	}
}

