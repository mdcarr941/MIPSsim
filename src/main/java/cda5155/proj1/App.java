package cda5155.proj1;

import java.util.List;

import org.omg.CORBA.PUBLIC_MEMBER;

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

    protected byte[] _word;
    byte[] word() {
        return _word;
    }

    protected InstType _type;
    InstType type() {
        return _type;
    }

    public Instruction(int address, byte[] word) throws IllegalArgumentException {
        if (4 != word.length) throw new IllegalArgumentException("word must be exactly 4 bytes long.");
        this._address = address;
        this._word = word;
    }

    public static int getFirst3Bits(byte B) {
        return (B & 0xE0) >> 5;
    }

    public static int getSecond3Bits(byte B) {
        return (B & 0x1C) >> 2;
    }

    public static Instruction decode(int address, byte[] word) throws IllegalArgumentException {
        if (4 != word.length) throw new IllegalArgumentException("word must be exactly 4 bytes long.");
        int first3 = getFirst3Bits(word[0]);
        switch (first3) {
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
        return decode(address, string2word(wordString));
    }

    public static InstType getInstType(byte B) throws IllegalArgumentException {
        int first3 = getFirst3Bits(B);
        switch (first3) {
            case 0:
                return InstCat1.getInstType(B);
            case 1:
                return InstCat2.getInstType(B);
            case 2:
                return InstCat3.getInstType(B);
            case 3:
                return InstCat4.getInstType(B);
            case 4:
                return InstCat5.getInstType(B);
            default:
                throw new IllegalArgumentException("Argument must be between 0 and 4 inclusive.");
        }
    }

    // Convenience method for testing.
    public static InstType getInstType(int B) throws IllegalArgumentException {
        return getInstType((byte)B);
    }

    public static String word2string(byte[] word) {
        return App.byte2String(word[0]) + App.byte2String(word[1])
            + App.byte2String(word[2]) + App.byte2String(word[2]);
    }

    public static byte[] string2word(String input) {
        byte[] word = new byte[4];
        for (int k = 0; k < 4; ++k) {
            word[k] = App.string2Byte(input.substring(8*k, 8*(k + 1)));
        }
        return word;
    }

    public String toString() {
        return word2string(_word) + '\t' + Integer.toString(_address);
    }
}

abstract class InstCat1 extends Instruction {
    public InstCat1(int address, byte[] word) {
        super(address, word);
        _type = getInstType(word[0]);
    }

    public static InstType getInstType(byte B) throws IllegalArgumentException {
        int second3 = getSecond3Bits(B);
        switch (second3) {
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
    public InstJ(int address, byte[] word) {
        super(address, word);
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

    public InstCat2(int address, byte[] word) {
        super(address, word);
        _type = getInstType(word[0]);
        _dest = (word[0] & 3) << 3 | (word[1] & 0xE0) >> 5;
        _src1 = word[1] & 0x1F;
        _src2 = (word[2] & 0xF8) >> 3;
    }

    public static InstType getInstType(byte B) throws IllegalArgumentException {
        int second3 = getSecond3Bits(B);
        switch (second3) {
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

    public String toString() {
        String typeString;
        switch (_type) {
            case ADD:
                typeString = "ADD";
                break;
            default:
                throw new UnknownError("Invalid instruction type for category 2: " + _type);
        }
        return String.format("%s\t%s R%d, R%d, R%d", super.toString(), typeString, _dest, _src1, _src2);
    }
}

class InstCat3 extends Instruction {
    public InstCat3(int address, byte[] word) {
        super(address, word);
        _type = getInstType(word[0]);
    }

    public static InstType getInstType(byte B) throws IllegalArgumentException {
        int second3 = getSecond3Bits(B);
        switch (second3) {
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
}

class InstCat4 extends Instruction {
    public InstCat4(int address, byte[] word) {
        super(address, word);
        _type = getInstType(word[0]);
    }

    public static InstType getInstType(byte B) throws IllegalArgumentException {
        int second3 = getSecond3Bits(B);
        switch (second3) {
            case 0:
                return InstType.MULT;
            case 1:
                return InstType.DIV;
            default:
                throw new IllegalArgumentException("Value of argument must be either 0 or 1.");
        }
    }
}

class InstCat5 extends Instruction {
    public InstCat5(int address, byte[] word) {
        super(address, word);
        _type = getInstType(word[0]);
    }

    public static InstType getInstType(byte B) throws IllegalArgumentException {
        int second3 = getSecond3Bits(B);
        switch (second3) {
            case 0:
                return InstType.MFHI;
            case 1:
                return InstType.MFLO;
            default:
                throw new IllegalArgumentException("Value of argument must be either 0 or 1.");
        }
    }
}

public class App {
    public static byte string2Byte(String byteString) {
        byte B = 0;
        for (int k = 0; k < 8; ++k) {
            if (byteString.charAt(k) == '1') B |= 1 << (7-k);
        }
        return B;
    }

    public static String byte2String(byte B) {
        char[] chars = new char[] {'0', '0', '0', '0', '0', '0', '0', '0'};
        for (int k = 0; k < 8; ++k) {
            if ((B & (1 << (7-k))) > 0) chars[k] = '1';
        }
        return new String(chars);
    }

    public static byte[] createMemory(List<String> lines) {
        int numLines = lines.size();
        byte[] memory = new byte[256 + 4 * numLines];
        String line;
        for (int k = 0; k < numLines; ++k) {
            line = lines.get(k);
            for (int l = 0; l < 4; ++l) {
                memory[256 + 4*k + l] = string2Byte(line.substring(8 * l, 8 * (l + 1)));
            }
        }
        return memory;
    }

    public static void main(String[] args) throws IOException {
        Path path;
        if (args.length >= 2) {
            path = Paths.get(args[1]);
        }
        else {
            path = Paths.get("sample.txt");
        }
        byte[] memory = createMemory(Files.readAllLines(path));
    }
}
