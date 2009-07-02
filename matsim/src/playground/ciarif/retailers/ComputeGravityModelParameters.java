package playground.ciarif.retailers;

import java.util.ArrayList;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.matsim.core.api.experimental.population.Activity;
import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.facilities.Facility;
import org.matsim.core.population.PersonImpl;

import playground.gregor.demandmodeling.DistanceCalculator;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class ComputeGravityModelParameters {
	
	private final static Logger log = Logger.getLogger(MaxLinkRetailerStrategy.class);
	private Vector Beta;
	private Vector Alfa;
	private Vector X;
	private Vector Prob;
	
	void computeProbability () {
		
		
		
	}
	void computeAverages () {
		
	}
	
	void computeDistance () {
		
	}
	
	void computeBetas () {
		
	}
	public void computeInitialParameters(Controler controler, DenseDoubleMatrix2D prob_zone_shop, ArrayList<Consumer> consumers, ArrayList<ActivityFacility> shops ) {
		// TODO The prob_zone_shop Matrix and the one with probabilities which will be produced in this method
		// has the same number of columns. The idea is to go through all consumers and assign them the probability corresponding to 
		// the same column.
		DenseDoubleMatrix2D prob_pers_shop = new DenseDoubleMatrix2D(consumers.size(), prob_zone_shop.columns());
		DenseDoubleMatrix1D avg_prob_pers_shop = new DenseDoubleMatrix1D(consumers.size());
		DenseDoubleMatrix2D dist_pers_shop = new DenseDoubleMatrix2D(consumers.size(), prob_zone_shop.columns());
		DenseDoubleMatrix1D avg_dist_pers_shop = new DenseDoubleMatrix1D(consumers.size());
		DenseDoubleMatrix2D dim_shop = new DenseDoubleMatrix2D(consumers.size(), prob_zone_shop.columns());
		DenseDoubleMatrix1D avg_dim_pers_shop = new DenseDoubleMatrix1D(consumers.size());
		log.info (" The matrix prob_zone_shop has dimensions = " + prob_zone_shop.rows() + "," + prob_zone_shop.columns());
		log.info("The matrix prob_pers_shop has " + prob_pers_shop.rows() + " rows and "  + prob_pers_shop.columns() + " columns ");
		for (Consumer c:consumers) {
			double prob_sum=0;
			double dist_sum=0;
			double dim_sum=0;
			
			for (int i=0; i<=prob_pers_shop.columns()-1; i++){
			
				double prob = prob_zone_shop.get(Integer.parseInt((c.getRzId()).toString()), i);
				double dist = shops.get(i).getActivityOption("shop").getFacility().calcDistance(c.getPerson().getSelectedPlan().getFirstActivity().getCoord()); // TODO check if everything is correct in this computation and in the following ones!!!!!
				double dimension = shops.get(i).getActivityOption("shop").getCapacity();
				prob_pers_shop.set(Integer.parseInt((c.getId()).toString()), i, prob);
				prob_sum = prob_sum + prob;
				dist_pers_shop.set (Integer.parseInt((c.getId()).toString()), i, dist);
				dist_sum = dist_sum + dist;
				dim_shop.set(Integer.parseInt((c.getId()).toString()), i, dimension);
				dim_sum= dim_sum + dimension;
				//log.info("The probability that the consumer " + c.getId() + " Living in the Retail Zone " + c.getRzId() +" goes in the shop " + i + " is " + prob_pers_shop.get(Integer.parseInt((c.getId()).toString()), i));
			}
			avg_prob_pers_shop.set(Integer.parseInt((c.getId()).toString()), prob_sum/prob_pers_shop.columns());
			avg_dist_pers_shop.set(Integer.parseInt((c.getId()).toString()), dist_sum/prob_pers_shop.columns());
			avg_dim_pers_shop.set(Integer.parseInt((c.getId()).toString()), dim_sum/prob_pers_shop.columns());
		}
		log.info(prob_pers_shop);
		log.info(avg_prob_pers_shop);
	}
}
