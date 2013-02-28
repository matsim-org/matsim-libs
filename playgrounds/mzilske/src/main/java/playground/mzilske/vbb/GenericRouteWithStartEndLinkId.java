package playground.mzilske.vbb;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.AbstractRoute;
import org.matsim.core.population.routes.GenericRoute;

public class GenericRouteWithStartEndLinkId extends AbstractRoute implements GenericRoute, Cloneable {

	public GenericRouteWithStartEndLinkId(Id startLinkId, Id endLinkId) {
		super(startLinkId, endLinkId);
	}

	@Override
	public void setRouteDescription(Id startLinkId, String routeDescription, Id endLinkId) {
		String[] parts = routeDescription.split("===", 2);
		setStartLinkId(new IdImpl(parts[0]));
		setEndLinkId(new IdImpl(parts[1]));
	}

	@Override
	public String getRouteDescription() {
		return getStartLinkId() + "===" + getEndLinkId();
	}

	@Override
	public String getRouteType() {
		return "genericRouteFromTo";
	}

}
