package playground.balac.carsharing.preprocess.membership;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;

import playground.balac.carsharing.router.CarSharingStation;
import playground.balac.carsharing.router.CarSharingStations;
import playground.balac.retailers.IO.LinksRetailerReader;
import playground.balac.retailers.data.LinkRetailersImpl;


public class LinksCSReader
{
  public static final String CONFIG_LINKS = "links";
  public static final String CONFIG_LINKS_PAR = "freeLinksParameter";
  public static final String CONFIG_GROUP = "CsPreprocess";
  private String linkIdFile;
  protected TreeMap<Id<Link>, LinkImpl> allLinks = new TreeMap<>();
  //protected TreeMap<Id, LinkImpl> freeLinks = new TreeMap();
  protected ArrayList<LinkImpl> freeLinks = new ArrayList<LinkImpl>();
  private ArrayList<LinkImpl> currentLinks = new ArrayList<LinkImpl>();
  private ScenarioImpl scenario;
  private CarSharingStations carSharingStations;
  private QuadTree<Person> personQuadTree;

  private static final Logger log = Logger.getLogger(LinksRetailerReader.class);

  public LinksCSReader(ScenarioImpl scenario, CarSharingStations csStations)
  {
    this.scenario = scenario;
    this.carSharingStations = csStations;
    init();
  }

  public void init()
  {
    this.linkIdFile = this.scenario.getConfig().findParam("CsPreprocess", "links");
    detectCSActualLinks();
    createPersonQuadTree();
    if (this.linkIdFile != null) {
      readFreeLinks();
    }
    else
      createFreeLinks();
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
        LinkRetailersImpl l = new LinkRetailersImpl(this.scenario.getNetwork().getLinks().get(lId), this.scenario.getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));

        this.freeLinks.add( l);
        this.allLinks.put(l.getId(), l);
      }
      br.close();
    }
    catch (IOException e) {
    }
  }

  private void detectCSActualLinks() {
	  ArrayList<LinkImpl> links = new ArrayList<LinkImpl>();

    for (CarSharingStation af : this.carSharingStations.getStationsArr()) {
      LinkImpl fLink = (LinkImpl)this.scenario.getNetwork().getLinks().get(af.getLinkId());
      links.add( fLink);
    }

    this.currentLinks = links;
  }

  private void createFreeLinks()
  {
    double referenceDensity = 120.0D;
    String freeLinksParameterString = "0.5";
    Double freeLinksParameterInt = Double.valueOf(Double.parseDouble(freeLinksParameterString));
    Integer newLinksMax = Integer.valueOf((int)Math.round(this.currentLinks.size() * freeLinksParameterInt.doubleValue()));
    int numberLinks = this.scenario.getNetwork().getLinks().values().size();
    log.info("number links = " + numberLinks);
    log.info("free links size will be = " + newLinksMax);
    int size = this.scenario.getNetwork().getLinks().values().toArray().length;
    Object[] arr = this.scenario.getNetwork().getLinks().values().toArray();
    List<Object> list = Arrays.asList(arr);
    Collections.shuffle(list);
    int rd = -1;
    while (this.freeLinks.size() < newLinksMax.intValue() && rd < size - 1) {
      
    	 
    	  rd++;
    	  LinkImpl link = (LinkImpl)list.get(rd);
    	  
        if (this.currentLinks.contains((link))) {
        	log.info("On the link " + link.getId() + " there is already a Station"); 
        }
        else if (this.freeLinks.contains(link)) {
        	log.info("The link " + link.getId() + " is already in the list");
        } else {
        	double density = this.personQuadTree.getDisk(link.getCoord().getX(), link.getCoord().getY(), 100.0D).size();
        	if ((this.freeLinks.size() < newLinksMax.intValue()) && 
        			(density > referenceDensity)) {
        		this.freeLinks.add(link);
        		log.info("the link " + link.getId() + " has been added to the free links");
        		log.info("free links are" + this.freeLinks);
        		this.allLinks.put(link.getId(), link);
          }
        }   
    	  

    	  
      
    }
  }

  public ArrayList<LinkImpl> getFreeLinks()
  {
    return this.freeLinks;
  }

  public ArrayList<LinkImpl> getCurrentLinks()
  {
    return this.currentLinks;
  }

  public final void createPersonQuadTree() {
    log.info("TEST");
    double minx = (1.0D / 0.0D);
    double miny = (1.0D / 0.0D);
    double maxx = (-1.0D / 0.0D);
    double maxy = (-1.0D / 0.0D);

    for (ActivityFacility f : this.scenario.getActivityFacilities().getFacilities().values()) {
      if (f.getCoord().getX() < minx) minx = f.getCoord().getX();
      if (f.getCoord().getY() < miny) miny = f.getCoord().getY();
      if (f.getCoord().getX() > maxx) maxx = f.getCoord().getX();
      if (f.getCoord().getY() > maxy) maxy = f.getCoord().getY();
    }
    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
    QuadTree<Person> personQuadTree = new QuadTree<>(minx, miny, maxx, maxy);
    for (Person p : this.scenario.getPopulation().getPersons().values()) {
      Coord c = ((ActivityFacility)this.scenario.getActivityFacilities().getFacilities().get(((PlanImpl)p.getSelectedPlan()).getFirstActivity().getFacilityId())).getCoord();
      personQuadTree.put(c.getX(), c.getY(), p);
    }
    log.info("PersonQuadTree has been created");
    this.personQuadTree = personQuadTree;
  }
}