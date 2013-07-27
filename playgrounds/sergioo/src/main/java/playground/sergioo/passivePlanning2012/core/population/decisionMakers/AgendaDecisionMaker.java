package playground.sergioo.passivePlanning2012.core.population.decisionMakers;

import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.NetworkUtils;

import playground.sergioo.passivePlanning2012.core.population.PlaceSharer;
import playground.sergioo.passivePlanning2012.core.population.agenda.Agenda;
import playground.sergioo.passivePlanning2012.core.population.agenda.Agenda.AgendaElement;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.EndTimeDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.RouteDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.TypeOfActivityFacilityDecisionMaker;

public class AgendaDecisionMaker extends PlaceSharer implements EndTimeDecisionMaker, TypeOfActivityFacilityDecisionMaker, RouteDecisionMaker {
	
	//Attributes
	private final Scenario scenario;
	private final Agenda agenda = new Agenda();
	private double lastActivityDuration;
	private Id futureFacilityId;
	private String futureActivityType;
	private double futureActivityStartTime;
	private final Set<String> modes;
	private final boolean carAvailability;
	
	//Methods
	public AgendaDecisionMaker(Scenario scenario, boolean carAvailability, Set<String> modes) {
		super();
		this.scenario = scenario;
		this.modes = modes;
		this.carAvailability = carAvailability;
	}
	public void setLastActivityDuration(double lastActivityDuration) {
		this.lastActivityDuration = lastActivityDuration;
	}
	public void setFutureFacilityId(Id futureFacilityId) {
		this.futureFacilityId = futureFacilityId;
	}
	public void setFutureActivityType(String futureActivityType) {
		this.futureActivityType = futureActivityType;
	}
	public void setFutureActivityStartTime(double futureActivityStartTime) {
		this.futureActivityStartTime = futureActivityStartTime;
	}
	@Override
	public Tuple<String, Id> decideTypeOfActivityFacility(double time, Id startFacilityId) {
		//TODO
		Network decisionNetwork = createDecisionNetwork(time, startFacilityId);
		return null;
	}
	private Network createDecisionNetwork(double time, Id startFacilityId) {
		//TODO
		Network decisionNetwork = NetworkImpl.createNetwork();
		for(KnownPlace place:knownPlaces.values())
			for(AgendaElement agendaElement:agenda.getElements().values())
				return null;
		return null;
	}
	@Override
	public List<? extends PlanElement> decideRoute(double time,
			Id startFacilityId, Id endFacilityId, String mode, TripRouter tripRouter) {
		ActivityFacility startFacility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(startFacilityId);
		ActivityFacility endFacility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(endFacilityId);
		return tripRouter.calcRoute(mode, startFacility, endFacility, time, null);
	}
	@Override
	public double decideEndTime(double startTime, double maximumEndTime,
			String typeOfActivity, Id facilityId) {
		return maximumEndTime;
	}

}
