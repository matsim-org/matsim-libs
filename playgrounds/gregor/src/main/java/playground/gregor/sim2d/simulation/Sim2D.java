package playground.gregor.sim2d.simulation;

import java.rmi.RemoteException;
import java.util.ArrayList;
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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;
import org.matsim.vis.otfvis.server.OnTheFlyServer;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.PositionInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;

import playground.gregor.sim2d.otfdebug.readerwriter.Agent2DWriter;
import playground.gregor.sim2d.otfdebug.readerwriter.ForceArrowWriter;
import playground.gregor.sim2d.peekabot.PeekABotClient;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

public class Sim2D {


	private final Config config;
	private final Population population;
	private final NetworkLayer network;
	private static EventsManager events;
	private double stopTime;
	private final Network2D network2D;
	private double startTime;
	private List<AgentSnapshotInfo> agentData;
	private final Map<MultiPolygon, List<Link>> floors;

	protected final PriorityBlockingQueue<Agent2D> activityEndsList = new PriorityBlockingQueue<Agent2D>(500, new Agent2DDepartureTimeComparator());
	private final double endTime;
	protected final PriorityBlockingQueue<Agent2D>  agentsToRemoveList  = new PriorityBlockingQueue<Agent2D>(500, new Agent2DDepartureTimeComparator());
	private PeekABotClient peekABotVis;

	double offX = 0;
	double offY = 0;
	private boolean drawFloorPlan;
	
	
	public Sim2D(final Network network, final Map<MultiPolygon,List<Link>> floors, final Population plans, final EventsManager events, final StaticForceField sff, final Config config){
		this.config = config;
//		this.endTime = this.config.simulation().getEndTime();
		this.endTime = 10*3600+20*60;
		this.network = (NetworkLayer) network;
		Map<MultiPolygon,NetworkLayer> f = new HashMap<MultiPolygon, NetworkLayer>();
		for (Entry<MultiPolygon,List<Link>> e : floors.entrySet()) {
			f.put(e.getKey(),this.network);
//			fg = new StaticForceFieldGenerator(e.getKey());
		}
		
		this.floors = floors;

//		QuadTree<Force> q;
//		q = fg.loadStaticForceField();
//		StaticForceFieldWriter s = new StaticForceFieldWriter();
//		s.write("test.xml", q.values());
//
//		q = new StaticForceFieldReader("test.xml").getStaticForceField();

//		new StaticForceFieldToShape(q).createShp();
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
//			try {
//				Thread.sleep(10);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		this.peekABotVis.restAgents();
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
			this.peekABotVis.removeBot(Integer.parseInt(agent.getId().toString()));
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
		if (this.peekABotVis != null && this.drawFloorPlan) {
			drawFloorPlan();
		}

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
			if (this.peekABotVis != null) {
				if (this.offX == 0 ) {
					this.offX = agent.getPosition().x;
					this.offY = agent.getPosition().y;
				}
				float x = (float) (agent.getPosition().x - this.offX);
				float y = (float) (agent.getPosition().y -this.offY);
				float z = (float) agent.getPosition().z;
				this.peekABotVis.addBot(Integer.parseInt(agent.getId().toString()), x, y, z);
				this.peekABotVis.setBotColor(Integer.parseInt(agent.getId().toString()), MatsimRandom.getRandom().nextFloat(), MatsimRandom.getRandom().nextFloat(), MatsimRandom.getRandom().nextFloat());
			}
		}

	}


	protected void afterSimStep(final double time) {
		if (this.peekABotVis != null) {
			visualizeAgents(time);
		}
	}


	private void visualizeAgents(double time) {
		updatePositionInfos();
		for (AgentSnapshotInfo asi : this.agentData) {
			
			float x = (float) (asi.getEasting() - this.offX);
			float y = (float) (asi.getNorthing() - this.offY);
			this.peekABotVis.setBotPosition(Integer.parseInt(asi.getId().toString()), x, y, 0.f,(float)asi.getAzimuth());
//			if ((vx*vx + vy*vy) > 1) {
//			throw new RuntimeException();
//			}			
		}
		
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
		this.agentData = new ArrayList<AgentSnapshotInfo>();
		for (Floor floor : this.network2D.getFloors()) {
			for (Agent2D agent : floor.getAgents()) {
				Coordinate coord = agent.getPosition();
				double velocity = floor.getAgentVelocity(agent);
				Force f = floor.getAgentForce(agent);
				double alpha = getPhaseAngle(f);
				alpha /= TWO_PI;
				alpha *= 360;
				alpha += 90;
//				AgentSnapshotInfo pos = new OTFAgentsListHandler.ExtendedPositionInfo(agent.getId(),coord.x,coord.y,0,alpha,velocity,AgentState.AGENT_MOVING,Math.abs(agent.getId().hashCode())%10,1);
				AgentSnapshotInfo pos = new PositionInfo(agent.getId(),coord.x,coord.y,0.,alpha);
				pos.setColorValueBetweenZeroAndOne(velocity);
				pos.setAgentState(AgentState.PERSON_DRIVING_CAR) ;
				pos.setType(Math.abs(agent.getId().hashCode())%10) ;
				pos.setUserDefined(1);
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

	/*package*/ Network getNetwork() {
		return this.network;
	}

	private static final void setEvents(final EventsManager events) {
		Sim2D.events = events;
	}



	private void drawFloorPlan() {
//    	this.peekABotVis.initPolygon(1, 4, 1, .5f, 0, 0);
//    	this.peekABotVis.addPolygonCoord(1,10, 10, 3);
//    	this.peekABotVis.addPolygonCoord(1,10, 0, 3);
//    	this.peekABotVis.addPolygonCoord(1,0, 0, 3);
//    	this.peekABotVis.addPolygonCoord(1,0, 15, 3);
//    	this.peekABotVis.init();
		int count = 0;
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		
//		int wallCount = 0;
		for (Entry<MultiPolygon, List<Link>> e : this.floors.entrySet()) {
			MultiPolygon mp = e.getKey();
			for (int i = 0; i < mp.getNumGeometries(); i++) {
				Geometry geo = mp.getGeometryN(i);
				Coordinate[] coords = geo.getCoordinates();
				this.peekABotVis.initPolygon(++count, coords.length, 0.5f, 0.5f, 0.5f, 3);
				for (int j = 0; j < coords.length; j++) {
					float x = (float) (coords[j].x-this.offX);
					if (x < minX) {
						minX = x;
					} else if (x > maxX) {
						maxX = x;
					}
					
					float y = (float) (coords[j].y-this.offY);
					if (y < minY) {
						minY = y;
					} else if (y > maxY) {
						maxY = y;
					}
					float z = (float) coords[j].z;
					this.peekABotVis.addPolygonCoord(count, x, y, 0);
					if (j > 1) {
						float x0 = (float) (coords[j-1].x-this.offX);
						float x1 = (float) (coords[j].x-this.offX);
						float y0 = (float) (coords[j-1].y-this.offY);
						float y1 = (float) (coords[j].y-this.offY);
						this.peekABotVis.initPolygon(++count, coords.length, 0.5f, 0.5f, 0.5f, 3);
						this.peekABotVis.addPolygonCoord(count, x0, y0, 0);
						this.peekABotVis.addPolygonCoord(count, x1, y1, 0);
						this.peekABotVis.addPolygonCoord(count, x1, y1, 3);
						this.peekABotVis.addPolygonCoord(count, x0, y0, 3);
						this.peekABotVis.addPolygonCoord(count, x0, y0, 0);
					}
				}
			}
		}
//		for (Entry<MultiPolygon, List<Link>> e : this.floors.entrySet()) {
//			MultiPolygon mp = e.getKey();
//			for (int i = 0; i < mp.getNumGeometries(); i++) {
//				Geometry geo = mp.getGeometryN(i);
//				Coordinate[] coords = geo.getCoordinates();
//				this.peekABotVis.initPolygon(++count, coords.length, 0.5f, 0.5f, 0.5f, 3);
//				for (int j = 0; j < coords.length; j++) {
//					float x = (float) (coords[j].x-this.offX);
//					float y = (float) (coords[j].y-this.offY);
//					float z = (float) coords[j].z;
//					this.peekABotVis.addPolygonCoord(count, x, y, 3);
//				}
//			}
//		}
		
    	this.peekABotVis.initPolygon(++count, 4, 1, .5f, 0, 0);
    	this.peekABotVis.addPolygonCoord(count,minX, minY, 0);
    	this.peekABotVis.addPolygonCoord(count,minX, maxY, 0);
    	this.peekABotVis.addPolygonCoord(count,maxX, maxY, 0);
    	this.peekABotVis.addPolygonCoord(count,maxX, minY, 0);
		
		this.peekABotVis.init();
	}


	public void addPeekABotClient(PeekABotClient peekABot, boolean drawFloorPlan) {
		this.peekABotVis = peekABot;
		this.drawFloorPlan = drawFloorPlan;
		
	}




}
