package playground.gregor.ctsim.simulation.physics;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import playground.gregor.ctsim.run.CTRunner;
import playground.gregor.ctsim.simulation.CTEvent;
import playground.gregor.ctsim.simulation.CTEventsPaulPriorityQueue;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;

import java.util.HashMap;
import java.util.Map;

public class CTNetwork {


	private final CTEventsPaulPriorityQueue events = new CTEventsPaulPriorityQueue();
	private final CTNetsimEngine engine;

	private Map<Id<Link>, CTLink> links = new HashMap<>();
	private Map<Id<Node>, CTNode> nodes = new HashMap<>();

	private Network network;
	private EventsManager em;

	public CTNetwork(Network network, EventsManager em, CTNetsimEngine engine) {
		this.network = network;
		this.em = em;
		this.engine = engine;
		init();
	}

	private void init() {
		for (Node n : this.network.getNodes().values()) {
			double mxCap = 0;
			for (Link l : n.getInLinks().values()) {
				if (l.getCapacity() > mxCap) {
					mxCap = l.getCapacity();
				}
			}
			for (Link l : n.getOutLinks().values()) {
				if (l.getCapacity() > mxCap) {
					mxCap = l.getCapacity();
				}
			}

			CTNode ct = new CTNode(n.getId(), n, this, mxCap / 1.33);
			this.nodes.put(n.getId(), ct);
		}
		for (Link l : this.network.getLinks().values()) {
			if (links.get(l.getId()) != null) {
				continue;
			}
			Link rev = getRevLink(l);
			CTLink ct = new CTLink(l, rev, em, this, this.nodes.get(l.getFromNode().getId()), this.nodes.get(l.getToNode().getId()));
			links.put(l.getId(), ct);
			if (rev != null) {
				links.put(rev.getId(), ct);
			}

		}
		for (CTNode ctNode : this.nodes.values()) {
			ctNode.init();
			ctNode.getCTCell().debug(em);
		}
	}

	private Link getRevLink(Link l) {
		for (Link rev : l.getToNode().getOutLinks().values()) {
			if (rev.getToNode() == l.getFromNode()) {
				return rev;
			}
		}
		return null;
	}

	public CTNetsimEngine getEngine() {
		return this.engine;
	}

	public EventsManager getEventsManager() {
		return this.em;
	}

	public void doSimStep(double time) {
		if (CTRunner.DEBUG) {
			draw(time);
		}

		while (this.events.peek() != null && events.peek().getExecTime() < time + 1) {
			CTEvent e = events.poll();

			if (e.isInvalid()) {
				continue;
			}
			e.execute();
		}
	}

	private void draw(double time) {
		for (CTLink link : getLinks().values()) {
			Link ll = link.getDsLink();
			double dx  = ll.getToNode().getCoord().getX()-ll.getFromNode().getCoord().getX();
			double dy  = ll.getToNode().getCoord().getY()-ll.getFromNode().getCoord().getY();
			dx /= ll.getLength();
			dy /= ll.getLength();
			for (CTCell cell : link.getCells()) {
				drawCell(cell, time,dx,dy);
			}
		}
	}

	private void drawCell(CTCell cell, double time, double dx, double dy) {
		for (CTPed ped : cell.getPeds()) {
			double oX = (5 - (ped.hashCode() % 10)) / 10.;
			double oY = (5 - ((23 * ped.hashCode()) % 10)) / 10.;

			double x = cell.getX() + oX / 2.;
			double y = cell.getY() + oY / 2.;

			XYVxVyEventImpl e = new XYVxVyEventImpl(Id.createPersonId(ped.hashCode()), x, y, dx*ped.getDesiredDir(), dy*ped.getDesiredDir(), time);
			this.em.processEvent(e);
		}
	}

	public Map<Id<Link>, CTLink> getLinks() {
		return this.links;
	}

	public void run() {
		double time = 0;
		while (events.peek() != null && events.peek().getExecTime() < 3600) {

			CTEvent e = events.poll();
			if (CTRunner.DEBUG && e.getExecTime() > time + 1) {
				time = e.getExecTime();
				draw(time);

			}
			if (e.isInvalid()) {
				continue;
			}
			e.execute();
		}

	}

	public void addEvent(CTEvent e) {
		this.events.add(e);
	}

	CTNode getCTNode(Id<Node> id) {
		return this.nodes.get(id);
	}



	public void afterSim() {

	}


}
