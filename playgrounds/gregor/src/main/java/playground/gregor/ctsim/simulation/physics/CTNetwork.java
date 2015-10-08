package playground.gregor.ctsim.simulation.physics;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import playground.gregor.ctsim.simulation.CTEvent;
import playground.gregor.ctsim.simulation.CTEventsPaulPriorityQueue;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;

import java.util.HashMap;
import java.util.Map;

public class CTNetwork {


    private final CTEventsPaulPriorityQueue events = new CTEventsPaulPriorityQueue();

    private Map<Id<Link>, CTLink> links = new HashMap<>();
    private Map<Id<Node>, CTNode> nodes = new HashMap<>();

    private Network network;
    private EventsManager em;

    public CTNetwork(Network network, EventsManager em) {
        this.network = network;
        this.em = em;
        init();
    }

    private void init() {
        for (Node n : this.network.getNodes().values()) {
            CTNode ct = new CTNode(n.getId(), n, this);
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
            ctNode.getCTCell().debug(em);
        }
    }

    public void run() {
        double time = 0;
        while (events.peek() != null) {

            CTEvent e = events.poll();
            if (e.getExecTime() > time + 0.1) {
                time = e.getExecTime();
                draw(time);

            }
            if (e.isInvalid()) {
                continue;
            }
            e.execute();
        }

    }

    private void draw(double time) {
        for (CTLink link : getLinks().values()) {
            for (CTCell cell : link.getCells()) {
                drawCell(cell, time);
            }
        }
    }

    private void drawCell(CTCell cell, double time) {
        for (CTPed ped : cell.getPeds()) {
            double oX = (5 - (ped.hashCode() % 10)) / 10.;
            double oY = (5 - ((23 * ped.hashCode()) % 10)) / 10.;

            double x = cell.getX() + oX / 2.;
            double y = cell.getY() + oY / 2.;

            XYVxVyEventImpl e = new XYVxVyEventImpl(Id.createPersonId(ped.hashCode()), x, y, 0, 0, time);
            this.em.processEvent(e);
        }
    }

    public void addEvent(CTEvent e) {
        this.events.add(e);
    }

    private Link getRevLink(Link l) {
        for (Link rev : l.getToNode().getOutLinks().values()) {
            if (rev.getToNode() == l.getFromNode()) {
                return rev;
            }
        }
        return null;
    }

    CTNode getCTNode(Id<Node> id) {
        return this.nodes.get(id);
    }

    public Map<Id<Link>, CTLink> getLinks() {
        return this.links;
    }
}
