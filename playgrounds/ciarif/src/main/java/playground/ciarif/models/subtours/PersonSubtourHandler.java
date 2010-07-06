package playground.ciarif.models.subtours;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class PersonSubtourHandler {


	//////////////////////////////////////////////////////////////////////
	// Member variables
	//////////////////////////////////////////////////////////////////////

	private PersonSubtour pers_sub = new PersonSubtour(); //qui vanno i subtours
	private static final String E = "e";
	private static final String W = "w";
	private static final String S = "s";
	//private static final String H = "h";
	//private static final String RIDE = "ride";
	//private static final String PT = "pt";
	//private static final String CAR = "car";
	//private static final String BIKE = "bike";
	//private static final String WALK = "walk";

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
			ActivityImpl start_act = (ActivityImpl)plan.getPlanElements().get(sub.getNodes().get(0));
			sub.setStart_coord(start_act.getCoord());
			sub.setId(i);
			System.out.println("sub nodes" + sub.getNodes());
			ArrayList<Integer> subtour = subtours.get(i);
			int mainpurpose = 3; //mainpurpose:  0 := work; 1 := edu; 2 := shop 3:=leisure
			double d = 0.0;
			Coord start = ((ActivityImpl)plan.getPlanElements().get(subtour.get(0))).getCoord();
			Coord prev = start;
			String type = null;
			for (int k=1; k<subtour.size()-1; k=k+1) {
				type = ((ActivityImpl)plan.getPlanElements().get(subtour.get(k))).getType().substring(0,1);
				if (mainpurpose == 1){
					if (type.equals(W)) { mainpurpose = 0;}
				}
				else if (mainpurpose == 2) {
					if (type.equals(W)) { mainpurpose = 0;}
					else if (type.equals(E)) { mainpurpose = 1;}
				}
				else if (mainpurpose == 3) {
					if (type.equals(W)) {mainpurpose = 0;}
					else if (type.equals(E)) {mainpurpose = 1;}
					else if (type.equals(S)) {mainpurpose = 2;}
				}
				Coord curr = ((ActivityImpl)plan.getPlanElements().get(subtour.get(k))).getCoord();
				if (curr.getX()>0 && curr.getY()>0) {d = d + CoordUtils.calcDistance(curr, prev);}
				prev = curr;

				// Getting the main mode at the sub-tour level
				String mode =((LegImpl)plan.getPlanElements().get(subtour.get(k)-1)).getMode();
				int license = 0;
				if (((PersonImpl) plan.getPerson()).hasLicense()){license =1;}
				int modechoice = 0;
				if (mode == TransportMode.car) {modechoice=0;}
				else if (mode == TransportMode.pt) {modechoice=1;}
				else if ((mode == TransportMode.car) && (license==0)) {modechoice=2;}
				else if (mode == TransportMode.bike) {modechoice=3;}
				else if (mode == TransportMode.walk) {modechoice=4;}
				if (sub.getMode() > modechoice) {sub.setMode(modechoice);}
			}
			System.out.println("subtour mode = " + sub.getMode());
			sub.setPurpose(mainpurpose);
			if (prev.getX()>0 && prev.getY()>0) {d = d + CoordUtils.calcDistance(start, prev);}// In the for-cycle the trip to home is not accounted
			d = d/1000.0; // distance in the model is in Km
			sub.setDistance(d);

			// Defining previous sub-tour
			 // The sub-tour starts at the agent's home location
			int prev_subtour = -1;
			System.out.println("subtour first node = " + subtour.get(0));
			if (subtour.get(0) == 0) {prev_subtour = -1;}
			else {
				for (int j=subtours.size()-1; j>=0; j=j-1) {
					if (subtours.get(j).contains(subtour.get(0))) {
						System.out.println ("prev_subtour = " + subtours.get(j));
						prev_subtour = j; break;
					}
				}
			}
			sub.setPrev_subtour(prev_subtour);
			System.out.println ("prev_subtour = " + prev_subtour);
			pers_sub.setSubtour(sub);


			//System.out.println ("prev_subtour idx = " + sub.getPrev_subtour());
			System.out .println ("index i = " + i);
			System.out .println ("subtours size = " + pers_sub.getSubtours().size());
		}
		for (int i=0; i<=pers_sub.getSubtours().size()-1;i=i+1) {
			int prev_sub = pers_sub.getSubtours().get(i).getPrev_subtour();
			System.out.println ("prev_subtour idx = " + prev_sub);
			if (prev_sub>=0) {
				for (int j=0;j<=pers_sub.getSubtours().size()-1;j=j+1){
					if (pers_sub.getSubtours().get(j).getId()==prev_sub) {
						int prev_mode = pers_sub.getSubtours().get(j).getMode();
						System.out.println ("prev_mode = " + prev_mode);
						pers_sub.getSubtours().get(i).setPrev_mode(prev_mode);
					}
				}
			}
			else {
				pers_sub.getSubtours().get(i).setPrev_mode(-1);
			}
		}

	}

	//////////////////////////////////////////////////////////////////////
	// Run Methods
	//////////////////////////////////////////////////////////////////////

	public void run (final Plan plan, final TreeMap<Integer, ArrayList<Integer>> subtours, int subtour_idx) {
		handleSubTours(plan,subtours,subtour_idx);
	}

	//////////////////////////////////////////////////////////////////////
	// Get Methods
	//////////////////////////////////////////////////////////////////////

	public PersonSubtour getPers_sub() {
		return pers_sub;
	}

}