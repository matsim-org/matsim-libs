package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.population.PersonImpl;

import playground.ciarif.retailers.data.Consumer;
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
  private Controler controler;
  private ArrayList<ActivityFacility> retailers_shops = new ArrayList<ActivityFacility>();
  private ArrayList<ActivityFacility> shops;
  private ArrayList<Consumer> consumers;
  private Collection<ActivityFacility> controlerFacilities;
  private Map<Id, PersonImpl> persons;
  private RetailZones retailZones;
  private ArrayList<ActivityFacility> sampledShops;

  public GravityModel(Controler controler, double[] b, ArrayList<ActivityFacility> retailers_shops, ArrayList<Consumer> consumers)
  {
    this.betas = b;
    this.controler = controler;
    this.retailers_shops = retailers_shops;
    this.controlerFacilities = controler.getFacilities().getFacilities().values();
    this.shops = this.findScenarioShops(this.controlerFacilities);
    this.consumers = consumers;
    this.persons = (controler.getPopulation().getPersons());
    
  }
  public void init() {
	  
	String type_of_partition = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_PARTITION);
	int number_of_zones =0;
	int n = (int)Double.parseDouble(controler.getConfig().findParam(CONFIG_GROUP,CONFIG_ZONES));
	if (type_of_partition.equals("symmetric")){
		number_of_zones = (int) Math.pow(n,2);}
	else {throw new RuntimeException("In config file, param = "+CONFIG_ZONES+" in module = "+CONFIG_GROUP+" at the moment can only take the value 'symmetric'!"); }
	//TODO Define the asymmetric version, at the moment only the symmetric is possible
	if (number_of_zones == 0) { throw new RuntimeException("In config file, param = "+CONFIG_ZONES+" in module = "+CONFIG_GROUP+" not defined!");}

	double samplingRateShops = 1;
	double samplingNumberShops = 1;
	boolean zoneBasedSampling = true;
	if (controler.getConfig().findParam(CONFIG_GROUP, CONFIG_SAMPLE__TYPE_SHOPS).equals("zoneBasedSampling")) {
		samplingNumberShops = Double.parseDouble(controler.getConfig().findParam(CONFIG_GROUP, CONFIG_SAMPLE_NUMBER_SHOPS));
		if (samplingRateShops>1 || samplingRateShops<0) { throw new RuntimeException("In config file, param = "+CONFIG_SAMPLE__TYPE_SHOPS+" in module = "+CONFIG_GROUP+" must be set to a value between 0 and 1!!!");}
	}
	else if (controler.getConfig().findParam(CONFIG_GROUP, CONFIG_SAMPLE__TYPE_SHOPS).equals("randomSampling")){
	}
	else { } //TODO put a warning here
	double samplingRatePersons = 1;
	if (controler.getConfig().findParam(CONFIG_GROUP, CONFIG_SAMPLE_PERSONS) != null) {
		samplingRatePersons = Double.parseDouble(controler.getConfig().findParam(CONFIG_GROUP, CONFIG_SAMPLE_PERSONS));
		if (samplingRatePersons>1 || samplingRatePersons<0) { throw new RuntimeException("In config file, param = "+CONFIG_SAMPLE_PERSONS+" in module = "+CONFIG_GROUP+" must be set to a value between 0 and 1!!!");}
	}
	else {} //TODO put a warning here
}

	
  
  public double computePotential(ArrayList<Integer> solution){
    double global_likelihood = 0;
    int a = 0;
    for (ActivityFacility c : this.retailers_shops) {
      Coord coord = this.controler.getNetwork().getLink(((Integer)solution.get(a)).toString()).getCoord();
      ++a;
      double loc_likelihood = 0.0D;
      for (PersonImpl p : this.controler.getPopulation().getPersons().values()) {
        double pers_sum_potential = 0.0D;
        double pers_potential = 0.0D;
        double pers_likelihood = 0.0D;
        double dist1 = 0.0D;

        if (p.getSelectedPlan().getFirstActivity().getFacility().calcDistance(coord) == 0.0D) dist1 = 10.0D;
        else {
          dist1 = p.getSelectedPlan().getFirstActivity().getFacility().calcDistance(coord);
        }
        pers_potential = Math.pow(dist1, this.betas[0]) + Math.pow(c.getActivityOption("shop").getCapacity().doubleValue(), this.betas[1]);

        for (ActivityFacility s : this.shops) {
          double dist = 0.0D;

          if (this.retailers_shops.contains(s)){
        	  
            int index = this.retailers_shops.indexOf(s);
            Coord coord1 = this.controler.getNetwork().getLink(((Integer)solution.get(index)).toString()).getCoord();
            if (p.getSelectedPlan().getFirstActivity().getFacility().calcDistance(coord1) == 0.0D) {
            	dist = 10.0D;
            }
            else {
              dist = p.getSelectedPlan().getFirstActivity().getFacility().calcDistance(coord1);
            }

          }
          else if (s.calcDistance(p.getSelectedPlan().getFirstActivity().getCoord()) == 0.0D) {
        	  dist = 10.0D;
          } 
          
          else {
            dist = s.calcDistance(p.getSelectedPlan().getFirstActivity().getCoord());
          }

          double potential = Math.pow(dist, this.betas[0]) + Math.pow(s.getActivityOption("shop").getCapacity().doubleValue(), this.betas[1]);

          pers_sum_potential += potential;
        }

        pers_likelihood = pers_potential / pers_sum_potential;

        loc_likelihood += pers_likelihood;
      }

      global_likelihood += loc_likelihood;
    }
    return global_likelihood;
  }
  
  private ArrayList<ActivityFacility> findScenarioShops (Collection<ActivityFacility> controlerFacilities) {
	  
		ArrayList<ActivityFacility> shops = new ArrayList<ActivityFacility>();
		for (ActivityFacility f : controlerFacilities) {
			if (f.getActivityOptions().entrySet().toString().contains("shop")) {
				shops.add(f);
				log.info("The shop " + f.getId() + " has been added to the file 'shops'");
			}
			else {}
		}
		return shops;
	}
  private void createZones (Collection<PersonImpl> persons, ArrayList<ActivityFacility> controlerFacilities, int n, double samplingRatePersons, double samplingRateShops) {
		
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (PersonImpl p : persons) {
			if (p.getSelectedPlan().getFirstActivity().getCoord().getX() < minx) { minx = p.getSelectedPlan().getFirstActivity().getCoord().getX(); }
			if (p.getSelectedPlan().getFirstActivity().getCoord().getY() < miny) { miny = p.getSelectedPlan().getFirstActivity().getCoord().getY(); }
			if (p.getSelectedPlan().getFirstActivity().getCoord().getX() > maxx) { maxx = p.getSelectedPlan().getFirstActivity().getCoord().getX(); }
			if (p.getSelectedPlan().getFirstActivity().getCoord().getY() > maxy) { maxy = p.getSelectedPlan().getFirstActivity().getCoord().getY(); }
		}
		for (ActivityFacility shop : shops) {
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
				RetailZone rz = new RetailZone (id, x1, y1, x2, y2, samplingRateShops, samplingRatePersons);
				for (PersonImpl p : persons ) {
					Coord c = p.getSelectedPlan().getFirstActivity().getFacility().getCoord();
					if (c.getX()< x2 && c.getX()>=x1 && c.getY()<y2 && c.getY()>=y1) { 
						rz.addPersonToQuadTree(c,p);
					}		
				} 
				for (ActivityFacility af : shops) {
					Coord c = af.getCoord();
					if (c.getX()< x2 & c.getX()>=x1 & c.getY()<y2 & c.getY()>=y1) {
						rz.addShopToQuadTree(c,af);
					}
				}	
				this.retailZones.addRetailZone(rz);
				log.info("In the zone " + rz.getId() + ", " + rz.getSampledShops().size() + " have been sampled");
				this.sampledShops.addAll(rz.getSampledShops());
				a=a+1;
				j=j+1;
			}
			i=i+1;
		}
	}
}
