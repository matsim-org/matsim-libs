package playground.dhosse.prt;

import org.apache.commons.configuration.*;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;


public class PrtConfig
{
    private static final String PRT_PARAMETER_SET = "prt";

    
    private static final String OUTPUT_DIRECTORY = "outputDir";
    private static final String VEHICLE_CAPACITY = "vehicleCapacity";
    private static final String FIXED_COST = "fixedCost";
    private static final String VAR_COST_D = "varCostD";

    private String outputDir;
    private int vehicleMaximumCapacity = 1;
    private double fixedCost = 0.0;
    private double varCostD = 0.0;


    public PrtConfig(TaxiConfigGroup taxiCfg)
    {
        Configuration config = new MapConfiguration(
                taxiCfg.getParameterSets(PRT_PARAMETER_SET).iterator().next().getParams());
        outputDir = config.getString(OUTPUT_DIRECTORY);
        vehicleMaximumCapacity = config.getInt(VEHICLE_CAPACITY);
        fixedCost = config.getDouble(FIXED_COST);
        varCostD = config.getDouble(VAR_COST_D);
    }


    public int getVehicleCapacity()
    {
        return this.vehicleMaximumCapacity;
    }


    public double getFixedCost()
    {
        return this.fixedCost;
    }


    public double getVariableCostsD()
    {
        return this.varCostD;
    }
    
    
    public String getPrtOutputDirectory(){
        return this.outputDir;
    }
}
