package playground.michalm.util;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;


/**
 * @author michalm
 */
public class ParameterFileReader
{
    public static interface ParameterProcessor
    {
        void processParameter(String name, String value);
    }


    public static void readParameters(String file, final ParameterProcessor processor)
    {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                StringTokenizer st = new StringTokenizer(line);

                if (st.hasMoreTokens()) {
                    String key = st.nextToken();

                    if (key.charAt(0) != '#') {
                        String value = line.substring(key.length()).trim();
                        processor.processParameter(key, value);
                    }
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static Map<String, String> readParametersToMap(String file)
    {
        final Map<String, String> map = new HashMap<>();

        readParameters(file, new ParameterProcessor() {
            public void processParameter(String name, String value)
            {
                map.put(name, value);
            }
        });

        return map;
    }


    public static class ReflectionParameterProcessor
        implements ParameterProcessor
    {
        private Object object;
        private Class<?> clazz;


        public ReflectionParameterProcessor(Object object)
        {
            if (object instanceof Class) {
                this.object = null;
                this.clazz = (Class<?>)object;
            }
            else {
                this.object = object;
                this.clazz = object.getClass();
            }
        }


        public void processParameter(String name, String value)
        {
            Field field = null;

            try {
                field = clazz.getField(name);
            }
            catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }

            Class<?> type = field.getType();

            try {
                if (int.class.equals(type)) {
                    field.setInt(object, Integer.parseInt(value));
                }
                else if (double.class.equals(type)) {
                    field.setDouble(object, Double.parseDouble(value));
                }
                else if (boolean.class.equals(type)) {
                    field.setBoolean(object, value.charAt(0) != '0');
                }
                else if (String.class.equals(type)) {
                    field.set(object, value);
                }
                else {
                    throw new RuntimeException("Unhandled type: " + type);
                }
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    public static void readParametersByReflection(String file, Object object)
    {
        readParameters(file, new ReflectionParameterProcessor(object));
    }
}
