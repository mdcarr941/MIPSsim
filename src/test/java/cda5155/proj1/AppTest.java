package cda5155.proj1;

import java.util.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
//import java.nio.file.Path;
//import java.io.BufferedReader;
//import java.io.PrintWriter;
//import java.io.CharArrayWriter;

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

    public static void testString2word() {
        assertEquals(0, Memory.string2word("00000000000000000000000000000000"));
        assertEquals(1, Memory.string2word("00000000000000000000000000000001"));
        assertEquals(1 << 31, Memory.string2word("10000000000000000000000000000000"));
        assertEquals(-1, Memory.string2word("11111111111111111111111111111111"));
        assertEquals(1 << 27, Memory.string2word("00001000000000000000000000000000"));
        assertEquals(1 << 27 | 1 << 3, Memory.string2word("00001000000000000000000000001000"));
        assertEquals(-2, Memory.string2word("11111111111111111111111111111110"));
    }

    public static void testWord2string() {
        assertEquals("00000000000000000000000000000000", Memory.word2string(0));
        assertEquals("00000000000000000000000000000001", Memory.word2string(1));
        assertEquals("10000000000000000000000000000000", Memory.word2string(1 << 31));
        assertEquals("11111111111111111111111111111111", Memory.word2string(-1));
        assertEquals("00001000000000000000000000000000", Memory.word2string(1 << 27));
        assertEquals("00001000000000000000000000001000", Memory.word2string(1 << 27 | 1 << 3));
        assertEquals("11111111111111111111111111111110", Memory.word2string(-2));
    }

    public static void assertMemoryEqual(Memory memory, int[] contents) {
        for (int k = 0; k < contents.length; ++k) {
            assertEquals(memory.fetch(4 * k + 256), contents[k]);
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
            new Memory(splitLines("00000000000000000000000000000000")),
            new int[] {0}
        );
        assertMemoryEqual(
            new Memory(splitLines("00000000000000000000000000000001")),
            new int[] {1}
        );
        assertMemoryEqual(
            new Memory(splitLines("00000001000000010000000000000000")),
            new int[] {1 << 24 | 1 << 16}
        );
        assertMemoryEqual(
            new Memory(splitLines(String.join("\n",
                "00000000000000000000000000000001",
                "00000000000000000000000000000010",
                "00000000000000000000000000000011"
            ))),
            new int[] {1, 2, 3}
        );
        assertMemoryEqual(
            new Memory(splitLines(String.join("\n",
                "00000000000000000000000000000000",
                "00000000000000000000000000000001",
                "10000000000000000000000000000000",
                "11111111111111111111111111111111",
                "11111111111111111111111111111110"
            ))),
            new int[] {0, 1, 1 << 31, -1, -2}
        );
    }

    public void testGetFirst3Bits() {
        assertEquals(0, Instruction.getFirst3Bits(0));
        assertEquals(1, Instruction.getFirst3Bits(1 << 29));
        assertEquals(2, Instruction.getFirst3Bits(1 << 30));
        assertEquals(3, Instruction.getFirst3Bits(1 << 30 | 1 << 29));
        assertEquals(4, Instruction.getFirst3Bits(1 << 31));
        assertEquals(5, Instruction.getFirst3Bits(1 << 31 | 1 << 29));
        assertEquals(6, Instruction.getFirst3Bits(1 << 31 | 1 << 30));
        assertEquals(7, Instruction.getFirst3Bits(1 << 31 | 1 << 30 | 1 << 29));
    }

    public void testGetSecond3Bits() {
        assertEquals(0, Instruction.getSecond3Bits(0));
        assertEquals(1, Instruction.getSecond3Bits(1 << 26));
        assertEquals(2, Instruction.getSecond3Bits(1 << 27));
        assertEquals(3, Instruction.getSecond3Bits(1 << 27 | 1 << 26));
        assertEquals(4, Instruction.getSecond3Bits(1 << 28));
        assertEquals(5, Instruction.getSecond3Bits(1 << 28 | 1 << 26));
        assertEquals(6, Instruction.getSecond3Bits(1 << 28 | 1 << 27));
        assertEquals(7, Instruction.getSecond3Bits(1 << 28 | 1 << 27 | 1 << 26));
    }

    public void testGetInstType() {
        assertEquals(InstType.J, Instruction.getInstType(0));
        assertEquals(InstType.BEQ, Instruction.getInstType(1 << 26));
        assertEquals(InstType.BNE, Instruction.getInstType(2 << 26));
        assertEquals(InstType.BGTZ, Instruction.getInstType(3 << 26));
        assertEquals(InstType.SW, Instruction.getInstType(4 << 26));
        assertEquals(InstType.LW, Instruction.getInstType(5 << 26));
        assertEquals(InstType.BREAK, Instruction.getInstType(6 << 26));
        assertEquals(InstType.ADD, Instruction.getInstType(1 << 29));
        assertEquals(InstType.SUB, Instruction.getInstType(1 << 29 | 1 << 26));
        assertEquals(InstType.AND, Instruction.getInstType(1 << 29 | 2 << 26));
        assertEquals(InstType.OR, Instruction.getInstType(1 << 29 | 3 << 26));
        assertEquals(InstType.SRL, Instruction.getInstType(1 << 29 | 4 << 26));
        assertEquals(InstType.SRA, Instruction.getInstType(1 << 29 | 5 << 26));
        assertEquals(InstType.ADDI, Instruction.getInstType(2 << 29));
        assertEquals(InstType.ANDI, Instruction.getInstType(2 << 29 | 1 << 26));
        assertEquals(InstType.ORI, Instruction.getInstType(2 << 29 | 2 << 26));
        assertEquals(InstType.MULT, Instruction.getInstType(3 << 29));
        assertEquals(InstType.DIV, Instruction.getInstType(3 << 29 | 1 << 26));
        assertEquals(InstType.MFHI, Instruction.getInstType(4 << 29));
        assertEquals(InstType.MFLO, Instruction.getInstType(4 << 29 | 1 << 26));
    }

    public void testInstCat2() {
        assertEquals("00100000001000000000000000000000\t256\tADD R1, R0, R0",
            Instruction.decode(256, "00100000001000000000000000000000").toString()
        );
    }

    public void testInstCat3() {
        assertEquals("01000000101001010000000000001100\t292\tADDI R5, R5, #12",
            Instruction.decode(292, "01000000101001010000000000001100").toString()
        );
        assertEquals("01000000101001011000000000000000\t292\tADDI R5, R5, #-32768",
            Instruction.decode(292, "01000000101001011000000000000000").toString()
        );
    }

    public void testInstCat4() {
        assertEquals("01100000011001000000000000000000\t280\tMULT R3, R4",
            Instruction.decode(280, "01100000011001000000000000000000").toString()
        );
    }

    public void testInstCat5() {
        assertEquals("10000100101000000000000000000000\t284\tMFLO R5",
            Instruction.decode(284, "10000100101000000000000000000000").toString()
        );
    }

    public void testArraysCopyRange() {
        int[] vals = new int[] {1, 2, 3};
        int[] newVals = Arrays.copyOfRange(vals, 1, 2);
        assertEquals(1, newVals.length);
        assertEquals(2, newVals[0]);
    }

    public void testInstJ() {
        assertEquals("00000000000000000000000001000011\t308\tJ #268",
            Instruction.decode(308, "00000000000000000000000001000011").toString()
        );
    }

    public void testInstBranchCmpr() {
        InstBranchCmpr inst;
        inst = (InstBranchCmpr)Instruction.decode(268, "00000100001000100000000000001010");
        assertEquals(312, inst.target());
        assertEquals("00000100001000100000000000001010\t268\tBEQ R1, R2, #40",
            inst.toString()
        );
        inst = (InstBranchCmpr)Instruction.decode(268, "00000100011000101111111111111111");
        assertEquals(268, inst.target());
        assertEquals("00000100011000101111111111111111\t268\tBEQ R3, R2, #-4",
            inst.toString()
        );
        inst = (InstBranchCmpr)Instruction.decode(260, "00001000101000100000000000001011");
        assertEquals(308, inst.target());
        assertEquals("00001000101000100000000000001011\t260\tBNE R5, R2, #44",
            inst.toString()
        );
    }

    public void testInstLoadStore() {
        assertEquals("00010000110001010000000101011100\t296\tSW R5, 348(R6)",
            Instruction.decode(296, "00010000110001010000000101011100").toString()
        );
        assertEquals("00010100110000110000000100111100\t272\tLW R3, 316(R6)",
            Instruction.decode(272, "00010100110000110000000100111100").toString()
        );
    }

    public void testInstBGTZ() {
        assertEquals("00001100101000000000000000000001\t288\tBGTZ R5, #4",
            Instruction.decode(288, "00001100101000000000000000000001").toString()
        );
    }

    public void testInstBREAK() {
        assertEquals("00011000000000000000000000000000\t312\tBREAK",
            Instruction.decode(312, "00011000000000000000000000000000").toString()
        );
    }

    public String changeLineSep(String input) {
        return input.replaceAll("\r\n", MIPSsim.LINE_SEP);
    }

    public String getFileContents(String pathString) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(pathString));
        StringBuilder builder = new StringBuilder();
        int end = lines.size();
        for (int k = 0; k < end; ++k) {
            builder.append(lines.get(k) + '\n');
        }
        return changeLineSep(builder.toString()).trim();
    }

    public void testMemoryDisassemble() throws IOException {
        String expectedDisassembly = getFileContents("sample_disassembly.txt");
        Memory memory = new Memory("sample.txt");
        String disassembly = memory.disassemble();
        MIPSsim.writeDisassembly(disassembly);
        assertEquals(expectedDisassembly, disassembly);
    }

    public void testProcessorSimulate() throws IOException {
        String expected = getFileContents("sample_simulation.txt");
        Processor proc = new Processor("sample.txt");
        String simulation = proc.simulate();
        MIPSsim.writeSimulation(simulation);
        assertEquals(expected, simulation);
    }

    public void testOtherSampleDisassembly() throws IOException {
        String expected = getFileContents("other_disassembly.txt");
        Memory memory = new Memory("other_sample.txt");
        String disassembly = memory.disassemble();
        MIPSsim.write2file(disassembly, "other_disassembly_mine.txt");
        assertEquals(expected, disassembly);
    }

    public void testOtherSampleSimulation() throws IOException {
        String expected = getFileContents("other_simulation.txt");
        Processor proc = new Processor("other_sample.txt");
        String simulation = proc.simulate();
        MIPSsim.write2file(simulation, "other_simulation_mine.txt");
        //assertEquals(expected, simulation);
    }
}
