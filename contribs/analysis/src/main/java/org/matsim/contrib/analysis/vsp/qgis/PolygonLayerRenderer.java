package org.matsim.contrib.analysis.vsp.qgis;

import org.apache.log4j.Logger;
import org.matsim.contrib.analysis.vsp.qgis.utils.AccessibilityRuleWithFilterCreator;
import org.matsim.contrib.analysis.vsp.qgis.utils.ColorRangeUtils;

import java.awt.*;

public class PolygonLayerRenderer extends QGisRenderer {
    public static final Logger log = Logger.getLogger(PolygonLayerRenderer.class);

    private String nameOfAccessibilityLayer;
    private double lowerBound;
    private double upperBound;

    private String nameOfDensityLayer;
    private double minimumDensity;

    private int range;
    private ColorRangeUtils.ColorRange colorRange;

    private Rule[] rules;

    private String renderingAttribute;

    public PolygonLayerRenderer(VectorLayer layer, double lowerBound, double upperBound, int range,
                             String nameOfAccessibilityLayer, String nameOfDensityLayer, double minimumDensity) {
        this(layer, lowerBound, upperBound, range, ColorRangeUtils.ColorRange.RED_TO_GREEN,
                nameOfAccessibilityLayer, nameOfDensityLayer, minimumDensity);
    }

    public PolygonLayerRenderer(VectorLayer layer, double lowerBound, double upperBound, int range,
                             ColorRangeUtils.ColorRange colorRange,
                             String nameOfAccessibilityLayer, String nameOfDensityLayer, double minimumDensity) {
        super(QGisConstants.renderingType.PolygonRenderer, layer);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.range = range;
        this.colorRange = colorRange;
        this.nameOfAccessibilityLayer = nameOfAccessibilityLayer;
        this.nameOfDensityLayer = nameOfDensityLayer;
        this.minimumDensity = minimumDensity;
        init();
    }

    @Override
    public void init() {
        this.rules = new Rule[range];
        createRules();

        createQGisPolygonSymbolLayers();
    }

    private void createQGisPolygonSymbolLayers() {

        double colorRangeStep = 1.0 / (double)(range-1);

        for (int i = 0; i < range; i++) {
            QGisPolygonSymbolLayer currentLevel = new QGisPolygonSymbolLayer();
            currentLevel.setId(i);
            currentLevel.setColor(ColorRangeUtils.getColor(colorRange, colorRangeStep*i));
            currentLevel.setOutlineColor(new Color(0,0,0,255));
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

    public void setRenderingAttribute(String attr){

        renderingAttribute = attr;

    }

    String getRenderingAttribute(){

        return this.renderingAttribute;

    }

    Rule[] getRules() {
        return rules;
    }
}
