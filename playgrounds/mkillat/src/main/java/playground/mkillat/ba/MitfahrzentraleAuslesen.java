package playground.mkillat.ba;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class MitfahrzentraleAuslesen {


	
	public static String auslesen(String vonStadt, String zuStadt, String seite ) {
		// TODO Auto-generated method stub
		
		String id1=null;
		String id2=null;
		
		URL url = null;
		ArrayList list = new ArrayList();
		String zeile = null; 
		List<String> zeilen = new ArrayList<String>();
		Date currentTime = new Date();
		String filename = "C:\\Dokumente und Einstellungen\\Marie\\ba\\datei_" + vonStadt + "_" + zuStadt + "_"+ currentTime.getDate() + "_" + (currentTime.getMonth()+1) + "_" + (currentTime.getYear()+1900) + "_" +  currentTime.getHours() + "_Seite" + seite + ".html";
		
		if(vonStadt.equals("Hamburg")){
			id1 = "134";
		}
		if(zuStadt.equals("Hamburg")){
			id2 = "134";
		}
		if(vonStadt.equals("Berlin")){
			id1 = "30";
		}
		if(zuStadt.equals("Berlin")){
			id2 = "30";
		}
		
		
		try {
            url = new URL("http://www.mitfahrgelegenheit.de/mitfahrzentrale/" + vonStadt + "/" + zuStadt + ".html?page=" + seite + "&country_from=1&country_to=1,?city_from=" + id1 + "&radius_from=0&city_to=" + id2+ "&radius_to=0&date=any_date&day=" + currentTime.getDate() + "&month=" + (currentTime.getMonth()+1) + "&year=" + (currentTime.getYear()+1900) +  "&tolerance=0&");
 
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
