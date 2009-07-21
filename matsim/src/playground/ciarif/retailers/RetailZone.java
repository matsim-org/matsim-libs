package playground.ciarif.retailers;

import java.util.Collection;
//import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.collections.QuadTree;

public class RetailZone {
	
	//private final static Logger log = Logger.getLogger(MaxLinkRetailerStrategy.class);
	
	private Id id;
	private QuadTree<PersonImpl> personsQuadTree;   
	private QuadTree<ActivityFacility> shopsQuadTree;
	
	public RetailZone(Id id,final Double minx,final Double miny,final Double maxx,final Double maxy) { 
		this.id = id;
		this.personsQuadTree = new QuadTree<PersonImpl>(minx, miny, maxx, maxy);
		this.shopsQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
	}

	public Id getId() {
		Id id= this.id;
		return id;
	}

	public void addPersons(Collection<PersonImpl> persons) {
		for (PersonImpl p : persons ) {
			Coord c = p.getSelectedPlan().getFirstActivity().getFacility().getCoord();
			double x1 = this.personsQuadTree.getMaxEasting();
			double x2 = this.personsQuadTree.getMinEasting();
			double y1 = this.personsQuadTree.getMaxNorthing();
			double y2 = this.personsQuadTree.getMinNorthing();
			if (c.getX()< x1 && c.getX()>=x2 && c.getY()<y1 && c.getY()>=y2) { //TODO check if this souldn't be elsewhere, in principle such a method
				// is not supposed to check anything but just to add elements, probably this check should happen outside this class
				this.personsQuadTree.put(c.getX(),c.getY(),p);
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
	
	public QuadTree<PersonImpl> getPersonsQuadTree (){
		return this.personsQuadTree;
	}
	
	public QuadTree<ActivityFacility> getShopsQuadTree (){
		return this.shopsQuadTree;
	}
}

