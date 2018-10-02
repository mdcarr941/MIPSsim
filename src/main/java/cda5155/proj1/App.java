package cda5155.proj1;

import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.io.IOException;
import java.nio.file.*;

enum InstType {
    J, BEQ, BNE, BGTZ, SW, LW, BREAK,
    ADD, SUB, AND, OR, SRL, SRA,
    ADDI, ANDI, ORI,
    MULT, DIV,
    MFHI, MFLO
}

abstract class Instruction {
    protected int _address;
    int address() {
        return _address;
    }

    protected int _word;
    int word() {
        return _word;
    }

    protected InstType _type;
    InstType type() {
        return _type;
    }

    public Instruction(int address, int word) throws IllegalArgumentException {
        this._address = address;
        this._word = word;
    }

    public static int getFirst3Bits(int word) {
        return (word & 0xE0000000) >>> 29;
    }

    public static int getSecond3Bits(int word) {
        return (word & 0x1C000000) >>> 26;
    }

    public static Instruction decode(int address, int word) throws IllegalArgumentException {
        switch (getFirst3Bits(word)) {
            case 0:
                return InstCat1.decode(address, word);
            case 1:
                return new InstCat2(address, word);
            case 2:
                return new InstCat3(address, word);
            case 3:
                return new InstCat4(address, word);
            case 4:
                return new InstCat5(address, word);
            default:
                throw new IllegalArgumentException("Argument must be between 0 and 4 inclusive.");
        }
    }

    // Convenience method for testing.
    public static Instruction decode(int address, String wordString) throws IllegalArgumentException {
        return decode(address, Memory.string2word(wordString));
    }

    public static InstType getInstType(int word) throws IllegalArgumentException {
        switch (getFirst3Bits(word)) {
            case 0:
                return InstCat1.getInstType(word);
            case 1:
                return InstCat2.getInstType(word);
            case 2:
                return InstCat3.getInstType(word);
            case 3:
                return InstCat4.getInstType(word);
            case 4:
                return InstCat5.getInstType(word);
            default:
                throw new IllegalArgumentException("Argument must be between 0 and 4 inclusive.");
        }
    }

    public static int getFirstArg(int word) {
        return (word & 0x03E00000) >> 21;
    }

    public static int getSecondArg(int word) {
        return (word & 0x001F0000) >> 16;
    }

    public static int getThirdArg(int word) {
        return (word & 0x0000F800) >> 11;
    }

    public abstract String disassemble();

    public String toString() {
        return String.join("\t", Memory.word2string(_word), Integer.toString(_address), disassemble());
    }
}

abstract class InstCat1 extends Instruction {
    public InstCat1(int address, int word) {
        super(address, word);
        _type = getInstType(word);
    }

    public static InstType getInstType(int word) throws IllegalArgumentException {
        switch (getSecond3Bits(word)) {
            case 0:
                return InstType.J;
            case 1:
                return InstType.BEQ;
            case 2:
                return InstType.BNE;
            case 3:
                return InstType.BGTZ;
            case 4:
                return InstType.SW;
            case 5:
                return InstType.LW;
            case 6:
                return InstType.BREAK;
            default:
                throw new IllegalArgumentException("Value of argument must be between 0 and 6 inclusive.");
        }
    }

    public String toString() {
        throw new UnsupportedOperationException("InstCat1 cannot be converted to a string.");
    }
}

class InstJ extends InstCat1 {
    public InstJ(int address, int word) {
        super(address, word);
    }

    public String disassemble() {
        return "J";
    }
}

class InstCat2 extends Instruction {
    protected int _dest;
    int dest() {
        return _dest;
    }

    protected int _src1;
    int src1() {
        return _src1;
    }

    protected int _src2;
    int src2() {
        return _src2;
    }

    public InstCat2(int address, int word) {
        super(address, word);
        _type = getInstType(word);
        _dest = getFirstArg(word);
        _src1 = getSecondArg(word);
        _src2 = getThirdArg(word);
    }

    public static InstType getInstType(int word) throws IllegalArgumentException {
        switch (getSecond3Bits(word)) {
            case 0:
                return InstType.ADD;
            case 1:
                return InstType.SUB;
            case 2:
                return InstType.AND;
            case 3:
                return InstType.OR;
            case 4:
                return InstType.SRL;
            case 5:
                return InstType.SRA;
            default:
                throw new IllegalArgumentException("Value of argument must be between 0 and 5 inclusive.");
        }
    }

    public String disassemble() {
        String typeString;
        switch (_type) {
            case ADD:
                typeString = "ADD";
                break;
            case SUB:
                typeString = "SUB";
                break;
            case AND:
                typeString = "AND";
                break;
            case OR:
                typeString = "OR";
                break;
            case SRL:
                typeString = "SRL";
                break;
            case SRA:
                typeString = "SRA";
                break;
            default:
                throw new UnknownError("Invalid instruction type for category 2: " + _type);
        }
        return String.format("%s R%d, R%d, R%d", typeString, _dest, _src1, _src2);
    }
}

class InstCat3 extends Instruction {
    protected int _dest;
    int dest() {
        return _dest;
    }

    protected int _src;
    int src() {
        return _src;
    }

    protected short _imm;
    short imm() {
        return _imm;
    }

    public InstCat3(int address, int word) {
        super(address, word);
        _type = getInstType(word);
        _dest = getFirstArg(word);
        _src = getSecondArg(word);
        _imm = getImmediateVal(word);
    }

    public static short getImmediateVal(int word) {
        return (short)(word & 0x0000FFFF);
    }

    public static InstType getInstType(int word) throws IllegalArgumentException {
        switch (getSecond3Bits(word)) {
            case 0:
                return InstType.ADDI;
            case 1:
                return InstType.ANDI;
            case 2:
                return InstType.ORI;
            default:
                throw new IllegalArgumentException("Value of argument must be between 0 and 2 inclusive.");
        }
    }

    public String disassemble() {
        String typeString;
        switch (_type) {
            case ADDI:
                typeString = "ADDI";
                break;
            case ANDI:
                typeString = "ANDI";
                break;
            case ORI:
                typeString = "ORI";
                break;
            default:
                throw new UnknownError("Invalid instruction type for category 3: " + _type);
        }
        return String.format("%s R%d, R%d, #%d", typeString, _dest, _src, _imm);
    }
}

class InstCat4 extends Instruction {
    protected int _src1;
    int src1() {
        return _src1;
    }

    protected int _src2;
    int src2() {
        return _src2;
    }

    public InstCat4(int address, int word) {
        super(address, word);
        _type = getInstType(word);
        _src1 = getFirstArg(word);
        _src2 = getSecondArg(word);
    }

    public static InstType getInstType(int word) throws IllegalArgumentException {
        switch (getSecond3Bits(word)) {
            case 0:
                return InstType.MULT;
            case 1:
                return InstType.DIV;
            default:
                throw new IllegalArgumentException("Value of argument must be either 0 or 1.");
        }
    }

    public String disassemble() {
        String typeString;
        switch (_type) {
            case MULT:
                typeString = "MULT";
                break;
            case DIV:
                typeString = "DIV";
                break;
            default:
                throw new UnknownError("Invalid instruction type for category 4: " + _type);
        }
        return String.format("%s R%d, R%d", typeString, _src1, _src2);
    }
}

class InstCat5 extends Instruction {
    protected int _dest;
    int dest() {
        return _dest;
    }

    public InstCat5(int address, int word) {
        super(address, word);
        _type = getInstType(word);
        _dest = getFirstArg(word);
    }

    public static InstType getInstType(int word) throws IllegalArgumentException {
        switch (getSecond3Bits(word)) {
            case 0:
                return InstType.MFHI;
            case 1:
                return InstType.MFLO;
            default:
                throw new IllegalArgumentException("Value of argument must be either 0 or 1.");
        }
    }

    public String disassemble() {
        String typeString;
        switch (_type) {
            case MFHI:
                typeString = "MFHI";
                break;
            case MFLO:
                typeString = "MFLO";
                break;
            default:
                throw new UnknownError("Invalid instruction type for category 5: " + _type);
        }
        return String.format("%s R%d", typeString, _dest);
    }
}

class Memory {
    protected int[] _words;

    protected static final int _min_addr = 256;

    public int min_addr() {
        return _min_addr;
    }

    protected int _max_addr;

    public int max_addr() {
        return _max_addr;
    }

    public Memory(List<String> lines) {
        _words = new int[lines.size()];
        _max_addr = 4 * _words.length + _min_addr;
        for (int k = 0; k < _words.length; ++k) {
            _words[k] = string2word(lines.get(k));
        }
    }

    public static int string2word(String intString) {
        int word = 0;
        for (int k = 0; k < 32; ++k) {
            if (intString.charAt(k) == '1') word |= 1 << (31-k);
        }
        return word;
    }

    public static String word2string(int word) {
        char[] chars = new char[32];
        for (int k = 0; k < 32; ++k) {
            if ( (word & (1 << (31-k))) == 0) chars[k] = '0';
            else chars[k] = '1';
        }
        return new String(chars);
    }

    public String toString() {
        StringBuffer buff = new StringBuffer(33 * _words.length);
        for (int k = 0; k < _words.length; ++k) {
            buff.append(word2string(_words[k]) + '\n');
        }
        return buff.toString();        
    }

    public static int getIndex(int addr) {
        return (addr - _min_addr) / 4;
    }

    public int fetch(int addr) throws IllegalArgumentException {
        try {
            return _words[getIndex(addr)];
        }
        catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                String.format("must have %d <= addr <= %d", _min_addr, _max_addr)
            );
        }
    }
}

class Processor {
    public static Instruction fetch(Memory memory, int pc) {
        return Instruction.decode(pc, memory.fetch(pc));
    }

    public static long decodeLong(byte[] memory, int addr) {
        return (long)(memory[addr] << 24 | memory[addr+1] << 16 | memory[addr+2] << 8 | memory[addr+3]);
    }

    public static void run(Memory memory, File dissembly, File simulation) {
        Instruction inst;
        int pc, max_addr = memory.max_addr();
        for (pc = 256; pc < max_addr; pc += 4) {
            inst = fetch(memory, pc);
            if (inst.type() == InstType.BREAK) break;
        }
    }
}

public class App {
    public static void main(String[] args) throws IOException {
        Path path;
        if (args.length >= 2) {
            path = Paths.get(args[1]);
        }
        else {
            path = Paths.get("sample.txt");
        }
        Memory memory = new Memory(Files.readAllLines(path));
    }
}
