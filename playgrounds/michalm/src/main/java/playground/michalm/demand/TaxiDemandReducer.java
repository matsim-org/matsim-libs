package playground.michalm.demand;

import java.io.*;
import java.util.Scanner;

import pl.poznan.put.util.random.*;


public class TaxiDemandReducer
{
    public static void main(String[] args)
        throws IOException
    {
        UniformRandom uniform = RandomUtils.getGlobalUniform();

        Scanner sc = new Scanner(new File(
                "d:\\michalm\\2013_07\\mielec-2-peaks-new-03-100\\taxiCustomers_03_pc.txt"));
        BufferedWriter bw = new BufferedWriter(new FileWriter(
                "d:\\michalm\\2013_07\\mielec-2-peaks-new-03-100\\taxiCustomers_01_pc.txt"));

        while (sc.hasNext()) {
            String id = sc.next();

            if (uniform.nextDouble(0, 1) < 1. / 3) {
                bw.write(id);
                bw.newLine();
            }
        }

        sc.close();
        bw.close();
    }
}
