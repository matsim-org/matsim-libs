/*
 * Created on Jul 24, 2004
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex.expansions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * @author henkel
 */
class BibtexPersonListParserTests {

    public static class Test {
        private final String string;
        private final String preLast;
        private final String last;
        private final String lineage;
        private final String first;

        public Test(String string,String preLast, String last, String lineage, String first){
            this.string = string;
            this.preLast = preLast;
            this.last = last;
            this.lineage = lineage;
            this.first = first;
        }
        
        
        /**
         * @return Returns the first.
         */
        public String getFirst() {
            return first;
        }
        /**
         * @return Returns the last.
         */
        public String getLast() {
            return last;
        }
        /**
         * @return Returns the lineage.
         */
        public String getLineage() {
            return lineage;
        }
        /**
         * @return Returns the preLast.
         */
        public String getPreLast() {
            return preLast;
        }
        /**
         * @return Returns the string.
         */
        public String getString() {
            return string;
        }
    }
    
    public static Test [] tests;
    
    static {
        try {
            BufferedReader in = 
                new BufferedReader(new FileReader("/home/machine/henkel/projects/26_javabib/personparsing/RESULTS.txt"));
            String line;
            ArrayList testsAsList = new ArrayList();
            int count=0;
            while((line=in.readLine())!=null){
                System.out.print("."); count++; count%=80; if(count==0) System.out.println();
                String components [] = line.split("\\|");
                for(int i=0;i<components.length;i++){
                    components[i]=components[i].replace('~',' ');
                    if(components[i].equals("")) components[i]=null;
                    
                }
                if(components.length!=6){
                    System.err.println("\nError parsing "+line);
                    continue;
                }
                testsAsList.add(new Test(
                  components[0],
                  components[2],
                  components[3],
                  components[4],
                  components[1]
                ));
            }
            tests = new Test[testsAsList.size()];
            testsAsList.toArray(tests);
        } catch(Exception e){
            e.printStackTrace();
        }
    }
    
}
