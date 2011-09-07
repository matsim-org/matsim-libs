/*******************************************************************************
 * Copyright (C) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
/**
 * 
 */
package vrp.algorithms.clarkeAndWright;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;

import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.api.VRP;
import vrp.basics.OtherDepotActivity;
import vrp.basics.Tour;
import vrp.basics.TourActivity;
import vrp.basics.VrpUtils;


/**
 * Implements the Single-Depot Clarke&Wright (parallel version) algorithm described at 
 * http://web.mit.edu/urban_or_book/www/book/chapter6/6.4.12.html.
 * 
 * @author stefan schroeder
 *
 */
public class ClarkeAndWright {
	
	static class Saving {
		
		private double saving;
		private String from;
		private String to;
		
		Saving(String from, String to, double saving) {
			this.to = to;
			this.from = from;
			this.saving = saving;
		}
		
		double getSaving() {
			return saving;
		}
		
		String getOrigin() {
			return from;
		}
		
		String getDestination() {
			return to;
		}
		
		@Override
		public String toString(){
			return "saving={"+from+","+to+","+saving+"}";
		}
	}
	
	static class DescendingOrderSavingComparator implements Comparator<Saving> {

		@Override
		public int compare(Saving o1, Saving o2) {
			if(o1.getSaving() < o2.getSaving()){
				return 1;
			}
			else if(o1.getSaving() > o2.getSaving()){
				return -1;
			}
			else{
				return 0;
			}
		}

	}

	private final static Logger logger = Logger.getLogger(vrp.algorithms.clarkeAndWright.ClarkeAndWright.class);
	private final static String TYPE = "Clarke&Wright"; 
	private final VRP vrp;
	private PriorityQueue<Saving> savings;
	private List<Tour> tours = new ArrayList<Tour>();
	private HashMap<String,Tour> tourAssignment = new HashMap<String, Tour>();
	
	
	public ClarkeAndWright(final VRP vrp){
		this.vrp = vrp;
		int iniDimension = vrp.getCustomers().values().size();
		int initialCapacity = iniDimension*iniDimension;			
		savings = new PriorityQueue<Saving>(initialCapacity,new DescendingOrderSavingComparator());
	}
	
	public void run() {
		verifyVrp();
		computeSavings();
		buildTours();
	}
	
	private void verifyVrp() {
		if(vrp.getConstraints() == null){
			throw new IllegalStateException("no constraints set");
		}
		if(vrp.getCosts() == null){
			throw new IllegalStateException("no costs set");
		}
	}

	public Collection<Tour> getSolution(){
		return tours;
	}

	public String getType() {
		return TYPE;
	}
	
	private void computeSavings(){
		logger.info("compute savings");
		Costs costMatrix = vrp.getCosts();
		for(Customer customer_i : vrp.getCustomers().values()){
			Node from = customer_i.getLocation();
			for(Customer customer_j : vrp.getCustomers().values()){
				Node to = customer_j.getLocation();
				if(customer_i.getId().equals(customer_j.getId())){
					continue;
				}
				if(costMatrix.getCost(from,to) != null){
					Node depot = vrp.getDepot().getLocation();
					double costSaving = costMatrix.getCost(depot,from) + 
						costMatrix.getCost(to,depot) - costMatrix.getCost(from,to);
					if(costSaving>0){
						Saving saving = new Saving(customer_i.getId(),customer_j.getId(),costSaving);	
						savings.add(saving);
					}
				}
			}
		}
	}
	
	private void buildTours(){
		logger.info("build tours");
		while(savingsListNotEmpty() && notAllCustomersAssigned()){
			Saving saving = savings.poll();
			String from = saving.getOrigin();
			String to = saving.getDestination();
			
			if(bothCustomersNotAssigned(from, to)){
				Customer depot = vrp.getDepot();
				Customer customer_i = vrp.getCustomers().get(from);
				Customer customer_j = vrp.getCustomers().get(to);
				Tour tour = VrpUtils.createRoundTour(depot, customer_i, customer_j);
				if(vrp.getConstraints().judge(tour)){
					tours.add(tour);
					updateTourAssignment(tour);
				}
				continue;
			}
			else if(bothCustomersAssigned(from, to)){
				Tour tour1 = tourAssignment.get(from);
				Tour tour2 = tourAssignment.get(to);
				if(tour1 == tour2){
					continue;
				}
				if(tour1 == null || tour2 == null){
					throw new IllegalStateException("tour is null, but shouldnt be null");
				}
				if(customerIsLastCustomer(from,tour1) && customerIsFirstCustomer(to,tour2)){
					Tour tour = mergeTours(tour1,tour2);
					if(tour == null){
						continue;
					}
					else{
						if(vrp.getConstraints().judge(tour)){
							tours.remove(tour1);
							tours.remove(tour2);
							tours.add(tour);
							updateTourAssignment(tour);
						}
					}
				}
				continue;
			}
			else { //=> one customer is already assigned
				if(tourAssignment.containsKey(from)){
					Tour tour = tourAssignment.get(from);
					if(customerIsLastCustomer(from, tour)){
						Tour newTour = new Tour();
						newTour.getActivities().addAll(tour.getActivities());
						TourActivity newActivity = VrpUtils.createTourActivity(vrp.getCustomers().get(to));
						newTour.getActivities().add(newTour.getActivities().size()-1,newActivity);
						if(vrp.getConstraints().judge(newTour)){
							tours.remove(tour);
							tours.add(newTour);
							updateTourAssignment(newTour);
						}
					}
				}
				else { //=> tourAssignment.contains(to) == true
					Tour tour = tourAssignment.get(to);
					if(customerIsFirstCustomer(to, tour)){
						Tour newTour = new Tour();
						newTour.getActivities().addAll(tour.getActivities());
						TourActivity newActivity = VrpUtils.createTourActivity(vrp.getCustomers().get(from));
						newTour.getActivities().add(1,newActivity);
						if(vrp.getConstraints().judge(newTour)){
							tours.remove(tour);
							tours.add(newTour);
							updateTourAssignment(newTour);
						}
					}
				}
			}
		}
		if(notAllCustomersAssigned()){
			createAndAddShuttleTours();
		}
	}

	private boolean bothCustomersAssigned(String from, String to) {
		return tourAssignment.containsKey(from) && tourAssignment.containsKey(to);
	}

	private boolean bothCustomersNotAssigned(String from, String to) {
		return !tourAssignment.containsKey(from) && !tourAssignment.containsKey(to);
	}

	private boolean customerIsFirstCustomer(String to, Tour tour) {
		Customer firstCustomer = tour.getActivities().get(1).getCustomer();
		return firstCustomer.getId().equals(to);
	}

	private boolean customerIsLastCustomer(String from, Tour tour) {
		Customer lastCustomer = tour.getActivities().get(tour.getActivities().size()-2).getCustomer();
		return lastCustomer.getId().equals(from);
	}

	private Tour mergeTours(Tour tour1, Tour tour2) {
		Tour tour = VrpUtils.createEmptyCustomerTour();
		assertTrue(tour1,2);
		assertTrue(tour2,2);
		tour.getActivities().addAll(tour1.getActivities());
		List<TourActivity> tourActs = tour2.getActivities().subList(1, tour2.getActivities().size()-1);
		tour.getActivities().addAll(tour.getActivities().size()-1, tourActs);
		return tour;
	}

	private void assertTrue(Tour tour, int i) {
		if(tour.getActivities().size() > i){
			return;
		}
		throw new IllegalStateException("tour not valid " + tour);
		
	}

	private void updateTourAssignment(Tour tour) {
		for(TourActivity act : tour.getActivities()){
			if(act instanceof OtherDepotActivity){
				continue;
			}
			else{
				tourAssignment.put(act.getCustomer().getId(), tour);
			}
		}
		
	}

	
	private void createAndAddShuttleTours() {
		List<Customer> customersNotYetAssigned = getCustomersWithoutTour();
		for(Customer customer : customersNotYetAssigned){
			createAndAddShuttleTour(customer);
		}
	}

	private List<Customer> getCustomersWithoutTour() {
		List<Customer> customers = new ArrayList<Customer>();
		for(Customer c : vrp.getCustomers().values()){
			if(!isDepot(c)){
				if(!tourAssignment.containsKey(c.getId())){
					customers.add(c);
				}
			}
		}
		return customers;
	}

	private boolean isDepot(Customer c) {
		return c == vrp.getDepot();
	}

	private void createAndAddShuttleTour(Customer customer) {
		Tour tour = VrpUtils.createRoundTour(vrp.getDepot(), customer);
		if(vrp.getConstraints().judge(tour)){
			tours.add(tour);
			updateTourAssignment(tour);
		}
		else{
			throw new IllegalStateException("could not create shuttle tour. a job ("+tour+") exists which cannot " +
					"be assigned to a tour given the tour-constraints " + vrp.getConstraints());
		}
	}

	private boolean savingsListNotEmpty(){
		if(savings.isEmpty()){
			return false;
		}
		return true;
	}
	
	private boolean notAllCustomersAssigned(){
		if(vrp.getCustomers().values().size() - 1 != tourAssignment.size()){
			return true;
		}
		return false;
	}
	
}
