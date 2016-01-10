package com.artemgrebenkin.java.internals;

import static junit.framework.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

import sun.misc.Unsafe;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import javax.management.openmbean.CompositeDataSupport;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

public class ObjectLayoutTest{
    private static Unsafe theUnsafe;

    @BeforeClass
    public static void setupUnsafe() throws NoSuchFieldException, IllegalAccessException, MalformedObjectNameException, MBeanException, InstanceNotFoundException, ReflectionException {
        Field theUnsafeInstance = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafeInstance.setAccessible(true);
        theUnsafe = (Unsafe) theUnsafeInstance.get(Unsafe.class);

        checkJVMConfiguration();
    }

    @Test
    public void printObjectHexFromMemory(){
        ExampleIntClass exampleIntClass = new ExampleIntClass(1147483647);

        printExampleIntClassLayout(exampleIntClass);

        assertEquals(1147483647, theUnsafe.getInt(exampleIntClass, 12L));
    }

    @Test
    public void testIntDirectFromMemory(){
        int exampleIntClassPointer = 123123123;

        ExampleIntClass exampleIntClass = new ExampleIntClass(exampleIntClassPointer);
        ExampleClass exampleClass = new ExampleClass(321321321, exampleIntClass);

        printExampleClassLayout(exampleClass);

        Long exampleIntClassCOOP = theUnsafe.getLong(exampleClass, 16L);
        System.out.println("OOPAddress of exampleIntClass:    " + Long.toHexString(exampleIntClassCOOP));
        //Multiply by 8
        long exampleIntClassOOP = exampleIntClassCOOP << 3;
        System.out.println("NativeAddress of exampleIntClass: " + Long.toHexString(exampleIntClassOOP));

        assertEquals(exampleIntClassPointer, theUnsafe.getInt(exampleIntClassOOP + 12L));

    }

    private static void checkJVMConfiguration() throws MalformedObjectNameException, MBeanException, InstanceNotFoundException, ReflectionException {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        ObjectName mbean = new ObjectName("com.sun.management:type=HotSpotDiagnostic");

        if(! isObjectAlignment8(server, mbean) & isCompressedOops(server, mbean)) {
            throw new RuntimeException("Current JVM or JVM options are not supported. Use -XX:+UseCompressedOops and" +
                    "-XX:ObjectAlignmentInBytes=8 or JDK > 7 < 10.");
        }
    }

    private static boolean isObjectAlignment8(MBeanServer server, ObjectName mbean) throws InstanceNotFoundException, MBeanException, ReflectionException {
        CompositeDataSupport alignmentValue = (CompositeDataSupport) server.invoke(mbean, "getVMOption", new Object[]{"ObjectAlignmentInBytes"}, new String[]{"java.lang.String"});
        return Integer.valueOf(alignmentValue.get("value").toString()) == 8;
    }

    private static boolean isCompressedOops(MBeanServer server, ObjectName mbean) throws InstanceNotFoundException, MBeanException, ReflectionException {
        CompositeDataSupport compressedOopsValue = (CompositeDataSupport) server.invoke(mbean, "getVMOption",
                new Object[]{"UseCompressedOops"}, new String[]{"java.lang.String"});
        return Boolean.valueOf(compressedOopsValue.get("value").toString());
    }

    private void printExampleIntClassLayout(ExampleIntClass exampleIntClass) {
        System.out.println();
        System.out.println("ExampleIntClass object layout:");
        System.out.println();
        System.out.println(" _____________________________________________________ ");
        System.out.println("| MARK                    | CLASS OOP   | INT COUNTER |");
        System.out.println("|-------------------------|-------------|-------------|");
        System.out.println("| " + toHexFromInt(theUnsafe.getInt(exampleIntClass, 0L))
                                + toHexFromInt(theUnsafe.getInt(exampleIntClass, 4L))  +
                           "| " + toHexFromInt(theUnsafe.getInt(exampleIntClass, 8L)) +
                           "| " + toHexFromInt(theUnsafe.getInt(exampleIntClass, 12L)) + "|");
        System.out.println("|_________________________|_____________|_____________|");
        System.out.println();
    }

    private void printExampleClassLayout(ExampleClass exampleClass) {
        System.out.println();
        System.out.println("ExampleIntClass object layout:");
        System.out.println();
        System.out.println(" __________________________________________________________________________ ");
        System.out.println("| MARK                    | CLASS OOP   | COUNTER     | EXAMPLECOUNTER OOP |");
        System.out.println("|-------------------------|-------------|-------------|--------------------|");
        System.out.println("| " + toHexFromInt(theUnsafe.getInt(exampleClass, 0L))
                + toHexFromInt(theUnsafe.getInt(exampleClass, 4L))  +
                "| " + toHexFromInt(theUnsafe.getInt(exampleClass, 8L)) +
                "| " + toHexFromInt(theUnsafe.getInt(exampleClass, 12L)) +
                "| " + toHexFromInt(theUnsafe.getInt(exampleClass, 16L)) + "       |");
        System.out.println("|_________________________|_____________|_____________|____________________|");
        System.out.println();
    }

    public static class ExampleIntClass {
        private int counter;

        public ExampleIntClass(int cnt){
            this.counter = cnt;
        }
    }

    public static class ExampleClass {
        private int counter;
        private ExampleIntClass exampleCnt;

        public ExampleClass(int cnt, ExampleIntClass xplCnt){
            this.counter = cnt;
            this.exampleCnt = xplCnt;
        }
    }

    private String toHexFromInt(int x) {
        //Integer.toHexString(0); -> 0 but int is 4 byte, so fill lost bits by 0 -> 00 00 00 00
        //Integer.toHexString(127); -> 7f -> 00 00 00 7f
        String s = Integer.toHexString(x);
        int deficit = 8 - s.length();
        for (int c = 0; c < deficit; c++) {
            s = "0" + s;
        }
        return prettifyHex(s);
    }

    private static String prettifyHex(String hex) {
        int newLenght = hex.length() / 2 + hex.length();
        char[] prettifiedHex = new char[newLenght];

        char[] chars = hex.toCharArray();

        for (int i = 0, k = 0; i < chars.length; i=i+2, k=k+3) {
            prettifiedHex[k] = chars[i];
            prettifiedHex[k+1] = chars[i+1];
            prettifiedHex[k+2] = ' ';
        }
        return String.valueOf(prettifiedHex);
    }
}