package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;

import java.util.Iterator;
import java.util.Vector;

public class Pwvm_LogbookCollection {

	private Vector<Pwvm_Logbook> logbookCollection = new Vector<Pwvm_Logbook>();
	
	/**
	 * Creates a new LogbookCollection object by splitting
	 * a given Logbook l. A logbook is split each time
	 * home business is visited. Thus the number of new
	 * logbooks depends on how many times the home business
	 * is visited.
	 * 
	 * Note that logbooks with one single trip are generated
	 * when the given logbook contains a trip starting and
	 * stopping at home business.
	 * 
	 * @param l the logbook to split
	 */
	public Pwvm_LogbookCollection(Pwvm_Logbook l) {
		
		// Create a temporary logbook
		// with home business, vehicle type and source type equal to the original logbook
		Pwvm_Logbook tmp = new Pwvm_Logbook(l.getTypeOfSource(), l.getVehicleType(), l.getHomeBusinessGeometryInLogbook(), l.getHomeBusinessId(), l.getHomeBusinessGeometryInVirtualWorld());
		tmp.setSourceGeometry(l.getSourceGeometry());
		tmp.setEconomicSector(l.getEconomicSector());
		tmp.setCompanySize(l.getCompanySize());
		
		for (Iterator<Pwvm_LogbookTrip> iterator = l.getTripsIterator(); iterator.hasNext();) {
			//Pwvm_LogbookTrip t = iterator.next();
			Pwvm_LogbookTrip t = new Pwvm_LogbookTrip(iterator.next());
			
			// Add this trip to the temporary logbook
			tmp.addTrip(t);
			
			if (t.getTypeOfDestination() == 4) {
				// the destination's type is home business
				
				// finalize this part
				// by adding it as a separate logbook to the collection
				logbookCollection.add(tmp);
					
				// Then clear the temporary logbook.
				// All trips will be deleted while its home business
				// declaration can be left unchanged.
//				tmp.clear();
				tmp = new Pwvm_Logbook(l.getTypeOfSource(), l.getVehicleType(), l.getHomeBusinessGeometryInLogbook(), l.getHomeBusinessId(), l.getHomeBusinessGeometryInVirtualWorld());
				tmp.setSourceGeometry(l.getSourceGeometry());
				tmp.setEconomicSector(l.getEconomicSector());
				tmp.setCompanySize(l.getCompanySize());
								
				// Its source type must be set to 4 (home business),
				// so as to indicate that the next part will start
				// from the home business
				tmp.setSourceType(4);

			}
			
		}
		
		// add the remaining part to the collection if not empty
		if (tmp.getNumberOfTrips() > 0)
			logbookCollection.add(tmp);

		logbookCollection.trimToSize();
		
	}

	public Iterator<Pwvm_Logbook> getLogbookIterator() {
		return logbookCollection.iterator();
	}


}
