package playground.ciarif.data;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.network.LinkImpl;


public interface LocationPlanner {

	void runStrategy(TreeMap<Id, LinkImpl> links);

}
