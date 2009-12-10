package playground.gregor.sim2d.simulation;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.AgentFactory;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.evacuation.otfvis.drawer.OTFBackgroundTexturesDrawer;
import org.matsim.evacuation.otfvis.readerwriter.AgentReader;
import org.matsim.evacuation.otfvis.readerwriter.AgentWriter;
import org.matsim.evacuation.otfvis.readerwriter.TextureDataWriter;
import org.matsim.evacuation.otfvis.readerwriter.TextutreDataReader;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;
import org.matsim.vis.otfvis.OTFClient;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler.ExtendedPositionInfo;
import org.matsim.vis.otfvis.opengl.drawer.SimpleBackgroundDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleBackgroundLayer;
import org.matsim.vis.otfvis.server.OnTheFlyServer;
import org.matsim.vis.snapshots.writers.PositionInfo.VehicleState;

import playground.gregor.sim2d.otfdebug.ForceArrowWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;

public class Sim2D {


	private final Config config;
	private final PopulationImpl population;
	private final NetworkLayer network;
	private static EventsManager events;
	private double stopTime;
	private final Network2D network2D;
	private double startTime;
	private AgentWriter agentWriter;
	private List<ExtendedPositionInfo> agentData;
	
	
	protected final PriorityBlockingQueue<Agent2D> activityEndsList = new PriorityBlockingQueue<Agent2D>(500, new Agent2DDepartureTimeComparator());
	private OnTheFlyServer myOTFServer;
	private final double endTime;
	private ForceArrowWriter forceArrowWriter;

	public Sim2D(final Network network, final Map<MultiPolygon,List<Link>> floors, final Population plans, final EventsManager events){
		this.config = Gbl.getConfig();
//		this.endTime = this.config.simulation().getEndTime();
		this.endTime = 10*3600+20*60;
		this.network = (NetworkLayer) network;
		Map<MultiPolygon,NetworkLayer> f = new HashMap<MultiPolygon, NetworkLayer>();
		for (Entry<MultiPolygon,List<Link>> e : floors.entrySet()) {
			f.put(e.getKey(),this.network);
		}
		this.network2D = new Network2D(this.network,f);
		((OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME)).setAgentSize(20.f);

		this.population = (PopulationImpl) plans;
		setEvents(events);
		SimulationTimer.reset(this.config.simulation().getTimeStepSize());
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
//			try {
//				Thread.sleep(10);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}

	}


	private boolean doSimStep(double time) {
		handleActivityEnds(time);
		moveFloors();
		if (time >= this.endTime) {
			return false;
		}
		return true;
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
		BasicVehicleType defaultVehicleType = new BasicVehicleTypeImpl(new IdImpl("defaultVehicleType"));

		for (Person p : this.population.getPersons().values()) {
//			PersonAgent agent = this.agentFactory.createPersonAgent(p);
			Agent2D agent = new Agent2D(p,this);

			if (agent.initialize()) {
				Floor floor = this.network2D.getFloors().get(0);
				floor.addAgent(agent);
			}
		}
		
	}


	protected void afterSimStep(final double time) {
		visualizeAgents(time);
		this.myOTFServer.updateStatus(time);
	}
	
	
	private void visualizeAgents(double time) {
		updatePositionInfos();
		this.agentWriter.setSrc(this.agentData);
		
		List<double []>forceData = updateForceInfos();
		this.forceArrowWriter.setSrc(forceData);
		
	}


	//DEBUG
	private List<double []> updateForceInfos() {
		List<double []> ret = new ArrayList<double[]>();
		for (Floor floor : this.network2D.getFloors()) {
			ret.addAll(floor.getForceInfos());
		}
		return ret;
	}


	private void updatePositionInfos() {
		this.agentData = new ArrayList<ExtendedPositionInfo>();
		for (Floor floor : this.network2D.getFloors()) {
			for (Agent2D agent : floor.getAgents()) {
				Coordinate coord = agent.getPosition();
				double velocity = floor.getAgentVelocity(agent);
				ExtendedPositionInfo pos = new ExtendedPositionInfo(agent.getId(),coord.x,coord.y,0,0,velocity,VehicleState.Driving,Math.abs(agent.getId().hashCode())%10,1);
				this.agentData.add(pos);
			}
		}
	}


	public static final EventsManager getEvents() {
		return events;
	}

	private static final void setEvents(final EventsManager events) {
		Sim2D.events = events;
	}


	public void setOTFStuff(OnTheFlyServer myOTFServer, AgentWriter agentWriter, ForceArrowWriter forceArrowWriter) {
		this.myOTFServer = myOTFServer;
		this.agentWriter = agentWriter;
		this.forceArrowWriter = forceArrowWriter;
		this.myOTFServer.reset();
		try {
			this.myOTFServer.pause();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
	}

}
