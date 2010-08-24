package playground.wrashid.PSF.matlab;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class MatlabExecutor {
	public static void main(String[] args) {
		try {
			// start matlab
			//Process proc = Runtime.getRuntime().exec("matlab.exe");
			
			// kill matlab
			//Process proc = Runtime.getRuntime().exec("taskkill /im matlab.exe /f");
			
			// start matlab script
			// path without the .m at the end
			Process proc= Runtime.getRuntime().exec("cmd.exe");
			BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream())); 
			PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(proc.getOutputStream())), true);
			
			String scriptPath="C:\\Documents and Settings\\wrashid\\My Documents\\MATLAB\\";
			String scriptName= "abc";
			out.println("c:");
			out.println("cd " + scriptPath);
			out.println("matlab -nodisplay -nojvm -r " + scriptName);
			out.flush();
			
			// TODO: wait until the output is available (or some file indicating it)
			
			// kill matlab 
			//Runtime.getRuntime().exec("taskkill /im matlab.exe /f");
			  
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
