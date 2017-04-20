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
import playground.gregor.sim2d_v4.events.debug.TextEvent;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static playground.gregor.misanthrope.simulation.physics.CTCell.MAX_CELL_WIDTH;
import static playground.gregor.misanthrope.simulation.physics.CTCell.MIN_CELL_WIDTH;


public class CTLink implements CTNetworkEntity {


    private static final Logger log = Logger.getLogger(CTLink.class);
    private static final double EPSILON = 0.00001;
    public static double DESIRED_WIDTH_IN_CELLS = 2;
    private static int EL_WRN_CNT = 0;

    private static int LENGTH_WRN_CNT = 0;



    private final CTNetwork network;
    private final CTNode dsNode;
    private final CTNode usNode;
    private final List<CTCell> cells = new ArrayList<>();
    private Link dsLink;
    private Link usLink;
    private EventsManager em;
    private CTLinkCell dsJumpOn;
    private CTLinkCell usJumpOn;
    private Set<CTCell> dsJumpOns;
    private Set<CTCell> usJumpOns;
    private double cellWidth;

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


        double tmp = ls.width / DESIRED_WIDTH_IN_CELLS;
        if (this.dsLink.getId().toString().contains("el")) {
            if (EL_WRN_CNT++ < 10) {
                log.warn("evacuation link needs to have unlimited flow cap! this is not implemented yet. Setting width to 50m, should be ok for most scenarios");
                if (EL_WRN_CNT == 10) {
                    log.warn(Gbl.FUTURE_SUPPRESSED);
                }
            }
            ls.x1 = ls.x0 += 20 * ls.dx;
            ls.y1 = ls.y0 += 20 * ls.dy;
            ls.length = 20;
            ls.width = 50;
            tmp = 5;
        }


        if (tmp < MIN_CELL_WIDTH) {
            tmp = ls.width;
        } else if (tmp > MAX_CELL_WIDTH) {
            int nrCells = (int) (ls.width / MAX_CELL_WIDTH) + 1;
            tmp = ls.width / nrCells;
        }

        this.cellWidth = tmp;
        double minLength = 3 * Math.sqrt(3) / 2 * this.cellWidth;
        pruneEnds(ls);

        double length = ls.length;
        if (length < minLength) {
            if (LENGTH_WRN_CNT++ < 10) {
                log.warn("Length of link: " + this.dsLink.getId() + " is too small. Increasing it from: " + length + " to: " + minLength);
                if (LENGTH_WRN_CNT == 10) {
                    log.warn(Gbl.FUTURE_SUPPRESSED);
                }
            }
            length = minLength;
        }


        if (Math.abs(ls.dy) < EPSILON) { //fixes a numerical issue
            ls.dy = 0;
        }
        double width = ls.width;

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
        double area = (1.5 * Math.sqrt(3) * (cellWidth / 2) * (cellWidth / 2));
        for (ProtoCell pt : cells) {
            CTCell c = new CTLinkCell(pt.x, pt.y, this.network, this, cellWidth / 2, area);
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
                    cell.addGe(ge);
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
        this.dsJumpOns = new HashSet<>();
        this.usJumpOns = new HashSet<>();
        for (double incr = 0; incr <= width; incr += cellWidth / 8.) {
            double dsX = bounds[1].x + ls.dy * incr - cellWidth * ls.dx / 2.;
            double dsY = bounds[1].y - ls.dx * incr - cellWidth * ls.dy / 2.;
            double usX = bounds[0].x + ls.dy * incr + cellWidth * ls.dx / 2.;
            double usY = bounds[0].y - ls.dx * incr + cellWidth * ls.dy / 2.;
            CTCell dsJumpOn = qt.getClosest(dsX, dsY);

            dsJumpOns.add(dsJumpOn);
            CTCell usJumpOn = qt.getClosest(usX, usY);
            usJumpOns.add(usJumpOn);


        }


        //create pseudo cells
        this.dsJumpOn = new CTLinkCell(Double.NaN, Double.NaN, this.network, this, cellWidth * 2, area);// * width);
        double dir = Math.PI / 2.;
        for (CTCell ctCell : dsJumpOns) {
            CTCellFace face = new CTCellFace(Double.NaN, Double.NaN, Double.NaN, Double.NaN, ctCell, dir);
            this.dsJumpOn.addFace(face);
            this.dsJumpOn.addNeighbor(ctCell);
            ctCell.addNeighbor(this.dsJumpOn);
        }
        this.usJumpOn = new CTLinkCell(0, 0, this.network, this, cellWidth * 2, area);// * width);
        dir = -Math.PI / 2.;
        for (CTCell ctCell : usJumpOns) {
            CTCellFace face = new CTCellFace(Double.NaN, Double.NaN, Double.NaN, Double.NaN, ctCell, dir);
            this.usJumpOn.addFace(face);
            this.usJumpOn.addNeighbor(ctCell);
            ctCell.addNeighbor(this.usJumpOn);
        }
    }

    private void pruneEnds(LineSegment ls) {
        List<LineSegment> usAdjacent = this.usNode.getNode().getOutLinks().values().stream().filter(l -> l != this.dsLink).map(LineSegment::createFromLink).collect(Collectors.toList());
        if (usAdjacent.size() > 0) {
            prune(ls, usAdjacent);

        }
        List<LineSegment> dsAdjacent = this.dsNode.getNode().getInLinks().values().stream().filter(l -> l != this.dsLink).map(LineSegment::createFromLink).map(LineSegment::getInverse).collect(Collectors.toList());
        if (dsAdjacent.size() > 0) {
            LineSegment tmp = ls.getInverse();
            prune(tmp, dsAdjacent);

            ls.x1 = tmp.x0;
            ls.y1 = tmp.y0;
            ls.length = tmp.length;
        }
    }

    private void prune(LineSegment ls, List<LineSegment> usAdjacent) {
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
        CGAL.pruneIntersectingLineSegments(ls, nb1);
        CGAL.pruneIntersectingLineSegments(ls, nb2);
    }


    public void debug() {
        if (CTRunner.DEBUG) {

            for (CTCell c : this.cells) {
                TextEvent textEvent = new TextEvent(0, c.id + "", c.getX(), c.getY());
                this.em.processEvent(textEvent);

                if (dsJumpOns.contains(c) || usJumpOns.contains(c)) {
                    c.debugFill(em, 0, 136, 43, 255);
                } else {
                    c.debugFill(em, 230, 242, 255, 255);
                }
            }

        }
    }

    private double getAngle(double frX, double frY, double toX1, double toY1, double toX2, double toY2) {

        final double l1 = Math.sqrt(3) / 4 * cellWidth;
        double cosAlpha = ((toX1 - frX) * (toX2 - frX) + (toY1 - frY) * (toY2 - frY)) / l1;
        double alpha = Math.acos(cosAlpha);
        if (CGAL.isLeftOfLine(toX1, toY1, frX, frY, toX2, toY2) < 0) {
            alpha -= Math.PI;
            alpha = -(Math.PI + alpha);
        }
        return alpha;
    }

    private List<ProtoCell> computeProtoCells(double dx, double dy, double width, double length, LineSegment ls) {
        List<ProtoCell> cells = new ArrayList<>();

        double w = width + cellWidth - cellWidth / 20;
        double l = length + cellWidth * Math.sqrt(3) / 2;


        Voronoi v = new Voronoi(0.0001);


        List<Double> xl = new ArrayList<>();
        List<Double> yl = new ArrayList<>();
        boolean even = true;
        for (double yIncr = -cellWidth * Math.sqrt(3) / 4; yIncr < l; yIncr += cellWidth * Math.sqrt(3) / 4) {


            double xIncr = 0;
            if (even) {
                xIncr += cellWidth * 0.75;
            }
            even = !even;
            int idx = 0;
            for (; xIncr < w; xIncr += cellWidth * 1.5) {
                double x = ls.x0 - dy * w / 2 + dy * cellWidth * Math.sqrt(3) / 8 + dx * yIncr + dy * xIncr;
                double y = ls.y0 + dx * w / 2 - dx * cellWidth * Math.sqrt(3) / 8 + dy * yIncr - dx * xIncr;

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
        List<GraphEdge> edges = v.generateVoronoi(xa, ya, minX - cellWidth, maxX + cellWidth, minY - cellWidth, maxY + cellWidth);
        for (GraphEdge ge : edges) {
            ProtoCell c0 = cells.get(ge.site1);
            c0.edges.add(ge);
            ProtoCell c1 = cells.get(ge.site2);
            c1.edges.add(ge);
            c0.nb.put(ge, c1);
            c1.nb.put(ge, c0);

        }
        return cells;
    }


    public Link getDsLink() {
        return this.dsLink;
    }

    public Link getUsLink() {
        return this.usLink;
    }



    public List<CTCell> getCells() {
        return cells;
    }

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


}
