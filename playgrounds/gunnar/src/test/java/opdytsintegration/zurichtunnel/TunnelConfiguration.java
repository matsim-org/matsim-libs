package opdytsintegration.zurichtunnel;

import java.util.LinkedHashSet;
import java.util.Set;

import optdyts.DecisionVariable;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.network.NetworkUtils;

import floetteroed.utilities.Units;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TunnelConfiguration implements DecisionVariable {

	private final TunnelFactory tunnelFactory;

	private final Set<Tunnel> tunnels = new LinkedHashSet<Tunnel>();

	TunnelConfiguration(final TunnelFactory tunnelFactory,
			final Tunnel... tunnels) {
		this.tunnelFactory = tunnelFactory;
		for (Tunnel tunnel : tunnels) {
			this.tunnels.add(tunnel);
		}
	}

	@Override
	public void implementInSimulation() {

		this.tunnelFactory.removeAllTunnels();

		final NetworkFactory nf = this.tunnelFactory.network.getFactory();

		for (Tunnel tunnel : this.tunnels) {

			final double length = NetworkUtils.getEuclidianDistance(
					tunnel.fromNode.getCoord(), tunnel.toNode.getCoord());
			final double freespeed = tunnel.maxSpeed_km_h * Units.M_S_PER_KM_H;
			final double capacity = tunnel.capacity_veh_h / 3600.0
					* this.tunnelFactory.network.getCapacityPeriod();
			
			{
				final Link l1 = nf.createLink(
						Id.create(tunnel.link1Id, Link.class), tunnel.fromNode,
						tunnel.toNode);
				l1.setLength(length);
				l1.setFreespeed(freespeed);
				l1.setCapacity(capacity);
				l1.setNumberOfLanes(tunnel.lanes);
				this.tunnelFactory.network.addLink(l1);
			}

			{
				final Link l2 = nf.createLink(
						Id.create(tunnel.link2Id, Link.class), tunnel.toNode,
						tunnel.fromNode);
				l2.setLength(length);
				l2.setFreespeed(freespeed);
				l2.setCapacity(capacity);
				l2.setNumberOfLanes(tunnel.lanes);
				this.tunnelFactory.network.addLink(l2);
			}
		}

		// System.out.println(">>>>> number of links AFTER addition of tunnel: "
		// + this.tunnelFactory.network.getLinks().size());

	}

	@Override
	public String toString() {
		return this.tunnels.toString();
	}
}
