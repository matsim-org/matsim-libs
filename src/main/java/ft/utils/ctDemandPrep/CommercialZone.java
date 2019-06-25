package ft.utils.ctDemandPrep;

import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.locationtech.jts.geom.Geometry;

public class CommercialZone {
	Geometry geom;
	String zone;
	String area;
	String name;
	String use;

	CommercialZone(String zone, Geometry geom, String name, String area, String use) {
		this.zone = zone;
		this.geom = geom;
		this.area = area;
		this.name = name;
		this.use = use;

	}

	public String getName() {

		return name;

	}

	public String getArea() {

		return area;

	}

	public String getUse() {

		return use;

	}

	public String getZone() {

		return zone;

	}

}
