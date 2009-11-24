package playground.ciarif.retailers.data;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;

public class RetailZone {
	
	private final static Logger log = Logger.getLogger(RetailZone.class);
	
	private Id id;
	private QuadTree<Person> personsQuadTree;   
	private QuadTree<ActivityFacilityImpl> shopsQuadTree;
	private ArrayList<Person> persons = new ArrayList<Person>();
	private ArrayList<ActivityFacilityImpl> shops = new ArrayList<ActivityFacilityImpl>();
	private CoordImpl minCoord;
	private CoordImpl maxCoord;
	
	public RetailZone(Id id,final Double minx,final Double miny,final Double maxx,final Double maxy) { 
		this.id = id;
		this.personsQuadTree = new QuadTree<Person>(minx, miny, maxx, maxy);
		this.shopsQuadTree = new QuadTree<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		this.minCoord = new CoordImpl (minx.toString(),miny.toString());
		this.maxCoord = new CoordImpl (maxx.toString(), maxy.toString());		
	}

	public Id getId() {
		Id id= this.id;
		return id;
	}

	public void addPersonToQuadTree(Coord coord, Person person) {//TODO look if it is possible to eliminate the QuadTree and use another data structure
		this.personsQuadTree.put(coord.getX(),coord.getY(),person);
		this.persons.add(person);
	}

	public void addShopToQuadTree(Coord coord, ActivityFacilityImpl shop) { //TODO look if it is possible to eliminate the QuadTree and use another data structure
		this.shopsQuadTree.put(coord.getX(),coord.getY(),shop);	
		this.shops.add(shop);
	}
	
	public QuadTree<Person> getPersonsQuadTree (){
		return this.personsQuadTree;
	}
	
	public ArrayList<Person> getPersons (){
		return this.persons;
	}
	
	public ArrayList<ActivityFacilityImpl> getShops (){
		return this.shops;
	}
	
	public QuadTree<ActivityFacilityImpl> getShopsQuadTree (){
		return this.shopsQuadTree;
	}
	public CoordImpl getMaxCoord (){
		return this.maxCoord;
	}
	public CoordImpl getMinCoord (){
		return this.minCoord;
	}
}

