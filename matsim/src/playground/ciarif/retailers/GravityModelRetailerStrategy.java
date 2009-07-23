package playground.ciarif.retailers;

import java.util.ArrayList;
//import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;

import playground.ciarif.retailers.RetailerGA.RunRetailerGA;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class GravityModelRetailerStrategy {

	public static final String NAME = "gravityModelRetailerStrategy";
	private final static Logger log = Logger.getLogger(RetailersSequentialLocationListener.class);
	private Controler controler;
	private ArrayList<ActivityFacility> shops;
	private RetailZones retailZones;
	private ArrayList<ActivityFacility> retailersFacilities;
	private ArrayList<LinkRetailersImpl> links;

	public GravityModelRetailerStrategy(Controler controler, RetailZones retailZones, ArrayList<ActivityFacility> shops, ArrayList<ActivityFacility> facilities, ArrayList<LinkRetailersImpl> links) {
		
		this.controler = controler;
		this.shops = shops;
		this.retailZones = retailZones;
		this.retailersFacilities = facilities;
		this.links = links;
	}
	
	
	
	public void moveFacilities() {
		ArrayList<Consumer> consumers = new ArrayList<Consumer>();
		// TODO could try to use a double ""for" cycle in order to avoid to use the getIteration method
		// the first is 0...n where n is the number of times the gravity model needs to 
		// be computed, the second is 0...k, where k is the number of iterations needed 
		// in order to obtain a relaxed state, or maybe use a while.
		//int iter = controler.getIteration();
		//if (controler.getIteration()>0 & controler.getIteration()%5==0){
		
		DenseDoubleMatrix2D prob_i_j = new DenseDoubleMatrix2D (this.retailZones.getRetailZones().values().size(),shops.size());
		int consumer_count=0;
		int j=0;
		boolean first_shop = true;
		for (ActivityFacility f:shops) {
			
			//double sum_prob =0;
			double  zone_count =0;
			// gets the average probability of a 
			for (RetailZone rz : this.retailZones.getRetailZones().values()) {
				zone_count++;
				double counter = 0;
				double prob = 0;
				ArrayList<PersonImpl> persons = rz.getSampledPersons();
				
				for (PersonImpl p:persons) {
					
					if (first_shop) {
						Consumer consumer = new Consumer (consumer_count, p, rz.getId());
						consumers.add(consumer);
						consumer_count++;
					}
					for (PlanElement pe2 : p.getSelectedPlan().getPlanElements()) {
						
						if (pe2 instanceof ActivityImpl) {
							ActivityImpl act = (ActivityImpl) pe2;
							
							if (act.getType().equals("shop") && act.getFacility().getId().equals(f.getId())) {
								counter++;
								int i =Integer.parseInt(rz.getId().toString());
								prob = counter/persons.size();
								prob_i_j.set(i,j,prob);
							}
						}
					}
				}
			}
			
			first_shop=false;
			j=j+1;
		}	

					
		log.info("prob_i_j = " + prob_i_j);
		double [] b= this.computeParameters (prob_i_j, consumers); //TODO think better what should be kept here and what should be passed in the constructor
		RunRetailerGA rrGA = new RunRetailerGA();
		
		GravityModel gm = new GravityModel(controler, b, retailersFacilities, consumers, shops);
		ArrayList<Integer> solution = rrGA.runGA(this.createInitialLocationsForGA(),gm);
		log.info("The optimized solution is: " + solution);
		//TODO here the facilities should be moved
	}
	
	private ArrayList<Integer> createInitialLocationsForGA () {
		
		ArrayList<Integer> locations = new ArrayList<Integer> ();
		
		for (ActivityFacility af:retailersFacilities){
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
	
	public double[] computeParameters(DenseDoubleMatrix2D prob_zone_shop, ArrayList<Consumer> consumers) {	
		
		
		int number_of_consumers = consumers.size();
		int number_of_shops = prob_zone_shop.columns();
		log.info("This scenario has " + shops.size() + " shops " + consumers.size() + " consumers and " + number_of_shops + " zones");
	    DenseDoubleMatrix1D prob_pers_shop = new DenseDoubleMatrix1D(number_of_consumers * number_of_shops);
	    if (prob_pers_shop !=null) {log.info(" the prob_pers_shop matrix has been created");}
	    DenseDoubleMatrix1D regressand_matrix = new DenseDoubleMatrix1D(number_of_consumers * number_of_shops);
	    if (regressand_matrix !=null) {log.info(" the regressand matrix has been created");}
	    DenseDoubleMatrix2D variables_matrix = new DenseDoubleMatrix2D(number_of_consumers * number_of_shops, 2);
	    if (variables_matrix !=null) {log.info(" the variables matrix has been created");}
	    DenseDoubleMatrix1D dist_pers_shop = new DenseDoubleMatrix1D(number_of_consumers * number_of_shops);
	    if (dist_pers_shop !=null) {log.info(" the distance persons-shops matrix has been created");}
	    DenseDoubleMatrix1D dim_shop = new DenseDoubleMatrix1D(number_of_consumers * number_of_shops);
	    if (dim_shop !=null) {log.info(" the shop dimension matrix has been created");}
	    log.info(" The matrix prob_zone_shop has dimensions = " + prob_zone_shop.rows() + "," + prob_zone_shop.columns());
	    int cases = 0;
	    for (Consumer c : consumers) {
	      double prob_sum = 0.0D;
	      double dist_sum = 0.0D;
	      double dim_sum = 0.0D;
	      
	      for (int i = 0; i <= prob_zone_shop.columns() - 1; ++i)
	      {
	        double prob = prob_zone_shop.get(Integer.parseInt(c.getRzId().toString()), i);
	        double dist = ((ActivityFacility)shops.get(i)).getActivityOption("shop").getFacility().calcDistance(c.getPerson().getSelectedPlan().getFirstActivity().getCoord());
	        if (dist == 0.0D) {
	          dist = 10.0D;
	          cases = cases+1;  
	        }
	        
	        double dimension = ((ActivityFacility)shops.get(i)).getActivityOption("shop").getCapacity().doubleValue();
	        prob_pers_shop.set(Integer.parseInt(c.getId().toString()) * number_of_shops + i, prob);
	        prob_sum += prob;
	        dist_pers_shop.set(Integer.parseInt(c.getId().toString())* number_of_shops + i, dist);
	        dist_sum += dist;
	        dim_shop.set(Integer.parseInt(c.getId().toString())* number_of_shops + i, dimension);
	        dim_sum += dimension;
	      }
	      
	      double avg_prob_pers_shop = prob_sum / prob_zone_shop.columns();
	      double avg_dist_pers_shop = dist_sum / prob_zone_shop.columns();
	      double avg_dim_shop = dim_sum / prob_zone_shop.columns();

	      for (int j = 0; j <= prob_zone_shop.columns() - 1; ++j) {
	        int k = Integer.parseInt(c.getId().toString()) * prob_zone_shop.columns();
	        log.info("k = " + k);
	        log.info("j = " + j);
	        regressand_matrix.set(k + j, prob_pers_shop.get(k + j) / avg_prob_pers_shop);

	        variables_matrix.set(k + j, 0, Math.log(dist_pers_shop.get(k + j) / avg_dist_pers_shop));
	        variables_matrix.set(k + j, 1, Math.log(dim_shop.get(k + j) / avg_dim_shop));
	      }
	    }
	    log.info("A 'zero distance' has been detected and modified, in " + cases + " cases");
	    //OLSMultipleLinearRegression olsmr = new OLSMultipleLinearRegression();
	        
	    //olsmr.newSampleData(regressand_matrix.toArray(), variables_matrix.toArray());
	    double[] b = {-1, 0.04};
	    //double[] b = olsmr.estimateRegressionParameters();
	    log.info("Betas = " + b[0] + " " + b[1]);

	    return b;
	  }

	public void getMovedFacilities() {
		// TODO Auto-generated method stub
		
	}
}
