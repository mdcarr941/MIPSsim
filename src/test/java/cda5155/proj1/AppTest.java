package cda5155.proj1;

import java.util.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    public void testString2Byte() {
        assertEquals(App.string2Byte("00000000"), 0);
        assertEquals(App.string2Byte("00000001"), 1);
        assertEquals(App.string2Byte("00000010"), 2);
        assertEquals(App.string2Byte("00000011"), 3);
        assertEquals(App.string2Byte("01000011"), 67);
        assertEquals(App.string2Byte("10000000"), -128);
        assertEquals(App.string2Byte("10100111"), -89); // -01011000 -> -01011001 -> -89
        assertEquals(App.string2Byte("11111111"), -1); // -00000000 -> -00000001 -> -1
    }

    public static void testByte2String() {
        assertEquals(App.byte2String((byte)0), "00000000");
        assertEquals(App.byte2String((byte)1), "00000001");
        assertEquals(App.byte2String((byte)2), "00000010");
        assertEquals(App.byte2String((byte)3), "00000011");
        assertEquals(App.byte2String((byte)67), "01000011");
        assertEquals(App.byte2String((byte)-128), "10000000");
        assertEquals(App.byte2String((byte)-89), "10100111");
        assertEquals(App.byte2String((byte)-1), "11111111");
    }

    public static void assertMemoryEqual(byte[] memory, byte[] content) {
        assertEquals(memory.length - 256, content.length);
        for (int k = 0; k < content.length; ++k) {
            assertEquals(memory[256 + k], content[k]);
        }
    }

    public static List<String> splitLines(String input) {
        StringTokenizer tokenizer = new StringTokenizer(input, "\n\r");
        int len = tokenizer.countTokens();
        List<String> output = new ArrayList<String>(len);
        for (int k = 0; k < len; ++k) {
            output.add(tokenizer.nextToken());
        }
        return output;
    }  

    public void testCreateMemory() {
        assertMemoryEqual(
            App.createMemory(splitLines("00000000000000000000000000000000")),
            new byte[] {0, 0, 0, 0}
        );
        assertMemoryEqual(
            App.createMemory(splitLines("00000000000000000000000000000001")),
            new byte[] {0, 0, 0, 1}
        );
        assertMemoryEqual(
            App.createMemory(splitLines("00000001000000000000000000000000")),
            new byte[] {1, 0, 0, 0}
        );
        assertMemoryEqual(
            App.createMemory(splitLines("00000001000000010000000000000000")),
            new byte[] {1, 1, 0, 0}
        );
        assertMemoryEqual(
            App.createMemory(splitLines("00000000000000010000000100000000")),
            new byte[] {0, 1, 1, 0}
        );
        assertMemoryEqual(
            App.createMemory(splitLines("00000001000000010000000000000000\n00000000000000010000000100000000")),
            new byte[] {1, 1, 0, 0, 0, 1, 1, 0}
        );
        assertMemoryEqual(
            App.createMemory(splitLines(String.join("\n",
                "00100000" + "00100000" + "00000000" + "00000000",
                "00100000" + "11000000" + "00000000" + "00000000",
                "01000000" + "01000000" + "00000000" + "00000011",
                "00000100" + "00100010" + "00000000" + "00001010"
            ))),
            new byte[] {
                32, 32, 0, 0,
                32, -64, 0, 0,
                64, 64, 0, 3,
                4, 34, 0, 10
            }
        );
    }

    public void testGetInstType() {
        assertEquals(InstType.J, Instruction.getInstType(0));
        assertEquals(InstType.BEQ, Instruction.getInstType(1 << 2));
        assertEquals(InstType.BNE, Instruction.getInstType(2 << 2));
        assertEquals(InstType.BGTZ, Instruction.getInstType(3 << 2));
        assertEquals(InstType.SW, Instruction.getInstType(4 << 2));
        assertEquals(InstType.LW, Instruction.getInstType(5 << 2));
        assertEquals(InstType.BREAK, Instruction.getInstType(6 << 2));
        assertEquals(InstType.ADD, Instruction.getInstType(1 << 5));
        assertEquals(InstType.SUB, Instruction.getInstType(1 << 5 | 1 << 2));
        assertEquals(InstType.AND, Instruction.getInstType(1 << 5 | 2 << 2));
        assertEquals(InstType.OR, Instruction.getInstType(1 << 5 | 3 << 2));
        assertEquals(InstType.SRL, Instruction.getInstType(1 << 5 | 4 << 2));
        assertEquals(InstType.SRA, Instruction.getInstType(1 << 5 | 5 << 2));
        assertEquals(InstType.ADDI, Instruction.getInstType(1 << 6));
        assertEquals(InstType.ANDI, Instruction.getInstType(1 << 6 | 1 << 2));
        assertEquals(InstType.ORI, Instruction.getInstType(1 << 6 | 2 << 2));
        assertEquals(InstType.MULT, Instruction.getInstType(3 << 5));
        assertEquals(InstType.DIV, Instruction.getInstType(3 << 5 | 1 << 2));
        assertEquals(InstType.MFHI, Instruction.getInstType(1 << 7));
        assertEquals(InstType.MFLO, Instruction.getInstType(1 << 7 | 1 << 2));
    }

    public void testByteIndex() {
        char[] chars = new char[] {'a', 'b', 'd', 'l'};
        byte index = 0;
        assertEquals('a', chars[index]);
        index = 2;
        assertEquals('d', chars[index]);
    }

    public void testInstToString() {
        assertEquals("00100000001000000000000000000000\t256\tADD R1, R0, R0",
            Instruction.decode(256, "00100000001000000000000000000000")
        );
    }

    public void testInstCat2() {

    }
}
