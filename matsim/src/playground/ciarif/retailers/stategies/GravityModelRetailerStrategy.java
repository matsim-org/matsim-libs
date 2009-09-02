package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
//mport playground.ciarif.retailers.IO.WriteRetailersMatrices;
import playground.ciarif.retailers.RetailerGA.RunRetailerGA;
import playground.ciarif.retailers.data.Consumer;
import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.retailers.data.RetailZone;
import playground.ciarif.retailers.data.RetailZones;
import playground.ciarif.retailers.models.GravityModel;
import playground.ciarif.retailers.utils.Utils;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
//import playground.ciarif.retailers.utils.Utils;
//import playground.jjoubert.Utilities.DateString;
//import playground.ciarif.retailers.IO.WriteRetailersMatrices;

public class GravityModelRetailerStrategy implements RetailerStrategy { //TODO check the arguments that are passed from this class to 
	//the GravityModel class, in particular if it is really necessary to pass the controler. 

	//TODO have a look to variables here, it shouldn't happen that field here are the same as in the GravityModel class
	public static final String NAME = "gravityModelRetailerStrategy";
	private final static Logger log = Logger.getLogger(GravityModelRetailerStrategy.class);
	private Controler controler;
	private Map<Id,ActivityFacilityImpl> shops;
	private RetailZones retailZones;
	private Map<Id, ActivityFacilityImpl> retailerFacilities;
	private Map<Id, ActivityFacilityImpl> movedFacilities = new TreeMap<Id, ActivityFacilityImpl>();

	public GravityModelRetailerStrategy(Controler controler) { //TODO think better if it is necessary to give to all strategies the controler
		
		this.controler = controler;
	}
	
	private ArrayList<Integer> createInitialLocationsForGA (ArrayList<LinkRetailersImpl> links) {
		
		ArrayList<Integer> locations = new ArrayList<Integer> ();
		
		for (ActivityFacilityImpl af:retailerFacilities.values()){
			locations.add(Integer.parseInt(af.getLink().getId().toString()));
		}
		
		for (LinkRetailersImpl l:links) {
			if (locations.contains(Integer.parseInt(l.getId().toString()))) {}
			else {
				locations.add(Integer.parseInt(l.getId().toString()));
			}
		}
		
		log.info("Initial Locations = " + locations);
		return locations;
		
	}
	
	private double[] computeParameters(DenseDoubleMatrix2D prob_zone_shop, ArrayList<Consumer> consumers, Map<Id, Integer> shops_keys) {	
		
		int number_of_consumers = consumers.size();
		int number_of_retailer_shops = retailerFacilities.size();
		//WriteRetailersMatrices wrm = new WriteRetailersMatrices ();
		DenseDoubleMatrix1D regressand_matrix = new DenseDoubleMatrix1D(number_of_consumers);
	    if (regressand_matrix !=null) {log.info(" the regressand matrix has been created");}
	    DenseDoubleMatrix2D variables_matrix = new DenseDoubleMatrix2D(number_of_consumers, 2);
	    if (variables_matrix !=null) {log.info(" the variables matrix has been created");}
		log.info("This retailer owns " + number_of_retailer_shops + " shops and " + consumers.size() + " consumers ");
		log.info("This scenario has "+ this.retailZones.getRetailZones().size() + " zones");
		int cases = 0;
		for (Consumer c:consumers) {
			int consumer_index = Integer.parseInt(c.getId().toString());
	    	int zone_index = Integer.parseInt(c.getRzId().toString());
	    	ActivityFacilityImpl af = c.getShoppingFacility();
    		double prob= prob_zone_shop.get(zone_index,shops_keys.get(af.getId()));
    		regressand_matrix.set(consumer_index, Math.log(prob/prob_zone_shop.viewRow(zone_index).zSum()));
    		double dist1 = (af.getActivityOption("shop").getFacility().calcDistance(c.getPerson().getSelectedPlan().getFirstActivity().getCoord()));
	        if (dist1 == 0.0D) {
	          dist1 = 10.0D;
	          cases = cases+1;
	        }
	        double sumDist = 0;
	        double sumDim = 0;
	        double dist2 = 0;
	        double dim = 0;
    		for (ActivityFacilityImpl aaff:this.shops.values()) {
    			dist2 = aaff.calcDistance(c.getPerson().getSelectedPlan().getFirstActivity().getCoord());
    			sumDist = sumDist + dist2;
    			dim = aaff.getActivityOption("shop").getCapacity().doubleValue();
    			sumDim = sumDim + dim;
    		}
    		variables_matrix.set(consumer_index, 0, Math.log(dist1/(sumDist/shops.size())));
    		variables_matrix.set(consumer_index, 1, Math.log(af.getActivityOption("shop").getCapacity().doubleValue()/(sumDim/shops.size())));
	    	//}
	    }
		
	    log.info("A 'zero distance' has been detected and modified, in " + cases + " cases");
	    //wrm.writeRetailersMatrices(prob_zone_shop, "prob_zone_shop");
	    //wrm.writeRetailersMatrices(regressand_matrix, "regressand_matrix");
	    //wrm.writeRetailersMatrices(variables_matrix, "variables_matrix");
	    OLSMultipleLinearRegression olsmr = new OLSMultipleLinearRegression();
	    olsmr.newSampleData(regressand_matrix.toArray(), variables_matrix.toArray());
	    
	    double[] b = olsmr.estimateRegressionParameters();
	    log.info("Betas = " + b[0] + " " + b[1]);

	    return b;
	  }

	public Map<Id, ActivityFacilityImpl> getMovedFacilities() {
		log.info("moved Facilities are: " + movedFacilities);
		return this.movedFacilities;		
	}

	
	public Map<Id, ActivityFacilityImpl> moveFacilities(Map<Id, ActivityFacilityImpl> retailerFacilities,ArrayList<LinkRetailersImpl> freeLinks) {
		
		this.retailerFacilities = retailerFacilities;
		GravityModel gm = new GravityModel(this.controler, retailerFacilities); 
		gm.init();
		this.shops= gm.getScenarioShops();
		this.retailZones = gm.getRetailZones();
		Map<Id,Integer> shops_keys= new TreeMap<Id,Integer>();
		ArrayList<Consumer> consumers = new ArrayList<Consumer>();
		DenseDoubleMatrix2D prob_i_j = new DenseDoubleMatrix2D (this.retailZones.getRetailZones().values().size(),shops.size());
		int consumer_count=0;
		int j=0;
		log.info("This scenario has " + shops.size() +" shops");
		for (ActivityFacilityImpl f:this.shops.values()) { 
			shops_keys.put(f.getId(),j);
			
			// gets the average probability of a person from a given zone going to a given shop 
			//(it is the same for all persons of a given zone)
			for (RetailZone rz : this.retailZones.getRetailZones().values()) {
				//zone_count++;
				double counter = 0;
				double prob = 0;
				ArrayList<PersonImpl> persons = rz.getPersons();
				
				for (PersonImpl p:persons) {
					boolean first_shop = true;
					for (PlanElement pe2 : p.getSelectedPlan().getPlanElements()) {
						
						if (pe2 instanceof ActivityImpl) {
							ActivityImpl act = (ActivityImpl) pe2;
							
							if (act.getType().equals("shop") && act.getFacility().getId().equals(f.getId())) {
								if (first_shop && this.retailerFacilities.containsKey(f.getId())) {
									Consumer consumer = new Consumer (consumer_count, p, rz.getId());
									consumer.setShoppingFacility(f);
									consumers.add(consumer);
									consumer_count++;
								}
								counter++;
								int i =Integer.parseInt(rz.getId().toString());
								prob = counter/persons.size();
								prob_i_j.set(i,j,prob);
								first_shop=false;
							}
						}
					}
				}
			}
			j=j+1;
		}	

		double [] b= this.computeParameters (prob_i_j, consumers, shops_keys);
		//double[] b = {-1, 1};
		gm.setBetas(b);
		RunRetailerGA rrGA = new RunRetailerGA();
		ArrayList<Integer> solution = rrGA.runGA(this.createInitialLocationsForGA(this.mergeLinks(freeLinks)),gm);		
		log.info("The optimized solution is: " + solution);
		int count=0;
		for (ActivityFacilityImpl af:this.retailerFacilities.values()) {
			if (solution.get(count) != Integer.parseInt(af.getLink().getId().toString())) {
				Utils.moveFacility(af,controler.getNetwork().getLink(solution.get(count).toString()),this.controler.getWorld());
				log.info("The facility " + af.getId() + " has been moved");
				this.movedFacilities.put(af.getId(), af);
				log.info("Link Id after = "+ af.getLink().getId());
				count=count+1;
			}
		}
		return movedFacilities;
	}

	private ArrayList<LinkRetailersImpl> mergeLinks(ArrayList<LinkRetailersImpl> freeLinks) {
		
		ArrayList<LinkRetailersImpl> availableLinks = new ArrayList<LinkRetailersImpl>();
		for (ActivityFacilityImpl af: this.retailerFacilities.values()){
			Id id = af.getLink().getId();
			LinkRetailersImpl link = new LinkRetailersImpl((LinkImpl)controler.getNetwork().getLink(id),controler.getNetwork());
			availableLinks.add(link);
		}
		availableLinks.addAll(freeLinks);
		return availableLinks;
	}
}
