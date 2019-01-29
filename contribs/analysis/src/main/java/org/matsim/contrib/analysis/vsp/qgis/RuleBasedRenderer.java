package org.matsim.contrib.analysis.vsp.qgis;

import org.apache.log4j.Logger;
import org.matsim.contrib.analysis.vsp.qgis.utils.AccessibilityRuleWithFilterCreator;
import org.matsim.contrib.analysis.vsp.qgis.utils.ColorRangeUtils;

import java.awt.*;

/**
 *
 * @author gthunig on 17.05.2017
 *
 */
public class RuleBasedRenderer extends QGisRenderer {
    public static final Logger log = Logger.getLogger(RuleBasedRenderer.class);

    private String nameOfAccessibilityLayer;
    private double lowerBound;
    private double upperBound;

    private String nameOfDensityLayer;
    private double minimumDensity;

    private int range;
    private int symbolSize;
    private ColorRangeUtils.ColorRange colorRange;

    private Rule[] rules;

    private String renderingAttribute;
    private boolean useHeader;
    private String fileHeader;

    public RuleBasedRenderer(VectorLayer layer, double lowerBound, double upperBound, int range, int symbolSize,
                             String nameOfAccessibilityLayer, String nameOfDensityLayer, double minimumDensity) {
        this(layer, lowerBound, upperBound, range, symbolSize, ColorRangeUtils.ColorRange.RED_TO_GREEN,
                nameOfAccessibilityLayer, nameOfDensityLayer, minimumDensity);
    }

    public RuleBasedRenderer(VectorLayer layer, double lowerBound, double upperBound, int range, int symbolSize,
                             ColorRangeUtils.ColorRange colorRange,
                             String nameOfAccessibilityLayer, String nameOfDensityLayer, double minimumDensity) {
        super(QGisConstants.renderingType.RuleRenderer, layer);
        this.fileHeader = layer.getHeader();
        this.useHeader = fileHeader != null;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.range = range;
        this.symbolSize = symbolSize;
        this.colorRange = colorRange;
        this.nameOfAccessibilityLayer = nameOfAccessibilityLayer;
        this.nameOfDensityLayer = nameOfDensityLayer;
        this.minimumDensity = minimumDensity;
        init();
    }

    public Rule[] getRules() {
        return rules;
    }

    public String getRenderingAttribute(){

        return this.renderingAttribute;

    }

    public void setRenderingAttribute(String attr){

        if(this.useHeader){

            if(this.fileHeader.contains(attr)){

                this.renderingAttribute = attr;

            } else{

                throw new RuntimeException("Rendering attribute " + attr + " does not exist in header!");

            }

        } else{

            throw new RuntimeException("The input file for this renderer has no header. Use method \"setRenderingAttribute(int columnIndex)\" instead!");

        }

    }

    public void setRenderingAttribute(int columnIndex){

        if(!this.useHeader){

            this.renderingAttribute = "field_" + Integer.toString(columnIndex);

        } else{

            throw new RuntimeException("The input file for this renderer has a header. Use method \"setRenderingAttribute(String attr)\" instead!");

        }

    }

    @Override
    public void init() {
        this.rules = new Rule[range];
        createRules();

        createQGisPointSymbolLayers();
    }

    private void createQGisPointSymbolLayers() {

        double colorRangeStep = 1.0 / (double)(range-1);
        double sizeMapUnitScale[] = {0,0};

        for (int i = 0; i < range; i++) {
            QGisPointSymbolLayer currentLevel = new QGisPointSymbolLayer();
            currentLevel.setId(i);
            currentLevel.setColor(ColorRangeUtils.getColor(colorRange, colorRangeStep*i));
            currentLevel.setColorBorder(new Color(0,0,0,255));
            currentLevel.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
            currentLevel.setSize(symbolSize);
            currentLevel.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
            currentLevel.setSizeMapUnitScale(sizeMapUnitScale);
            currentLevel.setPenStyle(QGisConstants.penstyle.no);
            currentLevel.setLayerTransparency(1);
            this.addSymbolLayer(currentLevel);
        }
    }

    private void createRules() {
        createFirstRule();
        createMidRules();
        createLastRule();
    }

    private void createFirstRule() {
        rules[0] = AccessibilityRuleWithFilterCreator.createRuleWithoutLowerBound(nameOfAccessibilityLayer,
                lowerBound, nameOfDensityLayer, minimumDensity, 0, String.valueOf(lowerBound));
    }

    private void createMidRules() {
        for (int i = 1; i < range -1; i++) {
            createMidRule(i);
        }
    }

    private void createMidRule(int i) {
        double lowerBound = calcLowerBound(i);
        double upperBound = calcUpperBound(i);
        rules[i] = AccessibilityRuleWithFilterCreator.createRule(nameOfAccessibilityLayer, lowerBound, upperBound,
                nameOfDensityLayer, minimumDensity, i,
                String.valueOf(lowerBound) + " - " + String.valueOf(upperBound));
    }

    private void createLastRule() {
        double lowerBound = calcLowerBound(range - 1);
        rules[range -1] = AccessibilityRuleWithFilterCreator.createRuleWithoutUpperBound(nameOfAccessibilityLayer,
                lowerBound, nameOfDensityLayer, minimumDensity,
                range -1, "> " + String.valueOf(lowerBound));
    }

    private double calcLowerBound(int i) {
        return lowerBound + ((upperBound-lowerBound)/(range-2)) * (i - 1);
    }

    private double calcUpperBound(int i) {
        return calcLowerBound(i + 1);
    }



}
