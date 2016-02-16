/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.balac.retailers.utils;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.balac.retailers.data.PersonPrimaryActivity;


public abstract class Utils
{
  private static final Logger log = Logger.getLogger(Utils.class);
  private static final double EPSILON = 0.0001D;
  private static QuadTree<Person> personQuadTree;
  private static QuadTree<ActivityFacility> facilityQuadTree;
  private static QuadTree<ActivityFacility> shopsQuadTree;
  private static QuadTree<ActivityFacility> shopsInsideQuadTree;
  private static QuadTree<ActivityFacility> shopsOutsideQuadTree;
  
  private static QuadTree<PersonPrimaryActivity> personPrimaryActivityQuadTree;
  private static double centerX = 683217.0; 
  private static double centerY = 247300.0;	
  public static final void moveFacility(ActivityFacilityImpl f, Link link)
  {
    double[] vector = new double[2];
    vector[0] = (link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY());
    vector[1] = (-(link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX()));

    Coord coord = new Coord(link.getCoord().getX() + vector[0] * EPSILON, link.getCoord().getY() + vector[1] * 0.0001D);
    f.setCoord(coord);
    f.setLinkId(link.getId());
  }

//TODO This class should be modified at least for the following aspects:
// - Avoid to pass the controler but pass directly the Facilities or the Persons which are actually used
// - Try to create a routine which can be used by all the different create<something>quadTree methods instead of
// repeat this for each of them

  public static final void setPersonQuadTree(QuadTree<Person> personQuadTree)
  {
    Utils.personQuadTree = personQuadTree;
  }

  public static final QuadTree<Person> getPersonQuadTree() {
    return personQuadTree;
  }

  public static final void setShopsQuadTree(QuadTree<ActivityFacility> newShopsQuadTree) {
    shopsQuadTree = newShopsQuadTree;
  }

  public static final QuadTree<ActivityFacility> getShopsQuadTree() {
    return shopsQuadTree;
  }
  
  public static final void setInsideShopsQuadTree(QuadTree<ActivityFacility> newShopsQuadTree) {
	    shopsInsideQuadTree = newShopsQuadTree;
	  }

  public static final QuadTree<ActivityFacility> getInsideShopsQuadTree() {
	    return shopsInsideQuadTree;
	  }
	  
	  
  public static final void setOutsideShopsQuadTree(QuadTree<ActivityFacility> newShopsQuadTree) {
	    shopsOutsideQuadTree = newShopsQuadTree;
	  }

  public static final QuadTree<ActivityFacility> getOutsideShopsQuadTree() {
	    return shopsOutsideQuadTree;
	  }	  
  
  
  

  public static final void setPersonPrimaryActivityQuadTree(QuadTree<PersonPrimaryActivity> newPersonPrimaryActivityQuadTree) {
    personPrimaryActivityQuadTree = newPersonPrimaryActivityQuadTree;
  }

  public static final QuadTree<PersonPrimaryActivity> getPersonPrimaryActivityQuadTree() {
    return personPrimaryActivityQuadTree;
  }

  public static final void setFacilityQuadTree(QuadTree<ActivityFacility> newFacilityQuadTree) {
    facilityQuadTree = newFacilityQuadTree;
  }

  public static final QuadTree<ActivityFacility> getFacilityQuadTree() {
    return facilityQuadTree;
  }

  public static final QuadTree<ActivityFacility> createFacilityQuadTree(MatsimServices controler) {
    double minx = (1.0D / 0.0D);
    double miny = (1.0D / 0.0D);
    double maxx = (-1.0D / 0.0D);
    double maxy = (-1.0D / 0.0D);

      for (Link l : controler.getScenario().getNetwork().getLinks().values()) {
      if (l.getCoord().getX() < minx) minx = l.getCoord().getX();
      if (l.getCoord().getY() < miny) miny = l.getCoord().getY();
      if (l.getCoord().getX() > maxx) maxx = l.getCoord().getX();
      if (l.getCoord().getY() <= maxy) continue; maxy = l.getCoord().getY();
    }
    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;

    QuadTree<ActivityFacility> facilityQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
      for (ActivityFacility f : controler.getScenario().getActivityFacilities().getFacilities().values()) {
          ((ActivityFacilityImpl) f).setLinkId(NetworkUtils.getNearestLink(((NetworkImpl) controler.getScenario().getNetwork()), f.getCoord()).getId());
      Coord c = f.getCoord();
      facilityQuadTree.put(c.getX(), c.getY(), f);
    }
    return facilityQuadTree;
  }

  public static final QuadTree<Person> createPersonQuadTree(MatsimServices controler) {
    double minx = (1.0D / 0.0D);
    double miny = (1.0D / 0.0D);
    double maxx = (-1.0D / 0.0D);
    double maxy = (-1.0D / 0.0D);

      for (ActivityFacility f : controler.getScenario().getActivityFacilities().getFacilities().values()) {
      if (f.getCoord().getX() < minx) minx = f.getCoord().getX();
      if (f.getCoord().getY() < miny) miny = f.getCoord().getY();
      if (f.getCoord().getX() > maxx) maxx = f.getCoord().getX();
      if (f.getCoord().getY() <= maxy) continue; maxy = f.getCoord().getY();
    }
    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
    QuadTree<Person> personQuadTree = new QuadTree<Person>(minx, miny, maxx, maxy);
      for (Person p : controler.getScenario().getPopulation().getPersons().values()) {
        Coord c = ((ActivityFacility) controler.getScenario().getActivityFacilities().getFacilities().get(((PlanImpl)p.getSelectedPlan()).getFirstActivity().getFacilityId())).getCoord();
      personQuadTree.put(c.getX(), c.getY(), p);
    }
    log.info("PersonQuadTree has been created");
    return personQuadTree; }

  public static final QuadTree<ActivityFacility> createShopsQuadTree(MatsimServices controler) {
    double minx = (1.0D / 0.0D);
    double miny = (1.0D / 0.0D);
    double maxx = (-1.0D / 0.0D);
    double maxy = (-1.0D / 0.0D);

      for (ActivityFacility f : controler.getScenario().getActivityFacilities().getFacilities().values()) {
      if (f.getCoord().getX() < minx) minx = f.getCoord().getX();
      if (f.getCoord().getY() < miny) miny = f.getCoord().getY();
      if (f.getCoord().getX() > maxx) maxx = f.getCoord().getX();
      if (f.getCoord().getY() <= maxy) continue; maxy = f.getCoord().getY();
    }
    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
    QuadTree<ActivityFacility> shopsQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);

      for (ActivityFacility f : controler.getScenario().getActivityFacilities().getFacilities().values()) {
      //log.info("activity options = " + f.getActivityOptions());
      if (f.getActivityOptions().containsKey("shopgrocery")) {
        Coord c = f.getCoord();
        shopsQuadTree.put(c.getX(), c.getY(), f);
      }
    }

    return shopsQuadTree;
  }
  public static final QuadTree<ActivityFacility> createShopsQuadTreeWithoutRetailers(MatsimServices controler, Map<Id<ActivityFacility>, ActivityFacilityImpl> retailerFacilities) {
	    double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);

      for (ActivityFacility f : controler.getScenario().getActivityFacilities().getFacilities().values()) {
	      if (f.getCoord().getX() < minx) minx = f.getCoord().getX();
	      if (f.getCoord().getY() < miny) miny = f.getCoord().getY();
	      if (f.getCoord().getX() > maxx) maxx = f.getCoord().getX();
	      if (f.getCoord().getY() <= maxy) continue; maxy = f.getCoord().getY();
	    }
	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
	    QuadTree<ActivityFacility> shopsQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);

      for (ActivityFacility f : controler.getScenario().getActivityFacilities().getFacilities().values()) {
	      //log.info("activity options = " + f.getActivityOptions());
	      if (f.getActivityOptions().containsKey("shopgrocery")) {
	        Coord c = f.getCoord();
	        if (!retailerFacilities.containsKey(f.getId()))
	        	shopsQuadTree.put(c.getX(), c.getY(), f);
	      }
	    }

	    return shopsQuadTree;
	  }
  
  public static final boolean addShopToShopsQuadTree(double x, double y, ActivityFacility shop) {
	  
	  return shopsQuadTree.put(x, y, shop);
	  
	  
  }
  public static final boolean removeShopFromShopsQuadTree(double x, double y,ActivityFacility shop) {
	  
	  return shopsQuadTree.remove(x, y, shop);
	  
	  
  }
  
  public static final QuadTree<ActivityFacility> createInsideShopsQuadTreeWIthoutRetailers(MatsimServices controler, Map<Id<ActivityFacility>, ActivityFacilityImpl> retailerFacilities) {
	    double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);
    Coord center = new Coord(centerX, centerY);
      for (ActivityFacility f : controler.getScenario().getActivityFacilities().getFacilities().values()) {
	      if (f.getCoord().getX() < minx) minx = f.getCoord().getX();
	      if (f.getCoord().getY() < miny) miny = f.getCoord().getY();
	      if (f.getCoord().getX() > maxx) maxx = f.getCoord().getX();
	      if (f.getCoord().getY() <= maxy) continue; maxy = f.getCoord().getY();
	    }
	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
	    QuadTree<ActivityFacility> shopsInsideQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);

      for (ActivityFacility f : controler.getScenario().getActivityFacilities().getFacilities().values()) {
	      //log.info("activity options = " + f.getActivityOptions());
	      if (f.getActivityOptions().containsKey("shopgrocery")) {
	        Coord c = f.getCoord();
	        if (!retailerFacilities.containsKey(f.getId())) {
	        	if (CoordUtils.calcEuclideanDistance(c, center) < 5000)
	        		shopsInsideQuadTree.put(c.getX(), c.getY(), f);
	        }
	      }
	    }

	    return shopsInsideQuadTree;
	  }

public static final boolean addInsideShopToShopsQuadTree(double x, double y, ActivityFacility shop) {
	  
	  return shopsInsideQuadTree.put(x, y, shop);
	  
	  
}
public static final boolean removeInsideShopFromShopsQuadTree(double x, double y,ActivityFacility shop) {
	  
	  return shopsInsideQuadTree.remove(x, y, shop);
	  
	  
}


public static final QuadTree<ActivityFacility> createOutsideShopsQuadTreeWIthoutRetailers(MatsimServices controler, Map<Id<ActivityFacility>, ActivityFacilityImpl> retailerFacilities) {
    double minx = (1.0D / 0.0D);
    double miny = (1.0D / 0.0D);
    double maxx = (-1.0D / 0.0D);
    double maxy = (-1.0D / 0.0D);
  Coord center = new Coord(centerX, centerY);
    for (ActivityFacility f : controler.getScenario().getActivityFacilities().getFacilities().values()) {
      if (f.getCoord().getX() < minx) minx = f.getCoord().getX();
      if (f.getCoord().getY() < miny) miny = f.getCoord().getY();
      if (f.getCoord().getX() > maxx) maxx = f.getCoord().getX();
      if (f.getCoord().getY() <= maxy) continue; maxy = f.getCoord().getY();
    }
    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
    QuadTree<ActivityFacility> shopsOutsideQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);

    for (ActivityFacility f : controler.getScenario().getActivityFacilities().getFacilities().values()) {
      //log.info("activity options = " + f.getActivityOptions());
      if (f.getActivityOptions().containsKey("shopgrocery")) {
        Coord c = f.getCoord();
        if (!retailerFacilities.containsKey(f.getId())) {
        	if (CoordUtils.calcEuclideanDistance(c, center) >= 5000)
        		shopsOutsideQuadTree.put(c.getX(), c.getY(), f);
        	
        }
      }
    }

    return shopsQuadTree;
  }

public static final boolean addOutsideShopToShopsQuadTree(double x, double y, ActivityFacility shop) {
  
  return shopsOutsideQuadTree.put(x, y, shop);
  
  
}
public static final boolean removeOutsideShopFromShopsQuadTree(double x, double y,ActivityFacility shop) {
  
  return shopsOutsideQuadTree.remove(x, y, shop);
  
  
}
  
  
  
//changed check for activity types to reflect different education and work activities
  public static final QuadTree<PersonPrimaryActivity> createPersonPrimaryActivityQuadTree(MatsimServices controler)
  {
    int i;
    double minx = (1.0D / 0.0D);
    double miny = (1.0D / 0.0D);
    double maxx = (-1.0D / 0.0D);
    double maxy = (-1.0D / 0.0D);

      for (ActivityFacility f : controler.getScenario().getActivityFacilities().getFacilities().values()) {
      if (f.getCoord().getX() < minx) minx = f.getCoord().getX();
      if (f.getCoord().getY() < miny) miny = f.getCoord().getY();
      if (f.getCoord().getX() > maxx) maxx = f.getCoord().getX();
      if (f.getCoord().getY() <= maxy) continue; maxy = f.getCoord().getY();
    }
    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
    QuadTree<PersonPrimaryActivity> personPrimaryActivityQuadTree = new QuadTree<PersonPrimaryActivity>(minx, miny, maxx, maxy);
    i = 0;
      for (Person p : controler.getScenario().getPopulation().getPersons().values()) {
      int primaryActivityCount = 0;
      boolean hasHome = false;
      boolean hasWork = false;
      boolean hasEducation = false;
      //boolean hasShop = false;

      if (p.getSelectedPlan().getPlanElements().toString().contains("type=shopgrocery")) {
        for (PlanElement pe : p.getSelectedPlan().getPlanElements())
        {
          if (pe instanceof Activity) {
            Coord c;
            Id<Link> activityLink;
            int ppaId;
            PersonPrimaryActivity ppa;
            Activity act = (Activity)pe;

            if (act.getType().equals("home")) {
              if (!(hasHome)) {
                  c = ((ActivityFacility) controler.getScenario().getActivityFacilities().getFacilities().get(act.getFacilityId())).getCoord();
                  activityLink = (NetworkUtils.getNearestLink(((NetworkImpl) controler.getScenario().getNetwork()), act.getCoord())).getId();
                //activityLink = (IdImpl)((ActivityFacility)controler.getFacilities().getFacilities().get(act.getFacilityId())).getLinkId();
                ppaId = Integer.parseInt(p.getId().toString()) * 10 + primaryActivityCount;
                ppa = new PersonPrimaryActivity(act.getType(), ppaId, p.getId(), activityLink);
                personPrimaryActivityQuadTree.put(c.getX(), c.getY(), ppa);

                hasHome = true;
                ++primaryActivityCount;
              }
            }
            else if (act.getType().startsWith("work")) {
              if (!(hasWork)) {
                  c = ((ActivityFacility) controler.getScenario().getActivityFacilities().getFacilities().get(act.getFacilityId())).getCoord();
                  activityLink = ((ActivityFacility) controler.getScenario().getActivityFacilities().getFacilities().get(act.getFacilityId())).getLinkId();
                ppaId = Integer.parseInt(p.getId().toString()) * 10 + primaryActivityCount;
                ppa = new PersonPrimaryActivity(act.getType(), ppaId, p.getId(), activityLink);
                personPrimaryActivityQuadTree.put(c.getX(), c.getY(), ppa);

                hasWork = true;
                ++primaryActivityCount;
              }
            } else {
              if ((!(act.getType().startsWith("education"))) ||
                (hasEducation)) continue;
                c = ((ActivityFacility) controler.getScenario().getActivityFacilities().getFacilities().get(act.getFacilityId())).getCoord();
                activityLink = ((ActivityFacility) controler.getScenario().getActivityFacilities().getFacilities().get(act.getFacilityId())).getLinkId();
              log.info("Act Link " + activityLink);
              ppaId = Integer.parseInt(p.getId().toString()) * 10 + primaryActivityCount;
              ppa = new PersonPrimaryActivity(act.getType(), ppaId, p.getId(), activityLink);
              personPrimaryActivityQuadTree.put(c.getX(), c.getY(), ppa);

              hasEducation = true;
              ++primaryActivityCount;
            }
          }
        }

        i += primaryActivityCount;
      }

      //log.info("Global Primary activity count = " + i);
    }

    return personPrimaryActivityQuadTree;
  }
  /**
   * Getting all the activities that are fixed in the simulation 
   * and that are not included in the location choice.
   */  
  public static final QuadTree<PersonPrimaryActivity> createPersonFixedActivityQuadTree(MatsimServices controler) {
	  
   
	  //controler.getConfig().getModule("locationchoice").getParams().get("flexible_types")
	  double minx = (1.0D / 0.0D);
	  double miny = (1.0D / 0.0D);
	  double maxx = (-1.0D / 0.0D);
	  double maxy = (-1.0D / 0.0D);

      for (ActivityFacility f : controler.getScenario().getActivityFacilities().getFacilities().values()) {
      if (f.getCoord().getX() < minx) minx = f.getCoord().getX();
      if (f.getCoord().getY() < miny) miny = f.getCoord().getY();
      if (f.getCoord().getX() > maxx) maxx = f.getCoord().getX();
      if (f.getCoord().getY() <= maxy) continue; maxy = f.getCoord().getY();
    }
    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
    QuadTree<PersonPrimaryActivity> personPrimaryActivityQuadTree = new QuadTree<PersonPrimaryActivity>(minx, miny, maxx, maxy);
    Activity previousActivity = null;
      for (Person p : controler.getScenario().getPopulation().getPersons().values()) {
      int primaryActivityCount = 0;
     

      if (p.getSelectedPlan().getPlanElements().toString().contains("type=shopgrocery")) {
        for (PlanElement pe : p.getSelectedPlan().getPlanElements())
        {
          if (pe instanceof Activity) {
            Coord c;
            Id<Link> activityLink;
            int ppaId;
            PersonPrimaryActivity ppa;
            Activity act = (Activity)pe;

            if (((Activity) pe).getType().equals( "shopgrocery" )) {
            	
            	if (!previousActivity.getType().startsWith("shopgrocery")) {

                    c = ((ActivityFacility) controler.getScenario().getActivityFacilities().getFacilities().get(act.getFacilityId())).getCoord();
                    activityLink = (NetworkUtils.getNearestLink(((NetworkImpl) controler.getScenario().getNetwork()), act.getCoord())).getId();
                     ppaId = Integer.parseInt(p.getId().toString()) * 10 + primaryActivityCount;
                     ppa = new PersonPrimaryActivity(act.getType(), ppaId, p.getId(), activityLink);
                     personPrimaryActivityQuadTree.put(c.getX(), c.getY(), ppa);                    
                     ++primaryActivityCount;
            		
            	}
            }
            else if (previousActivity != null && previousActivity.getType().equals("shopgrocery")) {
                c = ((ActivityFacility) controler.getScenario().getActivityFacilities().getFacilities().get(act.getFacilityId())).getCoord();
                activityLink = (NetworkUtils.getNearestLink(((NetworkImpl) controler.getScenario().getNetwork()), act.getCoord())).getId();
            	 	ppaId = Integer.parseInt(p.getId().toString()) * 10 + primaryActivityCount;
            	 	ppa = new PersonPrimaryActivity(act.getType(), ppaId, p.getId(), activityLink);
            	 	personPrimaryActivityQuadTree.put(c.getX(), c.getY(), ppa);                    
            	 	++primaryActivityCount;
            	
            
        }
          previousActivity = (Activity) pe;
        
      }
      }
    }

     
    }

    return personPrimaryActivityQuadTree;
  }
  
  
}
