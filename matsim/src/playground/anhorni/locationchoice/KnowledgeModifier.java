package playground.anhorni.locationchoice;

import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.plans.Knowledge;
import org.matsim.world.Location;


public abstract class KnowledgeModifier {
	
	/** 
	 * Mutates (possibly extends) the location set (set of facilities) which is 
	 * given in the knowledge of a person. Extended location set can be limited 
	 * (narrow locations) (kd-tree of facilities) or it can cover all facilities.
	 * The coice of an additional facility can be done as next best choice or randomly
	 */
	
	protected TreeMap<Id, ? extends Location> facilities;
	
	public KnowledgeModifier(TreeMap<Id, ? extends Location> facilities){
		this.facilities=facilities;
	}
	
	public abstract void modify(Knowledge knowledge);

}