package playground.wrashid.msimoni.analyses;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class DensityFlowTable {

	public static void main(String[] args) 
   {
		
		try {
		FileReader input_1= new FileReader("C:/Users/simonimi/Desktop/MFD analyses/text files/density_1pct.txt");
		FileReader input_2= new FileReader("C:/Users/simonimi/Desktop/MFD analyses/text files/outFlow_1pct.txt");
		FileReader input_3= new FileReader("C:/Users/simonimi/Desktop/MFD analyses/text files/linkselector.txt");
		FileWriter output_1= new FileWriter("C:/Users/simonimi/Desktop/MFD analyses/text files/new.txt");
		BufferedReader i1 = new BufferedReader(input_1);
		BufferedReader i2 = new BufferedReader(input_2);
		BufferedReader i3 = new BufferedReader(input_3);
		BufferedWriter o1 = new BufferedWriter (output_1);
		
		Map<String, String> map1 = new TreeMap<String, String>();
		Map<String, String> map2 = new TreeMap<String, String>();
		Set<String> set = new HashSet<String>();
				
		String s;
		while (true) {
			s=i1.readLine();
			if (s == null) break;
			
			String[] sArray = s.split("\t");
			map1.put(sArray[0], s);
			//System.out.println(sArray[0]);
		}
		i1.close();
		System.out.println("map1: " + map1.size());
		
		while (true) {
			s=i2.readLine();
			if (s == null) break;
			
			String[] sArray = s.split("\t");
			map2.put(sArray[0], s);
		}
		i2.close();
		System.out.println("map2: " + map2.size());
		
		while (true) {
			s=i3.readLine();
			if (s == null) break;
			set.add(s);
		}
		i3.close();
		System.out.println("selected links: " + set.size());
		
		for (Entry<String, String> entry : map1.entrySet()) {
			String key = entry.getKey();
			
			String id = key.split(" ")[0];
			
			if (!set.contains(id)) continue;
			
			String s1 = entry.getValue();
			String s2 = map2.get(key);
			
			if (s2 == null) throw new RuntimeException("No entry for key " + key + " found in map2!");
			
			o1.write(s1);
			o1.write("\t");
			o1.write(s2);
			o1.write("\n");
		}
		o1.flush();
		o1.close();
		
		
		
		// add link selector: it takes into consideration of links only within a certain area and longer than >10 m
		
		
//		String r;
//		String k;
//		r=i2.readLine();
		
//		int cont;
//		while (s!=null){
//			int a;
//			a=s.indexOf(':');
//			String ID_density=s.substring(0, a);
//			String Data_density=s.substring(a);
//			while (r!=null){
//				int b;
//				b=r.indexOf(':');
//				String ID_flow=r.substring(0, b);
//				String Data_flow=r.substring(b);
//				
//				if (ID_density.equals(ID_flow)){
//					k=ID_flow+" "+Data_density+" "+Data_flow;
//					//System.out.println(s);
//					//System.out.println(r);
//					o1.write(k);
//					System.out.println(k);
//					break;
//				}
//					r=i1.readLine();
//				
//			}
//			s=i1.readLine();
//		}
		//System.out.println(r);
		
		    }
		catch (IOException e) {
		       System.err.println("Error: " + e);
		}    
	}
	
}
