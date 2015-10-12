package playground.gregor.ctsim.simulation.physics;

import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;
import com.vividsolutions.jts.geom.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.events.debug.LineEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CTLink implements CTNetworkEntity {


    static final double WIDTH = 1;
    private static final double EPSILON = 0.00001;
    private final CTNetwork network;
    private final CTNode dsNode;
    private final CTNode usNode;
    private final List<CTCell> cells = new ArrayList<>();
    private Link dsLink;
    private Link usLink;
    private EventsManager em;

    public CTLink(Link l, Link rev, EventsManager em, CTNetwork ctNetwork, CTNode from, CTNode to) {
        this.dsLink = l;
        this.dsNode = to;
        this.usNode = from;
        this.usLink = rev;
        this.em = em;
        this.network = ctNetwork;
        init();
    }

    private void init() {

        //this requires a planar coordinate system
        double dx = (this.dsLink.getToNode().getCoord().getX() - this.dsLink.getFromNode().getCoord().getX()) / this.dsLink.getLength();
        double dy = (this.dsLink.getToNode().getCoord().getY() - this.dsLink.getFromNode().getCoord().getY()) / this.dsLink.getLength();

        if (Math.abs(dy) < EPSILON) { //fixes a numerical issue
            dy = 0;
        }

        debugBound(dx, dy);


        Coordinate[] bounds = new Coordinate[5];
        bounds[0] = new Coordinate(this.dsLink.getFromNode().getCoord().getX() - dy * this.dsLink.getCapacity() / 2,
                this.dsLink.getFromNode().getCoord().getY() + dx * this.dsLink.getCapacity() / 2);
        bounds[1] = new Coordinate(this.dsLink.getToNode().getCoord().getX() - dy * this.dsLink.getCapacity() / 2,
                this.dsLink.getToNode().getCoord().getY() + dx * this.dsLink.getCapacity() / 2);
        bounds[2] = new Coordinate(this.dsLink.getToNode().getCoord().getX() + dy * this.dsLink.getCapacity() / 2,
                this.dsLink.getToNode().getCoord().getY() - dx * this.dsLink.getCapacity() / 2);
        bounds[3] = new Coordinate(this.dsLink.getFromNode().getCoord().getX() + dy * this.dsLink.getCapacity() / 2,
                this.dsLink.getFromNode().getCoord().getY() - dx * this.dsLink.getCapacity() / 2);
        bounds[4] = new Coordinate(this.dsLink.getFromNode().getCoord().getX() - dy * this.dsLink.getCapacity() / 2,
                this.dsLink.getFromNode().getCoord().getY() + dx * this.dsLink.getCapacity() / 2);
        GeometryFactory geofac = new GeometryFactory();
        LinearRing lr = geofac.createLinearRing(bounds);
        Polygon p = (Polygon) geofac.createPolygon(lr, null).buffer(0.1);
        List<ProtoCell> cells = computeProtoCells(dx, dy);

        Geometry fromBnd = geofac.createLineString(new Coordinate[]{bounds[3], bounds[4]}).buffer(0.1);
        Geometry toBnd = geofac.createLineString(new Coordinate[]{bounds[1], bounds[2]}).buffer(0.1);

        Map<ProtoCell, CTCell> cellsMap = new HashMap<>();
        Map<ProtoCell, Geometry> geoMap = new HashMap<>();
        for (ProtoCell pt : cells) {
            CTCell c = new CTLinkCell(pt.x, pt.y, this.network, this);
            c.setArea(1.5 * Math.sqrt(3) * WIDTH * WIDTH);

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

        for (ProtoCell pt : cells) {
            CTCell cell = cellsMap.get(pt);
            Geometry ch = geoMap.get(pt);
            if (p.covers(ch)) {
                this.cells.add(cell);
                for (GraphEdge ge : pt.edges) {
                    debugGe(ge);
                    ProtoCell protoNeighbor = pt.nb.get(ge);
                    Geometry nCh = geoMap.get(protoNeighbor);
                    CTCell neighbor = null;
                    if (p.covers(nCh)) {
                        neighbor = cellsMap.get(protoNeighbor);
                    }
                    else {
                        if (fromBnd.intersects(nCh)) {
                            neighbor = this.usNode.getCTCell();
                            CTCellFace nFace = new CTCellFace(ge.x1, ge.y1, ge.x2, ge.y2, cell, -Math.PI / 2);
                            neighbor.addFace(nFace);
                        }
                        else {
                            if (toBnd.intersects(nCh)) {
                                neighbor = this.dsNode.getCTCell();
                                CTCellFace nFace = new CTCellFace(ge.x1, ge.y1, ge.x2, ge.y2, cell, Math.PI / 2);
                                neighbor.addFace(nFace);
                            }
                        }
                    }

                    if (neighbor != null) {
                        double dir = getAngle(cell.getX(), cell.getY(), (ge.x1 + ge.x2) / 2, (ge.y1 + ge.y2) / 2, cell.getX() + dy, cell.getY() - dx);
                        CTCellFace face = new CTCellFace(ge.x1, ge.y1, ge.x2, ge.y2, neighbor, dir);
                        cell.addFace(face);
                    }
                }
            }
        }


        for (CTCell c : this.cells) {
            c.debug(this.em);
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

    private List<ProtoCell> computeProtoCells(double dx, double dy) {
        List<ProtoCell> cells = new ArrayList<>();

        double w = this.dsLink.getCapacity() + WIDTH * 2;
        double l = this.dsLink.getLength() + WIDTH * Math.sqrt(3) / 2;


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
                double y = this.dsLink.getFromNode().getCoord().getY() + dx * w / 2 - dx * WIDTH * Math.sqrt(3) / 8 + dy * yIncr - dx * xIncr;
                double x = this.dsLink.getFromNode().getCoord().getX() - dy * w / 2 + dy * WIDTH * Math.sqrt(3) / 8 + dx * yIncr + dy * xIncr;
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

        double y0 = this.dsLink.getFromNode().getCoord().getY() + dx * w / 2;
        double x0 = this.dsLink.getFromNode().getCoord().getX() - dy * w / 2;
        double y2 = this.dsLink.getFromNode().getCoord().getY() - dx * w / 2;
        double x2 = this.dsLink.getFromNode().getCoord().getX() + dy * w / 2;
        double y1 = this.dsLink.getToNode().getCoord().getY() - dx * w / 2;
        double x1 = this.dsLink.getToNode().getCoord().getX() + dy * w / 2;
        double y3 = this.dsLink.getToNode().getCoord().getY() + dx * w / 2;
        double x3 = this.dsLink.getToNode().getCoord().getX() - dy * w / 2;

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

            ProtoCell c0 = cells.get(ge.site1);
            c0.edges.add(ge);
            ProtoCell c1 = cells.get(ge.site2);
            c1.edges.add(ge);
            c0.nb.put(ge, c1);
            c1.nb.put(ge, c0);

        }
        return cells;
    }

    private void debugGe(GraphEdge ge) {
        LineSegment s = new LineSegment();
        s.x0 = ge.x1;
        s.x1 = ge.x2;
        s.y0 = ge.y1;
        s.y1 = ge.y2;
        LineEvent le = new LineEvent(0, s, true, 128, 128, 128, 255, 10, 0.1, 0.2);
        em.processEvent(le);
    }

    private void debugBound(double dx, double dy) {


        {
            LineSegment s = new LineSegment();
            s.x0 = this.dsLink.getFromNode().getCoord().getX() - dy * this.dsLink.getCapacity() / 2;
            s.x1 = this.dsLink.getToNode().getCoord().getX() - dy * this.dsLink.getCapacity() / 2;
            s.y0 = this.dsLink.getFromNode().getCoord().getY() + dx * this.dsLink.getCapacity() / 2;
            s.y1 = this.dsLink.getToNode().getCoord().getY() + dx * this.dsLink.getCapacity() / 2;
            LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
            em.processEvent(le);
        }
        {
            LineSegment s = new LineSegment();
            s.x0 = this.dsLink.getFromNode().getCoord().getX() - dy * this.dsLink.getCapacity() / 2;
            s.x1 = this.dsLink.getFromNode().getCoord().getX() + dy * this.dsLink.getCapacity() / 2;
            s.y0 = this.dsLink.getFromNode().getCoord().getY() + dx * this.dsLink.getCapacity() / 2;
            s.y1 = this.dsLink.getFromNode().getCoord().getY() - dx * this.dsLink.getCapacity() / 2;
            LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
            em.processEvent(le);
        }
        {
            LineSegment s = new LineSegment();
            s.x0 = this.dsLink.getFromNode().getCoord().getX() + dy * this.dsLink.getCapacity() / 2;
            s.x1 = this.dsLink.getToNode().getCoord().getX() + dy * this.dsLink.getCapacity() / 2;
            s.y0 = this.dsLink.getFromNode().getCoord().getY() - dx * this.dsLink.getCapacity() / 2;
            s.y1 = this.dsLink.getToNode().getCoord().getY() - dx * this.dsLink.getCapacity() / 2;
            LineEvent le = new LineEvent(0, s, true, 0, 0, 0, 255, 0);
            em.processEvent(le);
        }
        {
            LineSegment s = new LineSegment();
            s.x0 = this.dsLink.getToNode().getCoord().getX() - dy * this.dsLink.getCapacity() / 2;
            s.x1 = this.dsLink.getToNode().getCoord().getX() + dy * this.dsLink.getCapacity() / 2;
            s.y0 = this.dsLink.getToNode().getCoord().getY() + dx * this.dsLink.getCapacity() / 2;
            s.y1 = this.dsLink.getToNode().getCoord().getY() - dx * this.dsLink.getCapacity() / 2;
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
        int sect = (int) Math.round(10 * (Math.PI / alpha));
        LineSegment ls = new LineSegment();
        ls.x0 = frX;
        ls.y0 = frY;
        ls.x1 = toX1;
        ls.y1 = toY1;
        if (sect == 60) {
            LineEvent le = new LineEvent(0, ls, true, 192, 0, 0, 255, 0);
            em.processEvent(le);
        }
        else {
            if (sect == 20) {
                LineEvent le = new LineEvent(0, ls, true, 192, 192, 0, 255, 0);
                em.processEvent(le);
            }
            else {
                if (sect == 12) {
                    LineEvent le = new LineEvent(0, ls, true, 0, 192, 0, 255, 0);
                    em.processEvent(le);
                }
                else {
                    if (sect == -12) {
                        LineEvent le = new LineEvent(0, ls, true, 0, 192, 192, 255, 0);
                        em.processEvent(le);
                    }
                    else {
                        if (sect == -20) {
                            LineEvent le = new LineEvent(0, ls, true, 0, 0, 192, 255, 0);
                            em.processEvent(le);
                        }
                        else {
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
        CTCell cell = null;
        CTPed p = new CTPed(cell, agent);
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
