package playground.ciarif.flexibletransports.preprocess.membership;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;

import playground.ciarif.flexibletransports.data.FlexTransPersonImpl;
import playground.ciarif.flexibletransports.router.CarSharingStation;
import playground.ciarif.flexibletransports.router.CarSharingStations;
import playground.ciarif.flexibletransports.utils.MembershipUtils;


public class MembershipAssigner {
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private final static Logger log = Logger.getLogger(MembershipAssigner.class);
	private Scenario scenario;
	 

	private CarSharingStations carStations;
	private String stationfilePath = "../../matsim/input/triangle/CS_Stations.txt";//input @burgess
	private QuadTree<Person> personsQuadTree;
	//private String stationfilePath = "/data/matsim/ciarif/input/zurich_10pc/CarSharing/Stationen_7.dat";//input @satawal
	//private final FtConfigGroup ftConfigGroup = new FtConfigGroup();
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public MembershipAssigner(Scenario scenario) {
		this.scenario = scenario;
		this.init();
	}
	


	
	private double computeAccessCSWork(PersonImpl pi) {
		
		Collection<CarSharingStation> closestStations = new TreeSet<CarSharingStation>();
		Coord c = new Coord(1.0D / 0.0D, 1.0D / 0.0D);
		double access = 0.0D;
		for (PlanElement pe : pi.getSelectedPlan().getPlanElements()) {
			
			if (pe instanceof Activity) {
            
				Activity act = (Activity)pe;
            
				if (act.getType().equals("work")) {
					c = ((ActivityFacility)scenario.getActivityFacilities().getFacilities().get(act.getFacilityId())).getCoord();
					break;
				}
			}
        }
		
		closestStations =  this.carStations.getClosestStations(c, 3, 5000);
		for (CarSharingStation station:closestStations) {
			
			access += station.getCars() * Math.exp(-2 * (CoordUtils.calcDistance(station.getCoord(), c))/1000);
		
		}
		return access;
	}




	private double computeAccessCSHome(PersonImpl pi) {
		Vector<CarSharingStation> closestStations = new Vector<CarSharingStation>();
		Coord c = new Coord(1.0D / 0.0D, 1.0D / 0.0D);
		double access = 0.0D;
		for (PlanElement pe : pi.getSelectedPlan().getPlanElements()) {
			
			if (pe instanceof Activity) {
            
				Activity act = (Activity)pe;
            
				if (act.getType().equals("home")) {
					c = ((ActivityFacility)scenario.getActivityFacilities().getFacilities().get(act.getFacilityId())).getCoord();
            		break;
				}
			}
			
		}
		
		closestStations =  this.carStations.getClosestStations(c, 3, 5000);
		for (CarSharingStation station:closestStations) {
        	
			access += station.getCars() * Math.exp(-2 * (CoordUtils.calcDistance(station.getCoord(), c))/1000);
			
			
		} 
		return access;
	}




	public void run() {
		this.modifyPlans();		
	}
	

	private void init() {
		
		log.info("Reading car stations...");
	    this.carStations = new CarSharingStations(this.scenario.getNetwork());
	    try {
	      this.carStations.readFile(this.stationfilePath);
	    } catch (FileNotFoundException e) {
	      throw new RuntimeException(e);
	    } catch (IOException e) {
	      throw new RuntimeException(e);
	    }
	    log.info("Reading car stations...done.");
		this.personsQuadTree = MembershipUtils.createPersonQuadTree (this.scenario);
	}




	private void modifyPlans() {
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			PersonImpl pi = (PersonImpl)person;
			if (person.getPlans().size() > 1) {
				log.error("More than one plan for person: " + pi.getId());
			}
			if (pi.getLicense().equalsIgnoreCase("yes")) {this.assignCarSharingMembership(pi);}
			else {pi.addTravelcard("unknown");}
			PlanImpl selectedPlan = (PlanImpl)pi.getSelectedPlan();
			final List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
			for (int j = 1; j < actslegs.size(); j=j+2) {
				final LegImpl leg = (LegImpl)actslegs.get(j);
				if (leg.getMode().startsWith("ride")& pi.getTravelcards().equals("ch-HT-mobility")) {
					leg.setMode("carsharing");
				}
			}
		}
		
	}
	

	private void assignCarSharingMembership(PersonImpl pi) {
		log.info("Processing person " + pi.getId());
		FlexTransPersonImpl ftPerson = new  FlexTransPersonImpl(pi);
		this.addFTAttributes(ftPerson);
		int choice = new MembershipModel(this.scenario).calcMembership(ftPerson);
		if (choice == 0) {
			pi.addTravelcard("ch-HT-mobility");
		}
		else {
			pi.addTravelcard("unknown");
		}
		log.info("travelcards = " + pi.getTravelcards());
	}




	private void addFTAttributes(FlexTransPersonImpl ftPerson) {
		//log.info("finding out home cs accessibility for person " + ftPerson.getId());
		ftPerson.setAccessHome(this.computeAccessCSHome(ftPerson));
		//Look if it works using to call the set method at the end of the compute method (putting void as return)
		//log.info("finding out work cs accessibility for person " + ftPerson.getId());
		ftPerson.setAccessWork(this.computeAccessCSWork(ftPerson));
		
		ftPerson.setDensityHome(this.computeDensityHome(ftPerson));
		
	}




	private double computeDensityHome(FlexTransPersonImpl pi) {

		Coord c = new Coord(1.0D / 0.0D, 1.0D / 0.0D);
		//QuadTree<Person> personsQuadTree = MembershipUtils.createPersonQuadTree (this.scenario);
		for (PlanElement pe : pi.getSelectedPlan().getPlanElements()) {
			
			if (pe instanceof Activity) {
            
				Activity act = (Activity)pe;
            
				if (act.getType().equals("home")) {
					c = ((ActivityFacility)scenario.getActivityFacilities().getFacilities().get(act.getFacilityId())).getCoord();
					break;
				}
			
			}
		}
		int density =  (this.personsQuadTree.get(c.getX(),c.getY(), 0.05)).size();
		log.info("density " + density);
		return density;
	}	

}
