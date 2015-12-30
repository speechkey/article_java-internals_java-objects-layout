package com.artemgrebenkin.java.internals;

import org.junit.BeforeClass;
import org.junit.Test;
import sun.misc.Unsafe;
import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class ObjectLayoutTest{
    private static Unsafe theUnsafe;

    @BeforeClass
    public static void setupUnsafe() throws NoSuchFieldException, IllegalAccessException {
        Field theUnsafeInstance = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafeInstance.setAccessible(true);
        theUnsafe = (Unsafe) theUnsafeInstance.get(Unsafe.class);
    }

    @Test
    public void printObjectHexFromMemory(){
        System.out.println();
        System.out.println("ExampleClass object layout:");
        ExampleClass obj = new ExampleClass(1147483647);
        System.out.println(" _____________________________________________________ ");
        System.out.println("| MARK                    | CLASS OOP   | FIELD       |");
        System.out.println("|-------------------------|-------------|-------------|");
        System.out.println("| " + toHexFromInt(theUnsafe.getInt(obj, 0L)) + toHexFromInt(theUnsafe.getInt(obj, 4L))  +
                "| " + toHexFromInt(theUnsafe.getInt(obj, 8L)) +
                "| " +  toHexFromInt(theUnsafe.getInt(obj, 12L)) + "|");
        System.out.println("|_________________________|_____________|_____________|");
        System.out.println();
    }

    @Test
    public void testIntDirectFromMemory(){
        ExampleClass obj = new ExampleClass(1147483647);
        int anInt = theUnsafe.getInt(obj, 12L);
        assertEquals(1147483647, anInt);
    }

    public static class ExampleClass {
        private int counter;

        public ExampleClass(int aCounter){
            this.counter = aCounter;
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