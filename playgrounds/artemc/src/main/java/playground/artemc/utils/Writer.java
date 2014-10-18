package playground.artemc.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Writer {
	
	BufferedWriter writer;

	public void creteFile(String path){
		try
		{
			File file = new File(path);
			file.createNewFile();
			this.writer = new BufferedWriter(new FileWriter(file));
		}
		catch(FileNotFoundException e)
		{
			System.out.println("File Not Found");
			System.exit( 1 );
		}
		catch(IOException e)
		{
			System.out.println("something messed up");
			System.exit( 1 );
		}
	}
		
	
	public void writeLine(String data) 
	{

		try
		{
			writer.write(data);
			writer.newLine();
		}
			
		catch(FileNotFoundException e)
		{
			System.out.println("File Not Found");
			System.exit( 1 );
		}
		catch(IOException e)
		{
			System.out.println("something messed up");
			System.exit( 1 );
		}
	}
	
	
	public void close(){
		try
		{
			writer.close();
		}
			
		catch(FileNotFoundException e)
		{
			System.out.println("File Not Found");
			System.exit( 1 );
		}
		catch(IOException e)
		{
			System.out.println("something messed up");
			System.exit( 1 );
		}
	}
}