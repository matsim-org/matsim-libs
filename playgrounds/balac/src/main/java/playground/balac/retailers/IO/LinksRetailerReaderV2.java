package playground.balac.retailers.IO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;

import playground.balac.retailers.data.LinkRetailersImpl;
import playground.balac.retailers.data.Retailer;
import playground.balac.retailers.data.Retailers;
import playground.balac.retailers.utils.Utils;

public class LinksRetailerReaderV2
{
  public static final String CONFIG_LINKS = "links";
  public static final String CONFIG_LINKS_PAR = "freeLinksParameter";
  public static final String CONFIG_LINKS_CRITERIA = "freeLinksCriteria";
  public static final String CONFIG_GROUP = "Retailers";
  private String linkIdFile;
  private MatsimServices controler;
  protected TreeMap<Id<Link>, LinkRetailersImpl> allLinks = new TreeMap<>();
  protected TreeMap<Id<Link>, LinkRetailersImpl> freeLinks = new TreeMap<>();
  private TreeMap<Id<Link>, LinkRetailersImpl> currentLinks = new TreeMap<>();
  private Retailers retailers;
  private double minRatio;
  private double ratioSum = 0.0D;
  private static final Logger log = Logger.getLogger(LinksRetailerReaderV2.class);

  public LinksRetailerReaderV2(MatsimServices controler, Retailers retailers)
  {
    this.controler = controler;
    this.retailers = retailers;
  }

  public void init()
  {
    this.linkIdFile = this.controler.getConfig().findParam("Retailers", "links");
    //WorldConnectLocations wcl = new WorldConnectLocations ();
    detectRetailersActualLinks();
    this.minRatio = findPersonsShopsMinRatio();
    if (this.linkIdFile != null) {
      readFreeLinks();
    }
    else {
      createFreeLinks();
    }

    this.allLinks.putAll(this.currentLinks);
  }

  public void updateFreeLinks()
  {
    LinkRetailersImpl link;
    TreeMap<Id<Link>,LinkRetailersImpl> links = new TreeMap<>();
    TreeMap<Id<Link>,LinkRetailersImpl> linksToRemove = new TreeMap<>();
    detectRetailersActualLinks();

    for (Iterator<LinkRetailersImpl> localIterator1 = this.allLinks.values().iterator(); localIterator1.hasNext(); ) {
    	link = localIterator1.next();
    	links.put(link.getId(), link);
    }
    for (Iterator<LinkRetailersImpl> localIterator1 = this.currentLinks.values().iterator(); localIterator1.hasNext(); ) {
    	link = localIterator1.next();
    	Id<Link> id = link.getId();
    	for (LinkRetailersImpl link2 : links.values()) {
    		Id<Link> id2 = link2.getId();
    		if (id == id2) {
    			linksToRemove.put(link2.getId(), link2);
    		}
    	}
    }
    for (LinkRetailersImpl l : linksToRemove.values()) {
      links.remove(l.getId());
    }
    this.freeLinks = links;
  }

  private void readFreeLinks()
  {
    try
    {
      FileReader fr = new FileReader(this.linkIdFile);
      BufferedReader br = new BufferedReader(fr);

      String curr_line = br.readLine();

      while ((curr_line = br.readLine()) != null) {
        String[] entries = curr_line.split("\t", -1);

        Id<Link> lId = Id.create(entries[0], Link.class);
          LinkRetailersImpl l = new LinkRetailersImpl(this.controler.getScenario().getNetwork().getLinks().get(lId), this.controler.getScenario().getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
       // l.setMaxFacOnLink(Integer.parseInt(entries[1]));
//        if (l.getUpMapping().size() > Integer.parseInt(entries[1]))
//        {
//          l.setMaxFacOnLink(l.getUpMapping().size());
//        }
//        else {
//          l.setMaxFacOnLink(Integer.parseInt(entries[1]));
//        }

        this.freeLinks.put(l.getId(), l);
        this.allLinks.put(l.getId(), l);
      }
      br.close();
    }
    catch (IOException e) {
    }
  }

  private void detectRetailersActualLinks() {
	    TreeMap<Id<Link>, LinkRetailersImpl> links = new TreeMap<Id<Link>, LinkRetailersImpl>();
	    for (Retailer r : this.retailers.getRetailers().values()) {
	      for (ActivityFacility af : r.getFacilities().values()) {
              Link fLink =  this.controler.getScenario().getNetwork().getLinks().get(af.getLinkId());
              LinkRetailersImpl link = new LinkRetailersImpl(fLink, this.controler.getScenario().getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
	        links.put(link.getId(), link);
	        log.info("The facility " + af.getId() + " is currently on the link: " + link.getId());
	      }
	    }
    this.currentLinks = links;
   // for (LinkRetailersImpl l : this.currentLinks.values())
     // log.info("Current Links list contains link: " + l.getId());
  }

  private double findPersonsShopsMinRatio()
  {
    double minRatio = (1.0D / 0.0D);

    for (Retailer r : this.retailers.getRetailers().values()) {
      for (ActivityFacility af : r.getFacilities().values()) {
    	  log.info("Quad tree = " + Utils.getFacilityQuadTree());
        Collection<ActivityFacility> facilities = Utils.getFacilityQuadTree().getDisk(af.getCoord().getX(), af.getCoord().getY(), 1000.0D);
        double ratio;
        double globalCapacity = 0.0D;
        double numberPersons = 0.0D;
        for (ActivityFacility facility : facilities)
        {
          if (facility.getActivityOptions().get("shopgrocery") != null) {
            double shopCapacity = facility.getActivityOptions().get("shopgrocery").getCapacity();
            globalCapacity += shopCapacity;
          }
        }

        numberPersons = Utils.getPersonQuadTree().getDisk(af.getCoord().getX(), af.getCoord().getY(), 1000.0D).size();
        log.info("The number of person around the facility " + af.getId() + " is: " + numberPersons);
        ratio = numberPersons / globalCapacity;
        this.ratioSum += ratio;
        if (ratio < minRatio) {
          minRatio = ratio;
        }
      }
    }
    return minRatio;
  }

  private void createFreeLinks() {
    double referenceRatio = (1.0D / 0.0D);
    String ratioCriteria = this.controler.getConfig().findParam("Retailers", "freeLinksCriteria");
    if (ratioCriteria.equals("minimum")) {
      log.info("The minimum ratio persons/shopsCapacity for the actual locations of this scenario is: " + this.minRatio);
      referenceRatio = this.minRatio;
    }
    else if (ratioCriteria.equals("average")) {
      log.info("The average ratio persons/shopsCapacity for the actual locations of this scenario is: " + (this.ratioSum / this.currentLinks.size()));
      referenceRatio = this.ratioSum / this.currentLinks.size();
    }
    else if (ratioCriteria.equals("one")) {
      referenceRatio = 1.0D;
    }
    else if (ratioCriteria.equals("random")){
    	referenceRatio = 0.0D;
    }
    else {
      throw new RuntimeException("The criteria to choose the available links has not been set");
    }

    String freeLinksParameterString = this.controler.getConfig().findParam("Retailers", "freeLinksParameter");
    Double freeLinksParameterInt = Double.valueOf(Double.parseDouble(freeLinksParameterString));
    Integer newLinksMax = Integer.valueOf((int)Math.round(this.currentLinks.size() * freeLinksParameterInt.doubleValue()));
      int numberLinks = this.controler.getScenario().getNetwork().getLinks().values().size();
    log.info("number links = " + numberLinks);
    int attempts = 0;
    while (this.freeLinks.size() < newLinksMax.intValue()) {
    	
      int rd = MatsimRandom.getRandom().nextInt(numberLinks);
        LinkRetailersImpl link = new LinkRetailersImpl((LinkImpl) this.controler.getScenario().getNetwork().getLinks().values().toArray()[rd], (NetworkImpl) this.controler.getScenario().getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
      if (this.currentLinks.containsKey(link.getId())) { log.info("On the link " + link.getId() + " there is already a facility");
      }
      else if (this.freeLinks.containsKey(link.getId())) { log.info("The link " + link.getId() + " is already in the list");
      }
      else if (link.getFreespeed() > 27) { log.info("The link " + link.getId() + " is on the highway!");
      }
      else {
        double ratio;
        Collection<ActivityFacility> facilities = Utils.getFacilityQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), 2000.0D);
        double globalCapacity = 0.0D;
        double numberPersons = 0.0D;
        int numberShops = 0;
        for (ActivityFacility facility : facilities) {
          if (facility.getActivityOptions().get("shopgrocery") != null) {
            double shopCapacity = facility.getActivityOptions().get("shopgrocery").getCapacity();
            globalCapacity += shopCapacity;
            numberShops = numberShops + 1;
          }
        }
       // log.info("Link " + link.getId());
       // log.info("Link X" + link.getCoord().getX());
      //  log.info("Link Y" + link.getCoord().getY());
        numberPersons = Utils.getPersonQuadTree().getDisk(link.getCoord().getX(), link.getCoord().getY(), 1500.0D).size();
      //  log.info("The number of person around the link " + link.getId() + " is: " + numberPersons);
        ratio = numberPersons / globalCapacity;
      //  log.info("The ratio persons/shopsCapacity of the link " + link.getId() + " is: " + ratio);
     //   log.info("The number of shops around the link " + link.getId() + " is: " + numberShops);

        if (attempts < numberLinks & referenceRatio > 0) {
          if ((((ratio > referenceRatio) ? 1 : 0) & ((numberPersons > 600.0D) ? 1 : 0) ) != 0 & numberShops < 2)
          {
        	  boolean kick = true;
        	  int counter = 0;        	 
        		 
        		 for(Link l:freeLinks.values()) {
        			 if (CoordUtils.calcEuclideanDistance(l.getCoord(), link.getCoord()) < 2500)
        				 counter++;
        		 }
        		 for(Link l:currentLinks.values()) {
        			 
        			 if (CoordUtils.calcEuclideanDistance(l.getCoord(), link.getCoord()) < 2500)
        				 counter++;
        		 }
        		 
        		 if ((counter + 1) * 600 > numberPersons)
        			 kick = false;
        	 
            if (kick) {            	  
            	this.freeLinks.put(link.getId(), link);
            	log.info("the link " + link.getId() + " has been added to the free links");
            	log.info("free links are" + this.freeLinks.keySet());
            	this.allLinks.put(link.getId(), link);
            }
          }
        }
        else if(referenceRatio == 0){
        	this.freeLinks.put(link.getId(), link);
            log.info("the link " + link.getId() + " has been added to the free links");
            log.info("free links are" + this.freeLinks.keySet());
            this.allLinks.put(link.getId(), link);
        }
        else {
          this.freeLinks.put(link.getId(), link);
          log.warn("the link " + link.getId() + " has been added to the free links even if it deosn't fullfill all requested attributes");
          log.info("free links are" + this.freeLinks.keySet());
          this.allLinks.put(link.getId(), link);
        }
      }

      ++attempts;
      log.info("Attempts = " + attempts);
      log.info("Free links size = " + this.freeLinks.size());
    }
  }

  public TreeMap<Id<Link>, LinkRetailersImpl> getFreeLinks()
  {
    return this.freeLinks;
  }
}