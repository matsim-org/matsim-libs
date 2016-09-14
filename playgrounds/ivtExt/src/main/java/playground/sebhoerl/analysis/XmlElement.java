package playground.sebhoerl.analysis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

public class XmlElement {
    final private String name;
    final HashMap<String, String> attributes = new HashMap<>();
    
    public XmlElement(String name) {
        this.name = name;
    }
    
    public <T> void addAttribute(String name, T value) {
        attributes.put(name, String.valueOf(value));
    }
    
    public String toString() {
        LinkedList<String> attributes = new LinkedList<>();
        
        for (Entry<String, String> entry : this.attributes.entrySet()) {
            attributes.add(String.format("%s=\"%s\"", entry.getKey(), entry.getValue()));
        }
        
        return String.format("<%s %s />", name, String.join(" ", attributes));
    }
}
