package playground.gregor.sim2d.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

import playground.gregor.sim2d.peekabot.Sim2DVis;

import com.vividsolutions.jts.geom.MultiPolygon;

public class Sim2D {


	private final Config config;
	private final Population population;
	private final NetworkLayer network;
	private static EventsManager events;
	private double stopTime;
	private final Network2D network2D;
	private double startTime;

	protected final PriorityBlockingQueue<Agent2D> activityEndsList = new PriorityBlockingQueue<Agent2D>(500, new Agent2DDepartureTimeComparator());
	private final double endTime;
	protected final PriorityBlockingQueue<Agent2D>  agentsToRemoveList  = new PriorityBlockingQueue<Agent2D>(500, new Agent2DDepartureTimeComparator());

	private Sim2DVis sim2DVis;
	
	
	public Sim2D(final Network network, final Map<MultiPolygon,List<Link>> floors, final Population plans, final EventsManager events, final StaticForceField sff, final Config config){
		this.config = config;
		this.endTime = 9*3600+30*60;
		this.network = (NetworkLayer) network;
		Map<MultiPolygon,NetworkLayer> f = new HashMap<MultiPolygon, NetworkLayer>();
		for (Entry<MultiPolygon,List<Link>> e : floors.entrySet()) {
			f.put(e.getKey(),this.network);
		}
		this.network2D = new Network2D(this.network,f,sff);

		this.population = plans;
		setEvents(events);
		SimulationTimer.reset(1);
	}


	public void run() {
		prepareSim();
		boolean cont = true;
		while (cont) {
			double time = SimulationTimer.getTime();
			cont = doSimStep(time);
			afterSimStep(time);
			if (cont) {
				SimulationTimer.incTime();
			}
		}
		if (this.sim2DVis != null){
			this.sim2DVis.reset();
		}
	}


	private boolean doSimStep(double time) {
		handleActivityEnds(time);
		handleAgentRemoves(time);
		moveFloors();
		if (time >= this.endTime) {
			return false;
		}
		return true;
	}

	private void handleAgentRemoves(double time) {
		while (this.agentsToRemoveList.peek() != null) {
			Agent2D agent = this.agentsToRemoveList.poll();
			//TODO works only as long as there is only one floor!!
			Floor floor = this.network2D.getFloors().get(0);
			floor.removeAgent(agent);
//			this.peekABotVis.removeBot(Integer.parseInt(agent.getId().toString()));
		}
	}


	public void scheduleAgentRemove(Agent2D agent2d) {
		this.agentsToRemoveList.add(agent2d);

	}

	protected void scheduleActivityEnd(final Agent2D agent) {
		this.activityEndsList.add(agent);
	}

	private void handleActivityEnds(final double time) {
		while (this.activityEndsList.peek() != null) {
			Agent2D agent = this.activityEndsList.peek();
			if (agent.getNextDepartureTime() <= time) {
				this.activityEndsList.poll();
				agent.depart();
			} else {
				return;
			}
		}
	}

	private void moveFloors() {
				for (Floor floor : this.network2D.getFloors()) {
					floor.move();
				}
	}


	private void prepareSim() {
		this.startTime = this.config.simulation().getStartTime();
		this.stopTime = this.config.simulation().getEndTime();

		if (this.startTime == Time.UNDEFINED_TIME) this.startTime = 0.0;
		if ((this.stopTime == Time.UNDEFINED_TIME) || (this.stopTime == 0)) this.stopTime = Double.MAX_VALUE;


		createAgents();

		double simStartTime = 0;
		Agent2D firstAgent = this.activityEndsList.peek();
		if (firstAgent != null) {
			simStartTime = Math.floor(Math.max(this.startTime, firstAgent.getNextDepartureTime()));
		}

		SimulationTimer.setSimStartTime(simStartTime);
		SimulationTimer.setTime(SimulationTimer.getSimStartTime());




	}


	private void createAgents() {
		if (this.population == null) {
			throw new RuntimeException("No valid Population found (plans == null)");
		}
		VehicleType defaultVehicleType = new VehicleTypeImpl(new IdImpl("defaultVehicleType"));

		for (Person p : this.population.getPersons().values()) {
			Agent2D agent = new Agent2D(p,this);

			if (agent.initialize()) {
				Floor floor = this.network2D.getFloors().get(0);
				floor.addAgent(agent);
			}

		}

	}


	protected void afterSimStep(final double time) {
		if (this.sim2DVis != null) {
			this.sim2DVis.draw(time);
		}
	}





//	//DEBUG
//	private List<double []> updateForceInfos() {
//		List<double []> ret = new ArrayList<double[]>();
//		for (Floor floor : this.network2D.getFloors()) {
//			ret.addAll(floor.getForceInfos());
//		}
//		return ret;
//	}





	public static final EventsManager getEvents() {
		return events;
	}

	/*package*/ Network getNetwork() {
		return this.network;
	}

	private static final void setEvents(final EventsManager events) {
		Sim2D.events = events;
	}






	public void setSim2DVis(Sim2DVis sim2DVis) {
		this.sim2DVis = sim2DVis;
		this.sim2DVis.setNetwork2D(this.network2D);
	}



}
