package playground.acmarmol.Avignon;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonSubTourExtractor extends AbstractPersonAlgorithm implements PlanAlgorithm {
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//private final Persons persons;
	private TreeMap<Id, ArrayList<SubtourInfo>> subtoursInfo = new TreeMap<Id, ArrayList<SubtourInfo>>();
	private TreeMap<Integer, ArrayList<Integer>> subtours = new TreeMap<Integer,ArrayList<Integer>>();
	private int subtour_idx;;
	private Person person;
	private ArrayList<SubtourInfo> personSubtours = new ArrayList<SubtourInfo>();
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSubTourExtractor() {
		System.out.println("    init " + this.getClass().getName() + " module...");
		//this.persons = persons;
		System.out.println("    done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	
	 private final boolean checkLeafs (Plan plan,TreeMap<Integer, ArrayList<Integer>> subtours, int subtour_idx) {
		boolean last_leaf = false;
		ArrayList<Integer> last_subtour = new ArrayList<Integer>();
		last_subtour = subtours.get(subtour_idx);
		if (last_subtour.contains(plan.getPlanElements().size()-1) && last_subtour.contains(0)){
			last_leaf = true;
		}
		return last_leaf;
	}
	 
	 private final ArrayList<Integer> removeSubTour (int start, int end,ArrayList<Integer> tour) {
			for (int i=0; i<(end-start); i=i+1){
				tour.remove(start+1);
			}
			return tour;
	 }
	
	private final TreeMap<Integer, ArrayList<Integer>> registerSubTour (Plan plan, ArrayList<Integer> start_end, ArrayList<Integer> tour, int subtour_idx, TreeMap<Integer, ArrayList<Integer>> subtours){
		ArrayList<Integer> subtour = new ArrayList<Integer>();
		int start = start_end.get(0);
		int end = start_end.get(1);
		StringBuilder actSequence = new StringBuilder();
		
		for (int i=start; i<=end; i=i+1){
			 subtour.add(tour.get(i));
			 Activity act = (Activity) plan.getPlanElements().get(tour.get(i));
			 actSequence.append(act.getType());
		}
				
		this.personSubtours.add(new SubtourInfo(actSequence.toString(), subtour));
		subtours.put(subtour_idx,subtour);
		return subtours;
	}
	
	private final ArrayList<Integer> extractSubTours(Plan plan, int start, int end, ArrayList<Integer> tour) {
		boolean is_leaf = false;
		int i=0;
		int leaf_start = start;
		int leaf_end = end;
		TreeMap<Integer,ActivityImpl> acts = new	TreeMap<Integer,ActivityImpl>();
		ActivityImpl act0 = ((ActivityImpl)plan.getPlanElements().get(tour.get(start)));
		acts.put(0,act0);
		while (is_leaf == false && i<=tour.size()-2){
			i=i+1;
			ActivityImpl acti = ((ActivityImpl)plan.getPlanElements().get(tour.get(i)));
			
			for (int j=Math.max(i-2,0);j>=tour.get(start);j=j-1){
				ActivityImpl actj = (ActivityImpl)plan.getPlanElements().get(tour.get(j));
				if ((acti.getCoord().getX() == actj.getCoord().getX()) &&
					    (acti.getCoord().getY() == actj.getCoord().getY())){
					is_leaf=true;
					leaf_start = j;
					leaf_end = i; 	
					break;
				}
			}//end for
			
			acts.put(i, acti);
		}
		ArrayList<Integer> start_end = new ArrayList<Integer>();
		start_end.add(0,leaf_start);
		start_end.add(1,leaf_end);
		return start_end;
	}
		
	private final void registerPlan (Plan plan, ArrayList<Integer> tour) {
		for (int i=0; i<=plan.getPlanElements().size()-1;i=i+2) {
			tour.add(i);
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		
	
		this.personSubtours = new ArrayList<SubtourInfo>();
		this.person = person;
		Plan plan = person.getSelectedPlan();
		ArrayList<Integer> tour = new ArrayList<Integer>();
		ArrayList<Integer> start_end = new ArrayList<Integer>();
		boolean all_leafs = false;		
		this.registerPlan (plan,tour);
				
		while (all_leafs == false){
			start_end = this.extractSubTours(plan,0, tour.size()-1,tour);
			subtours = this.registerSubTour(plan,start_end,tour,subtour_idx,subtours);
			all_leafs = this.checkLeafs(plan, subtours,subtour_idx);
			subtour_idx = subtour_idx+1;
			this.removeSubTour(start_end.get(0),start_end.get(1), tour);
		
		}
		
	
		this.subtoursInfo.put(person.getId(), this.personSubtours);

		//System.out.println("subtours end = " + subtours);
	}
	
	public void run(Plan plan){
	}
	
	//////////////////////////////////////////////////////////////////////
	// Get methods
	//////////////////////////////////////////////////////////////////////
	
	public TreeMap<Integer,ArrayList<Integer>> getSubtours() {
		return this.subtours;
	}
	
	public TreeMap<Id, ArrayList<SubtourInfo>> getPopulationSubtours() {
		return this.subtoursInfo;
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


