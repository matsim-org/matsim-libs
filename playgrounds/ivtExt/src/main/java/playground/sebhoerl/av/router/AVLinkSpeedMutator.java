package playground.sebhoerl.av.router;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import org.apache.commons.math3.distribution.GammaDistribution;

public class AVLinkSpeedMutator {
    final private GammaDistribution distributions[];
    final private double binSize;
    
    public AVLinkSpeedMutator(String path) {
        String line;
        
        double binSize = 120;
        int count = 0;
        
        LinkedList<Double> ks = new LinkedList<>();
        LinkedList<Double> thetas = new LinkedList<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("bin_size: ")) {
                    binSize = (double) Integer.parseInt(line.substring(10));
                } else if (line.startsWith("bins: ")) {
                    count = Integer.parseInt(line.substring(6));
                } else if (line.indexOf("\t") >= 0) {
                    String[] parts = line.split("\t");
                    
                    ks.add(Double.parseDouble(parts[0]));
                    thetas.add(Double.parseDouble(parts[1]));
                }
            }
            
            reader.close();
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not read mutator file. " + path);
        }
        
        distributions = new GammaDistribution[count];
        this.binSize = binSize;
        
        for (int i = 0; i < count; i++) {
            distributions[i] = new GammaDistribution(ks.get(i), thetas.get(i));
        }
    }
    
    private GammaDistribution getDistribution(double time) {
        return distributions[Math.min((int) Math.floor(time / binSize), distributions.length - 1)];
    }
    
    public double mutateLinkSpeed(double linkSpeed, double time) {
        double sample = Math.min(1.0, getDistribution(time).sample());
        return linkSpeed * (1 - sample);
    }
}
