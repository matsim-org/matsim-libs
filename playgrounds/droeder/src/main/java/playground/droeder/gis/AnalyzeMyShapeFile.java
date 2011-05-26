package playground.droeder.gis;

import java.io.IOException;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

public class AnalyzeMyShapeFile {
	
	public static void main(String[] args) throws IOException{
		new AnalyzeMyShapeFile().run("D:/VSP/BVG09_Auswertung/input/Bezirke_BVG_zone.SHP");
	}
	
	
//	private Map<Integer, Set<String>> values = new HashMap<Integer, Set<String>>();
	
	public void run(String shapeFile) throws IOException{
		FeatureSource features = ShapeFileReader.readDataFile(shapeFile);
		
//		for(Iterator<Feature> it = features.getFeatures().iterator(); it.hasNext(); ){
//			Feature f = it.next();
//			
//			for(int i = 0; i < f.getNumberOfAttributes(); i++){
//				if(this.values.containsKey(i)){
//					if(!this.values.get(i).contains(f.getAttribute(i).toString()) && !(this.values.get(i).size() > 100)){
//						this.values.get(i).add(f.getAttribute(i).toString());
//					}
//				}else{
//					Set<String> temp = new TreeSet<String>();
//					temp.add(f.getAttribute(i).toString());
//					this.values.put(i, temp);
//				}
//			}
//		}
//		
//		
//		for(Entry<Integer, Set<String>> e: this.values.entrySet()){
//			System.out.print(e.getKey() + "\t");
//			for(String s : e.getValue()){
//				System.out.print(s + "\t");
//			}
//			System.out.println();
//		}
		
		
		Feature ft = (Feature) features.getFeatures().iterator().next();
		for(int i = 0; i < ft.getNumberOfAttributes(); i++){
			System.out.print(i + " " + ft.getFeatureType().getAttributeType(i).getName() + "\t" + ft.getAttribute(i).getClass().toString());
			System.out.println();
		}
	}

}
