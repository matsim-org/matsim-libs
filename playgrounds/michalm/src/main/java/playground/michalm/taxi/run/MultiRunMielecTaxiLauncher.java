package playground.michalm.taxi.run;

import static playground.michalm.taxi.run.AlgorithmConfigs.*;

import java.io.File;
import java.util.Map;

import playground.michalm.util.ParameterFileReader;


public class MultiRunMielecTaxiLauncher
{
    public static void main(String... args)
    {
        int runs = 20;
        String paramFile = "d:/PP-rad/mielec/2014_02/params.in";
        String specificParamFile = args[0];

        Map<String, String> specificParams = ParameterFileReader
                .readParametersToMap(specificParamFile);
        String outputDir = new File(specificParamFile).getParent() + '/';
        specificParams.put("outputDir", outputDir);

        Map<String, String> generalParams = ParameterFileReader.readParametersToMap(paramFile);
        generalParams.putAll(specificParams);//side effect: overriding params with the specific ones
        TaxiLauncherParams params = new TaxiLauncherParams(generalParams);

        MultiRunTaxiLauncher.runAll(runs, params, //
                RULE_TW_xx,//
                RULE_TP_xx,//
                RULE_DSE_xx,//
                ZONE_TW_xx,//
                FIFO_RES_TW_xx,//
                ASSIGN_TW_xx,//
                ASSIGN_TP_xx,//
                ASSIGN_DSE_xx//
        );
    }
}
