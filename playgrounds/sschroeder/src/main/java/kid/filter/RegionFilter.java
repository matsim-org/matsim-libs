package kid.filter;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.filter.NetworkNodeFilter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class RegionFilter implements SimpleFeatureFilter, NetworkLinkFilter, NetworkNodeFilter{

	private List<SimpleFeature> regions;
	
	public RegionFilter(List<SimpleFeature> regions) {
		super();
		this.regions = regions;
	}

	public boolean judge(SimpleFeature feature) {
		Geometry featureGeo = (Geometry)feature.getDefaultGeometry();
		boolean featureIsInRegions = false;
		for(SimpleFeature f : regions){
			Geometry regionGeo = (Geometry)f.getDefaultGeometry();
			if(featureGeo.within(regionGeo)){
				featureIsInRegions = true;
				break;
			}
		}
		return featureIsInRegions;
	}

	@Override
	public boolean judgeNode(Node n) {
		GeometryFactory fact = new GeometryFactory();
		boolean nodeIsInRegions = false;
		Geometry nodeGeo = fact.createPoint(makeCoordinate(n.getCoord()));
		for(SimpleFeature f : regions){
			Geometry regionGeo = (Geometry)f.getDefaultGeometry();
			if(nodeGeo.within(regionGeo)){
				nodeIsInRegions = true;
				break;
			}
		}
		return nodeIsInRegions;
		
	}

	private Coordinate makeCoordinate(Coord coord) {
		return new Coordinate(coord.getX(),coord.getY());
	}

	@Override
	public boolean judgeLink(Link l) {
		if(judgeNode(l.getFromNode()) && judgeNode(l.getToNode())){
			return true;
		}
		return false;
	}
}
