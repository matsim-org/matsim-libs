package playground.smetzler.bike.old;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimFactory;

public interface BikeLinkFactory extends MatsimFactory {

	public BikeLink createBikeLink(Id<Link> id, Node from, Node to, Network network, double length, double freespeed, double capacity, double nOfLanes, String cycleway, String cyclewaySurface);

}