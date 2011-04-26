package playground.gregor.sim2d_v2.calibration.simulation.floor;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import playground.gregor.sim2d_v2.calibration.simulation.PhantomAgent2D;
import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZEventsHandler;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.CollisionPredictionAgentInteractionModule;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;

public class CollisionPredictionPhantomAgentInteractionModule extends CollisionPredictionAgentInteractionModule implements XYZEventsHandler,AgentDepartureEventHandler,AgentArrivalEventHandler{


	private final Map<Id,PhantomAgent2D> phantomAgents = new HashMap<Id, PhantomAgent2D>();

	public CollisionPredictionPhantomAgentInteractionModule(PhysicalFloor floor,
			Scenario2DImpl scenario) {
		super(floor, scenario);
		floor.getSim2D().getEventsManager().addHandler(this);
	}

	@Override
	protected void updateAgentQuadtree() {
		this.agentsQuad = new Quadtree();
		for (Agent2D agent : this.phantomAgents.values()) {
			Envelope e = new Envelope(agent.getPosition());
			this.agentsQuad.insert(e, agent);
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(XYZAzimuthEvent event) {
		if (Integer.parseInt(event.getPersonId().toString()) == -1 ) {
			return;
		}
		PhantomAgent2D a = this.phantomAgents.get(event.getPersonId());
		if (a.getLastUpdate() < 0) {
			a.setCurrentVelocity(0, 0);
		} else {
			double deltaT = event.getTime() - a.getLastUpdate();
			double dX = event.getX() - a.getPosition().x;
			double dY = event.getY() - a.getPosition().y;
			double vx = dX / deltaT;
			double vy = dY / deltaT;
			a.setCurrentVelocity(vx, vy);
		}
		a.moveToPostion(event.getCoordinate());
		a.setUpdateTime(event.getTime());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (Integer.parseInt(event.getPersonId().toString()) == -1 ) {
			return;
		}
		this.phantomAgents.remove(event.getTime());
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (Integer.parseInt(event.getPersonId().toString()) == -1 ) {
			return;
		}
		PhantomAgent2D a = new PhantomAgent2D();
		a.setCurrentVelocity(0,0);
		a.setUpdateTime(-1);
		a.setPostion(new Coordinate(0,0));
		this.phantomAgents.put(event.getPersonId(), a);

	}


}
