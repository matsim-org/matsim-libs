package playground.mkillat.ba;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuslesenGetTelNumber {

	public static String auslesen(String id ) {
		// TODO Auto-generated method stub
		
		
		
		URL url = null;
		ArrayList list = new ArrayList();
		String zeile = null; 
		List<String> zeilen = new ArrayList<String>();
		Date currentTime = new Date();
		String filename = "C:\\Dokumente und Einstellungen\\Marie\\ba\\datei_temp.html";
		
		
		
		try {
            url = new URL("http://www.mitfahrgelegenheit.de/lifts/show/" + id + "/1346191200");
 
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty(
                "user-agent",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.1.4322)");
            conn.setRequestProperty("Referer", "http://www.mitfahrgelegenheit.de/");
            conn.connect();
//          
            
             
	        	// Zugriff auf URL und auslesen dieser bis zeile gleich null
            	BufferedReader lies = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	            while ((zeile = lies.readLine()) != null) { 
	               zeilen.add(zeile);
	            } 
	            lies.close();
	          
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
        
        Writer fw = null; 
	      
	      try 
	      { 
	    	
	        fw = new FileWriter(filename);

	        for (int i = 0; i < zeilen.size(); i++) {
				fw.write(zeilen.get(i) + "\n");
				
			} 
	      } 
	      catch ( IOException e ) { 
	        System.err.println( "Konnte Datei nicht erstellen" ); 
	      } 
	      finally { 
	        if ( fw != null ) 
	          try { fw.close(); } catch ( IOException e ) { } 
	      }
		return filename;

   
	}
	
	
}
