package opdytsintegration;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class DistanceBasedFilter {

	private final Coord center;

	private final double radius;

	public DistanceBasedFilter(final double centerX, final double centerY,
			final double radius) {
		this.center = CoordUtils.createCoord(centerX, centerY);
		this.radius = radius;
	}

	public boolean accept(final Coord coord) {
		return (CoordUtils.calcEuclideanDistance(coord, this.center) <= this.radius);
	}

	public boolean accept(final Node node) {
		return this.accept(node.getCoord());
	}

	public boolean accept(final Link link) {
		// TODO This does not pick up secants.
		return (this.accept(link.getFromNode()) || this
				.accept(link.getToNode()));
	}

	public Set<Id<Link>> allAcceptedLinkIds(
			final Collection<? extends Link> candidateLinks) {
		final Set<Id<Link>> result = new LinkedHashSet<>();
		for (Link candidate : candidateLinks) {
			if (this.accept(candidate)) {
				result.add(candidate.getId());
			}
		}
		return result;
	}
}
