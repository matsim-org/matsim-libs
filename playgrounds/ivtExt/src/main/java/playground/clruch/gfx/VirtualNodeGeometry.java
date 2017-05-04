package playground.clruch.gfx;

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.opt.ConvexHull;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.OsmLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;

class VirtualNodeGeometry {
    Map<VirtualNode, Tensor> convexHulls = new LinkedHashMap<>(); // ordering matters

    VirtualNodeGeometry(MatsimStaticDatabase db, VirtualNetwork virtualNetwork) {
        if (virtualNetwork == null)
            return;
        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
            Tensor coords = Tensors.empty();
            for (Link link : virtualNode.getLinks()) {
                int index = db.getLinkIndex(link);
                OsmLink osmLink = db.getOsmLink(index);
                Coord coord = osmLink.getAt(.5);
                coords.append(Tensors.vector(coord.getX(), coord.getY()));
            }
            convexHulls.put(virtualNode, ConvexHull.of(coords));
        }
    }

    Map<VirtualNode, Shape> getShapes(MatsimMapComponent matsimMapComponent) {
        Map<VirtualNode, Shape> map = new LinkedHashMap<>(); // ordering matters
        for (Entry<VirtualNode, Tensor> entry : convexHulls.entrySet()) {
            Tensor hull = entry.getValue();
            Path2D path2d = new Path2D.Double();
            boolean init = false;
            for (Tensor vector : hull) {
                Coord coord = new Coord( //
                        vector.Get(0).number().doubleValue(), //
                        vector.Get(1).number().doubleValue());
                Point point = matsimMapComponent.getMapPositionAlways(coord);
                if (!init) {
                    init = true;
                    path2d.moveTo(point.getX(), point.getY());
                } else
                    path2d.lineTo(point.getX(), point.getY());
            }
            path2d.closePath();
            map.put(entry.getKey(), path2d);
        }
        return map;
    }
}
