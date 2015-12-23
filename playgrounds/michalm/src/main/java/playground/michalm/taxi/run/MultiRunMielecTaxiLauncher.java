package playground.michalm.taxi.run;

import static playground.michalm.taxi.run.AlgorithmConfigs.*;


public class MultiRunMielecTaxiLauncher
{
    public static void main(String... args)
    {
        int runs = 1;
        String paramDir = "d:/PP-rad/mielec/2014_02";
        String specificParamSubDir = args[0];

        TaxiLauncherParams params = TaxiLauncherParams.readParams(paramDir, specificParamSubDir);
        MultiRunTaxiLauncher.runAll(runs, params, //
                RULE_TW_xx, //
                RULE_TP_xx, //
                RULE_DSE_xx, //
                ZONE_TW_xx, //
                FIFO_RES_TW_xx, //
                ASSIGN_TW_xx, //
                ASSIGN_TP_xx, //
                ASSIGN_DSE_xx//
        );
    }
}
