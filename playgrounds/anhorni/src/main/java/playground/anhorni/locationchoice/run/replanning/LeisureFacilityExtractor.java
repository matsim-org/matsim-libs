package playground.anhorni.locationchoice.run.replanning;

import java.util.Collection;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.locationchoice.utils.QuadTreeRing;

public class LeisureFacilityExtractor {
	
	private final static Logger log = Logger.getLogger(LeisureFacilityExtractor.class);
	private QuadTreeRing<ActivityFacilityImpl> actTree;
	private boolean assignInAnyCase = false;
	private CoordImpl treeCenter = new CoordImpl(683508.5, 246832.9063);
	
	public LeisureFacilityExtractor(final QuadTreeRing<ActivityFacilityImpl> actTree) {
		this.actTree = actTree;
	}
	
	public final ActivityFacilityImpl getFacility(CoordImpl coordStart, double radius) {

		int maxCnt = 15;
		int cnt = 0;
		Collection<ActivityFacilityImpl> locs = new Vector<ActivityFacilityImpl>();
		CoordImpl center = coordStart;
		double smallRadius = Math.max(500.0, radius * 0.1);
		
		while (locs.size() == 0 && cnt < maxCnt) {
			center = this.shootIntoTheWild(coordStart, radius);
			locs = this.actTree.get(center.getX(), center.getY(), smallRadius);
			cnt++;
			
			// try to get the closest
//			ActivityFacility facility = null;
//			if (locs.size() == 0) {
//				if (radius < 100 * 1000) {
//					facility = this.getClosestFacility(coordStart, center, radius);
//				}
//				else {
//					locs = this.getFacilityForLongDistances(coordStart, radius);
//					if (locs.size() == 0) {
//						facility = this.getClosestFacility(
//								coordStart, this.getCenterForLongDistances(coordStart, radius), radius);
//					}
//				}
//				if (facility != null) {
//					return facility;
//				}
//			}		
		}		
		if (locs.size() == 0) {
			if (this.assignInAnyCase) {
				locs = this.getFacilitiesFromTree(coordStart, radius, 0.1, 1);	
			}
			else {
				return null;
			}
		}
		return this.getRandomFacility(locs);		
	}
	
	private ActivityFacilityImpl getRandomFacility(Collection<ActivityFacilityImpl> locs) {
		if (locs.size() == 0) {
			return null;
		}
		int r = 0;
		if (locs.size() > 1) {
			r = MatsimRandom.getRandom().nextInt(locs.size());
		}
		Vector<ActivityFacilityImpl> locations = new Vector<ActivityFacilityImpl>();
		locations.addAll(locs);
		return locations.get(r);
	}
	
	private Collection<ActivityFacilityImpl> getFacilitiesFromTree(CoordImpl coordStart, double radius, double q, int cnt) {
		
		Collection<ActivityFacilityImpl> locs = 
			this.actTree.get(coordStart.getX(), coordStart.getY(), (Math.pow(1.0 + q, cnt) * radius), (Math.pow(1.0 - q, cnt) * radius));
		
		while (locs.size() == 0) {
			if (cnt % 3 == 0 && cnt > 0) {
				if (!this.assignInAnyCase) {
					return new Vector<ActivityFacilityImpl>();
				}
			}
			locs = this.actTree.get(coordStart.getX(), coordStart.getY(), (Math.pow(1.0 + q, cnt) * radius), (Math.pow(1.0 - q, cnt) * radius));
			cnt++;
		}
		return locs;
	}
		
	private CoordImpl shootIntoTheWild(CoordImpl coordStart, double radius) {
		double xStart = coordStart.getX();
		double yStart = coordStart.getY();
		
		radius = Math.max(1.0, radius);
		double xDelta = this.getSign() * MatsimRandom.getRandom().nextInt((int)Math.round(radius));
		double yDelta = this.getSign() * Math.sqrt(Math.pow(radius, 2.0) - Math.pow(xDelta, 2.0));
		
		double xCenter = xStart + xDelta;
		double yCenter = yStart + yDelta;
		
		return new CoordImpl(xCenter, yCenter);	
	}
		
	private Collection<ActivityFacilityImpl> getFacilityForLongDistances(CoordImpl coordStart, double radius) {	
		CoordImpl center = this.getCenterForLongDistances(coordStart, radius);		
		return this.actTree.get(center.getX(), center.getY(), 8.0 * 1000.0);		
	}
	
	private CoordImpl getCenterForLongDistances(CoordImpl coordStart, double radius)  {
		double distance2Treecenter = treeCenter.calcDistance(coordStart);
		double factor = radius / distance2Treecenter;
		
		double xDelta = factor * (treeCenter.getX() - coordStart.getX());
		double yDelta = factor * (treeCenter.getY() - coordStart.getY());
		
		double xCenter = coordStart.getX() + xDelta;
		double yCenter = coordStart.getY() + yDelta;		
		return new CoordImpl(xCenter, yCenter);
	}

	private ActivityFacilityImpl getClosestFacility(CoordImpl coordStart, CoordImpl center, double radius) {
		ActivityFacilityImpl facility = this.actTree.get(center.getX(), center.getY());
		
		double difference = Math.abs(coordStart.calcDistance(facility.getCoord()) - radius);
		
		if (difference > 0.1 * radius) {
			return null;
		}
		return facility;
	}
	
	private double getSign() {
		double signfactor = -1.0;
		if (MatsimRandom.getRandom().nextDouble() > 0.5) {
			signfactor = 1.0;
		}
		return signfactor;
	}

	public void setAssignInAnyCase(boolean assignInAnyCase) {
		this.assignInAnyCase = assignInAnyCase;
	}
}
