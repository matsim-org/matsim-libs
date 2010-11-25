package playground.ciarif.flexibletransports.network;

import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.algorithms.CalcBoundingBox;
import org.matsim.core.utils.collections.QuadTree;

public class MyLinkUtils
{
  private QuadTree<LinkImpl> links = null;
  private Network network = null;

  private static final Logger log = Logger.getLogger(MyLinkUtils.class);

  public MyLinkUtils(Network network) {
    this.network = network;
  }

  public LinkImpl getClosestLink(double coordX, double coordY) {
    CalcBoundingBox bbox = new CalcBoundingBox();
    bbox.run(this.network);
    this.links = new QuadTree(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
    for (Link link : this.network.getLinks().values()) {
      this.links.put(link.getCoord().getX(), link.getCoord().getY(), (LinkImpl)link);
    }
    log.info("CoordX= " + coordX + "CoordY" + coordY);
    LinkImpl closestLink = (LinkImpl)this.links.get(coordX, coordY);
    log.info("Nearest link = " + closestLink.getId());
    return closestLink;
  }

  public static final LinkImpl getClosestLink(Network network, Coord coord)
  {
    log.info("Coord= " + coord);
    Map mylinks = new TreeMap();
    mylinks.putAll(network.getLinks());

    double distance = (1.0D / 0.0D);
    Id closestLinkId = new IdImpl(0L);
    for (Link link : network.getLinks().values()) {
      LinkImpl mylink = (LinkImpl)link;
      Double newDistance = Double.valueOf(mylink.calcDistance(coord));
      if (newDistance.doubleValue() < distance) {
        distance = newDistance.doubleValue();
        closestLinkId = link.getId();
      }

    }

    log.info("Nearest link = " + closestLinkId);
    return ((LinkImpl)network.getLinks().get(closestLinkId));
  }
}