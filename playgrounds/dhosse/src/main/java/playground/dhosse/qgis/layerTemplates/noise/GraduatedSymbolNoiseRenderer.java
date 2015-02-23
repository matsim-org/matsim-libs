package playground.dhosse.qgis.layerTemplates.noise;

import java.awt.Color;

import playground.dhosse.qgis.QGisConstants;
import playground.dhosse.qgis.QGisPointSymbolLayer;
import playground.dhosse.qgis.QGisRenderer;

public class GraduatedSymbolNoiseRenderer extends QGisRenderer {
	
	private String renderingAttribute;
	private Range[] ranges;
	private ColorRamp colorRamp = new ColorRamp();
	
	private QGisPointSymbolLayer source;

	public GraduatedSymbolNoiseRenderer() {

		super(QGisConstants.renderingType.graduatedSymbol);
		
		this.init();
		
	}
	
	private void init(){
		
		this.ranges = new Range[8];
		this.ranges[0] = new Range(0, 45, " 45");
		this.ranges[1] = new Range(45, 50, "45 - 50");
		this.ranges[2] = new Range(50, 55, "50 - 55");
		this.ranges[3] = new Range(55, 60, "55 - 60");
		this.ranges[4] = new Range(60, 65, "60 - 65");
		this.ranges[5] = new Range(65, 70, "65 - 70");
		this.ranges[6] = new Range(70, 75, "70 - 75");
		this.ranges[7] = new Range(75, 999, "> 75");

		double sizeMapUnitScale[] = {0,0};
		int size = 35;
		
		this.source = new QGisPointSymbolLayer();
		this.source.setId(0);
		this.source.setColor(new Color(37,157,85,255));
		this.source.setColorBorder(new Color(0,0,0,255));
		this.source.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		this.source.setSize(size);
		this.source.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		this.source.setSizeMapUnitScale(sizeMapUnitScale);
		this.source.setPenStyle(QGisConstants.penstyle.no);
		
		QGisPointSymbolLayer psl = new QGisPointSymbolLayer();
		psl.setId(0);
		psl.setColor(new Color(26,150,65,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(1);
		psl.setColor(new Color(137,203,97,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(2);
		psl.setColor(new Color(219,239,157,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(3);
		psl.setColor(new Color(254,222,154,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(4);
		psl.setColor(new Color(245,144,83,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(5);
		psl.setColor(new Color(215,25,28,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(6);
		psl.setColor(new Color(128,0,0,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(7);
		psl.setColor(new Color(73,0,0,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		this.addSymbolLayer(psl);
		
	}
	
	public Range[] getRanges(){
		return this.ranges;
	}
	
	public String getRenderingAttribute(){
		return this.renderingAttribute;
	}
	
	public void setRenderingAttribute(String attr){
		this.renderingAttribute = attr;
	}
	
	public ColorRamp getColorRamp() {
		return colorRamp;
	}

	public void setColorRamp(ColorRamp colorRamp) {
		this.colorRamp = colorRamp;
	}
	
	public QGisPointSymbolLayer getSourceSymbol(){
		return this.source;
	}

	public static class Range{
		
		private double lowerBound;
		private double upperBound;
		private String label;
		
		public Range(double lowerBound, double upperBound, String label){
			
			this.lowerBound = lowerBound;
			this.upperBound = upperBound;
			this.label = label;
			
		}

		public double getLowerBound() {
			return lowerBound;
		}

		public double getUpperBound() {
			return upperBound;
		}

		public String getLabel() {
			return label;
		}
		
	}
	
	public static class ColorRamp{
	
		private String type = "gradient";
		private String name = "[source]";
		private Color color1 = new Color(215,25,28,255);
		private Color color2 = new Color(26,150,65,255);
		private int discrete = 0;
		private int inverted = 1;
		private String mode = "equal";
		
		private String[] stops = {"0.25;253,174,97,255:","0.5;255,255,192,255:","0.75;166,217,106,255"};

		public String getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public Color getColor1() {
			return color1;
		}

		public Color getColor2() {
			return color2;
		}

		public int getDiscrete() {
			return discrete;
		}

		public String[] getStops() {
			return stops;
		}
		
		public int isInverted(){
			return this.inverted;
		}
		
		public String getMode(){
			return this.mode;
		}
		
	}

}