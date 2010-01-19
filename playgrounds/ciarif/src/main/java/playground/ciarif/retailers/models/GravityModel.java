package playground.ciarif.retailers.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.ciarif.retailers.data.PersonRetailersImpl;
import playground.ciarif.retailers.data.RetailZone;
import playground.ciarif.retailers.data.RetailZones;

public class GravityModel
{
  private static final Logger log = Logger.getLogger(GravityModel.class);
  
  public final static String CONFIG_GROUP = "GravityModel";
  public final static String CONFIG_ZONES = "zones";
  public final static String CONFIG_PARTITION = "partition";
  public final static String CONFIG_SAMPLE_NUMBER_SHOPS = "samplingNumberShops";
  public final static String CONFIG_SAMPLE__TYPE_SHOPS = "samplingTypeShops";
  public final static String CONFIG_SAMPLE_PERSONS = "samplingRatePersons";
  private double[] betas;
  private final Controler controler;
  private Map<Id,ActivityFacilityImpl> shops = new TreeMap<Id,ActivityFacilityImpl>();
  private final ActivityFacilities controlerFacilities;
  private final Map<Id, PersonRetailersImpl> retailersPersons = new TreeMap<Id, PersonRetailersImpl>();
  private final RetailZones retailZones = new RetailZones();
  private final Map<Id, ActivityFacilityImpl> retailersFacilities;
  private TreeMap<Integer,String> first;
  private final Map<Id, ? extends Person> persons;
  private int counter=0;
  private int nextCounterMsg =1;
  
 
  public GravityModel(Controler controler, Map<Id, ActivityFacilityImpl> retailerFacilities)
  {
    this.controler = controler;
    this.retailersFacilities = retailerFacilities;
    this.controlerFacilities = controler.getFacilities();
    this.shops = this.findScenarioShops(this.controlerFacilities.getFacilities().values());
    this.persons = (controler.getPopulation().getPersons());
    
  }
 
public void init() {
	  
	//String type_of_partition = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_PARTITION);
	int number_of_zones =0;
	int n = (int)Double.parseDouble(controler.getConfig().findParam(CONFIG_GROUP,CONFIG_ZONES));
	//if (type_of_partition.equals("symmetric")){
	number_of_zones = (int) Math.pow(n,2);
	//}
	//else {throw new RuntimeException("In config file, param = "+CONFIG_ZONES+" in module = "+CONFIG_GROUP+" at the moment can only take the value 'symmetric'!"); }
	//TODO Define the asymmetric version, at the moment only the symmetric is possible
	if (number_of_zones == 0) { throw new RuntimeException("In config file, param = "+CONFIG_ZONES+" in module = "+CONFIG_GROUP+" not defined!");}
	this.createZonesFromPersonsShops(n);
	this.findScenarioShops(this.controlerFacilities.getFacilities().values());
	Gbl.printMemoryUsage();
	
	for  (Person p:controler.getPopulation().getPersons().values()) {
		PersonRetailersImpl pr = new PersonRetailersImpl(p);
		this.retailersPersons.put(pr.getId(), pr);
	}
}

public double computePotential(ArrayList<Integer> solution){
	
	double global_likelihood = 0;
    int a = 0;
    
    for (ActivityFacility c : this.retailersFacilities.values()) {
    String linkId = this.first.get(solution.get(a));
    Coord coord = this.controler.getNetwork().getLinks().get(new IdImpl(linkId)).getCoord();
	++a;
	double loc_likelihood = 0.0D;
	
	for (PersonRetailersImpl pr : this.retailersPersons.values()) {
        
		double pers_sum_potential = 0.0D;
        double pers_potential = 0.0D;
        double pers_likelihood = 0.0D;
        
        ActivityFacility firstFacility = this.controlerFacilities.getFacilities().get(((PlanImpl) pr.getSelectedPlan()).getFirstActivity().getFacilityId());
       	double dist1 = ((ActivityFacilityImpl) firstFacility).calcDistance(coord);
        if (dist1 == 0.0D) {
        	dist1 = 10.0D;
        }
        pers_potential = Math.pow(dist1, this.betas[0]) + Math.pow(c.getActivityOptions().get("shop").getCapacity().doubleValue(), this.betas[1]);
        
        if (pr.getGlobalShopsUtility()==0) {
        	this.processPerson();
        	
        	for (ActivityFacility s : this.shops.values()) {
	          double dist = 0.0D;
	          int count=0;
	         
	          for (ActivityFacility af: this.retailersFacilities.values()){
		          
	        	  if (af.equals(s)){
		            int index = count;
		            Coord coord1 = this.controler.getNetwork().getLinks().get(new IdImpl(this.first.get(solution.get(index)))).getCoord();
		            
		            dist = ((ActivityFacilityImpl) firstFacility).calcDistance(coord1);
		            if (dist == 0.0D) {
		            	dist = 10.0D;
		            }
		          }
		          else if (CoordUtils.calcDistance(s.getCoord(), ((PlanImpl) pr.getSelectedPlan()).getFirstActivity().getCoord()) == 0.0D) {
		        	  dist = 10.0D;
		          } 
		          
		          else {
		            dist = CoordUtils.calcDistance(s.getCoord(), ((PlanImpl) pr.getSelectedPlan()).getFirstActivity().getCoord());
		          }
		          ++count;
	          } 
	
	          double potential = Math.pow(dist, this.betas[0]) + Math.pow(s.getActivityOptions().get("shop").getCapacity().doubleValue(), this.betas[1]);
	          ;
	          pers_sum_potential += potential;
        	}
        	pr.setGlobalShopsUtility(pers_sum_potential);
        }    
        pers_likelihood = pers_potential / pr.getGlobalShopsUtility();
        loc_likelihood += pers_likelihood;
      }

      global_likelihood += loc_likelihood;
    }
    
    return global_likelihood;
  }
  
  private Map<Id,ActivityFacilityImpl> findScenarioShops (Collection<? extends ActivityFacility> controlerFacilities) {
	  
		Map<Id,ActivityFacilityImpl> shops = new TreeMap<Id,ActivityFacilityImpl>();
		for (ActivityFacility f : controlerFacilities) {
			if (f.getActivityOptions().entrySet().toString().contains("shop")) {
				shops.put(f.getId(),(ActivityFacilityImpl)f);
			}
		}
		return shops;
	}
  private void createZonesFromPersonsShops (int n) {
		log.info("Zones are created");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Person p : persons.values()) {
			if (((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getX() < minx) { minx = ((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getX(); }
			if (((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getY() < miny) { miny = ((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getY(); }
			if (((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getX() > maxx) { maxx = ((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getX(); }
			if (((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getY() > maxy) { maxy = ((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getY(); }
		}
		for (ActivityFacility shop : shops.values()) {
			if (shop.getCoord().getX() < minx) { minx = shop.getCoord().getX(); }
			if (shop.getCoord().getY() < miny) { miny = shop.getCoord().getY(); }
			if (shop.getCoord().getX() > maxx) { maxx = shop.getCoord().getX(); }
			if (shop.getCoord().getY() > maxy) { maxy = shop.getCoord().getY(); }
		}
		minx -= 1.0; miny -= 1.0; maxx += 1.0; maxy += 1.0;
		log.info("Min x = " + minx );
		log.info("Min y = " + miny );
		log.info("Max x = " + maxx );
		log.info("Max y = " + maxy );
		/*minx= minx*0.75;
		miny= miny*0.75;
		maxx= maxx*0.75;
		maxy= maxy*0.75;*/ // TODO attention! This way it doesn't work (it works just by coincidence in the test scenario), but a way to avoid
		// too many retail zones with no, or just few, customers would be nice. Need to think again about it
			double x_width = (maxx - minx)/n;
			double y_width = (maxy - miny)/n;
			int a = 0;
			int i = 0;
			
			while (i<n) {
				int j = 0;
				while (j<n) {
					Id id = new IdImpl (a);
					double x1= minx + i*x_width;
					double x2= x1 + x_width;
					double y1= miny + j*y_width;
					double y2= y1 + y_width;
					RetailZone rz = new RetailZone (id, x1, y1, x2, y2);
					for (Person p : persons.values() ) {
						Coord c = this.controlerFacilities.getFacilities().get(((PlanImpl) p.getSelectedPlan()).getFirstActivity().getFacilityId()).getCoord();
						if (c.getX()< x2 && c.getX()>=x1 && c.getY()<y2 && c.getY()>=y1) { 
							rz.addPersonToQuadTree(c,p);
						}		
					} 
					for (ActivityFacility af : shops.values()) {
						Coord c = af.getCoord();
						if (c.getX()< x2 & c.getX()>=x1 & c.getY()<y2 & c.getY()>=y1) {
							rz.addShopToQuadTree(c,af);
						}
					}	
					this.retailZones.addRetailZone(rz);
					a=a+1;
					j=j+1;
				}
				i=i+1;
			} 
  }
  
  //This version of the method uses only facilities in order to define the zones
  private void createZonesFromFacilities (int n) {
		
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		
		for (ActivityFacility af : this.controlerFacilities.getFacilities().values()) {
			if (af.getCoord().getX() < minx) { minx = af.getCoord().getX(); }
			if (af.getCoord().getY() < miny) { miny = af.getCoord().getY(); }
			if (af.getCoord().getX() > maxx) { maxx = af.getCoord().getX(); }
			if (af.getCoord().getY() > maxy) { maxy = af.getCoord().getY(); }
		}
		minx -= 1.0; miny -= 1.0; maxx += 1.0; maxy += 1.0;
		log.info("Min x = " + minx );
		log.info("Min y = " + miny );
		log.info("Max x = " + maxx );
		log.info("Max y = " + maxy );
		/*minx= minx*0.75;
		miny= miny*0.75;
		maxx= maxx*0.75;
		maxy= maxy*0.75;*/
		double x_width = (maxx - minx)/n;
		double y_width = (maxy - miny)/n;
		int a = 0;
		int i = 0;
		
		while (i<n) {
			int j = 0;
			while (j<n) {
				Id id = new IdImpl (a);
				double x1= minx + i*x_width;
				double x2= x1 + x_width;
				double y1= miny + j*y_width;
				double y2= y1 + y_width;
				RetailZone rz = new RetailZone (id, x1, y1, x2, y2);
				for (Person p : persons.values() ) { //TODO think if it is not better to put the following in a separate method
					// like it is now it is not needed to go through all zones again in order to assign them persons and shops
					// the other way is probably cleaner and this part below doesn't need to appear twice
					Coord c = this.controlerFacilities.getFacilities().get(((PlanImpl) p.getSelectedPlan()).getFirstActivity().getFacilityId()).getCoord();
					if (c.getX()< x2 && c.getX()>=x1 && c.getY()<y2 && c.getY()>=y1) { 
						rz.addPersonToQuadTree(c,p);
					}		
				} 
				for (ActivityFacility af : shops.values()) {
					Coord c = af.getCoord();
					if (c.getX()< x2 & c.getX()>=x1 & c.getY()<y2 & c.getY()>=y1) {
						rz.addShopToQuadTree(c,af);
					}
				}	
				this.retailZones.addRetailZone(rz);
				a=a+1;
				j=j+1;
			}
			i=i+1;
		} 
	}
  
  	public void processPerson() {
		this.counter++;
		if (this.counter == this.nextCounterMsg) {
			this.nextCounterMsg *= 2;
			printEventsCount();
		}
	}

	private void printEventsCount() {
		log.info(" Person # " + this.counter +" have been processed" );
		Gbl.printMemoryUsage();
	}

	  public Map<Id,ActivityFacilityImpl> getScenarioShops () {
		  return this.shops;
	  }
	  public RetailZones getRetailZones() {
		  return this.retailZones;
	  }
	  public boolean setBetas (double [] betas){
		  this.betas=betas;
		  return true;
	  }
	  
	  public boolean setFirst (TreeMap<Integer, String> first){
		  this.first=first;
		  return true;
	  }
}
