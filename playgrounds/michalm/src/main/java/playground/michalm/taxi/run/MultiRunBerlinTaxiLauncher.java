package playground.michalm.taxi.run;

import java.io.File;
import java.util.Map;

import playground.michalm.util.ParameterFileReader;


public class MultiRunBerlinTaxiLauncher
{
    public static void main(String... args)
    {
        int runs = 20;
        String paramFile = "d:/michalm/Berlin_2014_11/params.in";
        String specificParamFile = args[0];

        Map<String, String> specificParams = ParameterFileReader
                .readParametersToMap(specificParamFile);
        String outputDir = new File(specificParamFile).getParent() + '/';
        specificParams.put("outputDir", outputDir);

        Map<String, String> generalParams = ParameterFileReader.readParametersToMap(paramFile);
        generalParams.putAll(specificParams);//side effect: overriding params with the specific ones
        TaxiLauncherParams params = new TaxiLauncherParams(generalParams);

        MultiRunTaxiLauncher.runAll(runs, params);
    }
}
