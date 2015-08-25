package playground.michalm.taxi.run;

import static playground.michalm.taxi.run.AlgorithmConfig.*;

import java.util.EnumSet;


class AlgorithmConfigs
{
    static final EnumSet<AlgorithmConfig> RULE_TW_xx = EnumSet.of(//
            RULE_TW_TD, //
            RULE_TW_FF, //
            RULE_TW_15M //
    );

    static final EnumSet<AlgorithmConfig> RULE_TP_xx = EnumSet.of(//
            RULE_TP_TD, //
            RULE_TP_FF, //
            RULE_TP_15M //
    );

    static final EnumSet<AlgorithmConfig> RULE_DSE_xx = EnumSet.of(//
            RULE_DSE_TD, //
            RULE_DSE_FF, //
            RULE_DSE_15M //
    );

    static final EnumSet<AlgorithmConfig> ZONE_TW_xx = EnumSet.of(//
            ZONE_TW_TD, //
            ZONE_TW_FF, //
            ZONE_TW_15M //
    );

    static final EnumSet<AlgorithmConfig> FIFO_RES_TW_xx = EnumSet.of(//
            FIFO_RES_TW_FF, //
            FIFO_RES_TW_15M//
    );

    static final EnumSet<AlgorithmConfig> FIFO_RES_TP_xx = EnumSet.of(//
            FIFO_RES_TP_FF, //
            FIFO_RES_TP_15M//
    );

    static final EnumSet<AlgorithmConfig> ASSIGN_TW_xx = EnumSet.of(//
            ASSIGN_TW_FF, //
            ASSIGN_TW_15M //
    );

    static final EnumSet<AlgorithmConfig> ASSIGN_TP_xx = EnumSet.of(//
            ASSIGN_TP_FF, //
            ASSIGN_TP_15M //
    );

    static final EnumSet<AlgorithmConfig> ASSIGN_DSE_xx = EnumSet.of(//
            ASSIGN_DSE_FF, //
            ASSIGN_DSE_15M //
    );
}
