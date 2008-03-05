package playground.ciarif.models.subtours;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.BasicAct;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.identifiers.IdI;


import playground.balmermi.census2000.data.Persons;

public class PersonSubTourExtractor extends PersonAlgorithm implements PlanAlgorithmI {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final Persons persons;
	private TreeMap<Integer, ArrayList<Integer>> subtours = new TreeMap<Integer,ArrayList<Integer>>();
	private int subtour_idx;;
	 
		
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSubTourExtractor(final Persons persons) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.persons = persons;
		System.out.println("    done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	
	 private final boolean checkLeafs (Plan plan,TreeMap<Integer, ArrayList<Integer>> subtours, int subtour_idx) {
		boolean last_leaf = false;
		ArrayList<Integer> last_subtour = new ArrayList<Integer>();
		last_subtour = subtours.get(subtour_idx);
		if (last_subtour.contains(plan.getActsLegs().size()-1) && last_subtour.contains(0)){
			last_leaf = true;
		}
		return last_leaf;
	}
	 
	 private final ArrayList<Integer> removeSubTour (int start, int end,ArrayList<Integer> tour) {
			for (int i=0; i<(end-start); i=i+1){
				tour.remove(start+1);
				System.out.println("tour " + tour);
			}
			return tour;
	 }
	
	private final TreeMap<Integer, ArrayList<Integer>> registerSubTour (Plan plan, ArrayList<Integer> start_end, ArrayList<Integer> tour, int subtour_idx, TreeMap<Integer, ArrayList<Integer>> subtours){
		ArrayList<Integer> subtour = new ArrayList<Integer>();
		int start = start_end.get(0);
		int end = start_end.get(1);
		for (int i=start; i<=end; i=i+1){
			 subtour.add(tour.get(i));
		}
		subtours.put(subtour_idx,subtour);
		return subtours;
	}
	
	private final ArrayList<Integer> extractSubTours(Plan plan, int start, int end, ArrayList<Integer> tour) {
		boolean is_leaf = false;
		int i=0;
		int leaf_start = start;
		int leaf_end = end;
		TreeMap<Integer,Act> acts = new	TreeMap<Integer,Act>();
		Act act0 = ((Act)plan.getActsLegs().get(tour.get(start)));
		acts.put(0,act0);
		while (is_leaf == false && i<=tour.size()-2){
			i=i+1;
			Act acti = ((Act)plan.getActsLegs().get(tour.get(i)));
			for (int j=i-1;j>=tour.get(start);j=j-1){
				Act actj = (Act)plan.getActsLegs().get(tour.get(j));
				if ((acti.getCoord().getX() == actj.getCoord().getX()) &&
					    (acti.getCoord().getY() == actj.getCoord().getY())){
					is_leaf=true;
					leaf_start = j;
					leaf_end = i; 	
					break;
				}
			}
			acts.put(i, acti);
		}
		ArrayList<Integer> start_end = new ArrayList<Integer>();
		start_end.add(0,leaf_start);
		start_end.add(1,leaf_end);
		return start_end;
	}
		
	private final void registerPlan (Plan plan, ArrayList<Integer> tour) {
		for (int i=0; i<=plan.getActsLegs().size()-1;i=i+2) {
			tour.add(i);
		}
	}
	//TODO@ !!!!!  Così probabilmente non basta, va fatto ancora qualcosa perchè funzioni bene anche per estrarre i subtour
	// quando non si vuole stimare il mode choice
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
				
		Plan plan = person.getSelectedPlan();
		//int subtour_idx =0;
		ArrayList<Integer> tour = new ArrayList<Integer>();
		ArrayList<Integer> start_end = new ArrayList<Integer>();
		boolean all_leafs = false;		
		//TreeMap<Integer, ArrayList<Integer>> subtours = new TreeMap<Integer,ArrayList<Integer>>();
		this.registerPlan (plan,tour);
		
		while (all_leafs == false){
			start_end = this.extractSubTours(plan,0, tour.size()-1,tour);
			subtours = this.registerSubTour(plan,start_end,tour,subtour_idx,subtours);
			all_leafs = this.checkLeafs(plan, subtours,subtour_idx);
			subtour_idx = subtour_idx+1;
			this.removeSubTour(start_end.get(0),start_end.get(1), tour);
		}
		//chiamare qui un metodo che faccia le seguenti cose: riordini i subtours, definisca chi è il precedente di chi,
		//dia un id ragionevole ai subtours
		System.out.println("subtours end = " + subtours);
		//writeSubTours ("output/output_persons_subtours.txt"); 
	}
	
	public void run(Plan plan){
		}
	
	//////////////////////////////////////////////////////////////////////
	// Get methods
	//////////////////////////////////////////////////////////////////////
	
	public TreeMap<Integer,ArrayList<Integer>> getSubtours() {
		return this.subtours;
	}
	
	public int getSubtourIdx() {
		return this.subtour_idx;
	}
	
	//////////////////////////////////////////////////////////////////////
	// Write methods
	//////////////////////////////////////////////////////////////////////

	public void writeSubTours (String outfile) {
	
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}	
	}
}


