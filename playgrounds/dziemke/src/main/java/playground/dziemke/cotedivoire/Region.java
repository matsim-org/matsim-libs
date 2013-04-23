package playground.dziemke.cotedivoire;

import java.util.List;
import com.vividsolutions.jts.geom.Geometry;

public class Region {

	private int id;
	private String name;
	private int population;
	private Geometry geometry;
	private List <Prefecture> subprefectures;
		
	public Region() {
	}	
		
	public Region(int id, String name, Geometry geometry) {
		this.id = id;
		this.name = name;
		this.population = assignPopulation(name);
		this.geometry = geometry;
	}
	
	private int assignPopulation(String name) {
		// data source: http://www.axl.cefan.ulaval.ca/afrique/cotiv.htm
		int i = 0;
		if (name.equals("Agnéby")) {i = 720900;}
		else if (name.equals("Bafing")) {i = 178400;}
		else if (name.equals("Bas-Sassandra")) {i = 443200;}
		else if (name.equals("Denguélé")) {i = 277000;}
		else if (name.equals("Dix-Huit Montagnes")) {i = 1125800;}
		else if (name.equals("Fromager")) {i = 679900;}
		else if (name.equals("Haut-Sassandra")) {i = 1186600;}
		else if (name.equals("Lacs")) {i = 597500;}
		else if (name.equals("Lagunes")) {i = 4210200;}
		else if (name.equals("Marahoué")) {i = 651700;}
		else if (name.equals("Moyen-Cavally")) {i = 443200;}
		else if (name.equals("Moyen-Comoé")) {i = 488200;}
		else if (name.equals("N'zi-Comoé")) {i = 909800;}
		else if (name.equals("Savanes")) {i = 1215100;}
		else if (name.equals("Sud-Bandama")) {i = 826300;}
		else if (name.equals("Sud-Comoé")) {i = 536500;}
		else if (name.equals("Vallée du Bandama")) {i = 1335500;}
		else if (name.equals("Worodougou")) {i = 400200;}
		else if (name.equals("Zanzan")) {i = 839000;}
		return i;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public int getPopulation() {
		return this.population;
	}

	public void setPopulation(int population) {
		this.population = population;
	}
	
	public Geometry getGeometry() {
		return this.geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}
	
	public List <Prefecture> getPrefecture() {
		return this.subprefectures;
	}

	public void setPrefectures(List <Prefecture> subprefectures) {
		this.subprefectures = subprefectures;
	}

}