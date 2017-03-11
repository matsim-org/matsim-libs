package playground.gregor.misanthrope.simulation.physics;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import playground.gregor.gis.cutoutnetwork.BountingBoxFilter;
import playground.gregor.misanthrope.run.CTRunner;
import playground.gregor.misanthrope.simulation.CTEvent;
import playground.gregor.misanthrope.simulation.CTEventsPaulPriorityQueue;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static playground.gregor.misanthrope.simulation.physics.CTCell.MIN_CELL_WIDTH;

public class CTNetwork {

    private static final Logger log = Logger.getLogger(CTNetwork.class);

    private static int WIDTH_WRN_CNT = 0;

    private final CTEventsPaulPriorityQueue events = new CTEventsPaulPriorityQueue();
    private final CTNetsimEngine engine;
    private Map<Id<Link>, CTLink> links = new HashMap<>();
    private Map<Id<Node>, CTNode> nodes = new ConcurrentHashMap<>();
    private Network network;
    private EventsManager em;
    private BountingBoxFilter filter;

    public CTNetwork(Network network, EventsManager em, CTNetsimEngine engine) {
        this.network = network;
        this.em = em;
        this.engine = engine;
        init();
    }

    private void init() {
//        if (CTRunner.DEBUG) {
//            TextEvent e1 = new TextEvent(0, "alpha: " + WIDTH / 2, 10 + WIDTH, -Debugger3.WIDTH / 2 - 3);
//
//            this.em.processEvent(e1);
//            TextEvent e2 = new TextEvent(0, "rho_m: " + CTCell.RHO_M, 10 + WIDTH, -Debugger3.WIDTH / 2 - 3 - 2);
//            this.em.processEvent(e2);
//            TextEvent e3 = new TextEvent(0, "v_0: " + CTCell.V_0, 10 + WIDTH, -Debugger3.WIDTH / 2 - 3 - 4);
//            this.em.processEvent(e3);
//            TextEvent e4 = new TextEvent(0, "gamma: " + CTCell.GAMMA, 10 + WIDTH, -Debugger3.WIDTH / 2 - 3 - 6);
//            this.em.processEvent(e4);
//            TextEvent e5 = new TextEvent(0, "p_0: " + CTCell.P0, 10 + WIDTH, -Debugger3.WIDTH / 2 - 3 - 8);
//            this.em.processEvent(e5);
//            TextEvent e6 = new TextEvent(0, "#left -> right: " + Debugger3.AGENTS_LR, 10 + WIDTH, -Debugger3.WIDTH / 2 - 3 - 10);
//            this.em.processEvent(e6);
//            TextEvent e7 = new TextEvent(0, "#right -> left: " + Debugger3.AGENTS_RL, 10 + WIDTH, -Debugger3.WIDTH / 2 - 3 - 12);
//            this.em.processEvent(e7);
//            TextEvent e8 = new TextEvent(0, "inflow [1/s]: left -> right: " + 1 / Debugger3.INV_INFLOW, 10 + WIDTH, -Debugger3.WIDTH / 2 - 3 - 14);
//            this.em.processEvent(e8);
//            TextEvent e9 = new TextEvent(0, "inflow [1/s]: right -> left: " + 1 / Debugger3.INV_INFLOW, 10 + WIDTH, -Debugger3.WIDTH / 2 - 3 - 16);
//            this.em.processEvent(e9);
//        }


        log.info("creating and initializing links and nodes");
        this.network.getLinks().values().forEach(l -> {

            double width = l.getCapacity() / 1.33;
            if (width < MIN_CELL_WIDTH) {
                if (WIDTH_WRN_CNT++ < 10) {
                    log.warn("Width of link: " + l.getId() + " is too small. Increasing it from: " + width + " to: " + MIN_CELL_WIDTH);
                    if (WIDTH_WRN_CNT == 10) {
                        log.warn(Gbl.FUTURE_SUPPRESSED);
                    }
                }
                width = MIN_CELL_WIDTH;
                l.setCapacity(width * 1.33);
            }

        });
        this.network.getNodes().values().parallelStream().forEach(n -> {
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
            mxCap = Math.min(mxCap, 133);
            CTNode ct = new CTNode(n.getId(), n, this, mxCap / 1.33);
            this.nodes.put(n.getId(), ct);
        });
        this.network.getLinks().values().stream().filter(l -> links.get(l.getId()) == null).forEach(l -> {
            Link rev = getRevLink(l);
            CTLink ct = new CTLink(l, rev, em, this, this.nodes.get(l.getFromNode().getId()), this.nodes.get(l.getToNode().getId()));
            links.put(l.getId(), ct);
            if (rev != null) {
                links.put(rev.getId(), ct);
            }
        });

        this.links.values().forEach(CTLink::init);
        this.filter = new BountingBoxFilter(650608, 651253, 9893743, 9894168);

//        this.links.values().stream().filter(l -> this.filter.test(l.getDsLink().getFromNode())).forEach(CTLink::debug);
//        this.links.values().stream().filter(l -> l.getDsLink().getId().toString().contains("el")).forEach(CTLink::debug);
        this.links.values().forEach(CTLink::debug);
        this.nodes.values().forEach(CTNode::init);

        log.info("verifying network");
        checkNetwork();
        log.info("done.");

    }

    private Link getRevLink(Link l) {
        for (Link rev : l.getToNode().getOutLinks().values()) {
            if (rev.getToNode() == l.getFromNode()) {
                return rev;
            }
        }
        return null;
    }

    private void checkNetwork() {
        Set<CTCell> allCells = new HashSet<>();
        for (CTNode n : this.nodes.values()) {
            for (CTCellFace face : n.getCTCell().getFaces()) {
                if (face.nb == null) {
                    throw new RuntimeException("node cell face is null!");
                }
            }
            for (CTCell ctCell : n.getCTCell().getNeighbors()) {
                allCells.add(ctCell);
                if (!ctCell.getNeighbors().contains(n.getCTCell())) {
                    throw new RuntimeException("missing backward pointer!");
                }
            }
        }
        for (CTLink l : this.links.values()) {
            for (CTCell c : l.getCells()) {
                for (CTCellFace face : c.getFaces()) {
                    if (face.nb == null) {
                        throw new RuntimeException("link cell face is null!");
                    }
                }
                for (CTCell ctCell : c.getNeighbors()) {
                    allCells.add(ctCell);
                    if (!ctCell.getNeighbors().contains(c)) {
                        throw new RuntimeException("missing backward pointer!");
                    }
                }
            }
        }
        log.info("Network consists of " + allCells.size() + " cells");
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
//        this.links.values().stream().flatMap(l->l.getCells().stream()).forEach(c->c.updateIntendedCellJumpTimeAndChooseNextJumper(time));
//        this.nodes.values().stream().forEach(n->n.getCTCell().updateIntendedCellJumpTimeAndChooseNextJumper(time));


        while (this.events.peek() != null && events.peek().getExecTime() < time + 1) {
            CTEvent e = events.poll();


            if (e.isInvalid()) {
                continue;
            }
            e.execute();
        }


    }

    private void draw(double time) {

//        links.values().stream().filter(l -> l.getDsLink().getId().toString().contains("el")).forEach(link -> {
//        links.values().stream().filter(l -> this.filter.test(l.getDsLink().getFromNode())).forEach(link -> {
        links.values().forEach(link -> {

            Link ll = link.getDsLink();

            double dx = ll.getToNode().getCoord().getX() - ll.getFromNode().getCoord().getX();
            double dy = ll.getToNode().getCoord().getY() - ll.getFromNode().getCoord().getY();
            dx /= ll.getLength();
            dy /= ll.getLength();
            for (CTCell cell : link.getCells()) {
                drawCell(cell, time, dx, dy);
            }

        });

    }

    private boolean accept(Link ll) {
        if (ll.getId().toString().contains("el")) {
            return true;
        }
        for (Link l : ll.getToNode().getOutLinks().values()) {
            if (l.getId().toString().contains("el")) {
                return true;
            }
        }

        return false;

    }

    private void drawCell(CTCell cell, double time, double dx, double dy) {
        for (CTPed ped : cell.getPeds()) {
            double oX = (5 - (ped.hashCode() % 10)) / (20. / cell.getWidth());
            double oY = (5 - ((23 * ped.hashCode()) % 10)) / (20. / cell.getWidth());

            double x = cell.getX() + oX / 2.;
            double y = cell.getY() + oY / 2.;

            XYVxVyEventImpl e = new XYVxVyEventImpl(ped.getDriver().getId(), x, y, dx * ped.getDesiredDir(), dy * ped.getDesiredDir(), time);
            this.em.processEvent(e);
        }
    }

    public Map<Id<Link>, CTLink> getLinks() {
        return this.links;
    }

    public void run() {
        double time = 0;
        while (events.peek() != null && events.peek().getExecTime() < 3600 * 240) {

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

    public void afterSim() {


    }

}
