package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.retailers.utils.Utils;

public class RandomRetailerStrategy extends RetailerStrategyImpl
{

	private TreeMap<Id, ActivityFacilityImpl> movedFacilities = new TreeMap<Id, ActivityFacilityImpl>();

	public RandomRetailerStrategy(Controler controler) {
		super(controler);
	}
	
	
	public Map<Id, ActivityFacilityImpl> moveFacilities(Map<Id, ActivityFacilityImpl> facilities, TreeMap<Id, LinkRetailersImpl> freeLinks) 
	{
		log.info("available Links are= " + freeLinks);
		log.info("The facilities are= " + facilities);
		this.retailerFacilities=facilities;
		for (ActivityFacilityImpl f : this.retailerFacilities.values()) 
		{
			int rd = MatsimRandom.getRandom().nextInt(freeLinks.size());
			LinkRetailersImpl newLink =(LinkRetailersImpl) freeLinks.values().toArray()[rd];
			LinkRetailersImpl oldLink = new LinkRetailersImpl(this.controler.getNetwork().getLinks().get(f.getLinkId()), controler.getNetwork(), 0.0, 0.0);
			Utils.moveFacility((ActivityFacilityImpl) f,newLink);
			freeLinks.put(oldLink.getId(),oldLink );
			this.movedFacilities.put(f.getId(),f);
		}
		return this.movedFacilities;
	}
}
