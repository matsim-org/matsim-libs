package playground.johannes.gsv.misc;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialSparseGraphBuilder;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.NumericAttributeColorizer;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.SpatialGraphKMLWriter;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;



public class TomTomBastCompare {

	public static void main(String args[]) throws IOException {
		Map<String, String> bastValues = readFile("/home/johannes/prosim2201/gsv/Z_ExternerZugriff/Modena/xx_Neue_Ordnerstruktur/04_Modell/04_Straßennetz/Zähldaten/Bundesweit- BAST/BAST - Automatische Zählstellen 2012.csv", 1, 14, ";");
		Map<String, String> tomtomValues = readFile("/home/johannes/prosim2201/gsv/B_Produktion/PV/01_Datenbasis_Verkehr/Matrizen/TomTom_Matrix/bast/bast_gesamtfluss_zeitunabhaengig.csv", 0, 1, ",");
		Map<String, String> idMap = readFile("/home/johannes/prosim2201/gsv/C_Vertrieb/2012_07_03_Weiterentwicklung_MIV_Matrizen/Zählstellen/Zählstellen_Tom_Tom_v5.csv", 0, 1, "\t");
		
		TObjectDoubleHashMap<String> tomtomSum = new TObjectDoubleHashMap<String>();
		
		for(Entry<String, String> entry : tomtomValues.entrySet()) {
			String bastKey = idMap.get(entry.getKey());
			double val = Double.parseDouble(entry.getValue()) / 5.0;
			tomtomSum.adjustOrPutValue(bastKey, val, val);
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/johannes/gsv/countscompare.txt"));
		writer.write("BastID\tbastValue\ttomtomValue\tfraction");
		writer.newLine();
		
		TObjectDoubleHashMap<String> factors = new TObjectDoubleHashMap<String>();
		
		TObjectDoubleIterator<String> it = tomtomSum.iterator();
		for(int i = 0; i < tomtomSum.size(); i++) {
			it.advance();
					
			double tomtomVal = it.value();
			String bastValStr = bastValues.get(it.key());
			double bastVal = 0;
			if(!bastValStr.isEmpty()) {
				bastVal = Double.parseDouble(bastValStr) * 365;		
			}
			
			writer.write(it.key());
			writer.write("\t");
			writer.write(String.valueOf(bastVal));
			writer.write("\t");
			writer.write(String.valueOf(tomtomVal));
			writer.write("\t");
			double factor = bastVal/it.value();
			if(bastVal == 0)
				writer.write("NaN");
			else {
				writer.write(String.valueOf(factor));
				factors.put(it.key(), factor);
			}
			writer.newLine();
		}
		
		writer.close();
		
		writeKML("/home/johannes/gsv/factors.kml", factors);
	}
	
	private static Map<String, String> readFile(String file, int keyIdx, int valIdx, String separator) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		Map<String, String> values = new HashMap<String, String>();
		
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split(separator, -1);
			String key = tokens[keyIdx];
			String val = tokens[valIdx];
			values.put(key, val);
		}
		
		reader.close();
		
		return values;
	}
	
	private static void writeKML(String file, TObjectDoubleHashMap<String> factors) throws IOException {
		Map<String, Point> coords = readCoords("/home/johannes/prosim2201/gsv/C_Vertrieb/2012_07_03_Weiterentwicklung_MIV_Matrizen/Zählstellen/Zählstellen_Tom_Tom_v5.csv");
		
		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder(CRSUtils.getCRS(4326));
		SpatialSparseGraph graph = builder.createGraph();
		
		TObjectDoubleIterator<String> it = factors.iterator();
		TObjectDoubleHashMap<Vertex> vFactors = new TObjectDoubleHashMap<Vertex>();
		for(int i = 0; i < factors.size(); i++) {
			it.advance();
			Vertex v = builder.addVertex(graph, coords.get(it.key()));
			vFactors.put(v, Math.min(it.value(), 200));
		}
		
		SpatialGraphKMLWriter kmlWriter = new SpatialGraphKMLWriter();
		KMLIconVertexStyle style = new KMLIconVertexStyle(graph);
		style.setVertexColorizer(new NumericAttributeColorizer(vFactors));
		kmlWriter.setKmlVertexStyle(style);
		kmlWriter.addKMZWriterListener(style);
		kmlWriter.setDrawVertices(true);
		
		kmlWriter.write(graph, file);
	}
	
//	private static void writeKML(String file, TObjectDoubleHashMap<String> factors) throws IOException {
//		Map<String, String> coords = readCoords("");
//		ObjectFactory kmlFactory = new ObjectFactory();
//		
//		KMZWriter writer = new KMZWriter(file);
//		DocumentType kmlDocument = kmlFactory.createDocumentType();
//
//		FolderType kmlFolder = kmlFactory.createFolderType();
//		/*
//		 * Get all style types that will be used in this folder.
//		 */
//		Map<Object, StyleType> styleMap = new HashMap<Object, StyleType>();
//		Set<StyleType> styleSet = new HashSet<StyleType>();
//
//		TObjectDoubleHashMap<String> values = new TObjectDoubleHashMap<String>(factors.size());
//		double valueArray[] = new double[factors.size()];
//		TObjectDoubleIterator<String> it = factors.iterator();
//		for(int i = 0; i < factors.size(); i++) {
//			it.advance();
//			double val = Math.min(it.value(), 200.0);
//			valueArray[i] = val;
//			values.put(it.key(), val);
//		}
//
//		LinearDiscretizer discretizer = new LinearDiscretizer(valueArray, 20);
//		Colorizable colorizable = new NumericAttributeColorizer(values);
//
//		TDoubleObjectHashMap<StyleType> tmpStyles = new TDoubleObjectHashMap<StyleType>();
//		for (Object obj : values.keys()) {
//			String countId = (String) obj;
//			double diff = values.get(countId);
//			double val = discretizer.discretize(diff);
//			double bin = discretizer.index(diff);
//
//			StyleType kmlStyle = tmpStyles.get(val);
//			if (kmlStyle == null) {
//				Color c = colorizable.getColor(countId);
//
//				LineStyleType kmlLineStyle = kmlFactory.createLineStyleType();
//				kmlLineStyle.setColor(new byte[] { (byte) c.getAlpha(),
//						(byte) c.getBlue(), (byte) c.getGreen(),
//						(byte) c.getRed() });
//				kmlLineStyle.setWidth(bin);
//
//				kmlStyle = kmlFactory.createStyleType();
//				kmlStyle.setId("link." + val);
//				kmlStyle.setLineStyle(kmlLineStyle);
//
//				styleSet.add(kmlStyle);
//				tmpStyles.put(val, kmlStyle);
//			}
//			
//			styleMap.put(countId, kmlStyle);
//		}
//		
//		/*
//		 * Add all style types to the selector group.
//		 */
//		for (StyleType kmlStyle : styleSet) {
//			kmlFolder.getAbstractStyleSelectorGroup().add(
//					kmlFactory.createStyle(kmlStyle));
//		}
//		/*
//		 * Add vertices and edges.
//		 */
//		it = factors.iterator();
//		for(int i = 0; i < factors.size(); i++) {
//			it.advance();
//			String countId = it.key();
//
//			LineStringType kmlLineString = kmlFactory.createLineStringType();
//
//			PointType kmlPoint = kmlFactory.createPointType();
//			kmlPoint.getCoordinates().add(coords.get(it.key()));
//
//			PlacemarkType kmlPlacemark = kmlFactory.createPlacemarkType();
//			kmlPlacemark.setAbstractGeometryGroup(kmlFactory.createPoint(kmlPoint));
//			kmlPlacemark.setStyleUrl(styleMap.get(it.key()).getId());
//			
//			kmlFolder.getAbstractFeatureGroup().add(kmlFactory.createPlacemark(kmlPlacemark));
//			
//		}
//
//		
//		kmlDocument.getAbstractFeatureGroup().add(kmlFactory.createFolder(kmlFolder));
//		// }
//		/*
//		 * Write the KML document.
//		 */
//		KmlType kmlType = kmlFactory.createKmlType();
//		kmlType.setAbstractFeatureGroup(kmlFactory.createDocument(kmlDocument));
//		writer.writeMainKml(kmlType);
//
//		/*
//		 * Close the KMZ writer.
//		 */
//		// for (KMZWriterListener listener : writerListeners)
//		// listener.closeWriter(writer);
//
//		writer.close();
//
//	}
	
	private static Map<String, Point> readCoords(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		GeometryFactory factory = new GeometryFactory();
		Map<String, Point> values = new HashMap<String, Point>();
		
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split("\t", -1);
			String key = tokens[1];
			String x = tokens[3];
			String y = tokens[4];
			values.put(key, factory.createPoint(new Coordinate(Double.parseDouble(x), Double.parseDouble(y))));
		}
		
		reader.close();
		
		return values;
	}
	
}
