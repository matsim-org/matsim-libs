package playground.mzilske.osm;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.filter.v0_6.AreaFilter;

public class ClipIncompleteEntities extends AreaFilter {

	public ClipIncompleteEntities(IdTrackerType idTrackerType,
			boolean clipIncompleteEntities, boolean completeWays,
			boolean completeRelations) {
		super(idTrackerType, true, completeWays, completeRelations);
	}

	@Override
	protected boolean isNodeWithinArea(Node arg0) {
		return true;
	}

}
