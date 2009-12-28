package playground.anhorni.choiceSetGeneration.depr;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.anhorni.choiceSetGeneration.choicesetextractors.ChoiceSetExtractor;
import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;
import playground.anhorni.choiceSetGeneration.helper.SpanningTree;
import playground.anhorni.choiceSetGeneration.helper.ZHFacility;

public class ExtractWalkChoiceSetsEllipse extends ChoiceSetExtractor {


	private final static Logger log = Logger.getLogger(ExtractWalkChoiceSetsEllipse.class);
	private QuadTree<ZHFacility> shoppingQuadTree = null;
	private double walkingSpeed = 1.39;
	
	public ExtractWalkChoiceSetsEllipse(Controler controler, List<ZHFacility> zhFacilities, double walkingSpeed, 
			List<ChoiceSet> choiceSets, int tt) {
		
		super(controler, choiceSets, tt);
		this.initShoppingTree(zhFacilities);
		this.walkingSpeed = walkingSpeed;
	}
	public void run() {
		log.info("computing walk choice sets...:");
		super.computeChoiceSets();
	}
	
	
	@Override
	protected void computeChoiceSet(ChoiceSet choiceSet, SpanningTree spanningTree, String type,
			Controler controler, int tt) {

/*
		Coord referencePoint = choiceSet.getReferencePoint();			
		double radius = choiceSet.getTravelTimeBudget()/2.0 * this.walkingSpeed;	
		
		ArrayList<ZHFacility> facilities = (ArrayList<ZHFacility>)this.shoppingQuadTree.get(
				referencePoint.getX(), referencePoint.getY(), radius);
				
		Iterator<ZHFacility> zhfacility_it = facilities.iterator();
		while (zhfacility_it.hasNext()) {
			ZHFacility facility = zhfacility_it.next();
			if (!inEllipse(facility, choiceSet)) {
				facilities.remove(facility);
			}
		}
		if (facilities != null) {
			choiceSet.addFacilities(facilities, null, null);
		}
*/
	}
	
	private boolean inEllipse(ZHFacility facility, ChoiceSet choiceSet) {
		
			double dist0 = CoordUtils.calcDistance(choiceSet.getTrip().getBeforeShoppingAct().getCoord(), facility.getMappedPosition());
			double dist1 = CoordUtils.calcDistance(choiceSet.getTrip().getAfterShoppingAct().getCoord(), facility.getMappedPosition());
			
			if (dist0 + dist1 <= choiceSet.getTravelTimeBudget() * this.walkingSpeed) return true;
			else return false;
	}
	
	private void initShoppingTree(List<ZHFacility> zhFacilities) {
		TreeMap<Id, ZHFacility> facilities = new TreeMap<Id, ZHFacility>();
		
		Iterator<ZHFacility> zhfacility_it = zhFacilities.iterator();
		while (zhfacility_it.hasNext()) {
			ZHFacility facility = zhfacility_it.next();
			facilities.put(facility.getId(), facility);
		}			
		this.shoppingQuadTree = this.builFacQuadTree(facilities);
	}


	private QuadTree<ZHFacility> builFacQuadTree(TreeMap<Id, ZHFacility> facilities_of_type) {
		Gbl.startMeasurement();
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
	
		for (final ZHFacility f : facilities_of_type.values()) {
			if (f.getExactPosition().getX() < minx) { minx = f.getExactPosition().getX(); }
			if (f.getExactPosition().getY() < miny) { miny = f.getExactPosition().getY(); }
			if (f.getExactPosition().getX() > maxx) { maxx = f.getExactPosition().getX(); }
			if (f.getExactPosition().getY() > maxy) { maxy = f.getExactPosition().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<ZHFacility> quadtree = new QuadTree<ZHFacility>(minx, miny, maxx, maxy);
		for (final ZHFacility f : facilities_of_type.values()) {
			quadtree.put(f.getExactPosition().getX(),f.getExactPosition().getY(),f);
		}
		log.info("    done");
		Gbl.printRoundTime();
		return quadtree;
	}
}
