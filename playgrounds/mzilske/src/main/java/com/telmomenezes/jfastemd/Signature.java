package com.telmomenezes.jfastemd;


/**
 * @author Telmo Menezes (telmo@telmomenezes.com)
 *
 */
public class Signature {
    private int numberOfFeatures;
    private Feature[] features;
    private double[] weights;
    
    public int getNumberOfFeatures() {
        return numberOfFeatures;
    }
    
    public void setNumberOfFeatures(int numberOfFeatures) {
        this.numberOfFeatures = numberOfFeatures;
    }

    public Feature[] getFeatures() {
        return features;
    }

    public void setFeatures(Feature[] features) {
        this.features = features;
    }

    public double[] getWeights() {
        return weights;
    }

    public void setWeights(double[] weights) {
        this.weights = weights;
    }
}