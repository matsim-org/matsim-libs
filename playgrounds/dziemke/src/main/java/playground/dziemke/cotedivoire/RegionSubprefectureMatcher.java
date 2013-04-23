package playground.dziemke.cotedivoire;

import java.util.List;
import org.matsim.core.utils.geometry.geotools.MGC;
import com.vividsolutions.jts.geom.Point;

public class RegionSubprefectureMatcher implements Runnable{
	private List <Region> regions = ShapeReader.read("D:/Workspace/container/cotedivoire/input/CIV_adm1.shp");
	private List <Prefecture> subprefectures = PrefectureReader.read("D:/Workspace/container/cotedivoire/input/SUBPREF_POS_LONLAT.TSV");
	
	
	public static void main(String[] args) {
		RegionSubprefectureMatcher potsdamPop = new RegionSubprefectureMatcher();
		potsdamPop.run();	
	}
	
	
	public void run() {
		matchPrefecturesToRegions();
	}
	
	
	public void matchPrefecturesToRegions() {
		List <Region> regions = this.regions;
		List <Prefecture> subprefectures = this.subprefectures;
		System.out.println("Anzahl an Region: " + regions.size());
		
		int counter = 0;
		
		for (int n = 0; n < regions.size(); n++) {
			System.out.println("Region: " + regions.get(n).getName() + "; Fläche: " + regions.get(n).getGeometry().getArea());
			
			for (int i=0; i<subprefectures.size(); i++) {
				Point point;
				double longitude = subprefectures.get(i).getLongitude();
				double latitude = subprefectures.get(i).getLatitude();
				point = MGC.xy2Point(longitude, latitude);
				if (regions.get(n).getGeometry().contains(point)) {
					//System.out.println(point.toString());
					System.out.println("Die Subpräfektur mit der ID " + subprefectures.get(i).getId() + " befindet sich in der Region " +
							regions.get(n).getName() + ", welche ein Bevoelkerung von " + regions.get(n).getPopulation() + " hat.");
					counter++;
				}
			}
		}
		System.out.println("Es wurden insgesamt " + counter + " Subpräfekturen zugewiesen.");
	}
	
}