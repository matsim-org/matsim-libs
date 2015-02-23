package playground.dhosse.qgis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

import playground.dhosse.qgis.layerTemplates.GraduatedSymbolNoiseRenderer;
import playground.dhosse.qgis.layerTemplates.GraduatedSymbolNoiseRenderer.ColorRamp;
import playground.dhosse.qgis.layerTemplates.GraduatedSymbolNoiseRenderer.Range;

public class QGisFileWriter {
	
	private final QGisWriter writer;
	
	public QGisFileWriter(QGisWriter writer){
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
		
		for(QGisLayer layer : this.writer.getLayers().values()){
			
			writeLayerTreeLayer(out, layer);
			
		}
		
		out.write("\t</layer-tree-group>\n");
		
	}
	
	private void writeLayerTreeLayer(BufferedWriter out, QGisLayer layer) throws IOException{
		
		out.write("\t\t<layer-tree-layer expanded=\"1\" checked=\"Qt::Checked\" id=\"" + layer.getId().toString() + "\" name=\"" + layer.getName() + "\">\n");
		out.write("\t\t\t<customproperties/>\n");
		out.write("\t\t</layer-tree-layer>\n");
		
	}
	
	public void writeRelations(BufferedWriter out) throws IOException{
		
		out.write("\t<relations/>\n");
		
	}
	
	public void writeMapCanvas(BufferedWriter out) throws IOException{
		
		out.write("\t<mapcanvas>\n");
		
		out.write("\t\t<units>meters</units>\n");
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
		
		for(QGisLayer layer : this.writer.layers.values()){
			
			writeItem(out, layer);
			
		}
		
		out.write("\t\t</custom-order>\n");
		
		out.write("\t</layer-tree-canvas>\n");
		
	}
	
	private void writeItem(BufferedWriter out, QGisLayer layer) throws IOException{
		
		out.write("\t\t\t<item>" + layer.getId().toString() + "</item>\n");
		
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
		
		out.write("\t\t\t\t<legendlayerfile isInOverview=\"0\" layerid=\"" + layer.getId().toString()  + "\" visible=\"1\"/>\n");
		
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
		
		if(layer.getGeometryType().equals(QGisConstants.geometryType.No_geometry)){
			
			out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" geometry=\"" + layer.getGeometryType().toString().replace("_", " ") + "\" type=\"vector\" hasScaleBasedVisibilityFlag=\"0\">\n");
			
		} else{
			
			out.write("\t\t<maplayer minimumScale=\"0\" maximumScale=\"1e+08\" simplifyDrawingHints=\"1\" minLabelScale=\"0\" maxLabelScale=\"1e+08\" simplifyDrawingTol=\"1\" geometry=\"" + layer.getGeometryType().toString() + "\" simplifyMaxScale=\"1\" type=\"vector\" hasScaleBasedVisibilityFlag=\"0\" simplifyLocal=\"1\" scaleBasedLabelVisibilityFlag=\"0\">\n");
		
		}

		out.write("\t\t\t<id>" + layer.getId().toString() + "</id>\n");
		
		String base = layer.getPath();
		String relP = new File(writer.getWorkingDir()).toURI().relativize(new File(base).toURI()).toString();
		
		if(layer.getInputType().equals(QGisConstants.inputType.csv) && !layer.getGeometryType().equals(QGisConstants.geometryType.No_geometry)){

//			layer.getAttributes().
			
			out.write("\t\t\t<datasource>file:/" + relP + "?type=csv&amp;delimiter=;&amp;quote='&amp;escape='&amp;skipEmptyField=Yes&amp;xField=xCoord&amp;yField=yCoord&amp;spatialIndex=no&amp;subsetIndex=no&amp;watchFile=no</datasource>\n");
			
		} else{
			
			relP.replace("file:/", "");
			
			out.write("\t\t\t<datasource>" + relP + "</datasource>\n");
			
		}
		
		out.write("\t\t\t<title></title>\n");
		out.write("\t\t\t<abstract></abstract>\n");
		out.write("\t\t\t<keywordList>\n");
		
		out.write("\t\t\t\t<value></value>\n");
		
		out.write("\t\t\t</keywordList>\n");
		out.write("\t\t\t<layername>" + layer.getName() + "</layername>\n");
		out.write("\t\t\t<srs>\n");
		
		out.write("\t\t\t\t<spatialrefsys>\n");

		out.write("\t\t\t\t\t<proj4>" + this.writer.getSRS().getProj4() + "</proj4>\n");
		out.write("\t\t\t\t\t<srsid>" + this.writer.getSRS().getSrsid() + "</srsid>\n");
		out.write("\t\t\t\t\t<srid>" + this.writer.getSRS().getSrid() + "</srid>\n");
		out.write("\t\t\t\t\t<authid>" + this.writer.getSRS().getAuthid() + "</authid>\n");
		out.write("\t\t\t\t\t<description>" + this.writer.getSRS().getDescription() + "</description>\n");
		out.write("\t\t\t\t\t<projectionacronym>" + this.writer.getSRS().getProjectionacronym() + "</projectionacronym>\n");
		out.write("\t\t\t\t\t<ellipsoidacronym>" + this.writer.getSRS().getEllipsoidacronym() + "</ellipsoidacronym>\n");
		out.write("\t\t\t\t\t<geographicflag>true</geographicflag>\n");
		
		out.write("\t\t\t\t</spatialrefsys>\n");
		
		out.write("\t\t\t</srs>\n");
		
		if(layer.getInputType().equals(QGisConstants.inputType.csv)&&layer.getRenderer() != null){
			
			out.write("\t\t\t<provider encoding=\"System\">delimitedtext</provider>\n");
			
		} else{
			
			out.write("\t\t\t<provider encoding=\"System\">ogr</provider>\n");
			
		}
		
		out.write("\t\t\t<previewExpression></previewExpression>\n");
		
		if(layer.getVectorJoins().isEmpty()){
			out.write("\t\t\t<vectorjoins/>\n");
		} else{
			
			out.write("\t\t\t<vectorjoins>\n");
			
			for(VectorJoin vj : layer.getVectorJoins()){
				
				out.write("\t\t\t\t<join joinFieldName=\"" + vj.getJoinFieldName() +"\" targetFieldName=\"" + vj.getTargetFieldName() + "\""
						+ " memoryCache=\"1\" joinLayerId=\"" + vj.getJoinLayerId().toString() + "\"/>\n");
				
			}
			
			out.write("\t\t\t</vectorjoins>\n");
			
		}
		
		if(layer.getRenderer() != null){
			
			writeGeometryLayer(out,layer);
			
		}
		
		if(layer.getInputType().equals(QGisConstants.inputType.shp)||layer.getGeometryType().equals(QGisConstants.geometryType.No_geometry)){
			
			out.write("\t\t\t<customproperties/>\n");
			
		} else if(layer.getInputType().equals(QGisConstants.inputType.csv)){
			
			out.write("\t\t\t<customproperties>\n");
			
			out.write("\t\t\t\t<property key=\"labeling\" value=\"pal\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/addDirectionSymbol\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/angleOffset\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/blendMode\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferBlendMode\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferColorA\" value=\"255\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferColorB\" value=\"255\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferColorG\" value=\"255\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferColorR\" value=\"255\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferDraw\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferJoinStyle\" value=\"64\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferNoFill\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferSize\" value=\"1\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferSizeInMapUnits\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferSizeMapUnitMaxScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferSizeMapUnitMinScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/bufferTransp\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/centroidInside\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/centroidWhole\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/decimals\" value=\"3\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/displayAll\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/dist\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/distInMapUnits\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/distMapUnitMaxScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/distMapUnitMinScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/enabled\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fieldName\" value=\"\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontBold\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontCapitals\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontFamily\" value=\"MS Shell Dlg 2\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontItalic\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontLetterSpacing\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontLimitPixelSize\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontMaxPixelSize\" value=\"10000\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontMinPixelSize\" value=\"3\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontSize\" value=\"8.25\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontSizeInMapUnits\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontSizeMapUnitMaxScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontSizeMapUnitMinScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontStrikeout\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontUnderline\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontWeight\" value=\"50\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/fontWordSpacing\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/formatNumbers\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/isExpression\" value=\"true\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/labelOffsetInMapUnits\" value=\"true\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/labelOffsetMapUnitMaxScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/labelOffsetMapUnitMinScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/labelPerPart\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/leftDirectionSymbol\" value=\"&lt;\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/limitNumLabels\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/maxCurvedCharAngleIn\" value=\"20\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/maxCurvedCharAngleOut\" value=\"-20\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/maxNumLabels\" value=\"2000\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/mergeLines\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/minFeatureSize\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/multilineAlign\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/multilineHeight\" value=\"1\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/namedStyle\" value=\"Normal\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/obstacle\" value=\"true\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/placeDirectionSymbol\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/placement\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/placementFlags\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/plussign\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/preserveRotation\" value=\"true\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/previewBkgrdColor\" value=\"#ffffff\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/priority\" value=\"5\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/quadOffset\" value=\"4\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/repeatDistance\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/repeatDistanceMapUnitMaxScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/repeatDistanceMapUnitMinScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/repeatDistanceUnit\" value=\"1\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/reverseDirectionSymbol\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/rightDirectionSymbol\" value=\">\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/scaleMax\" value=\"10000000\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/scaleMin\" value=\"1\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/scaleVisibility\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowBlendMode\" value=\"6\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowColorB\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowColorG\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowColorR\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowDraw\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowOffsetAngle\" value=\"135\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowOffsetDist\" value=\"1\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowOffsetGlobal\" value=\"true\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowOffsetMapUnitMaxScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowOffsetMapUnitMinScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowOffsetUnits\" value=\"1\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowRadius\" value=\"1.5\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowRadiusAlphaOnly\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowRadiusMapUnitMaxScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowRadiusMapUnitMinScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowRadiusUnits\" value=\"1\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowScale\" value=\"100\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowTransparency\" value=\"30\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shadowUnder\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeBlendMode\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeBorderColorA\" value=\"255\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeBorderColorB\" value=\"128\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeBorderColorG\" value=\"128\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeBorderColorR\" value=\"128\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeBorderWidth\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeBorderWidthMapUnitMaxScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeBorderWidthMapUnitMinScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeBorderWidthUnits\" value=\"1\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeDraw\" value=\"false\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeFillColorA\" value=\"255\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeFillColorB\" value=\"255\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeFillColorG\" value=\"255\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeFillColorR\" value=\"255\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeJoinStyle\" value=\"64\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeOffsetMapUnitMaxScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeOffsetMapUnitMinScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeOffsetUnits\" value=\"1\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeOffsetX\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeOffsetY\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeRadiiMapUnitMaxScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeRadiiMapUnitMinScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeRadiiUnits\" value=\"1\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeRadiiX\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeRadiiY\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeRotation\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeRotationType\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeSVGFile\" value=\"\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeSizeMapUnitMaxScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeSizeMapUnitMinScale\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeSizeType\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeSizeUnits\" value=\"1\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeSizeX\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeSizeY\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeTransparency\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/shapeType\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/textColorA\" value=\"255\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/textColorB\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/textColorG\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/textColorR\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/textTransp\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/upsidedownLabels\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/wrapChar\" value=\"\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/xOffset\" value=\"0\"/>\n");
            out.write("\t\t\t\t<property key=\"labeling/yOffset\" value=\"0\"/>\n");
			
			out.write("\t\t\t</customproperties>\n");
			
		}
		
		if(layer.getRenderer() != null){
			
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
			
		}
		
		out.write("\t\t\t<edittypes>\n");
		
		for(String attribute : layer.getAttributes()){
			
			out.write("\t\t\t\t<edittype labelontop=\"0\" editable=\"0\" widgetv2type=\"TextEdit\" name=\"" + attribute + "\">\n");
			out.write("\t\t\t\t\t<widgetv2config IsMultiline=\"0\" fieldEditable=\"1\" UseHtml=\"0\" labelOnTop=\"0\"/>\n");
			out.write("\t\t\t\t</edittype>\n");
			
		}
		
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
	
	private void writeGeometryLayer(BufferedWriter out, QGisLayer layer) throws IOException {
	
		if(layer.getRenderer().getRenderingType().equals(QGisConstants.renderingType.categorizedSymbol)){
			
		} else if(layer.getRenderer().getRenderingType().equals(QGisConstants.renderingType.graduatedSymbol)){
			
			GraduatedSymbolNoiseRenderer renderer = (GraduatedSymbolNoiseRenderer) layer.getRenderer();
			
			out.write("\t\t\t<renderer-v2 attr=\"" + renderer.getRenderingAttribute() + "\" symbollevels=\"0\" type=\"" + renderer.getRenderingType().toString() + "\">\n");
			
			out.write("\t\t\t\t<ranges>\n");
			
			for(int i = 0; i < renderer.getRanges().length; i++){
				
				out.write("\t\t\t\t\t<range symbol=\"" + i + "\" lower=\"" + renderer.getRanges()[i].getLowerBound() + "\" upper=\"" + renderer.getRanges()[i].getUpperBound() + "\" label=\"" + renderer.getRanges()[i].getLabel() + "\"/>\n");
				
			}
			
			out.write("\t\t\t\t</ranges>\n");
			
			ColorRamp cr = renderer.getColorRamp();
			String stops = "";
			
			for(String s : cr.getStops()){
				stops += s;
			}
			
			out.write("\t\t\t\t<colorramp type=\"" + cr.getType() + "\" name=\"" + cr.getName() + "\">\n");
			
			out.write("\t\t\t\t\t<prop k=\"color1\" v=\"" + cr.getColor1().getRed() + "," + cr.getColor1().getGreen() + "," + cr.getColor1().getBlue() + "," + cr.getColor1().getAlpha() + "\"/>\n");
			out.write("\t\t\t\t\t<prop k=\"color2\" v=\"" + cr.getColor2().getRed() + "," + cr.getColor2().getGreen() + "," + cr.getColor2().getBlue() + "," + cr.getColor2().getAlpha() + "\"/>\n");
			out.write("\t\t\t\t\t<prop k=\"discrete\" v=\"" + cr.getDiscrete() + "\"/>\n");
			out.write("\t\t\t\t\t<prop k=\"stops\" v=\"" + stops + "\"/>\n");
			
			out.write("\t\t\t\t</colorramp>\n");
			
			out.write("\t\t\t\t<invertedcolorramp value=\"" + cr.isInverted() + "\"/>\n");
			out.write("\t\t\t\t<mode name=\"" + cr.getMode() + "\"/>\n");
			
			out.write("\t\t\t\t<source-symbol>\n");
			
			QGisPointSymbolLayer source = renderer.getSourceSymbol();
			
			out.write("\t\t\t\t\t<symbol alpha=\"1\" type=\"" + source.getSymbolType().toString().toLowerCase() + "\" name=\"" + source.getId() + "\">\n");
			
			out.write("\t\t\t\t\t\t<layer pass=\"0\" class=\"" + layer.getLayerClass().toString() + "\" locked=\"0\">\n");
			
			out.write("\t\t\t\t\t\t\t<prop k=\"angle\" v=\"0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color\" v=\"" + source.getColor().getRed() + "," + source.getColor().getGreen() + "," + source.getColor().getBlue() + "," + source.getColor().getAlpha() + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"color_border\" v=\"" + source.getColorBorder().getRed() + "," + source.getColorBorder().getGreen() + "," + source.getColorBorder().getBlue() + "," + source.getColorBorder().getAlpha() + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"horizontal_anchor_point\" v=\"1\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"name\" v=\"" + source.getPointLayerSymbol().toString() + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset\" v=\"" + source.getOffset()[0] + "," + source.getOffset()[1] + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset_map_unit_scale\" v=\"" + source.getOffsetMapUnitScale()[0] + "," + source.getOffsetMapUnitScale()[1] + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"offset_unit\" v=\"" + source.getSizeUnits().toString() + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"outline_style\" v=\"" + source.getPenStyle().toString() + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"outline_width\" v=\"0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_map_unit_scale\" v=\"0,0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"outline_width_unit\" v=\"" + source.getSizeUnits().toString() + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"scale_method\" v=\"area\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"size\" v=\"" + Double.toString(source.getSize()) + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"size_map_unit_scale\" v=\"0,0\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"size_unit\" v=\"" + source.getSizeUnits().toString() + "\"/>\n");
			out.write("\t\t\t\t\t\t\t<prop k=\"vertical_anchor_point\" v=\"1\"/>\n");
			
			out.write("\t\t\t\t\t\t</layer>\n");
			
			out.write("\t\t\t\t\t</symbol>\n");
			
			out.write("\t\t\t\t</source-symbol>\n");
			
		} else if(layer.getRenderer().getRenderingType().equals(QGisConstants.renderingType.RuleRenderer)){
			
		} else if(layer.getRenderer().getRenderingType().equals(QGisConstants.renderingType.singleSymbol)){
			
			out.write("\t\t\t<renderer-v2 symbollevels=\"0\" type=\"" + layer.getRenderer().getRenderingType().toString() + "\">\n");
			
		}
		
		out.write("\t\t\t\t<symbols>\n");
		
		for(int i = 0; i < layer.getRenderer().getSymbolLayers().size(); i++){
			
			if(layer.getGeometryType().equals(QGisConstants.geometryType.Line)){
				
				writeLineLayer(out, layer, i);
				
			} else if(layer.getGeometryType().equals(QGisConstants.geometryType.Point)){
				
				writePointLayer(out, layer, i);
				
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
