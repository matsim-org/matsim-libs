/*
 * Created on Jul 26, 2004
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexFile;
import bibtex.dom.BibtexString;
import bibtex.parser.BibtexParser;
import bibtex.parser.ParseException;

/**
 * This is just for demonstrating how the parser works: the main method of this
 * class parses a bibtex file and prints the values of all author fields.
 * @author henkel
 */
public class ExtractAuthors {
    
    public static void main(String args[]){
        if(args.length!=1){
            System.err.println("usage: ExtractAuthors <bibtexFile>");
            return;
        }
        
        try {
            FileReader in = new FileReader(args[0]);
            BibtexParser parser = new BibtexParser(false);
            BibtexFile file=new BibtexFile();
            parser.parse(file,in);
            for(Iterator it=file.getEntries().iterator();it.hasNext();){
                Object potentialEntry = it.next();
                if(!(potentialEntry instanceof BibtexEntry)) continue;
                BibtexEntry entry = (BibtexEntry) potentialEntry;
                BibtexString authorString=(BibtexString) entry.getFieldValue("author");
                if(authorString==null) continue;
                String content = authorString.getContent();
                String tokens[] = content.split("\\s++");
                for(int i=0;i<tokens.length;i++){
                    if(tokens[i].toLowerCase().equals("and")) { System.out.println(); continue; }
                    else if(tokens[i].toLowerCase().equals("others")) continue;
                    System.out.print(tokens[i]+" ");
                }
                System.out.println();
            }
        } catch (FileNotFoundException e) {
            System.err.println("File '"+args[0]+"' not found.");
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}