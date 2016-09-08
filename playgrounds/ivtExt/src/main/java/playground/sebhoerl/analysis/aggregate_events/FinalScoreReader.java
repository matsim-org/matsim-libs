package playground.sebhoerl.analysis.aggregate_events;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Iterator;

import playground.sebhoerl.analysis.XmlElement;

public class FinalScoreReader {
    final private Writer writer;
    
    public FinalScoreReader(Writer writer) {
        this.writer = writer;
    }
    
    public void read(String path) throws FileNotFoundException, IOException {
        FileInputStream input = new FileInputStream(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        
        double score = Double.POSITIVE_INFINITY;
        long iteration = 0;
        
        Iterator<String> iterator = reader.lines().iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            
            if (!line.startsWith("ITERATION")) {
                String[] columns = line.split("\t");
                score = Double.parseDouble(columns[1]);
                iteration = Long.parseLong(columns[0]);
            }
        }
        
        reader.close();
        
        XmlElement element = new XmlElement("simulation");
        element.addAttribute("iterations", iteration);
        element.addAttribute("score", score);
        
        try {
            writer.write(element.toString() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
