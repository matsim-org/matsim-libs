package playground.dhosse.qgis;

import java.io.BufferedWriter;
import java.io.IOException;

public class QGisWriterHandler {
	
	private final QGisWriter writer;
	
	public QGisWriterHandler(QGisWriter writer){
		this.writer = writer;
	}
	
	public void writeHeaderAndStartElement(BufferedWriter out) throws IOException{
		
		out.write("<!DOCTYPE qgis PUBLIC 'http://mrcc.com/qgis.dtd' 'SYSTEM'>\n");
		out.write("<qgis projectname=\"\" version=\"" + QGisConstants.currentVersion + "\">\n");
		
	}
	
	public void writeTitle(BufferedWriter out) throws IOException{
		
		out.write("\t<title></title>\n");
		
	}
	
	public void writeLayerTreeGroup(BufferedWriter out) throws IOException{
		
		out.write("\t<layer-tree-group expanded=\"1\" checked=\"Qt::Checked\" name=\"\">\n");
		
		out.write("\t\t<customproperties/>\n");
		
		//write layer tree layers
		for(QGisLayer layer : this.writer.getLayers().values()){
			
			writeLayerTreeLayer(out, layer);
			
		}
		
		out.write("\t</layer-tree-group>\n");
		
	}
	
	private void writeLayerTreeLayer(BufferedWriter out, QGisLayer layer) throws IOException{
		
		out.write("\t\t<layer-tree-layer expanded=\"1\" checked=\"Qt::Checked\" id=\"" + layer.getId().toString() + this.writer.today + "\" name=\"" + layer.getName() + "\">\n");
		out.write("\t\t\t<customproperties/>\n");
		out.write("\t\t</layer-tree-layer>\n");
		
	}
	
	public void writeRelations(BufferedWriter out) throws IOException{
		
		out.write("\t<relations/>\n");
		
	}
	
	public void writeMapCanvas(BufferedWriter out) throws IOException{
		
		out.write("\t<mapcanvas>\n");
		
		out.write("\t\t<units>degrees</units>\n");
		out.write("\t\t<extent>\n");
		
		out.write("\t\t\t<xmin>" + this.writer.getExtent()[0] + "</xmin>\n");
		out.write("\t\t\t<ymin>" + this.writer.getExtent()[1] + "</ymin>\n");
		out.write("\t\t\t<xmax>" + this.writer.getExtent()[2] + "</xmax>\n");
		out.write("\t\t\t<ymax>" + this.writer.getExtent()[3] + "</ymax>\n");
		
		out.write("\t\t</extent>\n");
		out.write("\t\t<projections>0</projections>\n");
		writeDestinationSrs(out);
		out.write("\t\t<layer_coordinate_transform_info/>\n");
		
		out.write("\t</mapcanvas>\n");
		
	}
	
	private void writeDestinationSrs(BufferedWriter out) throws IOException{
		
		out.write("\t\t<destinationsrs>\n");
		
		out.write("\t\t\t<spatialrefsys>\n");
		
		out.write("\t\t\t\t<proj4>+proj=longlat +datum=WGS84 +no_defs</proj4>\n");
		out.write("\t\t\t\t<srsid>3452</srsid>\n");
		out.write("\t\t\t\t<srid>4326</srid>\n");
		out.write("\t\t\t\t<authid>EPSG:4326</authid>\n");
		out.write("\t\t\t\t<description>WGS 84</description>\n");
		out.write("\t\t\t\t<projectionacronym>longlat</projectionacronym>\n");
		out.write("\t\t\t\t<ellipsoidacronym>WGS84</ellipsoidacronym>\n");
		out.write("\t\t\t\t<geographicflag>true</geographicflag>\n");
		
		out.write("\t\t\t</spatialrefsys>\n");
		
		out.write("\t\t</destinationsrs>\n");
		
	}
	
	public void writeLayerTreeCanvas(BufferedWriter out) throws IOException{
		
		out.write("\t<layer-tree-canvas>\n");
		
		out.write("\t\t<custom-order enabled=\"0\">\n");
		
		for(QGisLayer layer : this.writer.layers.values()){
			
			writeItem(out, layer);
			
		}
		
		out.write("\t\t</custom-order>\n");
		
		out.write("\t</layer-tree-canvas>\n");
		
	}
	
	private void writeItem(BufferedWriter out, QGisLayer layer) throws IOException{
		
		out.write("\t\t\t<item>" + layer.getId().toString() + this.writer.today + "</item>\n");
		
	}
	
	public void writeLegend(BufferedWriter out) throws IOException{
		
		out.write("\t<legend updateDrawingOrder=\"true\">\n");
		
		for(QGisLayer layer : this.writer.getLayers().values()){
			
			writeLegendLayer(out, layer);
			
		}
		
		out.write("\t</legend>\n");
		
	}
	
	private void writeLegendLayer(BufferedWriter out, QGisLayer layer) throws IOException{
		
		out.write("\t\t<legendlayer drawingOrder=\"-1\" open=\"true\" checked=\"Qt::Checked\" name=\"" + layer.getName() + "\" showFeatureCount=\"0\">\n");
		
		out.write("\t\t\t<filegroup open=\"true\" hidden=\"false\">\n");
		
		out.write("\t\t\t\t<legendlayerfile isInOverview=\"0\" layerid=\"" + layer.getId().toString() + this.writer.today + "\" visible=\"1\"/>\n");
		
		out.write("\t\t\t</filegroup>\n");
		
		out.write("\t\t</legendlayer>\n");
		
	}
	
	public void writeProjectLayers(BufferedWriter out) throws IOException{
		
		out.write("\t<projectlayers layercount=\"" + this.writer.layers.size() + "\">\n");
		
		for(QGisLayer layer : this.writer.layers.values()){
			
			writeMapLayer(out, layer);
			
		}
		
		out.write("\t</projectlayers>\n");
		
	}
	
	private void writeMapLayer(BufferedWriter out, QGisLayer layer) throws IOException{
		
		out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" simplifyDrawingHints=\"1\" minLabelScale=\"0\" maxLabelScale=\"1e+08\" simplifyDrawingTol=\"1\" geometry=\"" + layer.getGeometryType().toString() + "\" simplifyMaxScale=\"1\" type=\"vector\" hasScaleBasedVisibilityFlag=\"0\" simplifyLocal=\"1\" scaleBasedLabelVisibilityFlag=\"0\">\n");
		
		out.write("\t\t\t<id>" + layer.getId().toString() + this.writer.today + "</id>\n");
		out.write("\t\t\t<datasource>" + "./" + layer.getId().toString() + ".shp" + "</datasource>\n");
		out.write("\t\t\t<title></title>\n");
		out.write("\t\t\t<abstract></abstract>\n");
		out.write("\t\t\t<keywordList>\n");
		
		out.write("\t\t\t\t<value></value>\n");
		
		out.write("\t\t\t</keywordList>\n");
		out.write("\t\t\t<layername>" + layer.getName() + "</layername>\n");
		out.write("\t\t\t<srs>\n");
		
		out.write("\t\t\t\t<spatialrefsys>\n");
		
		out.write("\t\t\t\t\t<proj4>+proj=longlat +datum=WGS84 +no_defs</proj4>\n");
		out.write("\t\t\t\t<srsid>3452</srsid>\n");
		out.write("\t\t\t\t<srid>4326</srid>\n");
		out.write("\t\t\t\t<authid>EPSG:4326</authid>\n");
		out.write("\t\t\t\t<description>WGS 84</description>\n");
		out.write("\t\t\t\t<projectionacronym>longlat</projectionacronym>\n");
		out.write("\t\t\t\t<ellipsoidacronym>WGS84</ellipsoidacronym>\n");
		out.write("\t\t\t\t<geographicflag>true</geographicflag>\n");
		
		out.write("\t\t\t\t</spatialrefsys>\n");
		
		out.write("\t\t\t</srs>\n");
		out.write("\t\t\t<provider encoding=\"System\">ogr</provider>\n");
		out.write("\t\t\t<previewExpression></previewExpression>\n");
		out.write("\t\t\t<vectorjoins/>\n");
		
		if(layer.getRenderer().getRenderingType().equals(QGisConstants.renderingType.categorizedSymbol)){
			
//			out.write("\t\t\t<renderer-v2 attr=\"" + ((CategorizedSymbolRenderer)layer.getRenderer()).getAttribute() + "\" symbollevels=\"0\" type=\"" + layer.getRenderer().getRenderingType().toString() + "\">\n");
//			out.write("\t\t\t\t<categories>\n");
//			CategorizedSymbolRenderer renderer = (CategorizedSymbolRenderer)layer.getRenderer();
//			
//			for(Category category : renderer.getCategories()){
//				
//				out.write("\t\t\t\t\t<category symbol=\"" + Integer.toString(category.getId()) + "\" value=\"" +
//						Integer.toString((int)category.getValue()) + "\" label=\"" + category.getLabel() + "\"" + "/>\n");
//				
//			}
//			
//			out.write("\t\t\t\t</categories>\n");
			
		} else if(layer.getRenderer().getRenderingType().equals(QGisConstants.renderingType.graduatedSymbol)){
			
			out.write("\t\t\t\t<ranges>\n");
			out.write("\t\t\t\t</ranges>\n");
			
		} else if(layer.getRenderer().getRenderingType().equals(QGisConstants.renderingType.RuleRenderer)){
			
		} else{
			out.write("\t\t\t<renderer-v2 symbollevels=\"0\" type=\"" + layer.getRenderer().getRenderingType().toString() + "\">\n");
		}
		
		out.write("\t\t\t\t<symbols>\n");
		
		if(layer.getGeometryType().equals(QGisConstants.geometryType.Line)){
			writeLineLayer(out, layer);
		} else if(layer.getGeometryType().equals(QGisConstants.geometryType.Point)){
			writePointLayer(out, layer);
		}
		
		out.write("\t\t\t\t</symbols>\n");
		out.write("\t\t\t\t<rotation/>\n");
		out.write("\t\t\t\t<sizescale scalemethod=\"area\"/>\n");
		
		out.write("\t\t\t</renderer-v2>\n");
		out.write("\t\t\t<customproperties/>\n");
		out.write("\t\t\t<featureBlendMode>0</featureBlendMode>\n");
		out.write("\t\t\t<layerTransparency>" + Integer.toString(layer.getLayerTransparency()) + "</layerTransparency>\n");
		out.write("\t\t\t<displayfield>id</displayfield>\n");
		out.write("\t\t\t<label>0</label>\n");
		out.write("\t\t\t<labelattributes>\n");
		
		out.write("\t\t\t\t<label fieldname=\"\" text=\"Beschriftung\"/>\n");
		out.write("\t\t\t\t<family fieldname=\"\" name=\"MS Shell Dlg 2\"/>\n");
		out.write("\t\t\t\t<size fieldname=\"\" units=\"pt\" value=\"12\"/>\n");
		out.write("\t\t\t\t<bold fieldname=\"\" on=\"0\"/>\n");
		out.write("\t\t\t\t<italic fieldname=\"\" on=\"0\"/>\n");
		out.write("\t\t\t\t<underline fieldname=\"\" on=\"0\"/>\n");
		out.write("\t\t\t\t<strikeout fieldname=\"\" on=\"0\"/>\n");
		out.write("\t\t\t\t<color fieldname=\"\" red=\"0\" blue=\"0\" green=\"0\"/>\n");
		out.write("\t\t\t\t<x fieldname=\"\"/>\n");
		out.write("\t\t\t\t<y fieldname=\"\"/>\n");
		out.write("\t\t\t\t<offset x=\"0\" y=\"0\" units=\"pt\" yfieldname=\"\" xfieldname=\"\"/>\n");
		out.write("\t\t\t\t<angle fieldname=\"\" value=\"0\" auto=\"0\"/>\n");
		out.write("\t\t\t\t<alignment fieldname=\"\" value=\"center\"/>\n");
		out.write("\t\t\t\t<buffercolor fieldname=\"\" red=\"255\" blue=\"255\" green=\"255\"/>\n");
		out.write("\t\t\t\t<buffersize fieldname=\"\" units=\"pt\" value=\"1\"/>\n");
		out.write("\t\t\t\t<bufferenabled fieldname=\"\" on=\"\"/>\n");
		out.write("\t\t\t\t<multilineenabled fieldname=\"\" on=\"\"/>\n");
		out.write("\t\t\t\t<selectedonly on=\"\"/>\n");
		
		out.write("\t\t\t</labelattributes>\n");
		out.write("\t\t\t<edittypes>\n");
		
		out.write("\t\t\t\t<edittype labelontop=\"0\" editable=\"0\" widgetv2type=\"TextEdit\" name=\"id\">\n");
		out.write("\t\t\t\t\t<widgetv2config IsMultiline=\"0\" fieldEditable=\"1\" UseHtml=\"0\" labelOnTop=\"0\"/>\n");
		out.write("\t\t\t\t</edittype>\n");
		
		out.write("\t\t\t\t<edittype labelontop=\"0\" editable=\"0\" widgetv2type=\"TextEdit\" name=\"length\">\n");
		out.write("\t\t\t\t\t<widgetv2config IsMultiline=\"0\" fieldEditable=\"1\" UseHtml=\"0\" labelOnTop=\"0\"/>\n");
		out.write("\t\t\t\t</edittype>\n");
			
		out.write("\t\t\t\t<edittype labelontop=\"0\" editable=\"0\" widgetv2type=\"TextEdit\" name=\"freespeed\">\n");
		out.write("\t\t\t\t\t<widgetv2config IsMultiline=\"0\" fieldEditable=\"1\" UseHtml=\"0\" labelOnTop=\"0\"/>\n");
		out.write("\t\t\t\t</edittype>\n");
			
		out.write("\t\t\t\t<edittype labelontop=\"0\" editable=\"0\" widgetv2type=\"TextEdit\" name=\"capacity\">\n");
		out.write("\t\t\t\t\t<widgetv2config IsMultiline=\"0\" fieldEditable=\"1\" UseHtml=\"0\" labelOnTop=\"0\"/>\n");
		out.write("\t\t\t\t</edittype>\n");
			
		out.write("\t\t\t\t<edittype labelontop=\"0\" editable=\"0\" widgetv2type=\"TextEdit\" name=\"nlanes\">\n");
		out.write("\t\t\t\t\t<widgetv2config IsMultiline=\"0\" fieldEditable=\"1\" UseHtml=\"0\" labelOnTop=\"0\"/>\n");
		out.write("\t\t\t\t</edittype>\n");
			
		out.write("\t\t\t</edittypes>\n");
		out.write("\t\t\t<editform></editform>\n");
		out.write("\t\t\t<editforminit></editforminit>\n");
		out.write("\t\t\t<featformsuppress>0</featformsuppress>\n");
		out.write("\t\t\t<annotationform></annotationform>\n");
		out.write("\t\t\t<editorlayout>generatedlayout</editorlayout>\n");
		out.write("\t\t\t<excludeAttributesWMS/>\n");
		out.write("\t\t\t<excludeAttributesWFS/>\n");
		out.write("\t\t\t<attributeactions/>\n");
		
		out.write("\t\t</maplayer>\n");
		
	}
	
	private void writePointLayer(BufferedWriter out, QGisLayer layer) throws IOException {

		int idx = 0;
		
		for(QGisSymbolLayer sl : layer.getRenderer().getSymbolLayers()){
			
			QGisPointSymbolLayer psl = (QGisPointSymbolLayer)sl;
			
			String color = Integer.toString(psl.getColor().getRed()) + "," 
					+ Integer.toString(psl.getColor().getBlue()) + "," +
					Integer.toString(psl.getColor().getGreen()) + ";" +
					Integer.toString(psl.getColor().getAlpha());
			
			String colorBorder = Integer.toString(psl.getColorBorder().getRed()) + "," 
					+ Integer.toString(psl.getColorBorder().getBlue()) + "," +
					Integer.toString(psl.getColorBorder().getGreen()) + ";" +
					Integer.toString(psl.getColorBorder().getAlpha());
			
			String offset = Double.toString(psl.getOffset()[0]) + "," + Double.toString(psl.getOffset()[1]);
			String offsetMapUnitScale = Double.toString(psl.getOffsetMapUnitScale()[0]) + "," +
					Double.toString(psl.getOffsetMapUnitScale()[1]); 
			
			//different to line layer
			out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"" + psl.getSymbolType().toString().toLowerCase() + "\" name=\"" + idx + "\">\n");
			
			out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"" + layer.getLayerClass().toString() + "\" locked=\"0\">\n");
			
			out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"" + color + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color_border\" v=\"" + colorBorder + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"" + psl.getPointLayerSymbol().toString() + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"" + offset + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"" + offsetMapUnitScale + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MM\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"solid\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"MM\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"" + Double.toString(psl.getSize()) + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"MM\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
			
			out.write("\t\t\t\t\t\t</layer>\n");
			
			out.write("\t\t\t\t\t</symbol>\n");
			
			idx++;
			
		}
		
	}

	private void writeLineLayer(BufferedWriter out, QGisLayer layer) throws IOException {
		
		int idx = 0;
		
		for(QGisSymbolLayer sl : layer.getRenderer().getSymbolLayers()){
			
			QGisLineSymbolLayer lsl = (QGisLineSymbolLayer)sl;
			
			String color = Integer.toString(lsl.getColor().getRed()) + "," 
					+ Integer.toString(lsl.getColor().getBlue()) + "," +
					Integer.toString(lsl.getColor().getGreen()) + ";" +
					Integer.toString(lsl.getColor().getAlpha());
			
			out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"" + lsl.getSymbolType().toString().toLowerCase() + "\" name=\"" + idx + "\">\n");
			
			out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"" + layer.getLayerClass().toString() + "\" locked=\"0\">\n");
			
			out.write("\t\t\t\t\t\t\t<prop k=\"capstyle\" v=\"square\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"" + color + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"customdash\" v=\"5;2\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"customdash_map_unit_scale\" v=\"0,0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"customdash_unit\" v=\"MM\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"draw_inside_polygon\" v=\"0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"joinstyle\" v=\"bevel\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"MM\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"penstyle\" v=\"solid\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"use_custom_dash\" v=\"0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"width\" v=\"" + lsl.getWidth() + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"width_map_unit_scale\" v=\"0,0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"width_unit\" v=\"MM\"/>\n");

			out.write("\t\t\t\t\t\t</layer>\n");
			
			out.write("\t\t\t\t\t</symbol>\n");
			
			idx++;
			
		}
		
	}

	public void writeProperties(BufferedWriter out) throws IOException{
		
		out.write("\t<properties>\n");
		
		out.write("\t\t<SpatialRefSys>\n");
		
		out.write("\t\t\t<ProjectCRSProj4String type=\"QString\">+proj=longlat +datum=WGS84 +no_defs</ProjectCRSProj4String>\n");
		out.write("\t\t\t<ProjectCrs type=\"QString\">EPSG:4326</ProjectCrs>\n");
		out.write("\t\t\t<ProjectCRSID type=\"int\">3452</ProjectCRSID>\n");
		
		out.write("\t\t</SpatialRefSys>\n");
		out.write("\t\t<Paths>\n");
		
		out.write("\t\t\t<Absolute type=\"bool\">false</Absolute>\n");
		
		out.write("\t\t</Paths>\n");
		out.write("\t\t<Gui>\n");
		
		out.write("\t\t\t<SelectionColorBluePart type=\"int\">0</SelectionColorBluePart>\n");
		out.write("\t\t\t<CanvasColorGreenPart type=\"int\">255</CanvasColorGreenPart>\n");
		out.write("\t\t\t<CanvasColorRedPart type=\"int\">255</CanvasColorRedPart>\n");
		out.write("\t\t\t<SelectionColorRedPart type=\"int\">255</SelectionColorRedPart>\n");
		out.write("\t\t\t<SelectionColorAlphaPart type=\"int\">255</SelectionColorAlphaPart>\n");
		out.write("\t\t\t<SelectionColorGreenPart type=\"int\">255</SelectionColorGreenPart>\n");
		out.write("\t\t\t<CanvasColorBluePart type=\"int\">255</CanvasColorBluePart>\n");
		
		out.write("\t\t</Gui>\n");
		out.write("\t\t<PositionPrecision>\n");
		
		out.write("\t\t\t<DecimalPlaces type=\"int\">2</DecimalPlaces>\n");
		out.write("\t\t\t<Automatic type=\"bool\">true</Automatic>\n");
		
		out.write("\t\t</PositionPrecision>\n");
		
		out.write("\t</properties>\n");
		
	}

	public void endFile(BufferedWriter out) throws IOException{
		
		out.write("</qgis>");
		
	}

}
