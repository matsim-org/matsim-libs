package playground.anhorni.csestimation;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.QuadTree;

public class Utils {
	
	public static QuadTree<ShopLocation> buildShopQuadTree(TreeMap<Id, ShopLocation> shops) {
		Gbl.startMeasurement();
		System.out.println("      building shop quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ShopLocation shop : shops.values()) {
			if (shop.getCoord().getX() < minx) { minx = shop.getCoord().getX(); }
			if (shop.getCoord().getY() < miny) { miny = shop.getCoord().getY(); }
			if (shop.getCoord().getX() > maxx) { maxx = shop.getCoord().getX(); }
			if (shop.getCoord().getY() > maxy) { maxy = shop.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<ShopLocation> shopQuadTree = new QuadTree<ShopLocation>(minx, miny, maxx, maxy);
		for (ShopLocation shop : shops.values()) {
			shopQuadTree.put(shop.getCoord().getX(), shop.getCoord().getY(), shop);
		}
		Gbl.printRoundTime();
		return shopQuadTree;
	}

}
