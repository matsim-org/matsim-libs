package playground.tnicolai.matsim4opus.config;

import org.matsim.core.config.Module;

public class AccessibilityParameterConfigModule extends Module{

	private static final long serialVersionUID = 2L;
	
	public static final String GROUP_NAME = "accessibilityParameter";
	
	private boolean useLogitScaleParameterFromMATSim;
	
	private boolean useCarParameterFromMATSim;
	
	private boolean useWalkParameterFromMATSim;
    
	private boolean useRawSumsWithoutLn;
    
	private double logitScaleParameter;
    
	private double betaCarTravelTime;
    
	private double betaCarTravelTimePower2;
    
	private double betaCarLnTravelTime;
    
	private double betaCarTravelDistance;
    
	private double betaCarTravelDistancePower2;
    
	private double betaCarLnTravelDistance;
    
	private double betaCarTravelCost;
    
	private double betaCarTravelCostPower2;
    
	private double betaCarLnTravelCost;
    
	private double betaWalkTravelTime;
    
	private double betaWalkTravelTimePower2;
    
	private double betaWalkLnTravelTime;
    
	private double betaWalkTravelDistance;
    
	private double betaWalkTravelDistancePower2;
    
	private double betaWalkLnTravelDistance;
    
	private double betaWalkTravelCost;
    
	private double betaWalkTravelCostPower2;
    
	private double betaWalkLnTravelCost;
	
	public AccessibilityParameterConfigModule(String name) {
		super(name);
	}

    public boolean isUseLogitScaleParameterFromMATSim() {
        return useLogitScaleParameterFromMATSim;
    }

    public void setUseLogitScaleParameterFromMATSim(boolean value) {
        this.useLogitScaleParameterFromMATSim = value;
    }

    public boolean isUseCarParameterFromMATSim() {
        return useCarParameterFromMATSim;
    }

    public void setUseCarParameterFromMATSim(boolean value) {
        this.useCarParameterFromMATSim = value;
    }

    public boolean isUseWalkParameterFromMATSim() {
        return useWalkParameterFromMATSim;
    }

    public void setUseWalkParameterFromMATSim(boolean value) {
        this.useWalkParameterFromMATSim = value;
    }

    public boolean isUseRawSumsWithoutLn() {
        return useRawSumsWithoutLn;
    }

    public void setUseRawSumsWithoutLn(boolean value) {
        this.useRawSumsWithoutLn = value;
    }

    public double getLogitScaleParameter() {
        return logitScaleParameter;
    }

    public void setLogitScaleParameter(double value) {
        this.logitScaleParameter = value;
    }

    public double getBetaCarTravelTime() {
        return betaCarTravelTime;
    }

    public void setBetaCarTravelTime(double value) {
        this.betaCarTravelTime = value;
    }

    public double getBetaCarTravelTimePower2() {
        return betaCarTravelTimePower2;
    }

    public void setBetaCarTravelTimePower2(double value) {
        this.betaCarTravelTimePower2 = value;
    }

    public double getBetaCarLnTravelTime() {
        return betaCarLnTravelTime;
    }

    public void setBetaCarLnTravelTime(double value) {
        this.betaCarLnTravelTime = value;
    }

    public double getBetaCarTravelDistance() {
        return betaCarTravelDistance;
    }

    public void setBetaCarTravelDistance(double value) {
        this.betaCarTravelDistance = value;
    }

    public double getBetaCarTravelDistancePower2() {
        return betaCarTravelDistancePower2;
    }

    public void setBetaCarTravelDistancePower2(double value) {
        this.betaCarTravelDistancePower2 = value;
    }

    public double getBetaCarLnTravelDistance() {
        return betaCarLnTravelDistance;
    }

    public void setBetaCarLnTravelDistance(double value) {
        this.betaCarLnTravelDistance = value;
    }

    public double getBetaCarTravelCost() {
        return betaCarTravelCost;
    }

    public void setBetaCarTravelCost(double value) {
        this.betaCarTravelCost = value;
    }

    public double getBetaCarTravelCostPower2() {
        return betaCarTravelCostPower2;
    }

    public void setBetaCarTravelCostPower2(double value) {
        this.betaCarTravelCostPower2 = value;
    }

    public double getBetaCarLnTravelCost() {
        return betaCarLnTravelCost;
    }

    public void setBetaCarLnTravelCost(double value) {
        this.betaCarLnTravelCost = value;
    }

    public double getBetaWalkTravelTime() {
        return betaWalkTravelTime;
    }

    public void setBetaWalkTravelTime(double value) {
        this.betaWalkTravelTime = value;
    }

    public double getBetaWalkTravelTimePower2() {
        return betaWalkTravelTimePower2;
    }

    public void setBetaWalkTravelTimePower2(double value) {
        this.betaWalkTravelTimePower2 = value;
    }

    public double getBetaWalkLnTravelTime() {
        return betaWalkLnTravelTime;
    }

    public void setBetaWalkLnTravelTime(double value) {
        this.betaWalkLnTravelTime = value;
    }

    public double getBetaWalkTravelDistance() {
        return betaWalkTravelDistance;
    }

    public void setBetaWalkTravelDistance(double value) {
        this.betaWalkTravelDistance = value;
    }

    public double getBetaWalkTravelDistancePower2() {
        return betaWalkTravelDistancePower2;
    }

    public void setBetaWalkTravelDistancePower2(double value) {
        this.betaWalkTravelDistancePower2 = value;
    }

    public double getBetaWalkLnTravelDistance() {
        return betaWalkLnTravelDistance;
    }

    public void setBetaWalkLnTravelDistance(double value) {
        this.betaWalkLnTravelDistance = value;
    }

    public double getBetaWalkTravelCost() {
        return betaWalkTravelCost;
    }

    public void setBetaWalkTravelCost(double value) {
        this.betaWalkTravelCost = value;
    }

    public double getBetaWalkTravelCostPower2() {
        return betaWalkTravelCostPower2;
    }

    public void setBetaWalkTravelCostPower2(double value) {
        this.betaWalkTravelCostPower2 = value;
    }

    public double getBetaWalkLnTravelCost() {
        return betaWalkLnTravelCost;
    }

    public void setBetaWalkLnTravelCost(double value) {
        this.betaWalkLnTravelCost = value;
    }

}
