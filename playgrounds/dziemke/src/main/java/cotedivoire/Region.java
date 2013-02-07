package cotedivoire;

import java.util.List;
import com.vividsolutions.jts.geom.Geometry;

public class Region {

	private int id;
	private String name;
	private Geometry geometry;
	private List <Prefecture> subprefectures;
		
	public Region() {
	}	
		
	public Region(int id, String name, Geometry geometry) {
		this.id = id;
		this.name = name;
		this.geometry = geometry;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}
	
	public List <Prefecture> getPrefecture() {
		return subprefectures;
	}

	public void setPrefectures(List <Prefecture> subprefectures) {
		this.subprefectures = subprefectures;
	}

}