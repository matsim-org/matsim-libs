package playground.ciarif.retailers;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;

import playground.ciarif.retailers.RetailerGA.RunRetailerGA;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class GravityModelRetailerStrategy {

	public static final String NAME = "gravityModelRetailerStrategy";
	private final static Logger log = Logger.getLogger(RetailersSequentialLocationListener.class);
	//private DenseDoubleMatrix2D prob_i_j;
	//private ArrayList<Consumer> consumers;
	private Controler controler;
	private ArrayList<ActivityFacility> shops;
	private RetailZones retailZones;
	private ArrayList<ActivityFacility> facilities;
	private ArrayList<LinkRetailersImpl> links;

	public GravityModelRetailerStrategy(Controler controler, RetailZones retailZones, ArrayList<ActivityFacility> shops, ArrayList<ActivityFacility> facilities, ArrayList<LinkRetailersImpl> links) {
		
		this.controler = controler;
		this.shops = shops;
		this.retailZones = retailZones;
		this.facilities = facilities;
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
				Collection<PersonImpl> persons = new ArrayList<PersonImpl> ();
				rz.getPersonsQuadTree().get(rz.getPersonsQuadTree().getMinEasting(),rz.getPersonsQuadTree().getMinNorthing(), rz.getPersonsQuadTree().getMaxEasting(), rz.getPersonsQuadTree().getMaxNorthing(), persons );
				
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
		
		ComputeGravityModelParameters cgmp = new ComputeGravityModelParameters ();
		double [] b= cgmp.computeParameters (controler, prob_i_j, consumers, shops); //TODO think better what should be kept here and what should be passed in the constructor
		RunRetailerGA rrGA = new RunRetailerGA();
		
		GravityModel gm = new GravityModel(controler, b, facilities, consumers, shops);
		ArrayList<Integer> solution = rrGA.runGA(this.createInitialLocationsForGA(),gm);
		log.info("The optimized solution is: " + solution);
		//TODO here the facilities should be moved
	}
	
	private ArrayList<Integer> createInitialLocationsForGA () {
		
		ArrayList<Integer> locations = new ArrayList<Integer> ();
		
		for (ActivityFacility af:facilities){
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



	public void getMovedFacilities() {
		// TODO Auto-generated method stub
		
	}
}
