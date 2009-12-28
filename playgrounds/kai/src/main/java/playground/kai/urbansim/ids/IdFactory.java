package playground.kai.urbansim.ids;

import org.matsim.api.core.v01.Id;

/**
 * In the urbansim interface, there are a fair number of lookups of the type personId -> householdId -> buildingId -> parcelId.
 * To get some stability into these, I wanted to have Ids that at least throw a runtime error if I do the wrong lookup.  
 * Therefore, in this part of the project, these Ids are typed.  This is not supposed to be used elsewhere in Matsim.
 * 
 * @author nagel
 * 
 * @date dec 2008
 *
 */
@Deprecated
public interface IdFactory {
	public Id createId(String str) ;
	
	public Id createId(long ii) ;
	
}
