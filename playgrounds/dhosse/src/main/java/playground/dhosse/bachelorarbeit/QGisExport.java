package playground.dhosse.bachelorarbeit;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class QGisExport extends MatsimXmlWriter implements MatsimWriter {

	private String outputFolder = "NetworkInspector.output/";
	
	private Map<String,Class<? extends Geometry>> files = new HashMap<String,Class<? extends Geometry>>();
	
	public QGisExport(Map<String,Class<? extends Geometry>> files) {
		
		this.files = files;
		
	}

	@Override
	public void write(String filename) {
		
		String version = "<qgis projectname=\"\" version=\"1.7.4-Wroclaw\">\n";
		QGisWriterHandler handler = new QGisWriterHandler();
		
		try {
			openFile(this.outputFolder+filename);
			writeQGisHead();
			this.writer.write(version);
			handler.startLegend(this.writer);
			
			for(String key : this.files.keySet()){
				handler.writeLegendLayer(this.writer,key);
			}
			
			handler.endLegend(this.writer);
			handler.startProjectLayers(this.writer, this.files.size());
			
			for(String key : this.files.keySet()){
				String geometry = "";
				String claz = "";
				String type = "";
				
				Class<? extends Geometry> clazz = this.files.get(key);
				
				if(clazz.equals(Polygon.class)){
					geometry = "Polygon";
					claz = "SimpleLine";
					type = "line";
				}
				if(clazz.equals(LineString.class)){
					geometry = "Line";
					claz = "SimpleLine";
					type = "line";
				}
				if(clazz.equals(Point.class)){
					geometry = "Point";
					claz = "SimpleMarker";
					type = "marker";
				}
				
				handler.writeProjectLayer(this.writer, key, geometry, claz, type);
				
			}
			
			handler.endProjectLayers(this.writer);
			handler.writeProperties(this.writer);
			handler.endDocument(this.writer);
			this.writer.flush();
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void writeQGisHead() {

		try {
			this.writer.write("<!DOCTYPE qgis PUBLIC 'http://mrcc.com/qgis.dtd' 'SYSTEM'>");
			this.writer.write(NL);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private static class QGisWriterHandler{

		private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
		private Date now = new Date();
		
		public void endDocument(BufferedWriter out) throws IOException{
			out.write("</qgis>");
		}
		
		public void startLegend(final BufferedWriter out) throws IOException{
			out.write("\t<legend>\n");
		}
		
		public void writeLegendLayer(BufferedWriter out, String key) throws IOException {
			String id = key+this.df.format(this.now);
			out.write("\t\t<legendlayer open=\"true\" checked=\"Qt::Checked\" name=\""+key+"\" showFeatureCount=\"0\">\n");
			out.write("\t\t\t<filegroup open=\"true\" hidden=\"false\">\n");
			out.write("\t\t\t\t<legendlayerfile isInOverview=\"0\" layerid=\""+id+"\" visible=\"1\"/>\n");
			out.write("\t\t\t</filegroup>\n");
			out.write("\t\t</legendlayer>\n");
		}

		public void endLegend(final BufferedWriter out) throws IOException{
			out.write("\t</legend>\n");
		}
		
		public void startProjectLayers(final BufferedWriter out, int size) throws IOException{
			out.write("\t<projectlayers layercount=\""+size+"\">\n");
		}
		
		public void writeProjectLayer(final BufferedWriter out, String key, String geometry, String clazz, String type) throws IOException{
			
			String id = key+this.df.format(this.now);
			
			if(key.equals("nodeTypes")){
				writeNodeTypesLayer(out,key,geometry,clazz,type,id);
			} else{
				
				out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" geometry=\""+geometry+"\" type =\"vector\"" +
						" hasScaleBasedVisibilityFlag=\"0\">\n");
				out.write("\t\t\t<id>"+id+"</id>\n");
				out.write("\t\t\t<datasource>"+"./"+key+".shp</datasource>\n");
				out.write("\t\t\t<layername>"+key+"</layername>\n");
				out.write("\t\t\t<transparencyLevelInt>255</transparencyLevelInt>\n");
				out.write("\t\t\t<provider encoding=\"UTF-8\">ogr</provider>\n");
				out.write("\t\t\t<vectorjoins/>\n");
				out.write("\t\t\t<renderer-v2 symbollevels=\"0\" type=\"singleSymbol\">\n");
				out.write("\t\t\t\t<symbols>\n");
				out.write("\t\t\t\t\t<symbol outputUnit=\"MM\" alpha=\"1\" type=\""+type+"\" name=\"0\">\n");
				out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\""+clazz+"\" locked=\"0\">\n");
				out.write("\t\t\t\t\t\t\t<prop k=\"capstyle\" v=\"square\"/>\n");
			
				String color = "";
			
				if(key.equals("network"))
					color="0,0,0,255";
				if(key.equals("envelope"))
					color = "255,85,0,255";
				if(key.equals("highDegreeNodes"))
					color = "255,0,170,255";
			
				out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\""+color+"\"/>\n");
				out.write("\t\t\t\t\t\t\t<prop k=\"customdash\" v=\"5;2\"/>\n");
				out.write("\t\t\t\t\t\t\t<prop k=\"joinstyle\" v=\"bevel\"/>\n");
				out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0\"/>\n");
				out.write("\t\t\t\t\t\t\t<prop k=\"penstyle\" v=\"solid\"/>\n");
				out.write("\t\t\t\t\t\t\t<prop k=\"use_custom_dash\" v=\"0\"/>\n");
				out.write("\t\t\t\t\t\t\t<prop k=\"width\" v=\"0.26\"/>\n");
				out.write("\t\t\t\t\t\t</layer>\n");
				out.write("\t\t\t\t\t</symbol>\n");
				out.write("\t\t\t\t</symbols>\n");
				out.write("\t\t\t</renderer-v2>\n");
				out.write("\t\t</maplayer>\n");
			}
		}
		
		private void writeNodeTypesLayer(BufferedWriter out, String key, String geometry, String clazz, String type, String id) throws IOException {
			
			out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" geometry=\""+geometry+"\" type =\"vector\"" +
					" hasScaleBasedVisibilityFlag=\"0\">\n");
			out.write("\t\t\t<id>"+id+"</id>\n");
			out.write("\t\t\t<datasource>"+"./"+key+".shp</datasource>\n");
			out.write("\t\t\t<layername>"+key+"</layername>\n");
			out.write("\t\t\t<transparencyLevelInt>255</transparencyLevelInt>\n");
			out.write("\t\t\t<provider encoding=\"UTF-8\">ogr</provider>\n");
			out.write("\t\t\t<vectorjoins/>\n");
			out.write("\t\t\t<renderer-v2 attr=\"type\" symbollevels=\"0\" type=\"categorizedSymbol\">\n");
			out.write("\t\t\t\t<categories>\n");
			out.write("\t\t\t\t\t<category symbol=\"0\" value=\"deadEnd\" label=\"deadEnd\"/>\n");
			out.write("\t\t\t\t\t<category symbol=\"1\" value=\"exit\" label=\"exit\"/>\n");
			out.write("\t\t\t\t\t<category symbol=\"2\" value=\"redundant\" label=\"redundant\"/>\n");
			out.write("\t\t\t\t</categories>\n");
			out.write("\t\t\t\t<symbols>\n");
			
			out.write("\t\t\t\t\t<symbol outputUnit=\"MM\" alpha=\"1\" type=\""+type+"\" name=\"0\">\n");
			out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\""+clazz+"\" locked=\"0\">\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,0,0,255\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color_border\" v=\"0,0,0,255\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"circle\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"2\"/>\n");
			out.write("\t\t\t\t\t\t</layer>\n");
			out.write("\t\t\t\t\t</symbol>\n");
			
			out.write("\t\t\t\t\t<symbol outputUnit=\"MM\" alpha=\"1\" type=\""+type+"\" name=\"1\">\n");
			out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\""+clazz+"\" locked=\"0\">\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"0,0,255,255\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color_border\" v=\"0,0,0,255\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"circle\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"2\"/>\n");
			out.write("\t\t\t\t\t\t</layer>\n");
			out.write("\t\t\t\t\t</symbol>\n");
			
			out.write("\t\t\t\t\t<symbol outputUnit=\"MM\" alpha=\"1\" type=\""+type+"\" name=\"2\">\n");
			out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\""+clazz+"\" locked=\"0\">\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"0,255,0,255\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color_border\" v=\"0,0,0,255\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"circle\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"2\"/>\n");
			out.write("\t\t\t\t\t\t</layer>\n");
			out.write("\t\t\t\t\t</symbol>\n");
			out.write("\t\t\t\t</symbols>\n");
			
			out.write("\t\t\t\t<source-symbol>\n");
			out.write("\t\t\t\t\t<symbol outputUnit=\"MM\" alpha=\"1\" type=\""+type+"\" name=\"0\">\n");
			out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\""+clazz+"\" locked=\"0\">\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"233,133,150,255\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color_border\" v=\"0,0,0,255\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"circle\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"2\"/>\n");
			out.write("\t\t\t\t\t\t</layer>\n");
			out.write("\t\t\t\t\t</symbol>\n");
			out.write("\t\t\t\t</source-symbol>\n");
			out.write("\t\t\t</renderer-v2>\n");
			out.write("\t\t</maplayer>\n");
//	                <colorramp type="gradient" name="[source]">
//	                    <prop k="color1" v="0,0,0,255"/>
//	                    <prop k="color2" v="255,255,255,255"/>
//	                </colorramp>
//	                <rotation field=""/>
//	                <sizescale field=""/>
//	            </renderer-v2>
//	            <customproperties/>
//	            <displayfield>ID</displayfield>
//	            <label>0</label>
//	        </maplayer>
		}

		public void endProjectLayers(BufferedWriter out) throws IOException{
			out.write("\t</projectlayers>\n");
		}
		
		public void writeProperties(BufferedWriter out) throws IOException{
			out.write("\t<properties>\n");
			out.write("\t\t<Paths>\n");
			out.write("\t\t\t<Absolute type=\"bool\">false</Absolute>\n");
			out.write("\t\t</Paths>\n");
			out.write("\t</properties>\n");
		}
		
	}

}
