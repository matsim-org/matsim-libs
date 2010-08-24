package playground.mmoyo.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**Extracts only station counts graphs and error graphs from a kmz file*/
public class KMZ_Extractor {
	String ERR_PREFIX = "error";
	String POINT = ".";
    String kmzFile;
    String outputDir;
	
    public KMZ_Extractor( String kmzFile, String outputDir){
    	this.kmzFile = kmzFile;
    	this.outputDir = outputDir;
    }
    
    public void extractGraphs(){
        try{
            byte[] buffer = new byte[1024];
            ZipInputStream zipInputStream = null;
            ZipEntry zipEntry;
            zipInputStream = new ZipInputStream(new FileInputStream(this.kmzFile));
            zipEntry = zipInputStream.getNextEntry();
           
            while (zipEntry != null) {
                String zipEntryName = zipEntry.getName();
                File file = new File(zipEntryName);
                String parentFile = file.getParent();
                String nameNoExtension = zipEntryName.substring(0, zipEntryName.indexOf(POINT));
                
                //if is in parent directory and (is numeric or errorGraph)
                if (parentFile==null && ( nameNoExtension.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+") || nameNoExtension.startsWith(ERR_PREFIX)  )){
                	System.out.println("extracting: "+ outputDir + zipEntryName );
                	
                	FileOutputStream fileoutputstream;
                	fileoutputstream = new FileOutputStream(this.outputDir + zipEntryName);             
                	int i;
                	while ((i = zipInputStream.read(buffer, 0, 1024)) > -1){
                		fileoutputstream.write(buffer, 0, i);
                	}
                	fileoutputstream.close(); 
                	zipInputStream.closeEntry();
                }
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    
	public static void main(String[] args) {
		String kmzFile;
        String outputDir;

		if (args.length == 2){
			kmzFile = args[0];
	        outputDir = args[1];
    	}else{
    		kmzFile = "../pruebas/src/pruebas/input/0.countscompare.kmz";
            outputDir = "../pruebas/src/pruebas/output/";
    	}

		new KMZ_Extractor(kmzFile,outputDir).extractGraphs();
    }

}