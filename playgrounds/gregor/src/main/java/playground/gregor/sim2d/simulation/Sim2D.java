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
import org.matsim.core.utils.collections.QuadTree;
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

import playground.gregor.sim2d.gisdebug.StaticForceFieldToShape;
import playground.gregor.sim2d.otfdebug.readerwriter.Agent2DWriter;
import playground.gregor.sim2d.otfdebug.readerwriter.ForceArrowWriter;

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
	private Agent2DWriter agentWriter;
	private List<ExtendedPositionInfo> agentData;
	
	
	protected final PriorityBlockingQueue<Agent2D> activityEndsList = new PriorityBlockingQueue<Agent2D>(500, new Agent2DDepartureTimeComparator());
	private OnTheFlyServer myOTFServer = null;
	private final double endTime;
	private ForceArrowWriter forceArrowWriter;
	protected final PriorityBlockingQueue<Agent2D>  agentsToRemoveList  = new PriorityBlockingQueue<Agent2D>(500, new Agent2DDepartureTimeComparator());

	public Sim2D(final Network network, final Map<MultiPolygon,List<Link>> floors, final Population plans, final EventsManager events, final StaticForceField sff){
		this.config = Gbl.getConfig();
//		this.endTime = this.config.simulation().getEndTime();
		this.endTime = 10*3600+20*60;
		this.network = (NetworkLayer) network;
		Map<MultiPolygon,NetworkLayer> f = new HashMap<MultiPolygon, NetworkLayer>();
		for (Entry<MultiPolygon,List<Link>> e : floors.entrySet()) {
			f.put(e.getKey(),this.network);
//			fg = new StaticForceFieldGenerator(e.getKey());
		}
		
//		QuadTree<Force> q;
//		q = fg.loadStaticForceField();
//		StaticForceFieldWriter s = new StaticForceFieldWriter();
//		s.write("test.xml", q.values());
//		
//		q = new StaticForceFieldReader("test.xml").getStaticForceField();
		
//		new StaticForceFieldToShape(q).createShp();
		this.network2D = new Network2D(this.network,f,sff);

		this.population = (PopulationImpl) plans;
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
				Force f = floor.getAgentForce(agent);
				double alpha = getPhaseAngle(f);
				alpha /= TWO_PI;
				alpha *= 360;
				alpha += 90;
				ExtendedPositionInfo pos = new ExtendedPositionInfo(agent.getId(),coord.x,coord.y,0,alpha,velocity,VehicleState.Driving,Math.abs(agent.getId().hashCode())%10,1);
				this.agentData.add(pos);
			}
		}
	}
	
	private static final double TWO_PI = 2 * Math.PI;
	private static final double PI_HALF =  Math.PI / 2;

	private double getPhaseAngle(Force f) {
		double alpha = 0.0;
		if (f.getFx() > 0) {
			alpha = Math.atan(f.getFy()/f.getFx());
		} else if (f.getFx() < 0) {
			alpha = Math.PI + Math.atan(f.getFy()/f.getFx());
		} else { // i.e. DX==0
			if (f.getFy() > 0) {
				alpha = PI_HALF;
			} else {
				alpha = -PI_HALF;
			}
		}
		if (alpha < 0.0) alpha += TWO_PI;
		return alpha;
	}

	public static final EventsManager getEvents() {
		return events;
	}

	private static final void setEvents(final EventsManager events) {
		Sim2D.events = events;
	}


	public void setOTFStuff(OnTheFlyServer myOTFServer, Agent2DWriter agentWriter2, ForceArrowWriter forceArrowWriter) {
		this.myOTFServer = myOTFServer;
		this.agentWriter = agentWriter2;
		this.forceArrowWriter = forceArrowWriter;
		this.myOTFServer.reset();
		try {
			this.myOTFServer.pause();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
	}




}
