package playground.ciarif.carpooling;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.ciarif.retailers.stategies.GravityModelRetailerStrategy;

public class CarPoolingListener implements IterationEndsListener {
	
	private Controler controler;
	private final static Logger log = Logger.getLogger(GravityModelRetailerStrategy.class);
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (controler.getIteration()%2==0 && controler.getIteration()>0) {
			this.controler = event.getControler();
			this.plansAnalyzer();
		}
		
	}


	private void plansAnalyzer() {
		// TODO Auto-generated method stub
		for (PersonImpl p:controler.getPopulation().getPersons().values()){
			log.info("PERSON " + p.getId() );
			PlanImpl plan = p.getSelectedPlan();
			
			for (PlanElement pe:plan.getPlanElements()) {
				
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					
					if (act.getType().equals("home") && plan.getNextLeg(act)!= null) {
						LegImpl homeWorkLeg = plan.getNextLeg(act);
						ActivityImpl workAct = plan.getNextActivity(homeWorkLeg);
						log.info("Home activity = "  + act.getType());
						if (homeWorkLeg.getMode().toString().equals("car") && workAct.getType().equals("work")) {
							log.info("Person Id = " + p.getId());
							log.info("Coord home activity = " + act.getCoord());
							log.info("Starting time leg = " + homeWorkLeg.getDepartureTime());
							log.info("Coord work Activity = "  + workAct.getCoord());
						}
					}
					
					if (act.getType().equals("work") && plan.getNextLeg(act)!=null) {
						LegImpl workHomeLeg = plan.getNextLeg(act);
						ActivityImpl homeAct = plan.getNextActivity(workHomeLeg);
						log.info("Work activity = "  + act.getType());
						if (workHomeLeg.getMode().toString().equals("car") && homeAct.getType().equals("home")) {
							log.info("Person Id = " + p.getId());
							log.info("Coord work activity" + act.getCoord());
							log.info("Starting time leg = " + workHomeLeg.getDepartureTime());
							log.info("Coord home activity = " + homeAct.getCoord());
						}	
					}
				}
			}
		}
	}
}
