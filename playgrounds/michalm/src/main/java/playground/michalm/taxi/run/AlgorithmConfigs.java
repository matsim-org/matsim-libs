package playground.michalm.taxi.run;

import static playground.michalm.taxi.run.AlgorithmConfig.*;

import java.util.EnumSet;

class AlgorithmConfigs
{
    static final EnumSet<AlgorithmConfig> NOS_TW_xx = EnumSet.of(//
    //        NOS_TW_TD,//
            NOS_TW_FF
    //NOS_TW_15M
            );

    static final EnumSet<AlgorithmConfig> NOS_TP_xx = EnumSet.of(//
    //        NOS_TP_TD, //
            NOS_TP_FF
    //NOS_TP_15M
            );

    static final EnumSet<AlgorithmConfig> NOS_DSE_xx = EnumSet.of(//
    //        NOS_DSE_TD, //
            NOS_DSE_FF
    //NOS_DSE_15M
            );

    static final EnumSet<AlgorithmConfig> OTS_TW_xx = EnumSet.of(//
            //OTS_TW_FF
            OTS_TW_15M);

    static final EnumSet<AlgorithmConfig> OTS_TP_xx = EnumSet.of(//
            //OTS_TP_FF
            OTS_TP_15M);

    static final EnumSet<AlgorithmConfig> RES_TW_xx = EnumSet.of(//
            //RES_TW_FF
            RES_TW_15M);

    static final EnumSet<AlgorithmConfig> RES_TP_xx = EnumSet.of(//
            //RES_TP_FF
            RES_TP_15M);

    static final EnumSet<AlgorithmConfig> APS_TW_xx = EnumSet.of(//
            //APS_TW_TD,
            //APS_TW_FF
            APS_TW_15M);

    static final EnumSet<AlgorithmConfig> APS_TP_xx = EnumSet.of(//
            //APS_TP_TD,
            //APS_TP_FF
            APS_TP_15M);

    static final EnumSet<AlgorithmConfig> APS_DSE_xx = EnumSet.of(//
            //APS_DSE_TD,
            //APS_DSE_FF
            APS_DSE_15M);
}
