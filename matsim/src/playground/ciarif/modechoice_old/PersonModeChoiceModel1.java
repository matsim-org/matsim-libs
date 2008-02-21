package playground.ciarif.models.subtours;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.vecmath.Vector2d;

import org.matsim.basic.v01.BasicAct;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;

import playground.balmermi.census2000.data.Persons;


public class PersonModeChoiceModel1 extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String RIDE = "ride";
	private static final String PT = "pt";
	private static final String CAR = "car";
	private static final String BIKE = "bike";
	private static final String WALK = "walk";
	private static final String E = "e";
	private static final String W = "w";
	private static final String S = "s";
	private static final String H = "h";
	//private static final String L = "leisure";
	private static final Coord ZERO = new Coord(0.0,0.0);
	private final Persons persons;
	private ModelModeChoice model;
	 
		
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonModeChoiceModel1(final Persons persons) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.persons = persons;
		System.out.println("    done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	private final void handleSubTours(final Plan plan, final TreeMap<Integer, ArrayList<Integer>> subtours, int subtour_idx) {
		
		// setting subtour parameters
		if (plan == null) { Gbl.errorMsg("Person id=" + plan.getPerson().getId() + "does not have a selected plan."); }
		Iterator<BasicAct> act_it = plan.getIteratorAct();
		CoordI home_coord = null;
		CoordI work_coord = null;
		//act_it.hasNext();
		double dist_h_w = 0.0;
		while (act_it.hasNext()) {
			Act act = (Act)act_it.next();
			System.out.println("Type = " + act.getType());
			if (H.equals(act.getType())) { home_coord = act.getCoord();}
			else if (W.equals(act.getType())) { work_coord = act.getCoord(); }
			System.out.println("home = " + home_coord);
		}
		if ((home_coord == null) || (home_coord.equals(ZERO))) { Gbl.errorMsg("No home coord defined!"); }
		if ((work_coord != null) && (work_coord.equals(ZERO))) { Gbl.errorMsg("Weird work coord defined!!!"); }
		if (work_coord != null) { dist_h_w = work_coord.calcDistance(home_coord); }
		TreeMap<Integer, Integer> modeSubTours = new TreeMap<Integer, Integer>();
		
		for (int i=subtour_idx-1; i>=0; i=i-1) {
			ArrayList<Integer> subtour = subtours.get(i);
			int mainpurpose = 3; //mainpurpose:  0 := work; 1 := edu; 2 := shop 3:=leisure
			double d = 0.0;
			CoordI start = ((Act)plan.getActsLegs().get(subtour.get(0))).getCoord();
			CoordI prev = start;
			String type = null;
			for (int k=1; k<subtour.size()-1; k=k+1) { 
				type = ((Act)plan.getActsLegs().get(subtour.get(k))).getType();
				if (mainpurpose == 1){
					if (type == W) { mainpurpose = 0; break; }
				}
				else if (mainpurpose == 2) {
					if (type == W) { mainpurpose = 0; break; }
					else if (type == E) { mainpurpose = 1; }
				}
				else if (mainpurpose == 3) {
					if (type == W) {mainpurpose = 0; break; }
					else if (type == E) {mainpurpose = 1; break;}
					else if (type == S) {mainpurpose = 2;}
				} 
				CoordI curr = ((Act)plan.getActsLegs().get(subtour.get(k))).getCoord();
				d = d + curr.calcDistance(prev);
				prev = curr;
			}
			d = d/1000.0;
			
			// Defining previous mode
			int prev_mode = 0;
			//System.out.println("subtour = " + subtour);
			System.out.println("subtour start =" + subtour.get(0));
			System.out.println("mode sub tours = " + modeSubTours);
			if (subtour.get(0) != 0) {
				for (int j=subtours.size()-1; j>=0; j=j-1) {
				System.out.println("subtour per prev_mode =" + subtours.get(j));
					if (subtours.get(j).contains(subtour.get(0))) {
						System.out.println("j = " + j);
						//System.out.println("i = " + i);
						System.out.println("prev_mode = " + modeSubTours.get(j));
						prev_mode = modeSubTours.get(j); break; //Questo è sbagliato, va visto qual'è quello giusto!!!
					}
				}	
			}
			
			// choose mode choice model based on main purpose
			if (plan.getPerson().getAge() >=18) {
				if (mainpurpose == 0) {model = new ModelModeChoiceWork18Plus();}
				else if (mainpurpose == 1) {model = new ModelModeChoiceEducation18Plus();}
				else if (mainpurpose == 2) {model = new ModelModeChoiceShop18Plus();}
				else if (mainpurpose == 3) {model = new ModelModeChoiceLeisure18Plus();}
				else { Gbl.errorMsg("This should never happen!"); }
			}
			else {
				if (mainpurpose == 1) {model = new ModelModeChoiceEducation18Minus ();}
				else {model = new ModelModeChoiceOther18Minus ();}
			}
					
			// generating a random bike ownership (see STRC2007 paper Ciari for more details)
			boolean has_bike = true;
			if (Gbl.random.nextDouble() < 0.44) { has_bike = false; }	
			
			
			// setting person parameters
			System.out.println(model);
			model.setDistanceHome2Work(dist_h_w);
			model.setAge(plan.getPerson().getAge());
			//model.setHHDimension(p.getHousehold().getPersonCount());
			model.setLicenseOwnership(plan.getPerson().hasLicense());
			model.setCar(plan.getPerson().getCarAvail());
			model.setTickets(plan.getPerson().getTravelcards());
			model.setLicenseOwnership(plan.getPerson().hasLicense());
			model.setBike(has_bike);
			model.setMale (plan.getPerson().getSex());
			int udeg = 3; // TODO The program should crash here, now only an initial value is given. afterwards something like that should replace it: int udeg = start.getMunicipality().getRegType();
			model.setUrbanDegree(udeg);
			model.setMainPurpose(mainpurpose);
			model.setDistanceTour(d); // model needs meters! TODO check dimensions of distances!!!!
			model.setPrevMode(prev_mode); //TODO rivedere il posizionamento del'inizializzazione del prev_mode
			model.setHomeCoord(home_coord);
			// getting the chosen mode
			int modechoice = model.calcModeChoice();
			String mode = null;
			if (modechoice == 0) { mode = CAR; }
			else if (modechoice == 1) { mode = PT; }
			else if (modechoice == 2) { mode = RIDE; }
			else if (modechoice == 3) { mode = BIKE; }
			else if (modechoice == 4) { mode = WALK; }
			else { Gbl.errorMsg("Mode choice returns undefined value!"); }
			
			modeSubTours.put(i,modechoice);
			System.out.println("mode sub tour = " + modeSubTours);
			System.out.println("prev_mode = " + prev_mode);
			System.out.println("modechoice " +  i + " = " + modechoice);
			System.out.println("subtour= " + subtour);
			
			for (int k=1; k<subtour.size(); k=k+1){
				System.out.println("k= " + k);
				((Leg)plan.getActsLegs().get(subtour.get(k)-1)).setMode(mode);
				System.out.println("k_sub= " + (subtour.get(k)-1));
				System.out.println("leg = " + ((Leg)plan.getActsLegs().get(subtour.get(k)-1)));
			}
		}
		System.out.println("dist_h_w = " + dist_h_w);
	}
	
	private final void removeSubTour (Plan plan, TreeMap<Integer, ArrayList<Integer>> subtours, int start, int end,int subtour_idx, ArrayList<Integer> tour) {
		for (int i=0; i<(end-start); i=i+1){
			tour.remove(start+1);
			System.out.println("tour " + tour);
		}
		
		extractSubTours(plan, 0, tour.size()-1, subtour_idx, subtours,tour);
	}
	private final void registerSubTour (Plan plan, TreeMap<Integer, ArrayList<Integer>> subtours, int start, int end, int subtour_idx, ArrayList<Integer> tour) {
		
		ArrayList<Integer> subtour = new ArrayList<Integer>();
		for (int i=start; i<=end; i=i+1){
			 subtour.add(tour.get(i));
		}
		System.out.println("subtours - pre =" + subtours);
		System.out.println("subtours_idx =" + subtour_idx);
		subtours.put(subtour_idx,subtour);
		subtour_idx=subtour_idx+1;
		if (tour.get(end) == plan.getActsLegs().size()-1){
			System.out.println("subtours - end =" + subtours);
			this.handleSubTours(plan,subtours,subtour_idx);
		}
		else {
			removeSubTour(plan,subtours, start, end, subtour_idx, tour);
		}
	}

	private final void extractSubTours(Plan plan,int start, int end,int subtour_idx, TreeMap<Integer, ArrayList<Integer>> subtours, ArrayList<Integer> tour) {
		System.out.println("start_= " + start);
		System.out.println("end_= " + end);
		boolean is_leaf = false;
		Act act_start = ((Act)plan.getActsLegs().get(tour.get(start)));
		Act act_end = ((Act)plan.getActsLegs().get(tour.get(end)));
		if ((act_start.getCoord().getX() == act_end.getCoord().getX()) &&
			    (act_start.getCoord().getY() == act_end.getCoord().getY())){
			is_leaf = true;
		}
		else {
			Gbl.errorMsg("The plan is not Home-based!");
		}
		for (int i=start;i<end-1;i=i+1){ 
			System.out.println("i= " + i);
			Act acti = (Act)plan.getActsLegs().get(tour.get(i));
			for (int j=end-1;j>i;j=j-1){
				System.out.println("i= " + i);
				System.out.println("j= " + j);
				System.out.println("tour_= " + tour);
					Act actj = (Act)plan.getActsLegs().get(tour.get(j));
				if ((acti.getCoord().getX() == actj.getCoord().getX()) &&
					    (acti.getCoord().getY() == actj.getCoord().getY())) {
					is_leaf = false;
					// subtour found: start=i & end=j
					//subtour_idx=subtour_idx+1;
					extractSubTours(plan, i, j, subtour_idx, subtours,tour);
					break;
				}
			}
		}
		if (is_leaf) {
			System.out.println("leaf= " + tour);
			registerSubTour(plan, subtours, start, end, subtour_idx,tour);
		}
	}
	
	private final void registerPlan (Plan plan, ArrayList<Integer> tour) {
		for (int i=0; i<=plan.getActsLegs().size()-1;i=i+2) {
			tour.add(i);
		}
	}
		
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
				
		Plan plan = person.getSelectedPlan();
		int subtour_idx =0;
		ArrayList<Integer> tour = new ArrayList<Integer>();
		this.registerPlan (plan,tour);
				
		TreeMap<Integer, ArrayList<Integer>> subtours = new TreeMap<Integer,ArrayList<Integer>>();
		//System.out.println(subtours);
		this.extractSubTours(plan,0,tour.size()-1,subtour_idx,subtours,tour);
		//this.registerSubTour(plan, subtours,start,end,subtour_idx,tour);
//		this.removeSubTour(plan,subtours,start,end,subtour_idx,tour);
//		this.handleSubTours(plan,subtours );
		System.out.println("Funziona?");
	}
	
	public void run(Plan plan){
		}
}
