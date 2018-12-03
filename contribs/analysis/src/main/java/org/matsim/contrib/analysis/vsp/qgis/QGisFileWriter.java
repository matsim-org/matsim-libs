package org.matsim.contrib.analysis.vsp.qgis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * This class does the actual "writing" of the qgis project file.
 * 
 * @author dhosse, gthunig
 *
 */

public class QGisFileWriter {
	
	private final QGisWriter writer;
	
	public QGisFileWriter(QGisWriter writer){
		this.writer = writer;
	}
	
	public void writeHeaderAndStartElement(BufferedWriter out) throws IOException{
		
		out.write("<!DOCTYPE qgis PUBLIC 'http://mrcc.com/qgis.dtd' 'SYSTEM'>\n");
		out.write("<qgis projectname=\"" + this.writer.getProjectname() + "\" version=\"" + QGisConstants.currentVersion + "\">\n");
		
	}
	
	public void writeTitle(BufferedWriter out) throws IOException{
		
		out.write("\t<title>" + this.writer.getTitle() + "</title>\n");
		
	}
	
	public void writeLayerTreeGroup(BufferedWriter out) throws IOException{
		
		out.write("\t<layer-tree-group expanded=\"1\" checked=\"Qt::Checked\" name=\"\">\n");
		
		out.write("\t\t<customproperties/>\n");
		
		for(QGisLayer layer : this.writer.getLayers()){
			
			writeLayerTreeLayer(out, layer);
			
		}
		
		out.write("\t</layer-tree-group>\n");
		
	}
	
	private void writeLayerTreeLayer(BufferedWriter out, QGisLayer layer) throws IOException{
		
		out.write("\t\t<layer-tree-layer expanded=\"1\" checked=\"Qt::Checked\" id=\"" + layer.getId().toString() + "\" name=\"" + layer.getName() + "\">\n");
		out.write("\t\t\t<customproperties/>\n");
		out.write("\t\t</layer-tree-layer>\n");
		
	}
	
	public void writeMapCanvas(BufferedWriter out) throws IOException{
		
		out.write("\t<mapcanvas>\n");
		
		out.write("\t\t<units>" + this.writer.getUnit().toString() + "</units>\n");
		out.write("\t\t<extent>\n");
		
		out.write("\t\t\t<xmin>" + this.writer.getEnvelope().getMinX() + "</xmin>\n");
		out.write("\t\t\t<ymin>" + this.writer.getEnvelope().getMinY() + "</ymin>\n");
		out.write("\t\t\t<xmax>" + this.writer.getEnvelope().getMaxX() + "</xmax>\n");
		out.write("\t\t\t<ymax>" + this.writer.getEnvelope().getMaxY() + "</ymax>\n");
		
		out.write("\t\t</extent>\n");
		out.write("\t\t<projections>1</projections>\n");
		this.writeDestinationSrs(out);
		out.write("\t\t<layer_coordinate_transform_info/>\n");
		
		out.write("\t</mapcanvas>\n");
		
	}
	
	private void writeDestinationSrs(BufferedWriter out) throws IOException{
		
		out.write("\t\t<destinationsrs>\n");
		
		out.write("\t\t\t<spatialrefsys>\n");
		
		out.write("\t\t\t\t<proj4>" + this.writer.getSRS().getProj4() + "</proj4>\n");
		out.write("\t\t\t\t<srsid>" + this.writer.getSRS().getSrsid() + "</srsid>\n");
		out.write("\t\t\t\t<srid>" + this.writer.getSRS().getSrid() + "</srid>\n");
		out.write("\t\t\t\t<authid>" + this.writer.getSRS().getAuthid() + "</authid>\n");
		out.write("\t\t\t\t<description>" + this.writer.getSRS().getDescription() + "</description>\n");
		out.write("\t\t\t\t<projectionacronym>" + this.writer.getSRS().getProjectionacronym() + "</projectionacronym>\n");
		out.write("\t\t\t\t<ellipsoidacronym>" + this.writer.getSRS().getEllipsoidacronym() + "</ellipsoidacronym>\n");
		out.write("\t\t\t\t<geographicflag>true</geographicflag>\n");
		
		out.write("\t\t\t</spatialrefsys>\n");
		
		out.write("\t\t</destinationsrs>\n");
		
	}
	
	public void writeLayerTreeCanvas(BufferedWriter out) throws IOException{
		
		out.write("\t<layer-tree-canvas>\n");
		
		out.write("\t\t<custom-order enabled=\"0\">\n");
		
		for(QGisLayer layer : this.writer.getLayers()){
			
			writeItem(out, layer);
			
		}
		
		out.write("\t\t</custom-order>\n");
		
		out.write("\t</layer-tree-canvas>\n");
		
	}
	
	private void writeItem(BufferedWriter out, QGisLayer layer) throws IOException{
		
		out.write("\t\t\t<item>" + layer.getId().toString() + "</item>\n");
		
	}
	
	public void writeProjectLayers(BufferedWriter out) throws IOException{
		
		out.write("\t<projectlayers layercount=\"" + this.writer.getLayers().size() + "\">\n");
		
		for(QGisLayer layer : this.writer.getLayers()){
			
			writeMapLayer(out, layer);
			
		}
		
		out.write("\t</projectlayers>\n");
		
	}
	
	private void writeMapLayer(BufferedWriter out, QGisLayer layer) throws IOException{
		
		if(layer.getType().equals(QGisConstants.layerType.vector)){
			
			writeVectorLayer(out, layer);
			
		} else if(layer.getType().equals(QGisConstants.layerType.raster)){
			
			writeRasterLayer(out, layer);
			
		} else {
			throw new RuntimeException("Cannot write map layer. Unknown format.");
		}
		
		out.write("\t\t</maplayer>\n");
		
	}
	
	private void writeVectorLayer(BufferedWriter out, QGisLayer layer) throws IOException {
		
		VectorLayer vlayer = (VectorLayer) layer;
		
		if(vlayer.getGeometryType().equals(QGisConstants.geometryType.No_geometry)){
			
			out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" geometry=\"" + vlayer.getGeometryType().toString().replace("_", " ") + "\" type=\"" + vlayer.getType() + "\" hasScaleBasedVisibilityFlag=\"0\">\n");
			
		} else{
			
			out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" simplifyDrawingHints=\"1\" minLabelScale=\"0\" maxLabelScale=\"1e+08\" simplifyDrawingTol=\"1\" geometry=\"" + vlayer.getGeometryType().toString() + "\" simplifyMaxScale=\"1\" type=\"vector\" hasScaleBasedVisibilityFlag=\"0\" simplifyLocal=\"1\" scaleBasedLabelVisibilityFlag=\"0\">\n");
		
		}

		out.write("\t\t\t<id>" + vlayer.getId().toString() + "</id>\n");
		
		String relP = Paths.get(this.writer.getWorkingDir()).relativize(Paths.get(layer.getPath())).toString();
		
		if(isCsvDataLayer(vlayer)){
			
			String datasource = relP + "?type=csv&amp;useHeader=" + vlayer.headerUsed() + "&amp;";
			
			if(vlayer.getDelimiter() != null){
				
				datasource += "delimiter=" + vlayer.getDelimiter() + "&amp;";
			}
			
			
			datasource += "quote='&amp;escape='&amp;skipEmptyField=Yes&amp;xField=" + vlayer.getXField() +
					"&amp;yField=" + vlayer.getYField() + "&amp;spatialIndex=no&amp;subsetIndex=no&amp;watchFile=no\n";
			
			out.write("\t\t\t<datasource>" + datasource + "</datasource>\n");
			
		} else{
			
			relP.replace("file:/", "");
			
			out.write("\t\t\t<datasource>" + relP + "</datasource>\n");
			
		}
		
		out.write("\t\t\t<title></title>\n");
		out.write("\t\t\t<abstract></abstract>\n");
		out.write("\t\t\t<keywordList>\n");
		
		out.write("\t\t\t\t<value></value>\n");
		
		out.write("\t\t\t</keywordList>\n");
		out.write("\t\t\t<layername>" + vlayer.getName() + "</layername>\n");
		out.write("\t\t\t<srs>\n");
		
		out.write("\t\t\t\t<spatialrefsys>\n");

		SRS srs = vlayer.getSrs();
		
		if(srs == null){
			
			srs = this.writer.getSRS();
			
		}
		
		out.write("\t\t\t\t\t<proj4>" + srs.getProj4() + "</proj4>\n");
		out.write("\t\t\t\t\t<srsid>" + srs.getSrsid() + "</srsid>\n");
		out.write("\t\t\t\t\t<srid>" + srs.getSrid() + "</srid>\n");
		out.write("\t\t\t\t\t<authid>" + srs.getAuthid() + "</authid>\n");
		out.write("\t\t\t\t\t<description>" + srs.getDescription() + "</description>\n");
		out.write("\t\t\t\t\t<projectionacronym>" + srs.getProjectionacronym() + "</projectionacronym>\n");
		out.write("\t\t\t\t\t<ellipsoidacronym>" + srs.getEllipsoidacronym() + "</ellipsoidacronym>\n");
		out.write("\t\t\t\t\t<geographicflag>true</geographicflag>\n");
		
		out.write("\t\t\t\t</spatialrefsys>\n");
		
		out.write("\t\t\t</srs>\n");
		
		if(vlayer.getInputType().equals(QGisConstants.inputType.csv)&&vlayer.getRenderer() != null){
			
			out.write("\t\t\t<provider encoding=\"System\">delimitedtext</provider>\n");
			
		} else{
			
			out.write("\t\t\t<provider encoding=\"System\">ogr</provider>\n");
			
		}
		
		if(vlayer.getVectorJoins().isEmpty()){
			
			out.write("\t\t\t<vectorjoins/>\n");
			
		} else{
			
			out.write("\t\t\t<vectorjoins>\n");
			
			for(VectorJoin vj : vlayer.getVectorJoins()){
				
				out.write("\t\t\t\t<join joinFieldName=\"" + vj.getJoinFieldName() +"\" targetFieldName=\"" + vj.getTargetFieldName() + "\""
						+ " memoryCache=\"1\" joinLayerId=\"" + vj.getJoinLayerId().toString() + "\"/>\n");
				
			}
			
			out.write("\t\t\t</vectorjoins>\n");
			
		}
		
		if(vlayer.getRenderer() != null){
			
			writeGeometryLayer(out,vlayer);
			
			out.write("\t\t\t<featureBlendMode>0</featureBlendMode>\n");
			out.write("\t\t\t<layerTransparency>" + Integer.toString(vlayer.getLayerTransparency()) + "</layerTransparency>\n");

		}
		
		out.write("\t\t\t<editform></editform>\n");
		out.write("\t\t\t<editforminit></editforminit>\n");
		out.write("\t\t\t<featformsuppress>0</featformsuppress>\n");
		out.write("\t\t\t<annotationform></annotationform>\n");
		out.write("\t\t\t<editorlayout>generatedlayout</editorlayout>\n");
		out.write("\t\t\t<excludeAttributesWMS/>\n");
		out.write("\t\t\t<excludeAttributesWFS/>\n");
		out.write("\t\t\t<attributeactions/>\n");
		
	}
	
	private boolean isCsvDataLayer(VectorLayer vlayer) {
		
		return(vlayer.getInputType().equals(QGisConstants.inputType.csv) && !vlayer.getGeometryType().equals(QGisConstants.geometryType.No_geometry));
	
	}

	private void writeRasterLayer(BufferedWriter out, QGisLayer layer)throws IOException {
		
		RasterLayer rlayer = (RasterLayer) layer;
		
		out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" type=\"" + rlayer.getType() + "\" hasScaleBasedVisibilityFlag=\"0\">\n");
		out.write("\t\t\t<id>" + rlayer.getId().toString() + "</id>\n");
		
		String relP = Paths.get(this.writer.getWorkingDir()).relativize(Paths.get(layer.getPath())).toString();
		
		out.write("\t\t\t<datasource>" + relP + "</datasource>\n");
		out.write("\t\t\t<layername>" + layer.getName() + "</layername>\n");
		
		out.write("\t\t\t<srs>\n");
		
		out.write("\t\t\t\t<spatialrefsys>\n");
		
		SRS srs = layer.getSrs();
		
		if(srs == null){
			
			srs = this.writer.getSRS();
			
		}
		
		out.write("\t\t\t\t\t<proj4>" + srs.getProj4() + "</proj4>\n");
		out.write("\t\t\t\t\t<srsid>" + srs.getSrsid() + "</srsid>\n");
		out.write("\t\t\t\t\t<srid>" + srs.getSrid() + "</srid>\n");
		out.write("\t\t\t\t\t<authid>" + srs.getAuthid() + "</authid>\n");
		out.write("\t\t\t\t\t<description>" + srs.getDescription() + "</description>\n");
		out.write("\t\t\t\t\t<projectionacronym>" + srs.getProjectionacronym() + "</projectionacronym>\n");
		out.write("\t\t\t\t\t<ellipsoidacronym>" + srs.getEllipsoidacronym() + "</ellipsoidacronym>\n");
		out.write("\t\t\t\t\t<geographicflag>true</geographicflag>\n");
		
		out.write("\t\t\t\t</spatialrefsys>\n");
		
		out.write("\t\t\t</srs>\n");
		out.write("\t\t\t<customproperties>\n");
		
		out.write("\t\t\t\t<property key=\"identify/format\" value=\"Value\"/>\n");
		
		out.write("\t\t\t</customproperties>\n");
		out.write("\t\t\t<provider>gdal</provider>\n");
		out.write("\t\t\t<noData>\n");
		
		out.write("\t\t\t\t<noDataList bandNo=\"1\" useSrcNoData=\"0\"/>\n");
		out.write("\t\t\t\t<noDataList bandNo=\"2\" useSrcNoData=\"0\"/>\n");
		out.write("\t\t\t\t<noDataList bandNo=\"3\" useSrcNoData=\"0\"/>\n");
		
		out.write("\t\t\t</noData>\n");
		out.write("\t\t\t<pipe>\n");
		
		QGisRasterRenderer renderer = (QGisRasterRenderer)layer.getRenderer();
		
		out.write("\t\t\t\t<rasterrenderer opacity=\"" + renderer.getOpacity() + "\" alphaBand=\"" + renderer.getAlphaBand() +
				"\" blueBand=\"" + renderer.getBlueBand() + "\" greenBand=\"" + renderer.getGreenBand() + "\" type=\"" + renderer.getType() +
				"\" redBand=\"" + renderer.getRedBand() + "\">\n");
		
		out.write("\t\t\t\t\t<rasterTransparency/>\n");
		
		out.write("\t\t\t\t</rasterrenderer>\n");
		out.write("\t\t\t\t<brightnesscontrast brightness=\"0\" contrast=\"0\"/>\n");
		out.write("\t\t\t\t<huesaturation colorizeGreen=\"128\" colorizeOn=\"0\" colorizeRed=\"255\" colorizeBlue=\"128\" grayscaleMode=\"0\" saturation=\"0\" colorizeStrength=\"100\"/>\n");
		out.write("\t\t\t\t<rasterresampler maxOversampling=\"2\"/>\n");
		
		out.write("\t\t\t</pipe>\n");
		out.write("\t\t\t<blendMode>0</blendMode>\n");
		
	}
	
	private void writeGeometryLayer(BufferedWriter out, VectorLayer layer) throws IOException {
	
		QGisRenderer qRenderer = layer.getRenderer();
		
		if(qRenderer.getRenderingType().equals(QGisConstants.renderingType.categorizedSymbol)){

			//TODO no classes for categorized renderer created so far...
			
		} else if(qRenderer.getRenderingType().equals(QGisConstants.renderingType.graduatedSymbol)){
			
			GraduatedSymbolRenderer renderer = (GraduatedSymbolRenderer)qRenderer;
			
			out.write("\t\t\t<renderer-v2 attr=\"" + renderer.getRenderingAttribute() + "\" symbollevels=\"0\" type=\"" + renderer.getRenderingType().toString() + "\">\n");
			
			out.write("\t\t\t\t<ranges>\n");
			
			for(int i = 0; i < renderer.getRanges().length; i++){

			    Range currentRange = renderer.getRanges()[i];
				out.write("\t\t\t\t\t<range symbol=\"" + i);
				if (currentRange.isLowerBoundSet())
                    out.write("\" lower=\"" + currentRange.getLowerBound());
                if (currentRange.isUpperBoundSet())
                    out.write("\" upper=\"" + currentRange.getUpperBound());
                out.write("\" label=\"" + renderer.getRanges()[i].getLabel() + "\"/>\n");
				
			}
			
			out.write("\t\t\t\t</ranges>\n");
			
		} else if(qRenderer.getRenderingType().equals(QGisConstants.renderingType.RuleRenderer)) {

			RuleBasedRenderer renderer = (RuleBasedRenderer) qRenderer;

			out.write("\t\t\t<renderer-v2 attr=\"" + renderer.getRenderingAttribute() + "\" symbollevels=\"0\" type=\"" + renderer.getRenderingType().toString() + "\">\n");

			out.write("\t\t\t\t<rules>\n");

			for (Rule rule : renderer.getRules()) {

				out.write("\t\t\t\t\t<rule filter=\"" + rule.getFilter()
						+ "\" symbol=\"" + rule.getSymbol() + "\" label=\"" + rule.getLabel() + "\"/>\n");

			}

			out.write("\t\t\t\t</rules>\n");

		} else if(qRenderer.getRenderingType().equals(QGisConstants.renderingType.PolygonRenderer)){

			PolygonLayerRenderer renderer = (PolygonLayerRenderer)qRenderer;

			out.write("\t\t\t<renderer-v2 attr=\"" + renderer.getRenderingAttribute() + "\" symbollevels=\"0\" type=\"" + QGisConstants.renderingType.RuleRenderer.toString() + "\">\n");

			out.write("\t\t\t\t<rules>\n");

			for(Rule rule : renderer.getRules()) {

				out.write("\t\t\t\t\t<rule filter=\"" + rule.getFilter()
						+ "\" symbol=\"" + rule.getSymbol() + "\" label=\"" + rule.getLabel() + "\"/>\n");

			}

			out.write("\t\t\t\t</rules>\n");


			} else if(qRenderer.getRenderingType().equals(QGisConstants.renderingType.singleSymbol)){
			
			out.write("\t\t\t<renderer-v2 symbollevels=\"0\" type=\"" + qRenderer.getRenderingType().toString() + "\">\n");
			
		}
		
		out.write("\t\t\t\t<symbols>\n");
		
		for(int i = 0; i < qRenderer.getSymbolLayers().size(); i++){
			
			if(layer.getGeometryType().equals(QGisConstants.geometryType.Line)){
				
				writeLineLayer(out, layer, i);
				
			} else if(layer.getGeometryType().equals(QGisConstants.geometryType.Point)){
				
				writePointLayer(out, layer, i);

			} else if(layer.getGeometryType().equals(QGisConstants.geometryType.Polygon)){

				writePolygonLayer(out, layer, i);

			} else{
				throw new RuntimeException("Unknown geometry type! Cannot write symbol layer!");
			}
			
		}
		
		out.write("\t\t\t\t</symbols>\n");
		out.write("\t\t\t\t<rotation/>\n");
		out.write("\t\t\t\t<sizescale scalemethod=\"area\"/>\n");
		
		out.write("\t\t\t</renderer-v2>\n");
		
	}

	private void writePointLayer(BufferedWriter out, QGisLayer layer, int idx) throws IOException {

		QGisPointSymbolLayer psl = (QGisPointSymbolLayer)layer.getRenderer().getSymbolLayers().get(idx);
		
		String color = Integer.toString(psl.getColor().getRed()) + "," 
				+ Integer.toString(psl.getColor().getGreen()) + ","
				+ Integer.toString(psl.getColor().getBlue()) + ","
				+ Integer.toString(psl.getColor().getAlpha());
		
		String colorBorder = Integer.toString(psl.getColorBorder().getRed()) + ","
				+ Integer.toString(psl.getColorBorder().getGreen()) + ","
				+ Integer.toString(psl.getColorBorder().getBlue()) + ","
				+ Integer.toString(psl.getColorBorder().getAlpha());
		
		String offset = Double.toString(psl.getOffset()[0]) + "," + Double.toString(psl.getOffset()[1]);
		String offsetMapUnitScale = Double.toString(psl.getOffsetMapUnitScale()[0]) + "," +
				Double.toString(psl.getOffsetMapUnitScale()[1]); 
		
		out.write("\t\t\t\t\t<symbol alpha=\"" + psl.getLayerTransparency() + "\" type=\"" + psl.getSymbolType().toString().toLowerCase() + "\" name=\"" + idx + "\">\n");
		
		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"" + layer.getLayerClass().toString() + "\" locked=\"0\">\n");
		
		out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"" + color + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"color_border\" v=\"" + colorBorder + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"" + psl.getPointLayerSymbol().toString() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"" + offset + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"" + offsetMapUnitScale + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"" + psl.getSizeUnits().toString() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"" + psl.getPenStyle().toString() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"" + psl.getSizeUnits().toString() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"" + Double.toString(psl.getSize()) + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"" + psl.getSizeUnits().toString() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
		
		out.write("\t\t\t\t\t\t</layer>\n");
		
		out.write("\t\t\t\t\t</symbol>\n");
			
	}

	private void writeLineLayer(BufferedWriter out, QGisLayer layer, int idx) throws IOException {
		
		QGisLineSymbolLayer lsl = (QGisLineSymbolLayer)layer.getRenderer().getSymbolLayers().get(0);
		
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
		out.write("\t\t\t\t\t\t\t<prop k=\"customdash_unit\" v=\"" + lsl.getSizeUnits().toString() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"draw_inside_polygon\" v=\"0\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"joinstyle\" v=\"bevel\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"0\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"0,0\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"" + lsl.getSizeUnits().toString() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"penstyle\" v=\"" + lsl.getPenStyle() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"use_custom_dash\" v=\"0\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"width\" v=\"" + lsl.getWidth() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"width_map_unit_scale\" v=\"0,0\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"width_unit\" v=\"" + lsl.getSizeUnits().toString() + "\"/>\n");

		out.write("\t\t\t\t\t\t</layer>\n");
		
		out.write("\t\t\t\t\t</symbol>\n");
			
	}

	private void writePolygonLayer(BufferedWriter out, QGisLayer layer, int idx) throws IOException {

		QGisPolygonSymbolLayer psl = (QGisPolygonSymbolLayer)layer.getRenderer().getSymbolLayers().get(idx);

		String color = Integer.toString(psl.getColor().getRed()) + ","
				+ Integer.toString(psl.getColor().getGreen()) + ","
				+ Integer.toString(psl.getColor().getBlue()) + ","
				+ Integer.toString(psl.getColor().getAlpha());

		String outlineColor = Integer.toString(psl.getOutlineColor().getRed()) + ","
				+ Integer.toString(psl.getOutlineColor().getGreen()) + ","
				+ Integer.toString(psl.getOutlineColor().getBlue()) + ","
				+ Integer.toString(psl.getOutlineColor().getAlpha());

		String offset = Double.toString(psl.getOffset()[0]) + "," + Double.toString(psl.getOffset()[1]);
		String offsetMapUnitScale = Double.toString(psl.getOffsetMapUnitScale()[0]) + "," +
				Double.toString(psl.getOffsetMapUnitScale()[1]);

		out.write("\t\t\t\t\t<symbol alpha=\"" + psl.getLayerTransparency() + "\" type=\"" + psl.getSymbolType().toString().toLowerCase() + "\" name=\"" + idx + "\">\n");

		out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"" + layer.getLayerClass().toString() + "\" locked=\"0\">\n");

		out.write("\t\t\t\t\t\t\t<prop k=\"border_width_map_unit_scale\" v=\"0,0,0,0,0,0\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"" + color + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"joinstyle\" v=\"bevel\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"" + offset + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"" + offsetMapUnitScale + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"" + psl.getSizeUnits().toString() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"outline_color\" v=\"" + outlineColor + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"" + psl.getPenStyle().toString() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"" + psl.getOutlineWidth() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"" + psl.getSizeUnits().toString() + "\"/>\n");
		out.write("\t\t\t\t\t\t\t<prop k=\"style\" v=\"" + psl.getPenStyle().toString() + "\"/>\n");

		out.write("\t\t\t\t\t\t</layer>\n");

		out.write("\t\t\t\t\t</symbol>\n");

	}

	public void writeProperties(BufferedWriter out) throws IOException{
		
		out.write("\t<properties>\n");
		
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
