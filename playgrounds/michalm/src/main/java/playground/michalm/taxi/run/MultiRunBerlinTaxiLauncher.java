package playground.michalm.taxi.run;

import java.io.File;
import java.util.Map;

import playground.michalm.util.ParameterFileReader;


public class MultiRunBerlinTaxiLauncher
{
    public static void main(String... args)
    {
        int runs = 1;
        String paramFile = args[0] + "/params.in";
        String specificParamFile = args[0] + "/" + args[1] + "/params.in";

        Map<String, String> specificParams = ParameterFileReader
                .readParametersToMap(specificParamFile);
        String outputDir = new File(specificParamFile).getParent() + '/';
        specificParams.put("outputDir", outputDir);

        Map<String, String> generalParams = ParameterFileReader.readParametersToMap(paramFile);
        generalParams.putAll(specificParams);//side effect: overriding params with the specific ones
        TaxiLauncherParams params = new TaxiLauncherParams(generalParams);

        MultiRunTaxiLauncher.run(runs, params);
//        MultiRunTaxiLauncher.runAll(runs, params, EnumSet.of(//
//                RULE_TW_FF, //
//                RULE_DSE_FF //
//        ));
    }
}
