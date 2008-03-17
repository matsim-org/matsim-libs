package playground.ciarif.models.subtours;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.BasicAct;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;

public class PersonSubtourHandler {
	
	
	//////////////////////////////////////////////////////////////////////
	// Member variables
	//////////////////////////////////////////////////////////////////////
	
	private PersonSubtour pers_sub = new PersonSubtour(); //qui vanno i subtours
	private static final String E = "e";
	private static final String W = "w";
	private static final String S = "s";
	private static final String H = "h";
	private static final String RIDE = "ride";
	private static final String PT = "pt";
	private static final String CAR = "car";
	private static final String BIKE = "bike";
	private static final String WALK = "walk";
		
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public PersonSubtourHandler (){
		
	}
	
	
	//////////////////////////////////////////////////////////////////////
	// Private Methods
	//////////////////////////////////////////////////////////////////////
	
	private final void handleSubTours(final Plan plan, final TreeMap<Integer, ArrayList<Integer>> subtours, int subtour_idx) {
		
		// setting subtour parameters
		
		//TreeMap<Integer, Integer> modeSubTours = new TreeMap<Integer, Integer>();
		for (int i=subtour_idx-1; i>=0; i=i-1) {
			Subtour sub = new Subtour();
			sub.setNodes(subtours.get(i));
			sub.setMode(100);
			Act start_act = (Act)plan.getActsLegs().get(sub.getNodes().get(0));
			sub.setStart_coord(start_act.getCoord());
			sub.setId(i);
			System.out.println("sub nodes" + sub.getNodes());
			ArrayList<Integer> subtour = subtours.get(i);
			int mainpurpose = 3; //mainpurpose:  0 := work; 1 := edu; 2 := shop 3:=leisure
			double d = 0.0;
			CoordI start = ((Act)plan.getActsLegs().get(subtour.get(0))).getCoord();
			CoordI prev = start;
			String type = null;
			for (int k=1; k<subtour.size()-1; k=k+1) { 
				type = ((Act)plan.getActsLegs().get(subtour.get(k))).getType();
				if (mainpurpose == 1){
					if (type == W) { mainpurpose = 0;}//; break; }
				}
				else if (mainpurpose == 2) {
					if (type == W) { mainpurpose = 0;}// break; }
					else if (type == E) { mainpurpose = 1; }
				}
				else if (mainpurpose == 3) {
					if (type == W) {mainpurpose = 0;}// break; }
					else if (type == E) {mainpurpose = 1;}// break;}
					else if (type == S) {mainpurpose = 2;}
				} 
				CoordI curr = ((Act)plan.getActsLegs().get(subtour.get(k))).getCoord();
				if (curr.getX()>0 & curr.getY()>0) {d = d + curr.calcDistance(prev);}
				prev = curr;
				String mode =((Leg)plan.getActsLegs().get(subtour.get(k)-1)).getMode();
				int modechoice = 0;
				if (mode == CAR) {modechoice=0;}
				else if (mode == PT) {modechoice=1;}
				else if (mode == RIDE) {modechoice=2;}
				else if (mode == BIKE) {modechoice=3;}
				else if (mode == WALK) {modechoice=4;}
				if (sub.getMode() > modechoice) {sub.setMode(modechoice);}
			}
			
			sub.setPurpose(mainpurpose);
			if (prev.getX()>0 & prev.getY()>0) {d = d + start.calcDistance(prev);}// In the for-cycle the trip to home is not accounted
			d = d/1000.0; // distance in the model is in Km
			sub.setDistance(d);
			
			// Defining previous mode
			int prev_subtour = -1;
			if (subtour.get(0) != 0) {
				for (int j=subtours.size()-1; j>=0; j=j-1) {
					if (subtours.get(j).contains(subtour.get(0))) {
						prev_subtour = j; break;
					}
				}
				sub.setPrev_subtour(prev_subtour);  
			}
			pers_sub.setSubtour(sub);
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// Run Methods
	//////////////////////////////////////////////////////////////////////
	
	public void run (final Plan plan, final TreeMap<Integer, ArrayList<Integer>> subtours, int subtour_idx) {
		handleSubTours (plan,subtours,subtour_idx);
	}

	//////////////////////////////////////////////////////////////////////
	// Get Methods
	//////////////////////////////////////////////////////////////////////
	
	public PersonSubtour getPers_sub() {
		return pers_sub;
	}
		
}