package org.matsim.application.prepare.network.params.ref;
import org.matsim.application.prepare.network.params.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
* Generated model, do not modify.
*/
final class GermanyNetworkParams_capacity_dead_end implements FeatureRegressor {

    public static GermanyNetworkParams_capacity_dead_end INSTANCE = new GermanyNetworkParams_capacity_dead_end();
    public static final double[] DEFAULT_PARAMS = {859.6049, 1368.6547, 317.08328, 602.73334, 1051.5116, 1365.0673, 479.5267, 1510.9359, 1261.82, 586.93774, 1752.8143, 345.83023, 251.44379, 196.05144, -100.92311, 556.4511, 248.56212, 8.751958, -112.63192, 353.46375, -499.09198, 67.84669, 238.25359, 69.855865, -147.35197, -24.82771, -398.5832, -312.16702, 645.81635, -429.90787, -56.959373, 73.37216, -8.621415, -20.72194, -5.8856874, -45.02695, 673.52795, 73.94616, -51.44936, 27.939749, -1045.293, 100.6221, 41.877926, 176.6136, 102.3786, -0.9688834, -220.64973, 137.63858, 215.92377, 41.44684, -20.3626, 16.380981, -151.44092, 52.8146, 10.459056, -165.38365, 34.63168, 32.307022, 218.85617, -109.07436, 67.68019, 6.774578, 178.27382, -206.08951, -1.2301626, 391.88242, -192.48239, 76.81782, -80.081535, -117.16598, 35.795372, 11.609657, -67.645164, -4.0473456, 189.128, -27.950788, 1.5682833, -23.019985, 382.52103, -4.099745, 68.12114, -223.34529, 273.18823, -89.40101, -5.440557, -10.679427, 109.98401, -64.30533, 71.05594, 27.412102, 4.8957453, 4.3622723, -190.4019, 233.06438, -11.0441675, -344.44733, 147.786, -23.268576, -33.001354, -1.6072537, -189.9128, -6.27801, -1.5817443, 91.38268, -136.90344, 66.1445, 155.11888, -52.789665, 12.548233, -99.26018, -10.74564, 7.0323977, 30.524338, -34.451485, -76.895966, 25.581684, -2.531495, 16.72898, 29.978521, -14.7835455, -68.298965, -2.481075, 15.709786, 101.65081, -137.88113, 3.3836243, 2.924429, -12.824084, -1.1771411, 21.445478, 31.226662, -12.21468, 11.967453, 5.482041, 50.30282, -0.40552256, -25.395111, -51.44317, -0.99815965, 9.2745495, -6.7587147, -23.885403, 3.0521197, -16.76911, 78.22408, 34.127068, -3.9190707, -75.568504, 8.654165, -3.9965763, -0.9473669, -9.223031, -3.693175, 9.046608, -67.85387, 2.1328363, 8.552537, 63.788208, 13.773914, -23.446692, -0.35650137, -4.8897696, 12.350961, -36.01829, -3.3446863, 13.035452, 2.4459727, -1.5044533, 25.509474, -0.022190453, -9.799512, 12.084186, 0.5939214, -38.01811, -8.539415, 25.965952, -14.307394, 6.077125, 0.27664867, 3.100341, -21.760082, 3.2573829, 1.4723984, -4.4738817, 9.860579, -11.05045, -0.23873898, -17.215534, -1.7263151, 1.2006466, -1.0414621, 3.6885643, -2.7444, 0.22894292, -48.507504, 17.920248, 0.692537, 4.6048656, -4.2964005, -0.4720382, 21.348267, 9.8496065, -2.6010177, 10.099594, -12.914126, 47.707207, -13.917312, 0.0, -0.051987253, 12.860103, 2.255951, -5.9641266, 21.498459, 4.267856, -19.037823, 2.985492, -0.44421604, 2.0518365, 1.7539244, 0.74282753, 7.382213, -5.6273985, -2.0897858, -0.048497323, -4.767571};

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        return predict(ft, DEFAULT_PARAMS);
    }

    @Override
    public double[] getData(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 121.64943999999997) / 102.75395159548076;
		data[1] = (ft.getDouble("speed") - 15.9616) / 4.368329731144388;
		data[2] = (ft.getDouble("num_lanes") - 1.856) / 0.8551397546600205;
		data[3] = ft.getDouble("change_speed");
		data[4] = ft.getDouble("change_num_lanes");
		data[5] = ft.getDouble("num_to_links");
		data[6] = ft.getDouble("junction_inc_lanes");
		data[7] = ft.getDouble("priority_lower");
		data[8] = ft.getDouble("priority_equal");
		data[9] = ft.getDouble("priority_higher");
		data[10] = ft.getDouble("is_secondary_or_higher");
		data[11] = ft.getDouble("is_primary_or_higher");
		data[12] = ft.getDouble("is_motorway");
		data[13] = ft.getDouble("is_link");

        return data;
    }

    @Override
    public double predict(Object2DoubleMap<String> ft, double[] params) {

        double[] data = getData(ft);
        for (int i = 0; i < data.length; i++)
            if (Double.isNaN(data[i])) throw new IllegalArgumentException("Invalid data at index: " + i);

        return score(data, params);
    }
    public static double score(double[] input, double[] params) {
        double var0;
        if (input[0] >= -0.8462881) {
            if (input[6] >= 1.5) {
                if (input[0] >= -0.76468533) {
                    if (input[0] >= -0.68789995) {
                        var0 = params[0];
                    } else {
                        var0 = params[1];
                    }
                } else {
                    if (input[0] >= -0.8073114) {
                        var0 = params[2];
                    } else {
                        var0 = params[3];
                    }
                }
            } else {
                if (input[0] >= -0.82147145) {
                    if (input[0] >= 1.1191838) {
                        var0 = params[4];
                    } else {
                        var0 = params[5];
                    }
                } else {
                    var0 = params[6];
                }
            }
        } else {
            if (input[0] >= -1.0267677) {
                if (input[0] >= -1.002243) {
                    if (input[0] >= -0.96248794) {
                        var0 = params[7];
                    } else {
                        var0 = params[8];
                    }
                } else {
                    var0 = params[9];
                }
            } else {
                var0 = params[10];
            }
        }
        double var1;
        if (input[0] >= 0.46680015) {
            if (input[0] >= 1.0344182) {
                if (input[1] >= 0.47922206) {
                    if (input[2] >= -0.41630626) {
                        var1 = params[11];
                    } else {
                        var1 = params[12];
                    }
                } else {
                    if (input[0] >= 1.3222904) {
                        var1 = params[13];
                    } else {
                        var1 = params[14];
                    }
                }
            } else {
                if (input[2] >= -0.41630626) {
                    var1 = params[15];
                } else {
                    var1 = params[16];
                }
            }
        } else {
            if (input[0] >= -0.69982165) {
                if (input[1] >= -0.792431) {
                    if (input[2] >= 0.75309324) {
                        var1 = params[17];
                    } else {
                        var1 = params[18];
                    }
                } else {
                    var1 = params[19];
                }
            } else {
                if (input[2] >= 1.9224927) {
                    if (input[0] >= -0.76804286) {
                        var1 = params[20];
                    } else {
                        var1 = params[21];
                    }
                } else {
                    if (input[0] >= -0.82147145) {
                        var1 = params[22];
                    } else {
                        var1 = params[23];
                    }
                }
            }
        }
        double var2;
        if (input[0] >= 1.4293422) {
            if (input[1] >= -0.15603217) {
                if (input[0] >= 1.4803379) {
                    if (input[2] >= 0.1683935) {
                        var2 = params[24];
                    } else {
                        var2 = params[25];
                    }
                } else {
                    var2 = params[26];
                }
            } else {
                var2 = params[27];
            }
        } else {
            if (input[0] >= 1.3335794) {
                var2 = params[28];
            } else {
                if (input[11] >= 0.5) {
                    if (input[0] >= 0.04920064) {
                        var2 = params[29];
                    } else {
                        var2 = params[30];
                    }
                } else {
                    if (input[0] >= -0.45715457) {
                        var2 = params[31];
                    } else {
                        var2 = params[32];
                    }
                }
            }
        }
        double var3;
        if (input[0] >= 1.3335794) {
            if (input[0] >= 2.015451) {
                if (input[0] >= 2.0768113) {
                    if (input[2] >= -0.41630626) {
                        var3 = params[33];
                    } else {
                        var3 = params[34];
                    }
                } else {
                    var3 = params[35];
                }
            } else {
                if (input[0] >= 1.6961446) {
                    var3 = params[36];
                } else {
                    var3 = params[37];
                }
            }
        } else {
            if (input[0] >= -0.87937677) {
                if (input[0] >= -0.86414623) {
                    if (input[0] >= -0.28796402) {
                        var3 = params[38];
                    } else {
                        var3 = params[39];
                    }
                } else {
                    var3 = params[40];
                }
            } else {
                if (input[0] >= -0.90594506) {
                    if (input[0] >= -0.8935368) {
                        var3 = params[41];
                    } else {
                        var3 = params[42];
                    }
                } else {
                    if (input[0] >= -0.96248794) {
                        var3 = params[43];
                    } else {
                        var3 = params[44];
                    }
                }
            }
        }
        double var4;
        if (input[1] >= -0.15603217) {
            if (input[2] >= 0.75309324) {
                if (input[0] >= -0.4062563) {
                    if (input[0] >= 1.246089) {
                        var4 = params[45];
                    } else {
                        var4 = params[46];
                    }
                } else {
                    if (input[0] >= -0.5137947) {
                        var4 = params[47];
                    } else {
                        var4 = params[48];
                    }
                }
            } else {
                if (input[4] >= -0.5) {
                    if (input[1] >= 1.7508751) {
                        var4 = params[49];
                    } else {
                        var4 = params[50];
                    }
                } else {
                    if (input[0] >= 1.2568915) {
                        var4 = params[51];
                    } else {
                        var4 = params[52];
                    }
                }
            }
        } else {
            if (input[2] >= 0.75309324) {
                if (input[0] >= -0.040090334) {
                    if (input[0] >= 0.20423117) {
                        var4 = params[53];
                    } else {
                        var4 = params[54];
                    }
                } else {
                    if (input[0] >= -0.8073114) {
                        var4 = params[55];
                    } else {
                        var4 = params[56];
                    }
                }
            } else {
                if (input[0] >= -0.45715457) {
                    if (input[0] >= 0.47254202) {
                        var4 = params[57];
                    } else {
                        var4 = params[58];
                    }
                } else {
                    if (input[0] >= -0.74955213) {
                        var4 = params[59];
                    } else {
                        var4 = params[60];
                    }
                }
            }
        }
        double var5;
        if (input[0] >= -0.82750535) {
            if (input[0] >= -0.680601) {
                if (input[0] >= -0.41589096) {
                    if (input[0] >= -0.28796402) {
                        var5 = params[61];
                    } else {
                        var5 = params[62];
                    }
                } else {
                    if (input[0] >= -0.44343248) {
                        var5 = params[63];
                    } else {
                        var5 = params[64];
                    }
                }
            } else {
                if (input[0] >= -0.68989503) {
                    var5 = params[65];
                } else {
                    if (input[0] >= -0.69982165) {
                        var5 = params[66];
                    } else {
                        var5 = params[67];
                    }
                }
            }
        } else {
            if (input[0] >= -0.87937677) {
                if (input[0] >= -0.86414623) {
                    var5 = params[68];
                } else {
                    var5 = params[69];
                }
            } else {
                if (input[0] >= -0.96248794) {
                    if (input[0] >= -0.9273068) {
                        var5 = params[70];
                    } else {
                        var5 = params[71];
                    }
                } else {
                    if (input[0] >= -1.0267677) {
                        var5 = params[72];
                    } else {
                        var5 = params[73];
                    }
                }
            }
        }
        double var6;
        if (input[0] >= -0.7369492) {
            if (input[0] >= -0.70867777) {
                if (input[10] >= 0.5) {
                    if (input[0] >= -0.10972269) {
                        var6 = params[74];
                    } else {
                        var6 = params[75];
                    }
                } else {
                    if (input[0] >= -0.5848869) {
                        var6 = params[76];
                    } else {
                        var6 = params[77];
                    }
                }
            } else {
                if (input[0] >= -0.7131058) {
                    var6 = params[78];
                } else {
                    if (input[0] >= -0.7254168) {
                        var6 = params[79];
                    } else {
                        var6 = params[80];
                    }
                }
            }
        } else {
            if (input[0] >= -0.74955213) {
                var6 = params[81];
            } else {
                if (input[0] >= -0.77728826) {
                    var6 = params[82];
                } else {
                    if (input[2] >= 0.1683935) {
                        var6 = params[83];
                    } else {
                        var6 = params[84];
                    }
                }
            }
        }
        double var7;
        if (input[1] >= -0.15603217) {
            if (input[0] >= -0.3979841) {
                if (input[0] >= -0.16801728) {
                    if (input[0] >= 0.1484669) {
                        var7 = params[85];
                    } else {
                        var7 = params[86];
                    }
                } else {
                    var7 = params[87];
                }
            } else {
                if (input[0] >= -0.4428972) {
                    var7 = params[88];
                } else {
                    if (input[0] >= -0.68989503) {
                        var7 = params[89];
                    } else {
                        var7 = params[90];
                    }
                }
            }
        } else {
            if (input[0] >= -0.22996137) {
                if (input[0] >= 0.04920064) {
                    if (input[0] >= 0.14014605) {
                        var7 = params[91];
                    } else {
                        var7 = params[92];
                    }
                } else {
                    if (input[0] >= -0.10972269) {
                        var7 = params[93];
                    } else {
                        var7 = params[94];
                    }
                }
            } else {
                if (input[0] >= -0.2746312) {
                    var7 = params[95];
                } else {
                    if (input[0] >= -0.34275508) {
                        var7 = params[96];
                    } else {
                        var7 = params[97];
                    }
                }
            }
        }
        double var8;
        if (input[0] >= -0.4512667) {
            if (input[2] >= -0.41630626) {
                if (input[0] >= -0.43516028) {
                    if (input[0] >= 1.7182848) {
                        var8 = params[98];
                    } else {
                        var8 = params[99];
                    }
                } else {
                    if (input[0] >= -0.44343248) {
                        var8 = params[100];
                    } else {
                        var8 = params[101];
                    }
                }
            } else {
                if (input[0] >= -0.01722017) {
                    if (input[0] >= 0.47254202) {
                        var8 = params[102];
                    } else {
                        var8 = params[103];
                    }
                } else {
                    var8 = params[104];
                }
            }
        } else {
            if (input[2] >= -0.41630626) {
                if (input[0] >= -0.48907548) {
                    if (input[0] >= -0.4726284) {
                        var8 = params[105];
                    } else {
                        var8 = params[106];
                    }
                } else {
                    if (input[0] >= -0.52839273) {
                        var8 = params[107];
                    } else {
                        var8 = params[108];
                    }
                }
            } else {
                if (input[0] >= -0.5803615) {
                    var8 = params[109];
                } else {
                    if (input[0] >= -0.82147145) {
                        var8 = params[110];
                    } else {
                        var8 = params[111];
                    }
                }
            }
        }
        double var9;
        if (input[2] >= 0.75309324) {
            if (input[0] >= 0.29619843) {
                if (input[0] >= 1.3367424) {
                    var9 = params[112];
                } else {
                    if (input[0] >= 0.46680015) {
                        var9 = params[113];
                    } else {
                        var9 = params[114];
                    }
                }
            } else {
                if (input[0] >= 0.20423117) {
                    var9 = params[115];
                } else {
                    if (input[4] >= -2.5) {
                        var9 = params[116];
                    } else {
                        var9 = params[117];
                    }
                }
            }
        } else {
            if (input[0] >= 1.3335794) {
                if (input[0] >= 1.7471402) {
                    if (input[4] >= -0.5) {
                        var9 = params[118];
                    } else {
                        var9 = params[119];
                    }
                } else {
                    if (input[0] >= 1.4293422) {
                        var9 = params[120];
                    } else {
                        var9 = params[121];
                    }
                }
            } else {
                if (input[0] >= 1.1876483) {
                    if (input[0] >= 1.221759) {
                        var9 = params[122];
                    } else {
                        var9 = params[123];
                    }
                } else {
                    if (input[0] >= 1.1512506) {
                        var9 = params[124];
                    } else {
                        var9 = params[125];
                    }
                }
            }
        }
        double var10;
        if (input[1] >= -0.15603217) {
            if (input[0] >= -0.68989503) {
                if (input[2] >= 0.75309324) {
                    if (input[0] >= -0.61958146) {
                        var10 = params[126];
                    } else {
                        var10 = params[127];
                    }
                } else {
                    if (input[0] >= 1.1512506) {
                        var10 = params[128];
                    } else {
                        var10 = params[129];
                    }
                }
            } else {
                if (input[2] >= -0.41630626) {
                    var10 = params[130];
                } else {
                    if (input[0] >= -0.90594506) {
                        var10 = params[131];
                    } else {
                        var10 = params[132];
                    }
                }
            }
        } else {
            if (input[0] >= -0.45715457) {
                if (input[1] >= -1.4288299) {
                    if (input[0] >= -0.42426047) {
                        var10 = params[133];
                    } else {
                        var10 = params[134];
                    }
                } else {
                    if (input[0] >= 1.8040724) {
                        var10 = params[135];
                    } else {
                        var10 = params[136];
                    }
                }
            } else {
                if (input[2] >= -0.41630626) {
                    if (input[0] >= -0.6213819) {
                        var10 = params[137];
                    } else {
                        var10 = params[138];
                    }
                } else {
                    if (input[0] >= -0.92930186) {
                        var10 = params[139];
                    } else {
                        var10 = params[140];
                    }
                }
            }
        }
        double var11;
        if (input[1] >= 3.3407278) {
            var11 = params[141];
        } else {
            if (input[0] >= 0.85053235) {
                if (input[0] >= 1.1512506) {
                    if (input[0] >= 1.1876483) {
                        var11 = params[142];
                    } else {
                        var11 = params[143];
                    }
                } else {
                    if (input[0] >= 1.1044399) {
                        var11 = params[144];
                    } else {
                        var11 = params[145];
                    }
                }
            } else {
                if (input[0] >= -0.6213819) {
                    if (input[0] >= -0.5848869) {
                        var11 = params[146];
                    } else {
                        var11 = params[147];
                    }
                } else {
                    if (input[0] >= -0.77728826) {
                        var11 = params[148];
                    } else {
                        var11 = params[149];
                    }
                }
            }
        }
        double var12;
        if (input[0] >= -0.44343248) {
            if (input[0] >= -0.43516028) {
                if (input[4] >= -1.5) {
                    if (input[0] >= 0.64917755) {
                        var12 = params[150];
                    } else {
                        var12 = params[151];
                    }
                } else {
                    if (input[0] >= 0.04920064) {
                        var12 = params[152];
                    } else {
                        var12 = params[153];
                    }
                }
            } else {
                var12 = params[154];
            }
        } else {
            if (input[0] >= -0.4856693) {
                if (input[1] >= -0.15603217) {
                    var12 = params[155];
                } else {
                    if (input[0] >= -0.4726284) {
                        var12 = params[156];
                    } else {
                        var12 = params[157];
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    var12 = params[158];
                } else {
                    if (input[0] >= -0.52839273) {
                        var12 = params[159];
                    } else {
                        var12 = params[160];
                    }
                }
            }
        }
        double var13;
        if (input[0] >= -1.0065739) {
            if (input[0] >= 1.2568915) {
                if (input[0] >= 1.4803379) {
                    if (input[0] >= 2.015451) {
                        var13 = params[161];
                    } else {
                        var13 = params[162];
                    }
                } else {
                    if (input[0] >= 1.4293422) {
                        var13 = params[163];
                    } else {
                        var13 = params[164];
                    }
                }
            } else {
                if (input[0] >= 1.1876483) {
                    var13 = params[165];
                } else {
                    if (input[0] >= -0.123736754) {
                        var13 = params[166];
                    } else {
                        var13 = params[167];
                    }
                }
            }
        } else {
            var13 = params[168];
        }
        double var14;
        if (input[0] >= -0.28796402) {
            if (input[0] >= -0.22996137) {
                if (input[0] >= 0.3694316) {
                    if (input[0] >= 0.64917755) {
                        var14 = params[169];
                    } else {
                        var14 = params[170];
                    }
                } else {
                    if (input[0] >= 0.3540551) {
                        var14 = params[171];
                    } else {
                        var14 = params[172];
                    }
                }
            } else {
                if (input[0] >= -0.27424192) {
                    var14 = params[173];
                } else {
                    var14 = params[174];
                }
            }
        } else {
            if (input[0] >= -0.32484823) {
                var14 = params[175];
            } else {
                if (input[0] >= -0.41589096) {
                    var14 = params[176];
                } else {
                    if (input[0] >= -0.5848869) {
                        var14 = params[177];
                    } else {
                        var14 = params[178];
                    }
                }
            }
        }
        double var15;
        if (input[10] >= 0.5) {
            if (input[2] >= 1.9224927) {
                var15 = params[179];
            } else {
                if (input[2] >= 0.75309324) {
                    if (input[0] >= 0.04920064) {
                        var15 = params[180];
                    } else {
                        var15 = params[181];
                    }
                } else {
                    var15 = params[182];
                }
            }
        } else {
            if (input[1] >= 2.0690746) {
                if (input[1] >= 3.3407278) {
                    var15 = params[183];
                } else {
                    var15 = params[184];
                }
            } else {
                if (input[2] >= 1.9224927) {
                    var15 = params[185];
                } else {
                    if (input[0] >= -0.9273068) {
                        var15 = params[186];
                    } else {
                        var15 = params[187];
                    }
                }
            }
        }
        double var16;
        if (input[0] >= -1.0267677) {
            if (input[1] >= -0.15603217) {
                if (input[0] >= -0.52839273) {
                    if (input[2] >= -0.41630626) {
                        var16 = params[188];
                    } else {
                        var16 = params[189];
                    }
                } else {
                    if (input[1] >= 0.47922206) {
                        var16 = params[190];
                    } else {
                        var16 = params[191];
                    }
                }
            } else {
                if (input[0] >= -0.92930186) {
                    if (input[2] >= 0.75309324) {
                        var16 = params[192];
                    } else {
                        var16 = params[193];
                    }
                } else {
                    var16 = params[194];
                }
            }
        } else {
            var16 = params[195];
        }
        double var17;
        if (input[0] >= -0.123736754) {
            if (input[0] >= 0.04920064) {
                if (input[2] >= -0.41630626) {
                    if (input[0] >= 1.0344182) {
                        var17 = params[196];
                    } else {
                        var17 = params[197];
                    }
                } else {
                    if (input[0] >= 1.7471402) {
                        var17 = params[198];
                    } else {
                        var17 = params[199];
                    }
                }
            } else {
                if (input[0] >= -0.034445778) {
                    var17 = params[200];
                } else {
                    var17 = params[201];
                }
            }
        } else {
            if (input[0] >= -0.74955213) {
                if (input[0] >= -0.7369492) {
                    if (input[0] >= -0.7254168) {
                        var17 = params[202];
                    } else {
                        var17 = params[203];
                    }
                } else {
                    var17 = params[204];
                }
            } else {
                if (input[0] >= -0.77728826) {
                    var17 = params[205];
                } else {
                    if (input[0] >= -0.80127764) {
                        var17 = params[206];
                    } else {
                        var17 = params[207];
                    }
                }
            }
        }
        double var18;
        if (input[0] >= -0.69982165) {
            if (input[0] >= -0.680601) {
                if (input[0] >= -0.5848869) {
                    if (input[0] >= -0.52839273) {
                        var18 = params[208];
                    } else {
                        var18 = params[209];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var18 = params[210];
                    } else {
                        var18 = params[211];
                    }
                }
            } else {
                if (input[0] >= -0.68989503) {
                    var18 = params[212];
                } else {
                    var18 = params[213];
                }
            }
        } else {
            if (input[0] >= -0.70867777) {
                var18 = params[214];
            } else {
                if (input[2] >= 1.9224927) {
                    var18 = params[215];
                } else {
                    if (input[0] >= -1.0267677) {
                        var18 = params[216];
                    } else {
                        var18 = params[217];
                    }
                }
            }
        }
        double var19;
        if (input[0] >= 2.0768113) {
            if (input[4] >= -1.5) {
                if (input[0] >= 2.8893833) {
                    var19 = params[218];
                } else {
                    var19 = params[219];
                }
            } else {
                var19 = params[220];
            }
        } else {
            if (input[1] >= -1.4288299) {
                if (input[0] >= 2.015451) {
                    var19 = params[221];
                } else {
                    if (input[11] >= 0.5) {
                        var19 = params[222];
                    } else {
                        var19 = params[223];
                    }
                }
            } else {
                var19 = params[224];
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19);
    }
}
