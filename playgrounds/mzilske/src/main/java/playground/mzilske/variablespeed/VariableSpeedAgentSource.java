package playground.mzilske.variablespeed;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;

public class VariableSpeedAgentSource implements AgentSource {

	private final VehicleType FAST_VEHICLETYPE; 
	private final VehicleType SLOW_VEHICLETYPE; 
	private Population population;
	private AgentFactory agentFactory;
	private QSim qsim;

	public VariableSpeedAgentSource(Population population, AgentFactory agentFactory, QSim qsim) {
		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;
		this.FAST_VEHICLETYPE = new VehicleTypeImpl(new IdImpl("fast"));
		FAST_VEHICLETYPE.setMaximumVelocity(66.66); // 240 km/h
		this.SLOW_VEHICLETYPE = new VehicleTypeImpl(new IdImpl("slow"));
		SLOW_VEHICLETYPE.setMaximumVelocity(2.77); // 10 km/h
	}

	@Override
	public void insertAgentsIntoMobsim() {
		for (Person p : population.getPersons().values()) {
			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(p);
			qsim.insertAgentIntoMobsim(agent);
			Plan plan = p.getSelectedPlan();
			int iLeg = 0;
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) leg.getRoute();
					Id vehicleLink = route.getStartLinkId(); // park vehicle at beginning of leg
					Id vehicleId = new IdImpl(p.getId() + "_" + iLeg);
					Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, iLeg == 0 ? FAST_VEHICLETYPE : SLOW_VEHICLETYPE);
					qsim.createAndParkVehicleOnLink(vehicle, vehicleLink);
					route.setVehicleId(vehicle.getId());
					iLeg++;
				}
			}
		}
	}

}
