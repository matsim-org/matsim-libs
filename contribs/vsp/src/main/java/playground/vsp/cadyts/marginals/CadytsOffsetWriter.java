package playground.vsp.cadyts.marginals;

import cadyts.utilities.misc.DynamicDataXMLFileIO;
import org.matsim.api.core.v01.Id;

class CadytsOffsetWriter extends DynamicDataXMLFileIO<Id<DistanceDistribution.DistanceBin>> {

	@Override
	protected String key2attrValue(Id<DistanceDistribution.DistanceBin> key) {
		return key.toString();
	}

	@Override
	protected Id<DistanceDistribution.DistanceBin> attrValue2key(String string) {
		return Id.create(string, DistanceDistribution.DistanceBin.class);
	}
}
