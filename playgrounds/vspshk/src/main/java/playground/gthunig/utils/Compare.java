package playground.gthunig.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Compare
{
    //This can be any folder locations which you want to compare
    File dir1 = new File("C:\\Users\\gthunig\\VSP\\runs-svn\\berlin_scenario_2016\\be_117j\\compare\\analysis_300_car");
    File dir2 = new File("C:\\Users\\gthunig\\VSP\\runs-svn\\berlin_scenario_2016\\be_117j\\compare\\analysis2_300_car");
    public static void main(String ...args)
    {
        Compare compare = new Compare();
        try
        {
            compare.getDiff(compare.dir1,compare.dir2);
        }
        catch(IOException ie)
        {
            ie.printStackTrace();
        }
    }

    public void getDiff(File dirA, File dirB) throws IOException
    {
        File[] fileList1 = dirA.listFiles();
        File[] fileList2 = dirB.listFiles();
        Arrays.sort(fileList1);
        Arrays.sort(fileList2);
        HashMap<String, File> map1;
        if(fileList1.length < fileList2.length)
        {
            map1 = new HashMap<String, File>();
            for(int i=0;i<fileList1.length;i++)
            {
                map1.put(fileList1[i].getName(),fileList1[i]);
            }

            compareNow(fileList2, map1);
        }
        else
        {
            map1 = new HashMap<String, File>();
            for(int i=0;i<fileList2.length;i++)
            {
                map1.put(fileList2[i].getName(),fileList2[i]);
            }
            compareNow(fileList1, map1);
        }
    }

    public void compareNow(File[] fileArr, HashMap<String, File> map) throws IOException
    {
        for(int i=0;i<fileArr.length;i++)
        {
            String fName = fileArr[i].getName();
            File fComp = map.get(fName);
            map.remove(fName);
            if(fComp!=null)
            {
                if(fComp.isDirectory())
                {
                    getDiff(fileArr[i], fComp);
                }
                else
                {
                    String cSum1 = checksum(fileArr[i]);
                    String cSum2 = checksum(fComp);
                    if(!cSum1.equals(cSum2))
                    {
                        System.out.println(fileArr[i].getName()+"\t\t"+ "different");
                    }
                    else
                    {
                        System.out.println(fileArr[i].getName()+"\t\t"+"identical");
                    }
                }
            }
            else
            {
                if(fileArr[i].isDirectory())
                {
                    traverseDirectory(fileArr[i]);
                }
                else
                {
                    System.out.println(fileArr[i].getName()+"\t\t"+"only in "+fileArr[i].getParent());
                }
            }
        }
        Set<String> set = map.keySet();
        Iterator<String> it = set.iterator();
        while(it.hasNext())
        {
            String n = it.next();
            File fileFrmMap = map.get(n);
            map.remove(n);
            if(fileFrmMap.isDirectory())
            {
                traverseDirectory(fileFrmMap);
            }
            else
            {
                System.out.println(fileFrmMap.getName() +"\t\t"+"only in "+ fileFrmMap.getParent());
            }
        }
    }

    public void traverseDirectory(File dir)
    {
        File[] list = dir.listFiles();
        for(int k=0;k<list.length;k++)
        {
            if(list[k].isDirectory())
            {
                traverseDirectory(list[k]);
            }
            else
            {
                System.out.println(list[k].getName() +"\t\t"+"only in "+ list[k].getParent());
            }
        }
    }

    public String checksum(File file)
    {
        try
        {
            InputStream fin = new FileInputStream(file);
            java.security.MessageDigest md5er = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int read;
            do
            {
                read = fin.read(buffer);
                if (read > 0)
                    md5er.update(buffer, 0, read);
            } while (read != -1);
            fin.close();
            byte[] digest = md5er.digest();
            if (digest == null)
                return null;
            String strDigest = "0x";
            for (int i = 0; i < digest.length; i++)
            {
                strDigest += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1).toUpperCase();
            }
            return strDigest;
        }
        catch (Exception e)
        {
            return null;
        }
    }
}