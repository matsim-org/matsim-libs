package playground.fhuelsmann.emission;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class HbefaHot {
	
		
		public void makeHbefaHot(String filename){
		
			// HbefaHot<String,String> 
			Map<String,String> HbefaHot = new HashMap<String,String>() ;
			
			// read all rows
			File file = new File(filename);
			BufferedReader reader = null;	
			
		try{
				reader = new BufferedReader(new FileReader(file));
				String hbefa = null;
				reader.readLine();
				while ((hbefa = reader.readLine()) != null) {
					
					// used a split method as the Java options don't consider the case of several semicolons ";;"
					String[] row = split(hbefa,";");
					
					// Set up Key and Value
					String hbefaHotKey = "";
					String hbefaHotValue ="";
				
					// 
						for(int i=0;i<14;i++)
							hbefaHotKey += row[i];
						
						for(int i=14;i<27;i++)
							hbefaHotValue += row[i];
						
	//					System.out.println("++++++++++++++++++++++++++" + hbefaHotKey +" "+ hbefaHotValue);
						
						// write HbefaHot
						HbefaHot.put(hbefaHotKey, hbefaHotValue);
						
						// new key and Value 
						hbefaHotKey="";
						hbefaHotValue="";
						
				
				}
			}catch(Exception e ){
				System.err.println(e);
					
				}
				
			}
		
		private String[] split(String hbefa,String symbol) {
			
			String[] result = new String[27];
			int index=0;
			String part="";
			for(int i=0;i<hbefa.length();i++){
				if (hbefa.substring(i, i+1).equals(symbol)){
					result[index++]= ""+ part;
					part="";
				}
				else {
					part+= hbefa.substring(i,i+1);

				}
				
			}
			result[index++]=part;
		return result;
		
	
		}
			
				
				
		

		
		
}



