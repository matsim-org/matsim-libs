package playground.fhuelsmann.emission;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;


import playground.fhuelsmann.emission.objects.HotValue;

public class HbefaHot {

	// HbefaHot<String,String> 
	Map<String,HotValue> HbefaHot = new HashMap<String,HotValue>() ;

	public void makeHbefaHot(String filename){

		File file = new File(filename);
		BufferedReader reader = null;	

		try{
			reader = new BufferedReader(new FileReader(file));
			String text = null;
			reader.readLine();
			while ((text = reader.readLine()) != null) {

				// split is implemented in this class, see explanation.  
				String[] row = split(text,";");
				String key="";
				String[] value = new String[2];

				//create the key, the key is an array , hotKey	
				for(int i=0;i<13;i++)
					if (!(i==8 || i==9))
						key +=row[i]+";";

				//create the value, the value is an array , hotValue	

				value[0] = row[15];
				value[1] = row[18];


				// erstllen von HbefaHot
				HbefaHot.put(key, new HotValue(value));

			}
		}
		catch(Exception e ){
			System.err.println(e);
		}
	}

	public Map<String, HotValue> getHbefaHot() {
		return HbefaHot;
	}

	public void setHbefaHot(Map<String, HotValue> hbefaHot) {
		HbefaHot = hbefaHot;
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