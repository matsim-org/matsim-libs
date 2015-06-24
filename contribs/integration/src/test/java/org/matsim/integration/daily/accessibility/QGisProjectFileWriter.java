package org.matsim.integration.daily.accessibility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


// TODO check if this class can be deleted

final class QGisProjectFileWriter {
//	private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
//	private Date now = new Date();
//	
//	static void endDocument(BufferedWriter out) throws IOException{
//		out.write("</qgis>");
//	}
//	
//	static void startLegend(final BufferedWriter out) throws IOException{
//		out.write("\t<legend>\n");
//	}
//	
//	void writeLegendLayer(BufferedWriter out, String key) throws IOException {
//		String id = key+this.df.format(this.now);
//		out.write("\t\t<legendlayer open=\"true\" checked=\"Qt::Checked\" name=\""+key+"\" showFeatureCount=\"0\">\n");
//		out.write("\t\t\t<filegroup open=\"true\" hidden=\"false\">\n");
//		out.write("\t\t\t\t<legendlayerfile isInOverview=\"0\" layerid=\""+id+"\" visible=\"1\"/>\n");
//		out.write("\t\t\t</filegroup>\n");
//		out.write("\t\t</legendlayer>\n");
//	}
//
//	static void endLegend(final BufferedWriter out) throws IOException{
//		out.write("\t</legend>\n");
//	}
//	
//	static void startProjectLayers(final BufferedWriter out, int size) throws IOException{
//		out.write("\t<projectlayers layercount=\""+size+"\">\n");
//	}
//	
//	void writeProjectLayer(final BufferedWriter out, String key, String geometry, String clazz, String type) throws IOException{
//		
//		String id = key+this.df.format(this.now);
//		
//		if(key.equals("nodeTypes")){
//			writeNodeTypesLayer(out,key,geometry,clazz,type,id);
//		} else{
//			
//			out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" geometry=\""+geometry+"\" type =\"vector\"" +
//					" hasScaleBasedVisibilityFlag=\"0\">\n");
//			out.write("\t\t\t<id>"+id+"</id>\n");
//			out.write("\t\t\t<datasource>"+"./"+key+".shp</datasource>\n");
//			out.write("\t\t\t<layername>"+key+"</layername>\n");
//			out.write("\t\t\t<transparencyLevelInt>255</transparencyLevelInt>\n");
//			out.write("\t\t\t<provider encoding=\"UTF-8\">ogr</provider>\n");
//			out.write("\t\t\t<vectorjoins/>\n");
//			out.write("\t\t\t<renderer-v2 symbollevels=\"0\" type=\"singleSymbol\">\n");
//			out.write("\t\t\t\t<symbols>\n");
//			out.write("\t\t\t\t\t<symbol outputUnit=\"MM\" alpha=\"1\" type=\""+type+"\" name=\"0\">\n");
//			out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\""+clazz+"\" locked=\"0\">\n");
//			out.write("\t\t\t\t\t\t\t<prop k=\"capstyle\" v=\"square\"/>\n");
//		
//			String color = "";
//		
//			if(key.equals("network"))
//				color="0,0,0,255";
//			if(key.equals("envelope"))
//				color = "255,85,0,255";
//			if(key.equals("highDegreeNodes"))
//				color = "255,0,170,255";
//		
//			out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\""+color+"\"/>\n");
//			out.write("\t\t\t\t\t\t\t<prop k=\"customdash\" v=\"5;2\"/>\n");
//			out.write("\t\t\t\t\t\t\t<prop k=\"joinstyle\" v=\"bevel\"/>\n");
//			out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0\"/>\n");
//			out.write("\t\t\t\t\t\t\t<prop k=\"penstyle\" v=\"solid\"/>\n");
//			out.write("\t\t\t\t\t\t\t<prop k=\"use_custom_dash\" v=\"0\"/>\n");
//			out.write("\t\t\t\t\t\t\t<prop k=\"width\" v=\"0.26\"/>\n");
//			out.write("\t\t\t\t\t\t</layer>\n");
//			out.write("\t\t\t\t\t</symbol>\n");
//			out.write("\t\t\t\t</symbols>\n");
//			out.write("\t\t\t</renderer-v2>\n");
//			out.write("\t\t</maplayer>\n");
//		}
//	}
//	
//	private static void writeNodeTypesLayer(BufferedWriter out, String key, String geometry, String clazz, String type, String id) throws IOException {
//		
//		out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" geometry=\""+geometry+"\" type =\"vector\"" +
//				" hasScaleBasedVisibilityFlag=\"0\">\n");
//		out.write("\t\t\t<id>"+id+"</id>\n");
//		out.write("\t\t\t<datasource>"+"./"+key+".shp</datasource>\n");
//		out.write("\t\t\t<layername>"+key+"</layername>\n");
//		out.write("\t\t\t<transparencyLevelInt>255</transparencyLevelInt>\n");
//		out.write("\t\t\t<provider encoding=\"UTF-8\">ogr</provider>\n");
//		out.write("\t\t\t<vectorjoins/>\n");
//		out.write("\t\t\t<renderer-v2 attr=\"type\" symbollevels=\"0\" type=\"categorizedSymbol\">\n");
//		out.write("\t\t\t\t<categories>\n");
//		out.write("\t\t\t\t\t<category symbol=\"0\" value=\"deadEnd\" label=\"deadEnd\"/>\n");
//		out.write("\t\t\t\t\t<category symbol=\"1\" value=\"exit\" label=\"exit\"/>\n");
//		out.write("\t\t\t\t\t<category symbol=\"2\" value=\"redundant\" label=\"redundant\"/>\n");
//		out.write("\t\t\t\t</categories>\n");
//		out.write("\t\t\t\t<symbols>\n");
//		
//		out.write("\t\t\t\t\t<symbol outputUnit=\"MM\" alpha=\"1\" type=\""+type+"\" name=\"0\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\""+clazz+"\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color_border\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"circle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"2\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol outputUnit=\"MM\" alpha=\"1\" type=\""+type+"\" name=\"1\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\""+clazz+"\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"0,0,255,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color_border\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"circle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"2\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol outputUnit=\"MM\" alpha=\"1\" type=\""+type+"\" name=\"2\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\""+clazz+"\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"0,255,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color_border\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"circle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"2\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		out.write("\t\t\t\t</symbols>\n");
//		
//		out.write("\t\t\t\t<source-symbol>\n");
//		out.write("\t\t\t\t\t<symbol outputUnit=\"MM\" alpha=\"1\" type=\""+type+"\" name=\"0\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\""+clazz+"\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"233,133,150,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color_border\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"circle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"2\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		out.write("\t\t\t\t</source-symbol>\n");
//		out.write("\t\t\t</renderer-v2>\n");
//		out.write("\t\t</maplayer>\n");
////                <colorramp type="gradient" name="[source]">
////                    <prop k="color1" v="0,0,0,255"/>
////                    <prop k="color2" v="255,255,255,255"/>
////                </colorramp>
////                <rotation field=""/>
////                <sizescale field=""/>
////            </renderer-v2>
////            <customproperties/>
////            <displayfield>ID</displayfield>
////            <label>0</label>
////        </maplayer>
//	}
//
//	static void endProjectLayers(BufferedWriter out) throws IOException{
//		out.write("\t</projectlayers>\n");
//	}
//	
//	static void writeProperties(BufferedWriter out) throws IOException{
//		out.write("\t<properties>\n");
//		out.write("\t\t<Paths>\n");
//		out.write("\t\t\t<Absolute type=\"bool\">false</Absolute>\n");
//		out.write("\t\t</Paths>\n");
//		out.write("\t</properties>\n");
//	}
//	
//	static void writeQGisHead(BufferedWriter out) throws IOException{
//		out.write("<!DOCTYPE qgis PUBLIC 'http://mrcc.com/qgis.dtd' 'SYSTEM'>\n");
////		out.write(NL);
//	}
//
//	static void writeEverythingWork(BufferedWriter out, String osmMapnikFile) throws IOException{
//		out.write("\t<title></title>\n");
//		
//		out.write("\t<layer-tree-group expanded=\"1\" checked=\"Qt::Checked\" name=\"\">\n");
//		out.write("\t\t<customproperties/>\n");
//		out.write("\t\t<layer-tree-layer expanded=\"1\" checked=\"Qt::Checked\" id=\"osm_mapnik_xml20141212003803940\" name=\"osm_mapnik.xml\">\n");
//		out.write("\t\t\t<customproperties/>\n");
//		out.write("\t\t</layer-tree-layer>\n");
//		out.write("\t\t<layer-tree-layer expanded=\"0\" checked=\"Qt::Checked\" id=\"Densities_copy20141205145729549\" name=\"Densities Transparency\">\n");
//		out.write("\t\t\t<customproperties/>\n");
//		out.write("\t\t</layer-tree-layer>\n");
//		out.write("\t\t<layer-tree-layer expanded=\"0\" checked=\"Qt::Checked\" id=\"accessibilities20141205141420466\" name=\"Accessibilities\">\n");
//		out.write("\t\t\t<customproperties/>\n");
//		out.write("\t\t</layer-tree-layer>\n");
//		out.write("\t</layer-tree-group>\n");
//		
//		out.write("\t<relations/>\n");
//		
//		out.write("\t<mapcanvas>\n");
//		out.write("\t\t<units>meters</units>\n");
//		out.write("\t\t<extent>\n");
//		out.write("\t\t\t<xmin>100000</xmin>																													<!-- make configurable -->\n");
//		out.write("\t\t\t<ymin>-3720000</ymin>																												<!-- make configurable -->\n");
//		out.write("\t\t\t<xmax>180000</xmax>																													<!-- make configurable -->\n");
//		out.write("\t\t\t<ymax>-3675000</ymax>																												<!-- make configurable -->\n");
//		out.write("\t\t</extent>\n");
//		out.write("\t\t<projections>1</projections>\n");
//		out.write("\t\t<destinationsrs>\n");
//		out.write("\t\t\t<spatialrefsys>\n");
//		out.write("\t\t\t\t<proj4>+proj=aea +lat_1=-18 +lat_2=-32 +lat_0=0 +lon_0=24 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs</proj4>					<!-- make configurable -->\n");
//		out.write("\t\t\t\t<srsid>100000</srsid>																											<!-- make configurable -->\n");
//		out.write("\t\t\t\t<srid>0</srid>\n");
//		out.write("\t\t\t\t<authid>USER:100000</authid>																									<!-- make configurable -->\n");
//		out.write("\t\t\t\t<description>WGS84_SA_Albers</description>																						<!-- make configurable -->\n");
//		out.write("\t\t\t\t<projectionacronym>aea</projectionacronym>\n");
//		out.write("\t\t\t\t<ellipsoidacronym></ellipsoidacronym>\n");
//		out.write("\t\t\t\t<geographicflag>false</geographicflag>\n");
//		out.write("\t\t\t</spatialrefsys>\n");
//		out.write("\t\t</destinationsrs>\n");
//		out.write("\t\t<layer_coordinate_transform_info>\n");
//		out.write("\t\t\t<layer_coordinate_transform destAuthId=\"USER:100000\" srcAuthId=\"USER:100000\" srcDatumTransform=\"-1\" destDatumTransform=\"-1\" layerid=\"accessibilities20141205141420466\"/>\n");
//		out.write("\t\t\t<layer_coordinate_transform destAuthId=\"USER:100000\" srcAuthId=\"USER:100000\" srcDatumTransform=\"-1\" destDatumTransform=\"-1\" layerid=\"Densities_copy20141205145729549\"/>\n");
//		out.write("\t\t\t<layer_coordinate_transform destAuthId=\"USER:100000\" srcAuthId=\"EPSG:3857\" srcDatumTransform=\"-1\" destDatumTransform=\"-1\" layerid=\"osm_mapnik_xml20141212003803940\"/>\n");
//		out.write("\t\t</layer_coordinate_transform_info>\n");
//		out.write("\t</mapcanvas>\n");
//		
//		out.write("\t<visibility-presets/>\n");
//		out.write("\t<layer-tree-canvas>\n");
//		out.write("\t\t<custom-order enabled=\"0\">\n");
//		out.write("\t\t\t<item>accessibilities20141205141420466</item>\n");
//		out.write("\t\t\t<item>Densities_copy20141205145729549</item>\n");
//		out.write("\t\t\t<item>osm_mapnik_xml20141212003803940</item>\n");
//		out.write("\t\t</custom-order>\n");
//		out.write("\t</layer-tree-canvas>\n");
//		
//		out.write("<projectlayers layercount=\"3\">\n");
//		
//		out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" simplifyDrawingHints=\"0\" minLabelScale=\"0\" maxLabelScale=\"1e+08\" simplifyDrawingTol=\"1\" geometry=\"Point\" simplifyMaxScale=\"1\" type=\"vector\" hasScaleBasedVisibilityFlag=\"0\" simplifyLocal=\"1\" scaleBasedLabelVisibilityFlag=\"0\">\n");
//		out.write("\t\t\t<id>Densities_copy20141205145729549</id>\n");
//		out.write("\t\t\t<datasource>file:./accessibilities.csv?type=csv&amp;useHeader=No&amp;xField=field_1&amp;yField=field_2&amp;spatialIndex=no&amp;subsetIndex=no&amp;watchFile=no</datasource>\n");
//		out.write("\t\t\t<layername>Densities Transparency</layername>\n");
//		
//		out.write("\t\t\t<srs>\n");
//		out.write("\t\t\t\t<spatialrefsys>\n");
//		out.write("\t\t\t\t\t<proj4>+proj=aea +lat_1=-18 +lat_2=-32 +lat_0=0 +lon_0=24 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs</proj4>\n");
//		out.write("\t\t\t\t\t<srsid>100000</srsid>\n");
//		out.write("\t\t\t\t\t<srid>0</srid>\n");
//		out.write("\t\t\t\t\t<authid>USER:100000</authid>\n");
//		out.write("\t\t\t\t\t<description>WGS84_SA_Albers</description>\n");
//		out.write("\t\t\t\t\t<projectionacronym>aea</projectionacronym>\n");
//		out.write("\t\t\t\t\t<ellipsoidacronym></ellipsoidacronym>\n");
//		out.write("\t\t\t\t\t<geographicflag>false</geographicflag>\n");
//		out.write("\t\t\t\t</spatialrefsys>\n");
//		out.write("\t\t\t</srs>\n");
//		
//		out.write("\t\t\t<provider encoding=\"System\">delimitedtext</provider>\n");
//				
////		out.write("\t\t\t<renderer-v2 attr=\"field_8\" symbollevels=\"0\" type=\"graduatedSymbol\">\n");
////		out.write("\t\t\t\t<ranges>\n");
////		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"0\" lower=\"0.000000\" upper=\"200.000000\" label=\" 0 - 200 \"/>					<!-- make configurable -->\n");
////		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"1\" lower=\"200.000000\" upper=\"2726.000000\" label=\" 200 - 2726 \"/>			<!-- make configurable -->\n");
////		out.write("\t\t\t\t</ranges>\n");
//		
//		out.write("\t\t\t<renderer-v2 attr=\"field_8\" symbollevels=\"0\" type=\"graduatedSymbol\">\n");
//		out.write("\t\t\t\t<ranges>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"0\" lower=\"0.000000\" upper=\"50.000000\" label=\" 0 - 50 \"/>					<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"1\" lower=\"50.000000\" upper=\"100.000000\" label=\" 50 - 100 \"/>			<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"2\" lower=\"100.000000\" upper=\"200.000000\" label=\" 100 - 200 \"/>			<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"3\" lower=\"200.000000\" upper=\"2726.000000\" label=\" 200 - 2726 \"/>			<!-- make configurable -->\n");
//		out.write("\t\t\t\t</ranges>\n");
//		
//		out.write("\t\t\t\t<symbols>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"0\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,255,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		// make size of density tiles 10% bigger to avoid unintended edges that show up if tiles are smaller 
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1100\"/>												<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"0.666667\" type=\"marker\" name=\"1\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,255,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1100\"/>												<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"0.333333\" type=\"marker\" name=\"2\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,255,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1100\"/>												<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"0\" type=\"marker\" name=\"3\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,255,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1100\"/>												<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t</symbols>\n");
//		
//		out.write("\t\t\t</renderer-v2>\n");
//		
//		out.write("\t\t</maplayer>\n");
//		out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" simplifyDrawingHints=\"0\" minLabelScale=\"0\" maxLabelScale=\"1e+08\" simplifyDrawingTol=\"1\" geometry=\"Point\" simplifyMaxScale=\"1\" type=\"vector\" hasScaleBasedVisibilityFlag=\"0\" simplifyLocal=\"1\" scaleBasedLabelVisibilityFlag=\"0\">\n");
//		out.write("\t\t\t<id>accessibilities20141205141420466</id>\n");
//		out.write("\t\t\t<datasource>file:./accessibilities.csv?type=csv&amp;useHeader=No&amp;xField=field_1&amp;yField=field_2&amp;spatialIndex=no&amp;subsetIndex=no&amp;watchFile=no</datasource>\n");
//		out.write("\t\t\t<layername>Accessibilities</layername>\n");
//				
//		out.write("\t\t\t<srs>\n");
//		out.write("\t\t\t\t<spatialrefsys>\n");
//		out.write("\t\t\t\t\t<proj4>+proj=aea +lat_1=-18 +lat_2=-32 +lat_0=0 +lon_0=24 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs</proj4>\n");
//		out.write("\t\t\t\t\t<srsid>100000</srsid>\n");
//		out.write("\t\t\t\t\t<srid>0</srid>\n");
//		out.write("\t\t\t\t\t<authid>USER:100000</authid>\n");
//		out.write("\t\t\t\t\t<description>WGS84_SA_Albers</description>\n");
//		out.write("\t\t\t\t\t<projectionacronym>aea</projectionacronym>\n");
//		out.write("\t\t\t\t\t<ellipsoidacronym></ellipsoidacronym>\n");
//		out.write("\t\t\t\t\t<geographicflag>false</geographicflag>\n");
//		out.write("\t\t\t\t</spatialrefsys>\n");
//		out.write("\t\t\t</srs>\n");
//		
//		out.write("\t\t\t<provider encoding=\"System\">delimitedtext</provider>\n");
//				
//		out.write("\t\t\t<renderer-v2 attr=\"field_3\" symbollevels=\"0\" type=\"graduatedSymbol\">\n");
//		out.write("\t\t\t\t<ranges>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"0\" lower=\"-67.382580\" upper=\"2.000000\" label=\" -67.3825795014 - 2.0000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"1\" lower=\"2.000000\" upper=\"2.500000\" label=\" 2.0000000000 - 2.5000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"2\" lower=\"2.500000\" upper=\"3.000000\" label=\" 2.5000000000 - 3.0000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"3\" lower=\"3.000000\" upper=\"3.500000\" label=\" 3.0000000000 - 3.5000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"4\" lower=\"3.500000\" upper=\"4.000000\" label=\" 3.5000000000 - 4.0000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"5\" lower=\"4.000000\" upper=\"4.500000\" label=\" 4.0000000000 - 4.5000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"6\" lower=\"4.500000\" upper=\"5.000000\" label=\" 4.5000000000 - 5.0000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"7\" lower=\"5.000000\" upper=\"5.500000\" label=\" 5.0000000000 - 5.5000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"8\" lower=\"5.500000\" upper=\"5.863296\" label=\" 5.5000000000 - 5.8632955777 \"/>\n");
//		out.write("\t\t\t\t</ranges>\n");
//		
//		out.write("\t\t\t\t<symbols>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"0\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"215,25,28,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"1\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"234,99,62,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"2\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"253,174,97,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"3\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"254,214,144,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"4\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,191,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"5\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"213,238,177,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"6\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"171,221,164,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"7\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"107,176,175,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"8\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"43,131,186,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t</symbols>\n");
//		
//		out.write("\t\t\t</renderer-v2>\n");
//		
//		out.write("\t\t</maplayer>\n");
//		
//		out.write("\t\t<maplayer minimumScale=\"-4.65661e-10\" maximumScale=\"1e+08\" type=\"raster\" hasScaleBasedVisibilityFlag=\"0\">\n");
//		out.write("\t\t\t<id>osm_mapnik_xml20141212003803940</id>\n");
//		//out.write("\t\t\t<datasource>C:/Users/Dominik/.qgis2/python/plugins/TileMapScaleLevels/datasets/osm_mapnik.xml</datasource>\n");
//		out.write("\t\t\t<datasource>" + osmMapnikFile + "</datasource>\n");
//		out.write("\t\t\t<layername>osm_mapnik.xml</layername>\n");
//		out.write("\t\t\t\n");
//		
//		out.write("\t\t\t<srs>\n");
//		out.write("\t\t\t\t<spatialrefsys>\n");
//		out.write("\t\t\t\t\t<proj4>+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs</proj4>\n");
//		out.write("\t\t\t\t\t<srsid>3857</srsid>\n");
//		out.write("\t\t\t\t\t<srid>3857</srid>\n");
//		out.write("\t\t\t\t\t<authid>EPSG:3857</authid>\n");
//		out.write("\t\t\t\t\t<description>WGS 84 / Pseudo Mercator</description>\n");
//		out.write("\t\t\t\t\t<projectionacronym>merc</projectionacronym>\n");
//		out.write("\t\t\t\t\t<ellipsoidacronym>WGS84</ellipsoidacronym>\n");
//		out.write("\t\t\t\t\t<geographicflag>false</geographicflag>\n");
//		out.write("\t\t\t\t</spatialrefsys>\n");
//		out.write("\t\t\t</srs>\n");
//		
//		out.write("\t\t\t<customproperties>\n");
//		out.write("\t\t\t\t<property key=\"identify/format\" value=\"Value\"/>\n");
//		out.write("\t\t\t</customproperties>\n");
//		out.write("\t\t\t<provider>gdal</provider>\n");
//		out.write("\t\t\t<noData>\n");
//		out.write("\t\t\t\t<noDataList bandNo=\"1\" useSrcNoData=\"0\"/>\n");
//		out.write("\t\t\t\t<noDataList bandNo=\"2\" useSrcNoData=\"0\"/>\n");
//		out.write("\t\t\t\t<noDataList bandNo=\"3\" useSrcNoData=\"0\"/>\n");
//		out.write("\t\t\t</noData>\n");
//		out.write("\t\t\t<pipe>\n");
//		out.write("\t\t\t\t<rasterrenderer opacity=\"0.490196\" alphaBand=\"0\" blueBand=\"3\" greenBand=\"2\" type=\"multibandcolor\" redBand=\"1\">\n");
//		out.write("\t\t\t\t\t<rasterTransparency/>\n");
//		out.write("\t\t\t\t</rasterrenderer>\n");
//		out.write("\t\t\t\t<brightnesscontrast brightness=\"0\" contrast=\"0\"/>\n");
//		out.write("\t\t\t\t<huesaturation colorizeGreen=\"128\" colorizeOn=\"0\" colorizeRed=\"255\" colorizeBlue=\"128\" grayscaleMode=\"0\" saturation=\"0\" colorizeStrength=\"100\"/>\n");
//		out.write("\t\t\t\t<rasterresampler maxOversampling=\"2\"/>\n");
//		out.write("\t\t\t</pipe>\n");
//		out.write("\t\t\t<blendMode>0</blendMode>\n");
//		out.write("\t\t</maplayer>\n");
//		out.write("\t</projectlayers>\n");
//	}
//	
//	
//	static void writeEverythingEdu(BufferedWriter out, String osmMapnikFile) throws IOException{
//		out.write("\t<title></title>\n");
//		
//		out.write("\t<layer-tree-group expanded=\"1\" checked=\"Qt::Checked\" name=\"\">\n");
//		out.write("\t\t<customproperties/>\n");
//		out.write("\t\t<layer-tree-layer expanded=\"1\" checked=\"Qt::Checked\" id=\"osm_mapnik_xml20141212003803940\" name=\"osm_mapnik.xml\">\n");
//		out.write("\t\t\t<customproperties/>\n");
//		out.write("\t\t</layer-tree-layer>\n");
//		out.write("\t\t<layer-tree-layer expanded=\"0\" checked=\"Qt::Checked\" id=\"Densities_copy20141205145729549\" name=\"Densities Transparency\">\n");
//		out.write("\t\t\t<customproperties/>\n");
//		out.write("\t\t</layer-tree-layer>\n");
//		out.write("\t\t<layer-tree-layer expanded=\"0\" checked=\"Qt::Checked\" id=\"accessibilities20141205141420466\" name=\"Accessibilities\">\n");
//		out.write("\t\t\t<customproperties/>\n");
//		out.write("\t\t</layer-tree-layer>\n");
//		out.write("\t</layer-tree-group>\n");
//		
//		out.write("\t<relations/>\n");
//		
//		out.write("\t<mapcanvas>\n");
//		out.write("\t\t<units>meters</units>\n");
//		out.write("\t\t<extent>\n");
//		out.write("\t\t\t<xmin>100000</xmin>																													<!-- make configurable -->\n");
//		out.write("\t\t\t<ymin>-3720000</ymin>																												<!-- make configurable -->\n");
//		out.write("\t\t\t<xmax>180000</xmax>																													<!-- make configurable -->\n");
//		out.write("\t\t\t<ymax>-3675000</ymax>																												<!-- make configurable -->\n");
//		out.write("\t\t</extent>\n");
//		out.write("\t\t<projections>1</projections>\n");
//		out.write("\t\t<destinationsrs>\n");
//		out.write("\t\t\t<spatialrefsys>\n");
//		out.write("\t\t\t\t<proj4>+proj=aea +lat_1=-18 +lat_2=-32 +lat_0=0 +lon_0=24 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs</proj4>					<!-- make configurable -->\n");
//		out.write("\t\t\t\t<srsid>100000</srsid>																											<!-- make configurable -->\n");
//		out.write("\t\t\t\t<srid>0</srid>\n");
//		out.write("\t\t\t\t<authid>USER:100000</authid>																									<!-- make configurable -->\n");
//		out.write("\t\t\t\t<description>WGS84_SA_Albers</description>																						<!-- make configurable -->\n");
//		out.write("\t\t\t\t<projectionacronym>aea</projectionacronym>\n");
//		out.write("\t\t\t\t<ellipsoidacronym></ellipsoidacronym>\n");
//		out.write("\t\t\t\t<geographicflag>false</geographicflag>\n");
//		out.write("\t\t\t</spatialrefsys>\n");
//		out.write("\t\t</destinationsrs>\n");
//		out.write("\t\t<layer_coordinate_transform_info>\n");
//		out.write("\t\t\t<layer_coordinate_transform destAuthId=\"USER:100000\" srcAuthId=\"USER:100000\" srcDatumTransform=\"-1\" destDatumTransform=\"-1\" layerid=\"accessibilities20141205141420466\"/>\n");
//		out.write("\t\t\t<layer_coordinate_transform destAuthId=\"USER:100000\" srcAuthId=\"USER:100000\" srcDatumTransform=\"-1\" destDatumTransform=\"-1\" layerid=\"Densities_copy20141205145729549\"/>\n");
//		out.write("\t\t\t<layer_coordinate_transform destAuthId=\"USER:100000\" srcAuthId=\"EPSG:3857\" srcDatumTransform=\"-1\" destDatumTransform=\"-1\" layerid=\"osm_mapnik_xml20141212003803940\"/>\n");
//		out.write("\t\t</layer_coordinate_transform_info>\n");
//		out.write("\t</mapcanvas>\n");
//		
//		out.write("\t<visibility-presets/>\n");
//		out.write("\t<layer-tree-canvas>\n");
//		out.write("\t\t<custom-order enabled=\"0\">\n");
//		out.write("\t\t\t<item>accessibilities20141205141420466</item>\n");
//		out.write("\t\t\t<item>Densities_copy20141205145729549</item>\n");
//		out.write("\t\t\t<item>osm_mapnik_xml20141212003803940</item>\n");
//		out.write("\t\t</custom-order>\n");
//		out.write("\t</layer-tree-canvas>\n");
//		
//		out.write("<projectlayers layercount=\"3\">\n");
//		
//		out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" simplifyDrawingHints=\"0\" minLabelScale=\"0\" maxLabelScale=\"1e+08\" simplifyDrawingTol=\"1\" geometry=\"Point\" simplifyMaxScale=\"1\" type=\"vector\" hasScaleBasedVisibilityFlag=\"0\" simplifyLocal=\"1\" scaleBasedLabelVisibilityFlag=\"0\">\n");
//		out.write("\t\t\t<id>Densities_copy20141205145729549</id>\n");
//		out.write("\t\t\t<datasource>file:./accessibilities.csv?type=csv&amp;useHeader=No&amp;xField=field_1&amp;yField=field_2&amp;spatialIndex=no&amp;subsetIndex=no&amp;watchFile=no</datasource>\n");
//		out.write("\t\t\t<layername>Densities Transparency</layername>\n");
//		
//		out.write("\t\t\t<srs>\n");
//		out.write("\t\t\t\t<spatialrefsys>\n");
//		out.write("\t\t\t\t\t<proj4>+proj=aea +lat_1=-18 +lat_2=-32 +lat_0=0 +lon_0=24 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs</proj4>\n");
//		out.write("\t\t\t\t\t<srsid>100000</srsid>\n");
//		out.write("\t\t\t\t\t<srid>0</srid>\n");
//		out.write("\t\t\t\t\t<authid>USER:100000</authid>\n");
//		out.write("\t\t\t\t\t<description>WGS84_SA_Albers</description>\n");
//		out.write("\t\t\t\t\t<projectionacronym>aea</projectionacronym>\n");
//		out.write("\t\t\t\t\t<ellipsoidacronym></ellipsoidacronym>\n");
//		out.write("\t\t\t\t\t<geographicflag>false</geographicflag>\n");
//		out.write("\t\t\t\t</spatialrefsys>\n");
//		out.write("\t\t\t</srs>\n");
//		
//		out.write("\t\t\t<provider encoding=\"System\">delimitedtext</provider>\n");
//				
////		out.write("\t\t\t<renderer-v2 attr=\"field_8\" symbollevels=\"0\" type=\"graduatedSymbol\">\n");
////		out.write("\t\t\t\t<ranges>\n");
////		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"0\" lower=\"0.000000\" upper=\"200.000000\" label=\" 0 - 200 \"/>					<!-- make configurable -->\n");
////		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"1\" lower=\"200.000000\" upper=\"2726.000000\" label=\" 200 - 2726 \"/>			<!-- make configurable -->\n");
////		out.write("\t\t\t\t</ranges>\n");
//		
//		out.write("\t\t\t<renderer-v2 attr=\"field_8\" symbollevels=\"0\" type=\"graduatedSymbol\">\n");
//		out.write("\t\t\t\t<ranges>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"0\" lower=\"0.000000\" upper=\"50.000000\" label=\" 0 - 50 \"/>					<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"1\" lower=\"50.000000\" upper=\"100.000000\" label=\" 50 - 100 \"/>			<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"2\" lower=\"100.000000\" upper=\"200.000000\" label=\" 100 - 200 \"/>			<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"3\" lower=\"200.000000\" upper=\"2726.000000\" label=\" 200 - 2726 \"/>			<!-- make configurable -->\n");
//		out.write("\t\t\t\t</ranges>\n");
//		
//		out.write("\t\t\t\t<symbols>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"0\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,255,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		// make size of density tiles 10% bigger to avoid unintended edges that show up if tiles are smaller 
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1100\"/>												<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"0.666667\" type=\"marker\" name=\"1\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,255,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1100\"/>												<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"0.333333\" type=\"marker\" name=\"2\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,255,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1100\"/>												<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"0\" type=\"marker\" name=\"3\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,255,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1100\"/>												<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t</symbols>\n");
//		
//		out.write("\t\t\t</renderer-v2>\n");
//		
//		out.write("\t\t</maplayer>\n");
//		out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" simplifyDrawingHints=\"0\" minLabelScale=\"0\" maxLabelScale=\"1e+08\" simplifyDrawingTol=\"1\" geometry=\"Point\" simplifyMaxScale=\"1\" type=\"vector\" hasScaleBasedVisibilityFlag=\"0\" simplifyLocal=\"1\" scaleBasedLabelVisibilityFlag=\"0\">\n");
//		out.write("\t\t\t<id>accessibilities20141205141420466</id>\n");
//		out.write("\t\t\t<datasource>file:./accessibilities.csv?type=csv&amp;useHeader=No&amp;xField=field_1&amp;yField=field_2&amp;spatialIndex=no&amp;subsetIndex=no&amp;watchFile=no</datasource>\n");
//		out.write("\t\t\t<layername>Accessibilities</layername>\n");
//				
//		out.write("\t\t\t<srs>\n");
//		out.write("\t\t\t\t<spatialrefsys>\n");
//		out.write("\t\t\t\t\t<proj4>+proj=aea +lat_1=-18 +lat_2=-32 +lat_0=0 +lon_0=24 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs</proj4>\n");
//		out.write("\t\t\t\t\t<srsid>100000</srsid>\n");
//		out.write("\t\t\t\t\t<srid>0</srid>\n");
//		out.write("\t\t\t\t\t<authid>USER:100000</authid>\n");
//		out.write("\t\t\t\t\t<description>WGS84_SA_Albers</description>\n");
//		out.write("\t\t\t\t\t<projectionacronym>aea</projectionacronym>\n");
//		out.write("\t\t\t\t\t<ellipsoidacronym></ellipsoidacronym>\n");
//		out.write("\t\t\t\t\t<geographicflag>false</geographicflag>\n");
//		out.write("\t\t\t\t</spatialrefsys>\n");
//		out.write("\t\t\t</srs>\n");
//		
//		out.write("\t\t\t<provider encoding=\"System\">delimitedtext</provider>\n");
//				
//		out.write("\t\t\t<renderer-v2 attr=\"field_3\" symbollevels=\"0\" type=\"graduatedSymbol\">\n");
//		out.write("\t\t\t\t<ranges>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"0\" lower=\"-67.382580\" upper=\"0.000000\" label=\" -67.3825795014 - 0.0000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"1\" lower=\"1.000000\" upper=\"1.500000\" label=\" 1.0000000000 - 1.5000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"2\" lower=\"1.500000\" upper=\"2.000000\" label=\" 1.5000000000 - 2.0000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"3\" lower=\"2.000000\" upper=\"2.500000\" label=\" 2.0000000000 - 2.5000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"4\" lower=\"2.500000\" upper=\"3.000000\" label=\" 2.5000000000 - 3.0000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"5\" lower=\"3.000000\" upper=\"3.500000\" label=\" 3.0000000000 - 3.5000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"6\" lower=\"3.500000\" upper=\"4.000000\" label=\" 3.5000000000 - 4.0000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"7\" lower=\"4.000000\" upper=\"4.500000\" label=\" 4.0000000000 - 4.5000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"8\" lower=\"4.500000\" upper=\"5.863296\" label=\" 4.5000000000 - 5.8632955777 \"/>\n");
//		out.write("\t\t\t\t</ranges>\n");
//		
//		out.write("\t\t\t\t<symbols>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"0\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"215,25,28,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"1\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"234,99,62,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"2\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"253,174,97,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"3\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"254,214,144,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"4\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,191,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"5\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"213,238,177,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"6\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"171,221,164,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"7\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"107,176,175,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"8\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"43,131,186,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t</symbols>\n");
//		
//		out.write("\t\t\t</renderer-v2>\n");
//		
//		out.write("\t\t</maplayer>\n");
//		
//		out.write("\t\t<maplayer minimumScale=\"-4.65661e-10\" maximumScale=\"1e+08\" type=\"raster\" hasScaleBasedVisibilityFlag=\"0\">\n");
//		out.write("\t\t\t<id>osm_mapnik_xml20141212003803940</id>\n");
//		//out.write("\t\t\t<datasource>C:/Users/Dominik/.qgis2/python/plugins/TileMapScaleLevels/datasets/osm_mapnik.xml</datasource>\n");
//		out.write("\t\t\t<datasource>" + osmMapnikFile + "</datasource>\n");
//		out.write("\t\t\t<layername>osm_mapnik.xml</layername>\n");
//		out.write("\t\t\t\n");
//		
//		out.write("\t\t\t<srs>\n");
//		out.write("\t\t\t\t<spatialrefsys>\n");
//		out.write("\t\t\t\t\t<proj4>+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs</proj4>\n");
//		out.write("\t\t\t\t\t<srsid>3857</srsid>\n");
//		out.write("\t\t\t\t\t<srid>3857</srid>\n");
//		out.write("\t\t\t\t\t<authid>EPSG:3857</authid>\n");
//		out.write("\t\t\t\t\t<description>WGS 84 / Pseudo Mercator</description>\n");
//		out.write("\t\t\t\t\t<projectionacronym>merc</projectionacronym>\n");
//		out.write("\t\t\t\t\t<ellipsoidacronym>WGS84</ellipsoidacronym>\n");
//		out.write("\t\t\t\t\t<geographicflag>false</geographicflag>\n");
//		out.write("\t\t\t\t</spatialrefsys>\n");
//		out.write("\t\t\t</srs>\n");
//		
//		out.write("\t\t\t<customproperties>\n");
//		out.write("\t\t\t\t<property key=\"identify/format\" value=\"Value\"/>\n");
//		out.write("\t\t\t</customproperties>\n");
//		out.write("\t\t\t<provider>gdal</provider>\n");
//		out.write("\t\t\t<noData>\n");
//		out.write("\t\t\t\t<noDataList bandNo=\"1\" useSrcNoData=\"0\"/>\n");
//		out.write("\t\t\t\t<noDataList bandNo=\"2\" useSrcNoData=\"0\"/>\n");
//		out.write("\t\t\t\t<noDataList bandNo=\"3\" useSrcNoData=\"0\"/>\n");
//		out.write("\t\t\t</noData>\n");
//		out.write("\t\t\t<pipe>\n");
//		out.write("\t\t\t\t<rasterrenderer opacity=\"0.490196\" alphaBand=\"0\" blueBand=\"3\" greenBand=\"2\" type=\"multibandcolor\" redBand=\"1\">\n");
//		out.write("\t\t\t\t\t<rasterTransparency/>\n");
//		out.write("\t\t\t\t</rasterrenderer>\n");
//		out.write("\t\t\t\t<brightnesscontrast brightness=\"0\" contrast=\"0\"/>\n");
//		out.write("\t\t\t\t<huesaturation colorizeGreen=\"128\" colorizeOn=\"0\" colorizeRed=\"255\" colorizeBlue=\"128\" grayscaleMode=\"0\" saturation=\"0\" colorizeStrength=\"100\"/>\n");
//		out.write("\t\t\t\t<rasterresampler maxOversampling=\"2\"/>\n");
//		out.write("\t\t\t</pipe>\n");
//		out.write("\t\t\t<blendMode>0</blendMode>\n");
//		out.write("\t\t</maplayer>\n");
//		out.write("\t</projectlayers>\n");
//	}
//	
//	static void writeEverythingOther(BufferedWriter out, String osmMapnikFile) throws IOException{
//		out.write("\t<title></title>\n");
//		
//		out.write("\t<layer-tree-group expanded=\"1\" checked=\"Qt::Checked\" name=\"\">\n");
//		out.write("\t\t<customproperties/>\n");
//		out.write("\t\t<layer-tree-layer expanded=\"1\" checked=\"Qt::Checked\" id=\"osm_mapnik_xml20141212003803940\" name=\"osm_mapnik.xml\">\n");
//		out.write("\t\t\t<customproperties/>\n");
//		out.write("\t\t</layer-tree-layer>\n");
//		out.write("\t\t<layer-tree-layer expanded=\"0\" checked=\"Qt::Checked\" id=\"Densities_copy20141205145729549\" name=\"Densities Transparency\">\n");
//		out.write("\t\t\t<customproperties/>\n");
//		out.write("\t\t</layer-tree-layer>\n");
//		out.write("\t\t<layer-tree-layer expanded=\"0\" checked=\"Qt::Checked\" id=\"accessibilities20141205141420466\" name=\"Accessibilities\">\n");
//		out.write("\t\t\t<customproperties/>\n");
//		out.write("\t\t</layer-tree-layer>\n");
//		out.write("\t</layer-tree-group>\n");
//		
//		out.write("\t<relations/>\n");
//		
//		out.write("\t<mapcanvas>\n");
//		out.write("\t\t<units>meters</units>\n");
//		out.write("\t\t<extent>\n");
//		out.write("\t\t\t<xmin>100000</xmin>																													<!-- make configurable -->\n");
//		out.write("\t\t\t<ymin>-3720000</ymin>																												<!-- make configurable -->\n");
//		out.write("\t\t\t<xmax>180000</xmax>																													<!-- make configurable -->\n");
//		out.write("\t\t\t<ymax>-3675000</ymax>																												<!-- make configurable -->\n");
//		out.write("\t\t</extent>\n");
//		out.write("\t\t<projections>1</projections>\n");
//		out.write("\t\t<destinationsrs>\n");
//		out.write("\t\t\t<spatialrefsys>\n");
//		out.write("\t\t\t\t<proj4>+proj=aea +lat_1=-18 +lat_2=-32 +lat_0=0 +lon_0=24 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs</proj4>					<!-- make configurable -->\n");
//		out.write("\t\t\t\t<srsid>100000</srsid>																											<!-- make configurable -->\n");
//		out.write("\t\t\t\t<srid>0</srid>\n");
//		out.write("\t\t\t\t<authid>USER:100000</authid>																									<!-- make configurable -->\n");
//		out.write("\t\t\t\t<description>WGS84_SA_Albers</description>																						<!-- make configurable -->\n");
//		out.write("\t\t\t\t<projectionacronym>aea</projectionacronym>\n");
//		out.write("\t\t\t\t<ellipsoidacronym></ellipsoidacronym>\n");
//		out.write("\t\t\t\t<geographicflag>false</geographicflag>\n");
//		out.write("\t\t\t</spatialrefsys>\n");
//		out.write("\t\t</destinationsrs>\n");
//		out.write("\t\t<layer_coordinate_transform_info>\n");
//		out.write("\t\t\t<layer_coordinate_transform destAuthId=\"USER:100000\" srcAuthId=\"USER:100000\" srcDatumTransform=\"-1\" destDatumTransform=\"-1\" layerid=\"accessibilities20141205141420466\"/>\n");
//		out.write("\t\t\t<layer_coordinate_transform destAuthId=\"USER:100000\" srcAuthId=\"USER:100000\" srcDatumTransform=\"-1\" destDatumTransform=\"-1\" layerid=\"Densities_copy20141205145729549\"/>\n");
//		out.write("\t\t\t<layer_coordinate_transform destAuthId=\"USER:100000\" srcAuthId=\"EPSG:3857\" srcDatumTransform=\"-1\" destDatumTransform=\"-1\" layerid=\"osm_mapnik_xml20141212003803940\"/>\n");
//		out.write("\t\t</layer_coordinate_transform_info>\n");
//		out.write("\t</mapcanvas>\n");
//		
//		out.write("\t<visibility-presets/>\n");
//		out.write("\t<layer-tree-canvas>\n");
//		out.write("\t\t<custom-order enabled=\"0\">\n");
//		out.write("\t\t\t<item>accessibilities20141205141420466</item>\n");
//		out.write("\t\t\t<item>Densities_copy20141205145729549</item>\n");
//		out.write("\t\t\t<item>osm_mapnik_xml20141212003803940</item>\n");
//		out.write("\t\t</custom-order>\n");
//		out.write("\t</layer-tree-canvas>\n");
//		
//		out.write("<projectlayers layercount=\"3\">\n");
//		
//		out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" simplifyDrawingHints=\"0\" minLabelScale=\"0\" maxLabelScale=\"1e+08\" simplifyDrawingTol=\"1\" geometry=\"Point\" simplifyMaxScale=\"1\" type=\"vector\" hasScaleBasedVisibilityFlag=\"0\" simplifyLocal=\"1\" scaleBasedLabelVisibilityFlag=\"0\">\n");
//		out.write("\t\t\t<id>Densities_copy20141205145729549</id>\n");
//		out.write("\t\t\t<datasource>file:./accessibilities.csv?type=csv&amp;useHeader=No&amp;xField=field_1&amp;yField=field_2&amp;spatialIndex=no&amp;subsetIndex=no&amp;watchFile=no</datasource>\n");
//		out.write("\t\t\t<layername>Densities Transparency</layername>\n");
//		
//		out.write("\t\t\t<srs>\n");
//		out.write("\t\t\t\t<spatialrefsys>\n");
//		out.write("\t\t\t\t\t<proj4>+proj=aea +lat_1=-18 +lat_2=-32 +lat_0=0 +lon_0=24 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs</proj4>\n");
//		out.write("\t\t\t\t\t<srsid>100000</srsid>\n");
//		out.write("\t\t\t\t\t<srid>0</srid>\n");
//		out.write("\t\t\t\t\t<authid>USER:100000</authid>\n");
//		out.write("\t\t\t\t\t<description>WGS84_SA_Albers</description>\n");
//		out.write("\t\t\t\t\t<projectionacronym>aea</projectionacronym>\n");
//		out.write("\t\t\t\t\t<ellipsoidacronym></ellipsoidacronym>\n");
//		out.write("\t\t\t\t\t<geographicflag>false</geographicflag>\n");
//		out.write("\t\t\t\t</spatialrefsys>\n");
//		out.write("\t\t\t</srs>\n");
//		
//		out.write("\t\t\t<provider encoding=\"System\">delimitedtext</provider>\n");
//				
////		out.write("\t\t\t<renderer-v2 attr=\"field_8\" symbollevels=\"0\" type=\"graduatedSymbol\">\n");
////		out.write("\t\t\t\t<ranges>\n");
////		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"0\" lower=\"0.000000\" upper=\"200.000000\" label=\" 0 - 200 \"/>					<!-- make configurable -->\n");
////		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"1\" lower=\"200.000000\" upper=\"2726.000000\" label=\" 200 - 2726 \"/>			<!-- make configurable -->\n");
////		out.write("\t\t\t\t</ranges>\n");
//		
//		out.write("\t\t\t<renderer-v2 attr=\"field_8\" symbollevels=\"0\" type=\"graduatedSymbol\">\n");
//		out.write("\t\t\t\t<ranges>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"0\" lower=\"0.000000\" upper=\"50.000000\" label=\" 0 - 50 \"/>					<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"1\" lower=\"50.000000\" upper=\"100.000000\" label=\" 50 - 100 \"/>			<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"2\" lower=\"100.000000\" upper=\"200.000000\" label=\" 100 - 200 \"/>			<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"3\" lower=\"200.000000\" upper=\"2726.000000\" label=\" 200 - 2726 \"/>			<!-- make configurable -->\n");
//		out.write("\t\t\t\t</ranges>\n");
//		
//		out.write("\t\t\t\t<symbols>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"0\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,255,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		// make size of density tiles 10% bigger to avoid unintended edges that show up if tiles are smaller 
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1100\"/>												<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"0.666667\" type=\"marker\" name=\"1\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,255,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1100\"/>												<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"0.333333\" type=\"marker\" name=\"2\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,255,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1100\"/>												<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"0\" type=\"marker\" name=\"3\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,255,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1100\"/>												<!-- make configurable -->\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t</symbols>\n");
//		
//		out.write("\t\t\t</renderer-v2>\n");
//		
//		out.write("\t\t</maplayer>\n");
//		out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" simplifyDrawingHints=\"0\" minLabelScale=\"0\" maxLabelScale=\"1e+08\" simplifyDrawingTol=\"1\" geometry=\"Point\" simplifyMaxScale=\"1\" type=\"vector\" hasScaleBasedVisibilityFlag=\"0\" simplifyLocal=\"1\" scaleBasedLabelVisibilityFlag=\"0\">\n");
//		out.write("\t\t\t<id>accessibilities20141205141420466</id>\n");
//		out.write("\t\t\t<datasource>file:./accessibilities.csv?type=csv&amp;useHeader=No&amp;xField=field_1&amp;yField=field_2&amp;spatialIndex=no&amp;subsetIndex=no&amp;watchFile=no</datasource>\n");
//		out.write("\t\t\t<layername>Accessibilities</layername>\n");
//				
//		out.write("\t\t\t<srs>\n");
//		out.write("\t\t\t\t<spatialrefsys>\n");
//		out.write("\t\t\t\t\t<proj4>+proj=aea +lat_1=-18 +lat_2=-32 +lat_0=0 +lon_0=24 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs</proj4>\n");
//		out.write("\t\t\t\t\t<srsid>100000</srsid>\n");
//		out.write("\t\t\t\t\t<srid>0</srid>\n");
//		out.write("\t\t\t\t\t<authid>USER:100000</authid>\n");
//		out.write("\t\t\t\t\t<description>WGS84_SA_Albers</description>\n");
//		out.write("\t\t\t\t\t<projectionacronym>aea</projectionacronym>\n");
//		out.write("\t\t\t\t\t<ellipsoidacronym></ellipsoidacronym>\n");
//		out.write("\t\t\t\t\t<geographicflag>false</geographicflag>\n");
//		out.write("\t\t\t\t</spatialrefsys>\n");
//		out.write("\t\t\t</srs>\n");
//		
//		out.write("\t\t\t<provider encoding=\"System\">delimitedtext</provider>\n");
//				
//		out.write("\t\t\t<renderer-v2 attr=\"field_3\" symbollevels=\"0\" type=\"graduatedSymbol\">\n");
//		out.write("\t\t\t\t<ranges>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"0\" lower=\"-67.382580\" upper=\"0.000000\" label=\" -67.3825795014 - 0.0000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"1\" lower=\"0.000000\" upper=\"0.500000\" label=\" 0.0000000000 - 0.5000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"2\" lower=\"0.500000\" upper=\"1.000000\" label=\" 0.5000000000 - 1.0000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"3\" lower=\"1.000000\" upper=\"1.500000\" label=\" 1.0000000000 - 1.5000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"4\" lower=\"1.500000\" upper=\"2.000000\" label=\" 1.5000000000 - 2.0000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"5\" lower=\"2.000000\" upper=\"2.500000\" label=\" 2.0000000000 - 2.5000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"6\" lower=\"2.500000\" upper=\"3.000000\" label=\" 2.5000000000 - 3.0000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"7\" lower=\"3.000000\" upper=\"3.500000\" label=\" 3.0000000000 - 3.5000000000 \"/>\n");
//		out.write("\t\t\t\t\t<range render=\"true\" symbol=\"8\" lower=\"3.500000\" upper=\"5.863296\" label=\" 3.5000000000 - 5.8632955777 \"/>\n");
//		out.write("\t\t\t\t</ranges>\n");
//		
//		out.write("\t\t\t\t<symbols>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"0\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"215,25,28,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"1\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"234,99,62,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"2\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"253,174,97,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"3\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"254,214,144,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"4\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"255,255,191,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"5\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"213,238,177,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"6\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"171,221,164,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"7\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"107,176,175,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"marker\" name=\"8\">\n");
//		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"SimpleMarker\" locked=\"0\">\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"43,131,186,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"rectangle\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"0,0,0,255\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"no\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"1010\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MapUnit\"/>\n");
//		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
//		out.write("\t\t\t\t\t\t</layer>\n");
//		out.write("\t\t\t\t\t</symbol>\n");
//		
//		out.write("\t\t\t\t</symbols>\n");
//		
//		out.write("\t\t\t</renderer-v2>\n");
//		
//		out.write("\t\t</maplayer>\n");
//		
//		out.write("\t\t<maplayer minimumScale=\"-4.65661e-10\" maximumScale=\"1e+08\" type=\"raster\" hasScaleBasedVisibilityFlag=\"0\">\n");
//		out.write("\t\t\t<id>osm_mapnik_xml20141212003803940</id>\n");
//		//out.write("\t\t\t<datasource>C:/Users/Dominik/.qgis2/python/plugins/TileMapScaleLevels/datasets/osm_mapnik.xml</datasource>\n");
//		out.write("\t\t\t<datasource>" + osmMapnikFile + "</datasource>\n");
//		out.write("\t\t\t<layername>osm_mapnik.xml</layername>\n");
//		out.write("\t\t\t\n");
//		
//		out.write("\t\t\t<srs>\n");
//		out.write("\t\t\t\t<spatialrefsys>\n");
//		out.write("\t\t\t\t\t<proj4>+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs</proj4>\n");
//		out.write("\t\t\t\t\t<srsid>3857</srsid>\n");
//		out.write("\t\t\t\t\t<srid>3857</srid>\n");
//		out.write("\t\t\t\t\t<authid>EPSG:3857</authid>\n");
//		out.write("\t\t\t\t\t<description>WGS 84 / Pseudo Mercator</description>\n");
//		out.write("\t\t\t\t\t<projectionacronym>merc</projectionacronym>\n");
//		out.write("\t\t\t\t\t<ellipsoidacronym>WGS84</ellipsoidacronym>\n");
//		out.write("\t\t\t\t\t<geographicflag>false</geographicflag>\n");
//		out.write("\t\t\t\t</spatialrefsys>\n");
//		out.write("\t\t\t</srs>\n");
//		
//		out.write("\t\t\t<customproperties>\n");
//		out.write("\t\t\t\t<property key=\"identify/format\" value=\"Value\"/>\n");
//		out.write("\t\t\t</customproperties>\n");
//		out.write("\t\t\t<provider>gdal</provider>\n");
//		out.write("\t\t\t<noData>\n");
//		out.write("\t\t\t\t<noDataList bandNo=\"1\" useSrcNoData=\"0\"/>\n");
//		out.write("\t\t\t\t<noDataList bandNo=\"2\" useSrcNoData=\"0\"/>\n");
//		out.write("\t\t\t\t<noDataList bandNo=\"3\" useSrcNoData=\"0\"/>\n");
//		out.write("\t\t\t</noData>\n");
//		out.write("\t\t\t<pipe>\n");
//		out.write("\t\t\t\t<rasterrenderer opacity=\"0.490196\" alphaBand=\"0\" blueBand=\"3\" greenBand=\"2\" type=\"multibandcolor\" redBand=\"1\">\n");
//		out.write("\t\t\t\t\t<rasterTransparency/>\n");
//		out.write("\t\t\t\t</rasterrenderer>\n");
//		out.write("\t\t\t\t<brightnesscontrast brightness=\"0\" contrast=\"0\"/>\n");
//		out.write("\t\t\t\t<huesaturation colorizeGreen=\"128\" colorizeOn=\"0\" colorizeRed=\"255\" colorizeBlue=\"128\" grayscaleMode=\"0\" saturation=\"0\" colorizeStrength=\"100\"/>\n");
//		out.write("\t\t\t\t<rasterresampler maxOversampling=\"2\"/>\n");
//		out.write("\t\t\t</pipe>\n");
//		out.write("\t\t\t<blendMode>0</blendMode>\n");
//		out.write("\t\t</maplayer>\n");
//		out.write("\t</projectlayers>\n");
//	}
}