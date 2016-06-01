package sergio;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.network.Link;

import gunnar.ihop2.regent.demandreading.Zone;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Trip {

	public final Zone fromZone;

	public final Zone toZone;

	public final List<Link> links;

	Trip(final Zone fromZone, final Zone toZone, final List<Link> route) {
		this.fromZone = fromZone;
		this.toZone = toZone;
		this.links = Collections.unmodifiableList(route);
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append(this.fromZone.getId());
		result.append(",");
		result.append(this.toZone.getId());
		for (Link link : this.links) {
			result.append(",");
			result.append(link.getId());
		}
		return result.toString();
	}
}
