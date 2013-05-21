package playground.dziemke.lor;

import com.vividsolutions.jts.geom.Geometry;

public class Lor {

	private String id;
	private String name;
	private Geometry geometry;
	// private List <Prefecture> subprefectures;
		
	public Lor() {
	}	
		
	public Lor(String id, String name, Geometry geometry) {
		this.id = id;
		this.name = name;
		this.geometry = geometry;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Geometry getGeometry() {
		return this.geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}
	
	/* public List <Prefecture> getPrefecture() {
		return subprefectures;
	}
	*/
	
	/*
	public void setPrefectures(List <Prefecture> subprefectures) {
		this.subprefectures = subprefectures;
	}
	*/

}