package playground.wdoering.grips.evacuationanalysis.control;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

import playground.wdoering.grips.evacuationanalysis.EvacuationAnalysis.Mode;

public interface Clusterizer {

	public LinkedList<Tuple<Id,Double>> getClusters(LinkedList<Tuple<Id, Double>> data, int n);
	
}
