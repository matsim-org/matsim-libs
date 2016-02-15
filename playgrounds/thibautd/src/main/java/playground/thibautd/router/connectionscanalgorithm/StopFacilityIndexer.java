package playground.thibautd.router.connectionscanalgorithm;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author thibautd
 */
public class StopFacilityIndexer {
	final TObjectIntMap<Id<TransitStopFacility>> idToIndex = new TObjectIntHashMap<>();
	final Id<TransitStopFacility>[] indexToId;

	public StopFacilityIndexer(TransitSchedule schedule) {
		final Set<Id<TransitStopFacility>> set = new TreeSet<>();
		for ( Id<TransitStopFacility> stop : schedule.getFacilities().keySet() ) {
			set.add( stop );
		}

		int i = 0;
		indexToId = new Id[ set.size() ];
		for ( Id<TransitStopFacility> id : set ) {
			idToIndex.put( id , i );
			indexToId[ i ] = id;
			i++;
		}
	}

	public Id<TransitStopFacility> getId( int index ) {
		return indexToId[ index ];
	}

	public int getIndex( Id<TransitStopFacility> id ) {
		return idToIndex.get( id );
	}
}
