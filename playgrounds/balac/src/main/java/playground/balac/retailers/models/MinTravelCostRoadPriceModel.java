package playground.balac.retailers.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.balac.retailers.data.LinkRetailersImpl;
import playground.balac.retailers.data.PersonPrimaryActivity;
import playground.balac.retailers.utils.Utils;



public class MinTravelCostRoadPriceModel extends RetailerModelImpl
{
  private static final Logger log = Logger.getLogger(MaxActivityModel.class);

  private TreeMap<Id<Link>, LinkRetailersImpl> availableLinks = new TreeMap<>();

  public MinTravelCostRoadPriceModel(MatsimServices controler, Map<Id<ActivityFacility>, ActivityFacilityImpl> retailerFacilities)
  {
    this.controler = controler;
    this.retailerFacilities = retailerFacilities;
      this.controlerFacilities = this.controler.getScenario().getActivityFacilities();
    this.shops = findScenarioShops(this.controlerFacilities.getFacilities().values());

      for (Person p : controler.getScenario().getPopulation().getPersons().values()) {
      Person pi = p;
      this.persons.put(pi.getId(), pi);
    }
  }

  public void init(TreeMap<Integer, String> first)
  {
    this.first = first;

    setInitialSolution(this.first.size());
    log.info("Initial solution = " + getInitialSolution());
    findScenarioShops(this.controlerFacilities.getFacilities().values());
    Gbl.printMemoryUsage();
   /* for (PersonImpl pi : this.persons.values()) {
      PersonRetailersImpl pr = new PersonRetailersImpl(pi);
      this.retailersPersons.put(pr.getId(), pr);
    }*/
    Utils.setPersonPrimaryActivityQuadTree(Utils.createPersonPrimaryActivityQuadTree(this.controler));
    Utils.setShopsQuadTree(Utils.createShopsQuadTree(this.controler));
    
    Utils.setInsideShopsQuadTree(Utils.createInsideShopsQuadTreeWIthoutRetailers(this.controler, this.retailerFacilities));
    Utils.setOutsideShopsQuadTree(Utils.createOutsideShopsQuadTreeWIthoutRetailers(this.controler, this.retailerFacilities));
    
    //TODO: kick out retailers stores
    for(ActivityFacility af: retailerFacilities.values()) {
    	 Utils.removeShopFromShopsQuadTree(af.getCoord().getX(), af.getCoord().getY(), af);
    	 Utils.removeInsideShopFromShopsQuadTree(af.getCoord().getX(), af.getCoord().getY(), af);
    	 Utils.removeOutsideShopFromShopsQuadTree(af.getCoord().getX(), af.getCoord().getY(), af);
    }
    
    for (Integer i = Integer.valueOf(0); i.intValue() < first.size(); i = Integer.valueOf(i.intValue() + 1)) {
      String linkId = this.first.get(i);
      double scoreSum = 0.0D;
        LinkRetailersImpl link = new LinkRetailersImpl(this.controler.getScenario().getNetwork().getLinks().get(Id.create(linkId, Link.class)), this.controler.getScenario().getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
      double centerX = 683217.0; 
      double centerY = 247300.0;
		Coord coord = new Coord(centerX, centerY);
      Collection<PersonPrimaryActivity> primaryActivities;
      if (CoordUtils.calcEuclideanDistance(link.getCoord(), coord) < 5000) {
    	  
	      primaryActivities = Utils.getPersonPrimaryActivityQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), 2700.0D);

      }
      else
	      primaryActivities = Utils.getPersonPrimaryActivityQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), 5000.0D);

      
      scoreSum = primaryActivities.size();
     
      
      link.setScoreSum(scoreSum);
      link.setPotentialCustomers(scoreSum);
      this.availableLinks.put(link.getId(), link);
    }
  }

  private void computePotentialCustomers() {
	  for (Integer i = Integer.valueOf(0); i.intValue() < first.size(); i = Integer.valueOf(i.intValue() + 1)) {
	      String linkId = this.first.get(i);

          LinkRetailersImpl link = new LinkRetailersImpl(this.controler.getScenario().getNetwork().getLinks().get(Id.create(linkId, Link.class)), this.controler.getScenario().getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
	      double centerX = 683217.0; 
	      double centerY = 247300.0;
		  Coord coord = new Coord(centerX, centerY);
	      Collection<ActivityFacility> facilities1;
	      Collection<ActivityFacility> facilities2;

	      if (CoordUtils.calcEuclideanDistance(link.getCoord(), coord) < 5000) {
		      facilities1 = Utils.getInsideShopsQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), 2700.0D);
		      facilities2 = Utils.getOutsideShopsQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), 2700.0D);
		      if (facilities1.size() ==0)
		      log.info("size 0");
		      
		      link.setPotentialCustomers(availableLinks.get(link.getId()).getScoreSum() / (((facilities2.size() ) * 1.5) + facilities1.size()));
	      }
	      else {	    	  
	    	  facilities1 = Utils.getInsideShopsQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), 5000.0D);
	    	  facilities2 = Utils.getOutsideShopsQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), 5000.0D);
	    	  if (facilities2.size() == 0)
			      log.info("size 0");
	    	  link.setPotentialCustomers(availableLinks.get(link.getId()).getScoreSum() / (((facilities1.size() ) * 0.5) + ((facilities2.size()))));

	      }
	        
	  
	      
	      link.setScoreSum(availableLinks.get(link.getId()).getScoreSum());
	      this.availableLinks.put(link.getId(), link);
	    }
	  
  }
  
 

  

  @Override
	public double computePotential(ArrayList<Integer> solution) {
	  
	  Double Fitness = 0.0D;
	  double centerX = 683217.0; 
      double centerY = 247300.0;
	  Coord coord = new Coord(centerX, centerY);
	  ActivityFacilityImpl af = (ActivityFacilityImpl) retailerFacilities.values().toArray()[0];
	  for (int s = 0; s < this.retailerFacilities.size(); ++s) {
		  String linkId = this.first.get(solution.get(s));
		 // Coord coord = new CoordImpl(1,1);
		  if (CoordUtils.calcEuclideanDistance(coord, this.availableLinks.get(Id.create(linkId, Link.class)).getCoord()) < 5000) {
			  Utils.addInsideShopToShopsQuadTree(this.availableLinks.get(Id.create(linkId, Link.class)).getCoord().getX(), this.availableLinks.get(Id.create(linkId, Link.class)).getCoord().getY(), af);
		  }
		  else
			  Utils.addOutsideShopToShopsQuadTree(this.availableLinks.get(Id.create(linkId, Link.class)).getCoord().getX(), this.availableLinks.get(Id.create(linkId, Link.class)).getCoord().getY(), af);

		  Utils.addShopToShopsQuadTree(this.availableLinks.get(Id.create(linkId, Link.class)).getCoord().getX(), this.availableLinks.get(Id.create(linkId, Link.class)).getCoord().getY(), af);
	  }
	  computePotentialCustomers();
	  //log.info("computed potential");
	  for (int s = 0; s < this.retailerFacilities.size(); ++s) {
		  String linkId = this.first.get(solution.get(s));
		  Fitness +=  this.availableLinks.get(Id.create(linkId, Link.class)).getPotentialCustomers();
	  }

	  for (int s = 0; s < this.retailerFacilities.size(); ++s) {
		  String linkId = this.first.get(solution.get(s));		
		  if (CoordUtils.calcEuclideanDistance(coord, this.availableLinks.get(Id.create(linkId, Link.class)).getCoord()) < 5000) {
			  Utils.removeInsideShopFromShopsQuadTree(this.availableLinks.get(Id.create(linkId, Link.class)).getCoord().getX(), this.availableLinks.get(Id.create(linkId, Link.class)).getCoord().getY(), af);
		  }
		  else
			  Utils.removeOutsideShopFromShopsQuadTree(this.availableLinks.get(Id.create(linkId, Link.class)).getCoord().getX(), this.availableLinks.get(Id.create(linkId, Link.class)).getCoord().getY(), af);

		  Utils.removeShopFromShopsQuadTree(this.availableLinks.get(Id.create(linkId, Link.class)).getCoord().getX(), this.availableLinks.get(Id.create(linkId, Link.class)).getCoord().getY(), af);
	  }
	  return Fitness;
  }

  public Map<Id<ActivityFacility>, ActivityFacilityImpl> getScenarioShops() {
    return this.shops;
  }
}
