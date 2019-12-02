package playgroundMeng.publicTransitServiceAnalysis.gridAnalysis;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.GridImp;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.LinkExtendImp;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.TransitStopFacilityExtendImp;
import playgroundMeng.publicTransitServiceAnalysis.others.PtAccessabilityConfig;

public class InfrastructureIntoGridDivider {

	public static void divideLinksIntoGrid(GridImp gridImp, List<LinkExtendImp> linkExtendImps) {

		GeometryFactory gf = new GeometryFactory();
		Geometry geometry = gridImp.getGeometry();
		for (LinkExtendImp linkExtendImp : linkExtendImps) {
			if (!linkExtendImp.isFindGrid()) {
				boolean bo = geometry.contains(gf
						.createPoint(new Coordinate(linkExtendImp.getCoord().getX(), linkExtendImp.getCoord().getY())));

				if (bo) {
					gridImp.getLinkExtendImps().add(linkExtendImp);
					linkExtendImp.setFindGrid(true);
				}
			}
		}
	}

	public static void divideStopsIntoGrid(GridImp gridImp,
			Collection<TransitStopFacilityExtendImp> transitStopFacilityExtendImps) {
		PtAccessabilityConfig ptAccessabilityConfig = PtAccessabilityConfig.getInstance();
		LinkedList<Double> xLinkedList = new LinkedList<Double>();
		LinkedList<Double> yLinkedList = new LinkedList<Double>();

		if (!gridImp.getLinkExtendImps().isEmpty()) {
			for (LinkExtendImp l : gridImp.getLinkExtendImps()) {
				xLinkedList.add(l.getCoord().getX());
				yLinkedList.add(l.getCoord().getY());
			}
			double maxDistance = Collections.max(ptAccessabilityConfig.getModeDistance().values());
			double minx = Collections.min(xLinkedList) - maxDistance;
			double miny = Collections.min(yLinkedList) - maxDistance;
			double maxx = Collections.max(xLinkedList) + maxDistance;
			double maxy = Collections.max(yLinkedList) + maxDistance;

			for (TransitStopFacilityExtendImp transitStopFacilityExtendImp : transitStopFacilityExtendImps) {
				if (transitStopFacilityExtendImp.getCoord().getX() > minx
						&& transitStopFacilityExtendImp.getCoord().getX() < maxx
						&& transitStopFacilityExtendImp.getCoord().getY() > miny
						&& transitStopFacilityExtendImp.getCoord().getY() < maxy) {
					gridImp.getTransitStopFacilityExtendImps().add(transitStopFacilityExtendImp);
				}
			}
		}
	}

}
