package playground.balac.carsharing.preprocess.membership;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
    this.links = new QuadTree<>(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
    for (Link link : this.network.getLinks().values()) {
      this.links.put(link.getCoord().getX(), link.getCoord().getY(), (LinkImpl)link);
    }

    LinkImpl closestLink = this.links.getClosest(coordX, coordY);

    return closestLink;
  }

  public static final LinkImpl getClosestLink(Network network, Coord coord)
  {
    Map<Id<Link>, Link> mylinks = new TreeMap<>();
    mylinks.putAll(network.getLinks());

    double distance = (1.0D / 0.0D);
    Id<Link> closestLinkId = Id.create(0L, Link.class);
    for (Link link : network.getLinks().values()) {
      LinkImpl mylink = (LinkImpl)link;
      Double newDistance = Double.valueOf(mylink.calcDistance(coord));
      if (newDistance.doubleValue() < distance) {
        distance = newDistance.doubleValue();
        closestLinkId = link.getId();
      }

    }

    return (LinkImpl)network.getLinks().get(closestLinkId);
  }
}