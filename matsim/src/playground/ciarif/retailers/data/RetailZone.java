package playground.ciarif.retailers.data;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.collections.QuadTree;

import playground.ciarif.retailers.stategies.MaxLinkRetailerStrategy;

public class RetailZone {
	
	private final static Logger log = Logger.getLogger(MaxLinkRetailerStrategy.class);
	
	private Id id;
	private QuadTree<PersonImpl> personsQuadTree;   
	private QuadTree<ActivityFacility> shopsQuadTree;
	private ArrayList<PersonImpl> sampledPersons = new ArrayList<PersonImpl>();
	private ArrayList<ActivityFacility> sampledShops = new ArrayList<ActivityFacility>();
	private double personsSamplingRate;
	private double shopsSamplingRate;
	
	public RetailZone(Id id,final Double minx,final Double miny,final Double maxx,final Double maxy, double shopsSamplingRate, double personsSamplingRate) { 
		this.id = id;
		this.personsQuadTree = new QuadTree<PersonImpl>(minx, miny, maxx, maxy);
		this.shopsQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		this.personsSamplingRate = personsSamplingRate;
		this.shopsSamplingRate = shopsSamplingRate;
	}

	public Id getId() {
		Id id= this.id;
		return id;
	}

	public void addPersonToQuadTree(Coord coord, PersonImpl person) {//TODO look if it is possible to eliminate the QuadTree and use another data structure
		this.personsQuadTree.put(coord.getX(),coord.getY(),person);
		double rd = MatsimRandom.getRandom().nextDouble();
		if (rd <= this.personsSamplingRate) {
			sampledPersons.add(person);
		}
	}

	public void addShopToQuadTree(Coord coord, ActivityFacility shop) { //TODO look if it is possible to eliminate the QuadTree and use another data structure
		this.shopsQuadTree.put(coord.getX(),coord.getY(),shop);	
		double rd = MatsimRandom.getRandom().nextDouble();
		if (rd <= this.shopsSamplingRate) {
			sampledShops.add(shop);
		}
	}
	
	public QuadTree<PersonImpl> getPersonsQuadTree (){
		return this.personsQuadTree;
	}
	
	public ArrayList<PersonImpl> getSampledPersons (){
		return this.sampledPersons;
	}
	
	public ArrayList<ActivityFacility> getSampledShops (){
		return this.sampledShops;
	}
	
	public QuadTree<ActivityFacility> getShopsQuadTree (){
		return this.shopsQuadTree;
	}

}

