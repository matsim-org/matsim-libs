package playground.kai.usecases.mentalmodule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;

@SuppressWarnings("unused")
public class MyPlanStrategyModule implements PlanStrategyModule,
ActivityEndEventHandler // this is just there as an example
{
	private static final Logger log = Logger.getLogger(MyPlanStrategyModule.class);

	ScenarioImpl sc;
	NetworkLayer net;
	Population pop;

	public MyPlanStrategyModule(Controler controler) {

		this.sc = controler.getScenario() ;
		this.net = this.sc.getNetwork() ;
		this.pop = this.sc.getPopulation() ;

	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void handlePlan(Plan plan) {
		log.error("calling handlePlan") ;
	}

	@Override
	public void prepareReplanning() {
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		log.error("calling handleEvent for an ActivityEndEvent") ;
	}

	@Override
	public void reset(int iteration) {
		log.error("calling reset") ;
	}


}
