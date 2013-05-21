package playground.dziemke.lor;

import java.util.List;

// adapted from RegionSubprefectureMatcher, which matches subprefectures with coordinates to the region (given as
// a shapefile) in which they geographically fall
// here, only the read-in procedure of the regions is used. LORs = regions

public class LorReader {
	private List <Lor> lors = ShapeReader.read("D:/Workspace/container/demand/input/LOR_SHP_EPSG_25833/Bezirksregion_EPSG_25833.shp");
		
	public static void main(String[] args) {
		LorReader lorReader = new LorReader();
		lorReader.run();	
	}
		
	public void run() {
		List <Lor> lors = this.lors;
		System.out.println("Anzahl an LORs: " + lors.size());
		for (int n = 0; n < lors.size(); n++) {
			System.out.println("LOR: " + lors.get(n).getName() + "; Fläche: " + lors.get(n).getGeometry().getArea());
		}
	}
	
	public List <Lor> getLors() {
		return this.lors;
	}
	
}