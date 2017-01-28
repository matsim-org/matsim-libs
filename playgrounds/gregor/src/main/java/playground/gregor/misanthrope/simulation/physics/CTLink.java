package playground.gregor.misanthrope.simulation.physics;

import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;
import com.vividsolutions.jts.geom.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vehicles.Vehicle;
import playground.gregor.misanthrope.run.CTRunner;
import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.events.debug.LineEvent;
import playground.gregor.sim2d_v4.events.debug.TextEvent;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static playground.gregor.misanthrope.run.CTRunner.WIDTH;

public class CTLink implements CTNetworkEntity {


    private static final Logger log = Logger.getLogger(CTLink.class);
    private static final double EPSILON = 0.00001;
    private static double LENGTH;
    //    private static double MX_LENGTH = ;

    private static int LENGTH_WRN_CNT = 0;

    static {
        LENGTH = 3 * Math.sqrt(3) / 2 * WIDTH;
    }

    private final CTNetwork network;
    private final CTNode dsNode;
    private final CTNode usNode;
    private final List<CTCell> cells = new ArrayList<>();
    private Link dsLink;
    private Link usLink;
    private EventsManager em;
    private CTLinkCell dsJumpOn;
    private CTLinkCell usJumpOn;

    public CTLink(Link l, Link rev, EventsManager em, CTNetwork ctNetwork, CTNode from, CTNode to) {
        this.dsLink = l;
        this.dsNode = to;
        this.usNode = from;
        this.usLink = rev;
        this.em = em;
        this.network = ctNetwork;

    }

    @Override
    public void init() {


        LineSegment ls = LineSegment.createFromLink(this.dsLink);

        pruneEnds(ls);


//        double length = this.dsLink.getLength();
//        double length = projLength;
        double length = ls.length;
        if (length < LENGTH) {
            if (LENGTH_WRN_CNT++ < 10) {
                log.warn("Length of link: " + this.dsLink.getId() + " is too small. Increasing it from: " + length + " to: " + LENGTH);
                if (LENGTH_WRN_CNT == 10) {
                    log.warn(Gbl.FUTURE_SUPPRESSED);
                }
            }
            length = LENGTH;
        }


        if (Math.abs(ls.dy) < EPSILON) { //fixes a numerical issue
            ls.dy = 0;
        }
        double width = this.dsLink.getCapacity() / 1.33;


        //wrong! el nodes/links need special params ...
        if (this.dsLink.getId().toString().contains("el")) {
//            length = LENGTH;
//            width = 100;
            log.warn("evacuation link needs to have unlimited flow cap! this is not implemented yet");
        }


        Coordinate[] bounds = new Coordinate[5];
        bounds[0] = new Coordinate(ls.x0 - ls.dy * width / 2,
                ls.y0 + ls.dx * width / 2);
        bounds[1] = new Coordinate(ls.x0 + ls.dx * length - ls.dy * width / 2,
                ls.y0 + ls.dy * length + ls.dx * width / 2);
        bounds[2] = new Coordinate(ls.x0 + ls.dx * length + ls.dy * width / 2,
                ls.y0 + ls.dy * length - ls.dx * width / 2);
        bounds[3] = new Coordinate(ls.x0 + ls.dy * width / 2,
                ls.y0 - ls.dx * width / 2);
        bounds[4] = bounds[0];

        GeometryFactory geofac = new GeometryFactory();
        LinearRing lr = geofac.createLinearRing(bounds);

        Polygon p = (Polygon) geofac.createPolygon(lr, null).buffer(0.1);
        List<ProtoCell> cells = computeProtoCells(ls.dx, ls.dy, width, length, ls);

        Geometry fromBnd = geofac.createLineString(new Coordinate[]{bounds[3], bounds[4]}).buffer(0.1);
        Geometry toBnd = geofac.createLineString(new Coordinate[]{bounds[1], bounds[2]}).buffer(0.1);

        Map<ProtoCell, CTCell> cellsMap = new HashMap<>();
        Map<ProtoCell, Geometry> geoMap = new HashMap<>();
        int id = 0;
        double area = (1.5 * Math.sqrt(3) * (WIDTH / 2) * (WIDTH / 2));
        for (ProtoCell pt : cells) {
//			double area = (1.5 * Math.sqrt(3) * (WIDTH / 2) * (WIDTH / 2));
            CTCell c = new CTLinkCell(pt.x, pt.y, this.network, this, WIDTH / 2, area);


            cellsMap.put(pt, c);
            Coordinate[] coords = new Coordinate[pt.edges.size() * 2];
            int idx = 0;
            for (GraphEdge ge : pt.edges) {
                coords[idx++] = new Coordinate(ge.x1, ge.y1);
                coords[idx++] = new Coordinate(ge.x2, ge.y2);
            }
            MultiPoint mp = geofac.createMultiPoint(coords);
            Geometry ch = mp.convexHull();
            geoMap.put(pt, ch);
        }
        Envelope e = new Envelope(bounds[0]);
        e.expandToInclude(bounds[1]);
        e.expandToInclude(bounds[2]);
        e.expandToInclude(bounds[3]);
        QuadTree<CTCell> qt = new QuadTree<>(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());

        for (ProtoCell pt : cells) {
            CTCell cell = cellsMap.get(pt);
            Geometry ch = geoMap.get(pt);
            if (p.covers(ch)) {
                this.cells.add(cell);
                qt.put(cell.getX(), cell.getY(), cell);
                for (GraphEdge ge : pt.edges) {
//					debugGe(ge);
                    ProtoCell protoNeighbor = pt.nb.get(ge);
                    Geometry nCh = geoMap.get(protoNeighbor);
                    CTCell neighbor = null;
                    if (p.covers(nCh)) {
                        neighbor = cellsMap.get(protoNeighbor);
                    } else {
                        if (fromBnd.intersects(nCh)) {
                            neighbor = this.usNode.getCTCell();
                            CTCellFace nFace = new CTCellFace(ge.x1, ge.y1, ge.x2, ge.y2, cell, -Math.PI / 2);
                            neighbor.addFace(nFace);
                        } else {
                            if (toBnd.intersects(nCh)) {
                                neighbor = this.dsNode.getCTCell();
                                CTCellFace nFace = new CTCellFace(ge.x1, ge.y1, ge.x2, ge.y2, cell, Math.PI / 2);
                                neighbor.addFace(nFace);
                            }
                        }
                    }

                    if (neighbor != null) {
                        double dir = getAngle(cell.getX(), cell.getY(), (ge.x1 + ge.x2) / 2, (ge.y1 + ge.y2) / 2, cell.getX() + ls.dy, cell.getY() - ls.dx);
                        CTCellFace face = new CTCellFace(ge.x1, ge.y1, ge.x2, ge.y2, neighbor, dir);
                        cell.addFace(face);

                    }
                }
            }
        }


        //identify cells
        Set<CTCell> dsJumpOns = new HashSet<>();
        Set<CTCell> usJumpOns = new HashSet<>();
        for (double incr = 0; incr <= width; incr += WIDTH / 2.) {
            double dsX = bounds[1].x + ls.dy * incr - WIDTH * ls.dx / 2.;
            double dsY = bounds[1].y - ls.dx * incr - WIDTH * ls.dy / 2.;
            double usX = bounds[0].x + ls.dy * incr + WIDTH * ls.dx / 2.;
            double usY = bounds[0].y - ls.dx * incr + WIDTH * ls.dy / 2.;
//			debugEntrances(dsX, usX, dsY, usY);
            CTCell dsJumpOn = qt.getClosest(dsX, dsY);

            dsJumpOns.add(dsJumpOn);
            CTCell usJumpOn = qt.getClosest(usX, usY);
            usJumpOns.add(usJumpOn);


        }


        //create pseudo cells
        this.dsJumpOn = new CTLinkCell(Double.NaN, Double.NaN, this.network, this, WIDTH * 2, area);// * width);
        double dir = Math.PI / 2.;
        for (CTCell ctCell : dsJumpOns) {
            CTCellFace face = new CTCellFace(Double.NaN, Double.NaN, Double.NaN, Double.NaN, ctCell, dir);
            this.dsJumpOn.addFace(face);
            this.dsJumpOn.addNeighbor(ctCell);
            ctCell.addNeighbor(this.dsJumpOn);
        }
        this.usJumpOn = new CTLinkCell(0, 0, this.network, this, WIDTH * 2, area);// * width);
        dir = -Math.PI / 2.;
        for (CTCell ctCell : usJumpOns) {
            CTCellFace face = new CTCellFace(Double.NaN, Double.NaN, Double.NaN, Double.NaN, ctCell, dir);
            this.usJumpOn.addFace(face);
            this.usJumpOn.addNeighbor(ctCell);
            ctCell.addNeighbor(this.usJumpOn);
        }

        //append cells
        if (CTRunner.DEBUG) {
            debugBound(bounds);
        }


//		if (CTRunner.DEBUG) {
//			for (CTCell c : dsJumpOns) {
//				c.g = 255;
//				c.debug(this.em);
//			}
//
//			for (CTCell c : usJumpOns) {
//				c.r = 255;
//				c.g = 0;
//				c.b = 0;
//				c.debug(this.em);
//			}
//		}
    }

    private void pruneEnds(LineSegment ls) {
        List<LineSegment> usAdjacent = this.usNode.getNode().getOutLinks().values().stream().filter(l -> l != this.dsLink).map(LineSegment::createFromLink).collect(Collectors.toList());
        if (usAdjacent.size() > 0) {
            double amount = getPruneAmmountUs(ls, usAdjacent);
            if (amount > 0) {
                ls.x0 = ls.x0 + ls.dx * amount;
                ls.y0 = ls.y0 + ls.dy * amount;
                ls.length -= amount;
            }
        }
        List<LineSegment> dsAdjacent = this.dsNode.getNode().getInLinks().values().stream().filter(l -> l != this.dsLink).map(LineSegment::createFromLink).map(LineSegment::getInverse).collect(Collectors.toList());
        if (dsAdjacent.size() > 0) {
            double amount = getPruneAmmountUs(ls.getInverse(), dsAdjacent);
            if (amount > 0) {
                ls.x1 = ls.x1 - ls.dx * amount;
                ls.y1 = ls.y1 - ls.dy * amount;
                ls.length -= amount;
            }
        }
    }

    private double getPruneAmmountUs(LineSegment ls, List<LineSegment> usAdjacent) {

        usAdjacent.add(ls);
        TreeMap<Double, LineSegment> sorted = usAdjacent.stream().collect(Collectors.toMap(LineSegment::getPseudoAngle, Function.identity(), (e1, e2) -> e1, TreeMap::new));


        LineSegment nb1, nb2;

        if (sorted.firstKey() == ls.getPseudoAngle()) {
            nb1 = sorted.lastEntry().getValue();
            nb2 = sorted.higherEntry(ls.getPseudoAngle()).getValue();
        } else if (sorted.lastKey() == ls.getPseudoAngle()) {
            nb1 = sorted.lowerEntry(ls.getPseudoAngle()).getValue();
            nb2 = sorted.firstEntry().getValue();
        } else {
            nb1 = sorted.lowerEntry(ls.getPseudoAngle()).getValue();
            nb2 = sorted.higherEntry(ls.getPseudoAngle()).getValue();
        }

        double amount1 = pruneUpStream(ls, nb1);
        double amount2 = pruneUpStream(ls, nb2);

        double amount = amount1 > amount2 ? amount1 : amount2;

        return amount;
//        System.out.println("Wow");

    }

    private double pruneUpStream(LineSegment ls, LineSegment n) {

        LineSegment ls1 = LineSegment.createFromCoords(ls.x0 + ls.dy * ls.width / 2, ls.y0 - ls.dx * ls.width / 2, ls.x1 + ls.dy * ls.width / 2, ls.y1 - ls.dx * ls.width / 2);
        LineSegment ls2 = LineSegment.createFromCoords(ls.x0 - ls.dy * ls.width / 2, ls.y0 + ls.dx * ls.width / 2, ls.x1 - ls.dy * ls.width / 2, ls.y1 + ls.dx * ls.width / 2);

        LineSegment n1 = LineSegment.createFromCoords(n.x0 + n.dy * n.width / 2, n.y0 - n.dx * n.width / 2, n.x1 + n.dy * n.width / 2, n.y1 - n.dx * n.width / 2);
        LineSegment n2 = LineSegment.createFromCoords(n.x0 - n.dy * n.width / 2, n.y0 + n.dx * n.width / 2, n.x1 - n.dy * n.width / 2, n.y1 + n.dx * n.width / 2);

        double coeff1 = CGAL.intersectCoeff(ls1, n1);
        double coeff2 = CGAL.intersectCoeff(ls1, n2);
        double coeff3 = CGAL.intersectCoeff(ls2, n1);
        double coeff4 = CGAL.intersectCoeff(ls2, n2);

        return Math.max(Math.max(coeff1, coeff2), Math.max(coeff3, coeff4));

    }

//    private LineSegment createLS() {
//
//    }

    public void debug() {
        if (CTRunner.DEBUG) {

            for (CTCell c : this.cells) {
                c.debug(this.em);
                TextEvent textEvent = new TextEvent(0, c.id + "", c.getX(), c.getY());
                this.em.processEvent(textEvent);
            }
        }
    }

    private double getAngle(double frX, double frY, double toX1, double toY1, double toX2, double toY2) {

        final double l1 = Math.sqrt(3) / 4 * WIDTH;
        double cosAlpha = ((toX1 - frX) * (toX2 - frX) + (toY1 - frY) * (toY2 - frY)) / l1;
        double alpha = Math.acos(cosAlpha);
        if (CGAL.isLeftOfLine(toX1, toY1, frX, frY, toX2, toY2) < 0) {
            alpha -= Math.PI;
            alpha = -(Math.PI + alpha);
        }
//        debugAngle(alpha,frX,frY,toX1,toY1);
        return alpha;
    }

    private List<ProtoCell> computeProtoCells(double dx, double dy, double width, double length, LineSegment ls) {
        List<ProtoCell> cells = new ArrayList<>();

        double w = width + WIDTH - WIDTH / 20;
        double l = length + WIDTH * Math.sqrt(3) / 2;


        Voronoi v = new Voronoi(0.0001);


        List<Double> xl = new ArrayList<>();
        List<Double> yl = new ArrayList<>();
        boolean even = true;
        for (double yIncr = -WIDTH * Math.sqrt(3) / 4; yIncr < l; yIncr += WIDTH * Math.sqrt(3) / 4) {


            double xIncr = 0;
            if (even) {
                xIncr += WIDTH * 0.75;
            }
            even = !even;
            int idx = 0;
            for (; xIncr < w; xIncr += WIDTH * 1.5) {
                double x = ls.x0 - dy * w / 2 + dy * WIDTH * Math.sqrt(3) / 8 + dx * yIncr + dy * xIncr;
                double y = ls.y0 + dx * w / 2 - dx * WIDTH * Math.sqrt(3) / 8 + dy * yIncr - dx * xIncr;

                //				double x = x0 + xIncr*ldx;
                yl.add(y);
                xl.add(x);
                ProtoCell cell = new ProtoCell(x, y, idx++);
                cells.add(cell);

            }

        }
        double[] xa = new double[xl.size()];
        double[] ya = new double[xl.size()];
        for (int i = 0; i < xl.size(); i++) {
            xa[i] = xl.get(i);
            ya[i] = yl.get(i);
        }


        double y0 = ls.y0 + dx * w / 2;
        double x0 = ls.x0 - dy * w / 2;
        double y2 = ls.y0 - dx * w / 2;
        double x2 = ls.x0 + dy * w / 2;
        double y1 = ls.y0 + dy * length - dx * w / 2;
        double x1 = ls.x0 + dx * length + dy * w / 2;
        double y3 = ls.y0 + dy * length + dx * w / 2;
        double x3 = ls.x0 + dx * length - dy * w / 2;

        double minX = x1 < x0 ? x1 : x0;
        minX = minX < x2 ? minX : x2;
        minX = minX < x3 ? minX : x3;
        double maxX = x1 > x0 ? x1 : x0;
        maxX = maxX > x2 ? maxX : x2;
        maxX = maxX > x3 ? maxX : x3;
        double minY = y1 < y0 ? y1 : y0;
        minY = minY < y2 ? minY : y2;
        minY = minY < y3 ? minY : y3;
        double maxY = y1 > y0 ? y1 : y0;
        maxY = maxY > y2 ? maxY : y2;
        maxY = maxY > y3 ? maxY : y3;
        List<GraphEdge> edges = v.generateVoronoi(xa, ya, minX - WIDTH, maxX + WIDTH, minY - WIDTH, maxY + WIDTH);
        for (GraphEdge ge : edges) {
//			debugGe(ge);
//			double aa = ge.x1-ge.x2;
//			double bb = ge.y1-ge.y2;
//			double cc = Math.sqrt(aa*aa+bb*bb);
//			log.info(cc);

            ProtoCell c0 = cells.get(ge.site1);
            c0.edges.add(ge);
            ProtoCell c1 = cells.get(ge.site2);
            c1.edges.add(ge);
            c0.nb.put(ge, c1);
            c1.nb.put(ge, c0);

        }
        return cells;
    }

    private void debugBound(Coordinate[] bounds) {
        if (!CTRunner.DEBUG) {
            return;
        }

        GeometryFactory geofac = new GeometryFactory();


        Optional<Geometry> g = this.cells.parallelStream().flatMap(ctCell -> ctCell.getFaces().stream()).map(f -> (Geometry) geofac.createMultiPoint(new Coordinate[]{new Coordinate(f.x0, f.y0), new Coordinate(f.x1, f.y1)})).reduce(Geometry::union);
        if (g.isPresent()) {
            Geometry hull = g.get().convexHull();
            Coordinate[] coords = hull.getCoordinates();
            for (int i = 1; i < coords.length; i++) {
                LineSegment s = new LineSegment();
                s.x0 = coords[i - 1].x;
                s.y0 = coords[i - 1].y;
                s.x1 = coords[i].x;
                s.y1 = coords[i].y;
                LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
                em.processEvent(le);
            }
        }
//        {
//            LineSegment s = new LineSegment();
//            s.x0 = bounds[0].x;
//            s.y0 = bounds[0].y;
//            s.x1 = bounds[1].x;
//            s.y1 = bounds[1].y;
//            LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
//            em.processEvent(le);
//        }
//        {
//            LineSegment s = new LineSegment();
//            s.x0 = bounds[1].x;
//            s.y0 = bounds[1].y;
//            s.x1 = bounds[2].x;
//            s.y1 = bounds[2].y;
//            LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
//            em.processEvent(le);
//        }
//        {
//            LineSegment s = new LineSegment();
//            s.x0 = bounds[2].x;
//            s.y0 = bounds[2].y;
//            s.x1 = bounds[3].x;
//            s.y1 = bounds[3].y;
//            LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
//            em.processEvent(le);
//        }
//        {
//            LineSegment s = new LineSegment();
//            s.x0 = bounds[3].x;
//            s.y0 = bounds[3].y;
//            s.x1 = bounds[0].x;
//            s.y1 = bounds[0].y;
//            LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
//            em.processEvent(le);
//        }

    }

    private void debugGe(GraphEdge ge) {
        if (!CTRunner.DEBUG) {
            return;
        }
        LineSegment s = new LineSegment();
        s.x0 = ge.x1;
        s.x1 = ge.x2;
        s.y0 = ge.y1;
        s.y1 = ge.y2;
        LineEvent le = new LineEvent(0, s, true, 128, 128, 128, 255, 10, 0.1, 0.2);
        em.processEvent(le);
    }

    private void debugEntrances(double dsX, double usX, double dsY, double usY) {
        {
            LineSegment s = new LineSegment();
            s.x0 = dsX;
            s.y0 = dsY;
            s.x1 = dsX + 0.1;
            s.y1 = dsY + 0.1;
            LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
            em.processEvent(le);
        }
        {
            LineSegment s = new LineSegment();
            s.x0 = usX;
            s.y0 = usY;
            s.x1 = usX + 0.1;
            s.y1 = usY + 0.1;
            LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
            em.processEvent(le);
        }
    }

    public Link getDsLink() {
        return this.dsLink;
    }

    public Link getUsLink() {
        return this.usLink;
    }

    private void debugAngle(double alpha, double frX, double frY, double toX1, double toY1) {
        if (!CTRunner.DEBUG) {
            return;
        }
        int sect = (int) Math.round(10 * (Math.PI / alpha));
        LineSegment ls = new LineSegment();
        ls.x0 = frX;
        ls.y0 = frY;
        ls.x1 = toX1;
        ls.y1 = toY1;
        if (sect == 60) {
            LineEvent le = new LineEvent(0, ls, true, 192, 0, 0, 255, 0);
            em.processEvent(le);
        } else {
            if (sect == 20) {
                LineEvent le = new LineEvent(0, ls, true, 192, 192, 0, 255, 0);
                em.processEvent(le);
            } else {
                if (sect == 12) {
                    LineEvent le = new LineEvent(0, ls, true, 0, 192, 0, 255, 0);
                    em.processEvent(le);
                } else {
                    if (sect == -12) {
                        LineEvent le = new LineEvent(0, ls, true, 0, 192, 192, 255, 0);
                        em.processEvent(le);
                    } else {
                        if (sect == -20) {
                            LineEvent le = new LineEvent(0, ls, true, 0, 0, 192, 255, 0);
                            em.processEvent(le);
                        } else {
                            if (sect == -60) {
                                LineEvent le = new LineEvent(0, ls, true, 192, 0, 192, 255, 0);
                                em.processEvent(le);
                            }
                        }
                    }
                }
            }
        }

    }

    public List<CTCell> getCells() {
        return cells;
    }

//    public void letAgentDepart(CTVehicle veh, double now) {
//
//    }

    public void letAgentDepart(MobsimDriverAgent agent, CTLink link, double now) {

        CTCell cell;
        if (agent.getCurrentLinkId() == this.dsLink.getId()) {
            cell = this.dsJumpOn;
        } else {
            if (agent.getCurrentLinkId() == this.usLink.getId()) {
                cell = this.usJumpOn;
            } else {
                throw new RuntimeException("agent tries to depart on wrong link");
            }
        }
        CTPed p = new CTPed(cell, agent);
        VehicleEntersTrafficEvent e = new VehicleEntersTrafficEvent(Math.ceil(now), p.getDriver().getId(), p.getDriver().getCurrentLinkId(), Id.create(p.getDriver().getId(), Vehicle.class), "walkct", 0);
        this.em.processEvent(e);
        cell.jumpOnPed(p, now);
        //TODO move following to jumpoff method in cell; create pseudo cell class for that purpose
        LinkEnterEvent le = new LinkEnterEvent(Math.ceil(now), Id.create(p.getDriver().getId(), Vehicle.class), p.getDriver().getCurrentLinkId());
        this.em.processEvent(le);
        cell.updateIntendedCellJumpTimeAndChooseNextJumper(now);
    }

    private final class ProtoCell {
        public Map<GraphEdge, ProtoCell> nb = new HashMap<>();
        List<GraphEdge> edges = new ArrayList<>();
        private double x;
        private double y;
        private long id;

        public ProtoCell(double x, double y, long id) {
            this.x = x;
            this.y = y;
            this.id = id;
        }
    }

//    private static final class LineSegment {
//        double frX,frY,toX,toY;
//    }

}
