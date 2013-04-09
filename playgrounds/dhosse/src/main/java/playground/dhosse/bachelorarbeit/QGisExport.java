package playground.dhosse.bachelorarbeit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class QGisExport {

	private String filename = "networkInspectorOutput.qgs";
	private String outputFolder = "NetworkInspector.output/";
	
	private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
	private Date now = new Date();
	
	private Map<String,Class<? extends Geometry>> files = new HashMap<String,Class<? extends Geometry>>();
	
	private final String qgisHead = "<!DOCTYPE qgis PUBLIC 'http://mrcc.com/qgis.dtd' 'SYSTEM'>" +
								"\n <qgis projectname=\"\" version=\"1.7.4-Wroclaw\">";
	
	public QGisExport(Map<String,Class<? extends Geometry>> files) {
		
		this.files = files;
		
	}
	
	public static void main(Map<String,Class<? extends Geometry>> files){
		
		new QGisExport(files).run();
		
	}
	

	public void run(){
		
		File file = new File(this.outputFolder+this.filename);
		FileWriter fw = null;
		String path = file.getAbsolutePath().replace(this.filename, "");
		path.replace("\\", "/");
		System.out.println(path);
		
		try {
			
			fw = new FileWriter(file);
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		BufferedWriter bw = new BufferedWriter(fw);
		
		try {
			
			bw.write(qgisHead);
			bw.write("\n<legend>\n");
			for(String key : this.files.keySet()){
				
				String id = key+this.df.format(this.now);

				bw.write("<legendlayer open=\"true\" checked=\"Qt::Checked\" name=\""+key+"\" showFeatureCount=\"0\">\n");
				bw.write("<filegroup open=\"true\" hidden=\"false\">\n");
				bw.write("<legendlayerfile isInOverview=\"0\" layerid=\""+id+"\" visible=\"1\"/>\n");
				bw.write("</filegroup>\n");
				bw.write("</legendlayer>\n");
				
			}
			
			bw.write("</legend>\n");
			bw.write("<projectlayers layercount=\""+this.files.size()+"\">\n");
			
			for(String key : this.files.keySet()){
				
				String geometry = "";
				String geometry2 = "";
				String claz = "";
				String id = key+this.df.format(this.now);
				Class<? extends Geometry> clazz = this.files.get(key);
				if(clazz.getName().equals(Polygon.class.getName())){
					geometry = "Polygon";
					claz = "SimpleLine";
					geometry2 = "line";
				}
				if(clazz.getName().equals(LineString.class.getName())){
					geometry = "Line";
					claz = "SimpleLine";
					geometry2 = "line";
				}
				if(clazz.getName().equals(Point.class.getName())){
					geometry = "Point";
					claz = "SimpleMarker";
					geometry2 = "marker";
				}
				
				bw.write("<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" geometry=\""+geometry+"\" type =\"vector\"" +
						" hasScaleBasedVisibilityFlag=\"0\">\n");
				bw.write("<id>"+id+"</id>\n");
				bw.write("<datasource>"+"./"+key+".shp</datasource>\n");
				bw.write("<layername>"+key+"</layername>\n");
				bw.write("<transparencyLevelInt>255</transparencyLevelInt>\n");
				bw.write("<provider encoding=\"UTF-8\">ogr</provider>\n");
				bw.write("<vectorjoins/>\n");
				bw.write("<renderer-v2 symbollevels=\"0\" type=\"singleSymbol\">\n");
				bw.write("<symbols>\n");
				bw.write("<symbol outputUnit=\"MM\" alpha=\"1\" type=\""+geometry2+"\" name=\"0\">\n");
				bw.write("<layer pass=\"0\" class=\""+claz+"\" locked=\"0\">\n");
				bw.write("<prop k=\"capstyle\" v=\"square\"/>\n");
				
				String color = "";
				
				if(key=="network")
					color="0,0,0,255";
				if(key=="smallClusters")
					color="255,0,0,255";
				if(key=="nodeTypes")
					color="150,150,150,255";
				if(key=="envelope")
					color="0,0,255,255";
				
				bw.write("<prop k=\"color\" v=\""+color+"\"/>\n");
				bw.write("<prop k=\"customdash\" v=\"5;2\"/>\n");
				bw.write("<prop k=\"joinstyle\" v=\"bevel\"/>\n");
				bw.write("<prop k=\"offset\" v=\"0\"/>\n");
				bw.write("<prop k=\"penstyle\" v=\"solid\"/>\n");
				bw.write("<prop k=\"use_custom_dash\" v=\"0\"/>\n");
				bw.write("<prop k=\"width\" v=\"0.26\"/>\n");
				bw.write("</layer>\n");
				bw.write("</symbol>\n");
				bw.write("</symbols>\n");
				bw.write("</renderer-v2>\n");
				bw.write("</maplayer>\n");
				
			}
			
			bw.write("</projectlayers>\n");
			bw.write("<properties>\n");
			bw.write("<Paths>\n");
			bw.write("<Absolute type=\"bool\">false</Absolute>\n");
			bw.write("</Paths>\n");
			bw.write("</properties>\n");
			bw.write("</qgis>");
			
//			<properties>
//	        <SpatialRefSys>
//	            <ProjectCrs type="QString">EPSG:4326</ProjectCrs>
//	        </SpatialRefSys>
//	        <Paths>
//	            <Absolute type="bool">false</Absolute>
//	        </Paths>
//	        <Gui>
//	            <SelectionColorBluePart type="int">0</SelectionColorBluePart>
//	            <CanvasColorGreenPart type="int">255</CanvasColorGreenPart>
//	            <CanvasColorRedPart type="int">255</CanvasColorRedPart>
//	            <SelectionColorRedPart type="int">255</SelectionColorRedPart>
//	            <SelectionColorAlphaPart type="int">255</SelectionColorAlphaPart>
//	            <SelectionColorGreenPart type="int">255</SelectionColorGreenPart>
//	            <CanvasColorBluePart type="int">255</CanvasColorBluePart>
//	        </Gui>
//	        <PositionPrecision>
//	            <DecimalPlaces type="int">2</DecimalPlaces>
//	            <Automatic type="bool">true</Automatic>
//	        </PositionPrecision>
//	    </properties>
			
			bw.flush();
			bw.close();
			
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}

}
