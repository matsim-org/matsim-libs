package playground.michalm.taxi.run;

public class MultiRunBerlinTaxiLauncher
{
    public static void main(String... args)
    {
        int runs = 20;
        String paramDir = args[0];
        String specificParamDir = args[0] + "/" + args[1];

        TaxiLauncherParams params = TaxiLauncherParams.readParams(paramDir, specificParamDir);
        MultiRunTaxiLauncher.run(runs, params);
//        MultiRunTaxiLauncher.runAll(runs, params, EnumSet.of(//
//                RULE_TW_FF, //
//                RULE_DSE_FF //
//        ));
    }
}
