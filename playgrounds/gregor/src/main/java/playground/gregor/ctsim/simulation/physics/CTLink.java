package playground.gregor.ctsim.simulation.physics;

import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;
import com.vividsolutions.jts.geom.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.events.debug.LineEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CTLink {


    private static final double EPSILON = 0.00001;

    private static final double WIDTH = 1;
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
            CTCell c = new CTCell(pt.x, pt.y);
            this.cells.add(c);
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
                for (GraphEdge ge : pt.edges) {
//					debugGE(ge,0,255,0);
                    ProtoCell protoNeighbor = pt.nb.get(ge);
                    Geometry nCh = geoMap.get(protoNeighbor);
                    CTCell neighbor = null;
                    if (p.covers(nCh)) {
                        neighbor = cellsMap.get(protoNeighbor);
                    }
                    else {
                        if (fromBnd.intersects(nCh)) {
                            neighbor = this.usNode.getCTCell();
                            CTCellFace nFace = new CTCellFace(ge.x1, ge.y1, ge.x2, ge.y2, cell);
                            neighbor.addFace(nFace);
                        }
                        else {
                            if (toBnd.intersects(nCh)) {
                                neighbor = this.dsNode.getCTCell();
                                CTCellFace nFace = new CTCellFace(ge.x1, ge.y1, ge.x2, ge.y2, cell);
                                neighbor.addFace(nFace);
                            }
                        }
                    }
                    CTCellFace face = new CTCellFace(ge.x1, ge.y1, ge.x2, ge.y2, neighbor);
                    cell.addFace(face);
                }
            }
        }


        for (CTCell c : this.cells) {
            c.debug(this.em);
        }
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
                LineSegment ls = new LineSegment();
                ls.x0 = x;
                ls.x1 = x + 0.1;
                ls.y0 = y;
                ls.y1 = y + 0.1;
                LineEvent le = new LineEvent(0, ls, true, 0, 0, 0, 255, 0);
                em.processEvent(le);
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

            LineSegment ls = new LineSegment();
//			ls.x0 = ge.x1;
//			ls.x1 = ge.x2;
//			ls.y0 = ge.y1;
//			ls.y1 = ge.y2;
//			LineEvent le = new LineEvent(0,ls,true,0,128,128,255,0);
//			em.processEvent(le);
        }
        return cells;
    }


    private void debugBound(double dx, double dy) {
        {
            LineSegment s = new LineSegment();
            s.x0 = this.dsLink.getFromNode().getCoord().getX();
            s.x1 = this.dsLink.getToNode().getCoord().getX();
            s.y0 = this.dsLink.getFromNode().getCoord().getY();
            s.y1 = this.dsLink.getToNode().getCoord().getY();
            LineEvent le = new LineEvent(0, s, true, 0, 128, 128, 255, 0, 0.2, 1);
            em.processEvent(le);


        }

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
