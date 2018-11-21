/* On my honor, I have neither given nor received any
 * unauthorized aid on this assignment */

 /** 
  * A simulator for a pseudo-MIPS ISA.
  * Written by Matthew Carr on October 3, 2018.
  * Update by Matthew Carr on November 20, 2018.
  */

package cda5155.MIPSsim;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

enum InstType {
    J, BEQ, BNE, BGTZ, SW, LW, BREAK,
    ADD, SUB, AND, OR, SRL, SRA,
    ADDI, ANDI, ORI,
    MULT, DIV,
    MFHI, MFLO
}

abstract class Instruction {
    public static final String assembleErrorMsg = "Cannot assemble instruction, unknown operation: ";

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

    // The value -1 is used as 
    // the default so that instruction which do not have source or destination
    // registers will be seen as non-conflicting by the the issue unit.

    // The index of the destination gpr.
    int dest() {
        return -1;
    }

    // The index of the gpr for the first source argument.
    int src1() {
        return -1;
    }

    // The value of the first source argument. To be set by the issue unit.
    public int src1val;

    // The index of the gpr for the second source argument.
    int src2() {
        return -1;
    }

    // The value of the second source argument. To be set by the issue unit.
    public int src2val;

    // The result of the instruction's computation. Varies between instructions.
    protected int _result;
    int result() throws RuntimeException {
        return _result;
    }

    public Instruction(int address, int word) throws IllegalArgumentException {
        this._address = address;
        this._word = word;
    }

    // This method should use the argument values and set result.
    public abstract void execute();

    public static int getFirst3Bits(int word) {
        return (word & 0xE0000000) >>> 29;
    }

    public static int shiftCatCode(int catCode) {
        return catCode << 29;
    }

    public static int getSecond3Bits(int word) {
        return (word & 0x1C000000) >>> 26;
    }

    public static int shiftOpCode(int opCode) {
        return opCode << 26;
    }

    protected static final String unknownInstMsg = "Unknown Instruction.";

    public static Instruction decode(int address, int word) throws IllegalArgumentException {
        switch (getFirst3Bits(word)) {
            case 0:
                return InstCat1.decode(address, word);
            case 1:
                return InstCat2.decode(address, word);
            case 2:
                return InstCat3.decode(address, word);
            case 3:
                return InstCat4.decode(address, word);
            case 4:
                return InstCat5.decode(address, word);
            default:
                throw new IllegalArgumentException(unknownInstMsg);
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
                throw new IllegalArgumentException(unknownInstMsg);
        }
    }

    public static int getFirstArg(int word) {
        return (word & 0x03E00000) >> 21;
    }

    public static int shiftFirstArg(int arg) {
        return arg << 21;
    }

    public static int shiftFirstArg(String[] tokens) {
        return shiftFirstArg(Integer.parseInt(tokens[1].substring(1)));
    }

    public static int getSecondArg(int word) {
        return (word & 0x001F0000) >> 16;
    }

    public static int shiftSecondArg(int arg) {
        return arg << 16;
    }

    public static int shiftSecondArg(String[] tokens) {
        return shiftSecondArg(Integer.parseInt(tokens[2].substring(1)));
    }

    public static int getThirdArg(int word) {
        return (word & 0x0000F800) >> 11;
    }

    public static int shiftThirdArg(int arg) {
        return arg << 11;
    }

    public static int shiftThirdArg(String[] tokens) {
        return shiftThirdArg(Integer.parseInt(tokens[3].substring(1)));
    }

    public static short getSignedLower16(int word) {
        return (short)(word & 0x0000FFFF);
    }

    public static int parseImm(String token) {
        if (token.startsWith("#")) token = token.substring(1);
        return Short.toUnsignedInt(Short.parseShort(token));
    }

    public static int shiftSignedLower16(String token) {
        if (token.startsWith("#")) token = token.substring(1);
        return Short.toUnsignedInt((short)(Integer.parseInt(token) >> 2));
    }

    public static int getUnsignedLower16(int word) {
        return word & 0x0000FFFF;
    }

    public abstract String disassemble();

    public String toString() {
        return String.join("\t", Memory.word2string(_word), Integer.toString(_address), disassemble());
    }

    public static String[] tokenize(String instruction) {
        String[] fields = instruction.split("\\s+|,");
        List<String> output = new ArrayList<String>(fields.length);
        for (String field : fields) {
            if (field.isEmpty()) continue;
            output.add(field);
        }
        return output.toArray(new String[output.size()]);
    }

    public static String assembleString(String instruction) {
        return Memory.word2string(assemble(tokenize(instruction)));
    }

    public static int assemble(String instruction) {
        return assemble(tokenize(instruction));
    }

    public static String assembleError(String op) {
        return String.format("%s: '%s'", assembleErrorMsg, op);
    }

    public static int assemble(String[] tokens) throws RuntimeException {
        String op = tokens[0];
        //J, BEQ, BNE, BGTZ, SW, LW, BREAK
        if (op.equals("J") || op.equals("BEQ") || op.equals("BNE") || op.equals("BGTZ") || op.equals("SW") || op.equals("LW") || op.equals("BREAK")) {
            return InstCat1.assemble(tokens);
        }
        // ADD, SUB, AND, OR, SRL, SRA
        else if (op.equals("ADD") || op.equals("SUB") || op.equals("AND") || op.equals("OR") || op.equals("SRL") || op.equals("SRA")) {
            return InstCat2.assemble(tokens);
        }
        // ADDI, ANDI, ORI
        else if (op.equals("ADDI") || op.equals("ANDI") || op.equals("ORI")) {
            return InstCat3.assemble(tokens);
        }
        // MULT, DIV
        else if (op.equals("MULT") || op.equals("DIV")) {
            return InstCat4.assemble(tokens);
        }
        // MFHI, MFLO
        else if (op.equals("MFHI") || op.equals("MFLO")) {
            return InstCat5.assemble(tokens);
        }
        throw new RuntimeException(assembleError(op));
    }
}

abstract class InstCat1 extends Instruction {
    public static final int catCode = 0;

    public InstCat1(int address, int word) {
        super(address, word);
    }

    public static InstCat1 decode(int address, int word) throws IllegalArgumentException {
        switch(getSecond3Bits(word)) {
            case 0:
                return new InstJ(address, word);
            case 1:
                return new InstBranchCmpr(address, word);
            case 2:
                return new InstBranchCmpr(address, word);
            case 3:
                return new InstBGTZ(address, word);
            case 4:
                return new InstSW(address, word);
            case 5:
                return new InstLW(address, word);
            case 6:
                return new InstBREAK(address, word);
            default:
                throw new IllegalArgumentException(unknownInstMsg);
        }
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
                throw new IllegalArgumentException(unknownInstMsg);
        }
    }

    public static int assemble(String[] tokens) {
        int word;
        String op = tokens[0];
        //J, BEQ, BNE, BGTZ, SW, LW, BREAK
        if (op.equals("J")) word = InstJ.assemble(tokens);
        else if (op.equals("BEQ") || op.equals("BNE")) word = InstBranchCmpr.assemble(tokens);
        else if (op.equals("BGTZ")) word = InstBGTZ.assemble(tokens);
        else if (op.equals("SW") || op.equals("LW")) word = InstLoadStore.assemble(tokens);
        else if (op.equals("BREAK")) word = InstBREAK.assemble(tokens);
        else throw new RuntimeException(assembleError(op));
        return shiftCatCode(catCode) | word;
    }
}

class InstJ extends InstCat1 {
    public static final int opCode = 0;

    protected int _target;
    int target() {
        return _target;
    }

    public InstJ(int address, int word) {
        super(address, word);
        _type = InstType.J;
        _target = (address + 4) & 0xC0000000 | (word & 0x03FFFFFF) << 2;
    }

    // result contains the target address to jump to.
    public void execute() {
        _result = _target;
    }

    public String disassemble() {
        return String.format("J #%d", _target);
    }

    public static int assemble(String[] tokens) {
        int offset = Integer.parseInt(tokens[1].substring(1)) >>> 2;
        return shiftOpCode(opCode) | offset;
    }

}

class InstBranchCmpr extends InstCat1 {
    protected int _rs;
    int src1() {
        return _rs;
    }

    protected int _rt;
    int src2() {
        return _rt;
    }

    protected int _offset;
    int offset() {
        return _offset;
    }

    protected int _target;
    int target() {
        return _target;
    }

    public InstBranchCmpr(int address, int word) {
        super(address, word);
        _type = getInstType(word);
        _rs = Instruction.getFirstArg(word);
        _rt = Instruction.getSecondArg(word);
        _offset = Instruction.getSignedLower16(word) << 2;
        _target = address + 4 + _offset;
    }

    // result contains the target address to jump to.
    public void execute() {
        switch (_type) {
            case BEQ:
                if (src1val == src2val) {
                    _result = _target;
                }
                else {
                    _result = _address + 4;
                }
                break;
            case BNE:
                if (src1val != src2val) {
                    _result = _target;
                }
                else {
                    _result = _address + 4;
                }
                break;
            default:
                throw new IllegalArgumentException(unknownInstMsg);
        }
    }

    public static InstType getInstType(int word) throws IllegalArgumentException {
        switch (getSecond3Bits(word)) {
            case 1:
                return InstType.BEQ;
            case 2:
                return InstType.BNE;
            default:
                throw new IllegalArgumentException(unknownInstMsg);
        }
    }

    public String disassemble() {
        String instString;
        switch (_type) {
            case BEQ:
                instString = "BEQ";
                break;
            case BNE:
                instString = "BNE";
                break;
            default:
                throw new IllegalArgumentException(unknownInstMsg);
        }
        return String.format("%s R%d, R%d, #%d", instString, _rs, _rt, _offset);
    }

    public static int assemble(String[] tokens) {
        String op = tokens[0];
        int word;
        if (op.equals("BEQ")) {
            word = shiftOpCode(1);
        }
        else if (op.equals("BNE")) {
            word = shiftOpCode(2);
        }
        else throw new RuntimeException(assembleError(op));
        word |= shiftFirstArg(tokens);
        word |= shiftSecondArg(tokens);
        word |= shiftSignedLower16(tokens[3]);
        return word;
    }
}

class InstBGTZ extends InstCat1 {
    public static final int opCode = 3;

    protected int _rs;
    int src1() {
        return _rs;
    }

    protected int _offset;
    int offset() {
        return _offset;
    }

    protected int _target;
    int target() {
        return _target;
    }

    public InstBGTZ(int address, int word) {
        super(address, word);
        _type = InstType.BGTZ;
        _rs = getFirstArg(word);
        _offset = getSignedLower16(word) << 2;
        _target = _address + 4 + _offset;
    }

    // result contains the target address to jump to.
    public void execute() {
        if (src1val > 0) {
            _result = _target;
        }
        else {
            _result = _address + 4;
        }
    }

    public String disassemble() {
        return String.format("BGTZ R%d, #%d", _rs, _offset);
    }

    public static int assemble(String[] tokens) {
        return shiftOpCode(opCode) | shiftFirstArg(tokens) | shiftSignedLower16(tokens[2]);
    }
}

abstract class InstLoadStore extends InstCat1 {
    protected int _base;
    int src1() {
        return _base;
    }

    protected int _rt;

    protected short _offset;
    short offset() {
        return _offset;
    }

    // The data word from memory. To be set by the MEM unit.
    public int data;

    public InstLoadStore(int address, int word) {
        super(address, word);
        _type = getInstType(word);
        _base = getFirstArg(word);
        _rt = getSecondArg(word);
        _offset = getSignedLower16(word);
    }

    // result contains the memory address to load or store.
    public void execute() {
        _result = src1val + _offset;
    }

    public static InstType getInstType(int word) throws IllegalArgumentException {
        switch (getSecond3Bits(word)) {
            case 4:
                return InstType.SW;
            case 5:
                return InstType.LW;
            default:
                throw new IllegalArgumentException(unknownInstMsg);
        }
    }

    public String disassemble() {
        String instString;
        switch (_type) {
            case SW:
                instString = "SW";
                break;
            case LW:
                instString = "LW";
                break;
            default:
                throw new IllegalArgumentException(unknownInstMsg);
        }
        return String.format("%s R%d, %d(R%d)", instString, _rt, _offset, _base);
    }

    public static int assemble(String[] tokens) {
        String op = tokens[0];
        int word;
        if (op.equals("SW")) {
            word = shiftOpCode(4);
        }
        else if (op.equals("LW")) {
            word = shiftOpCode(5);
        }
        else throw new RuntimeException(assembleError(op));
        Pattern pattern = Pattern.compile("(\\d+)\\(R(\\d+)\\)");
        Matcher matcher = pattern.matcher(tokens[2]);
        if (!matcher.matches()) {
            throw new RuntimeException("Invalid target address of branch: " + tokens[2]);
        }
        int baseReg = Integer.parseInt(matcher.group(2));
        int rt = Integer.parseInt(tokens[1].substring(1));
        short offset = Short.parseShort(matcher.group(1));
        return word | shiftFirstArg(baseReg) | shiftSecondArg(rt) | offset;
    }
}

class InstLW extends InstLoadStore {
    int dest() {
        return _rt;
    }

    public InstLW(int address, int word) {
        super(address, word);
    }
}

class InstSW extends InstLoadStore {
    int src2() {
        return _rt;
    }

    public InstSW(int address, int word) {
        super(address, word);
    }
}

class InstBREAK extends InstCat1 {
    public static final int opCode = 6;

    public InstBREAK(int address, int word) {
        super(address, word);
        _type = InstType.BREAK;
    }

    public void execute() {
        // nothing to do here
    }

    public String disassemble() {
        return "BREAK";
    }

    public static int assemble(String[] tokens) {
        return shiftOpCode(opCode);
    }
}

abstract class InstCat2 extends Instruction {
    public static final int catCode = 1;

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

    public static InstCat2 decode(int address, int word) {
        switch (getSecond3Bits(word)) {
            case 0:
                return new InstArithType(address, word);
            case 1:
                return new InstArithType(address, word);
            case 2:
                return new InstArithType(address, word);
            case 3:
                return new InstArithType(address, word);
            case 4:
                return new InstBitShift(address, word);
            case 5:
                return new InstBitShift(address, word);
            default:
                throw new IllegalArgumentException(unknownInstMsg);
        }
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
                throw new IllegalArgumentException(unknownInstMsg);
        }
    }

    public static int assemble(String[] tokens) {
        String op = tokens[0];
        int opCode;
        // ADD, SUB,  AND, OR
        if (op.equals("ADD") || op.equals("SUB") || op.equals("AND") || op.equals("OR")) {
            opCode = InstArithType.assemble(tokens);
        }
        // SRL, SRA
        else if (op.equals("SRL") || op.equals("SRA")) {
            opCode = InstBitShift.assemble(tokens);
        }
        else throw new RuntimeException(assembleError(op));
        return shiftCatCode(catCode) | opCode | assembleArgs(tokens);
    }

    public static int assembleArgs(String[] tokens) {
        return shiftFirstArg(tokens) | shiftSecondArg(tokens) | shiftThirdArg(tokens);
    }
}

class InstArithType extends InstCat2 {
    protected String _errMsg;

    public InstArithType(int address, int word) {
        super(address, word);
        _errMsg = "Invalid instruction type InstArithType: " + _type;
    }

    // result contains the result of an arithmetic expression.
    public void execute() {
        switch (_type) {
            case ADD:
                _result = src1val + src2val;
                break;
            case SUB:
                _result = src1val - src2val;
                break;
            case AND:
                _result = src1val & src2val;
                break;
            case OR:
                _result = src1val | src2val;
                break;
            default:
                throw new UnknownError(_errMsg);
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
            default:
                throw new UnknownError(_errMsg);
        }
        return String.format("%s R%d, R%d, R%d", typeString, _dest, _src1, _src2);
    }

    public static int assemble(String[] tokens) {
        String op = tokens[0];
        int opCode;
        if (op.equals("ADD")) opCode = 0;
        else if (op.equals("SUB")) opCode = 1;
        else if (op.equals("AND")) opCode = 2;
        else if (op.equals("OR")) opCode = 3;
        else throw new RuntimeException(assembleError(op));
        return shiftOpCode(opCode);
    }
}

class InstBitShift extends InstCat2 {
    protected String _errMsg;

    public InstBitShift(int address, int word) {
        super(address, word);
        _errMsg = "Invalid instruction type for InstBitShift: " + _type;
    }

    // result contains the output of the bitshift operation.
    public void execute() {
        switch (_type) {
            case SRL:
                _result = src1val >>> src2val;
                break;
            case SRA:
                _result = src1val >> src2val;
                break;
            default:
                throw new UnknownError(_errMsg);
        }
    }

    public String disassemble() {
        String typeString;
        switch (_type) {
            case SRL:
                typeString = "SRL";
                break;
            case SRA:
                typeString = "SRA";
                break;
            default:
                throw new UnknownError(_errMsg);
        }
        return String.format("%s R%d, R%d, #%d", typeString, _dest, _src1, _src2);
    }

    public static int assemble(String[] tokens) {
        String op = tokens[0];
        int opCode;
        if (op.equals("SRL")) opCode = 4;
        else if (op.equals("SRA")) opCode = 5;
        else throw new RuntimeException(assembleError(op));
        return shiftOpCode(opCode);
    }
}

abstract class InstCat3 extends Instruction {
    public static final int catCode = 2;

    protected int _dest;
    int dest() {
        return _dest;
    }

    protected int _src;
    int src1() {
        return _src;
    }

    public InstCat3(int address, int word) {
        super(address, word);
        _dest = getFirstArg(word);
        _src = getSecondArg(word);
    }

    public static InstCat3 decode(int address, int word) {
        switch (getSecond3Bits(word)) {
            case 0:
                return new InstADDI(address, word);
            case 1:
                return new InstLogicalImm(address, word);
            case 2:
                return new InstLogicalImm(address, word);
            default:
                throw new IllegalArgumentException(unknownInstMsg);
        }
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
                throw new IllegalArgumentException(unknownInstMsg);
        }
    }

    public static int assemble(String[] tokens) {
        String op = tokens[0];
        int opCode;
        if (op.equals("ADDI")) opCode = 0;
        else if (op.equals("ANDI")) opCode = 1;
        else if (op.equals("ORI")) opCode = 2;
        else throw new RuntimeException(assembleError(op));
        return shiftCatCode(catCode) | shiftOpCode(opCode) | shiftFirstArg(tokens)
            | shiftSecondArg(tokens) | parseImm(tokens[3]);
    }
}

class InstADDI extends InstCat3 {
    protected short _imm;
    short imm() {
        return _imm;
    }

    public InstADDI(int address, int word) {
        super(address, word);
        _type = InstType.ADDI;
        _imm = getSignedLower16(word);
    }

    // result contains the output of addition between src1val and immediate value
    public void execute() {
        _result = src1val + _imm;
    }

    public String disassemble() {
        return  String.format("ADDI R%d, R%d, #%d", _dest, _src, _imm);
    }
}

class InstLogicalImm extends InstCat3 {
    protected String _errMsg;

    protected int _imm;
    int imm() {
        return _imm;
    }

    public InstLogicalImm(int address, int word) {
        super(address, word);
        _type = getInstType(word);
        _imm = getUnsignedLower16(word);
        _errMsg = "Invalid instruction type for InstLogicalImm: " + _type;
    }

    // result contains the result of the logical operator between
    // src1val and the immediate value.
    public void execute() {
        switch (_type) {
            case ANDI:
                _result = src1val & _imm;
                break;
            case ORI:
                _result = src1val | _imm;
                break;
            default:

        }
    }

    public String disassemble() {
        String typeString;
        switch (_type) {
            case ANDI:
                typeString = "ANDI";
                break;
            case ORI:
                typeString = "ORI";
                break;
            default:
                throw new UnknownError(_errMsg);
        }
        return String.format("%s R%d, R%d, #%d", typeString, _dest, _src, _imm);
    }
}

class InstCat4 extends Instruction {
    public static final int catCode = 3;
    protected String _errMsg;

    int dest() {
        return RegisterFile.LO_HI_INDEX;
    }

    protected int _src1;
    int src1() {
        return _src1;
    }

    protected int _src2;
    int src2() {
        return _src2;
    }

    protected int _hi;
    int hi() {
        return _hi;
    }

    protected int _lo;
    int lo() {
        return _lo;
    }

    public InstCat4(int address, int word) {
        super(address, word);
        _type = getInstType(word);
        _src1 = getFirstArg(word);
        _src2 = getSecondArg(word);
        _errMsg = "Invalid instruction type for category 4: " + _type;
    }

    // result, hi, and lo are set by this method.
    public void execute() {
        switch (_type) {
            case MULT:
                long tmp = src1val * src2val;
                _lo = (int) (tmp & 0x00000000FFFFFFFFL);
                _hi = (int)((tmp & 0xFFFFFFFF00000000L) >>> 32);
                break;
            case DIV:
                _result = src1val / src2val;
                _lo = _result;
                _hi = src1val % src2val;
                break;
            default:
                throw new UnknownError(_errMsg);
        }
    }

    public static InstCat4 decode(int address, int word) {
        return new InstCat4(address, word);
    }

    public static InstType getInstType(int word) throws IllegalArgumentException {
        switch (getSecond3Bits(word)) {
            case 0:
                return InstType.MULT;
            case 1:
                return InstType.DIV;
            default:
                throw new IllegalArgumentException(unknownInstMsg);
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
                throw new UnknownError(_errMsg);
        }
        return String.format("%s R%d, R%d", typeString, _src1, _src2);
    }

    public static int assemble(String[] tokens) {
        String op = tokens[0];
        int opCode;
        if (op.equals("MULT")) opCode = 0;
        else if (op.equals("DIV")) opCode = 1;
        else throw new RuntimeException(assembleError(op));
        return shiftCatCode(catCode) | shiftOpCode(opCode) | shiftFirstArg(tokens) | shiftSecondArg(tokens);
    }
}

abstract class InstCat5 extends Instruction {
    public static final int catCode = 4;
    protected int _dest;
    int dest() {
        return _dest;
    }

    public InstCat5(int address, int word) {
        super(address, word);
        _type = getInstType(word);
        _dest = getFirstArg(word);
    }

    public void execute() {
        _result = src1val;
    }

    public static InstCat5 decode(int address, int word) {
        switch (getSecond3Bits(word)) {
            case 0:
                return new InstMFHI(address, word);
            case 1:
                return new InstMFLO(address, word);
            default:
                throw new IllegalArgumentException(unknownInstMsg);
        }
    }

    public static InstType getInstType(int word) throws IllegalArgumentException {
        switch (getSecond3Bits(word)) {
            case 0:
                return InstType.MFHI;
            case 1:
                return InstType.MFLO;
            default:
                throw new IllegalArgumentException(unknownInstMsg);
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

    public static int assemble(String[] tokens) {
        String op = tokens[0];
        int opCode;
        if (op.equals("MFHI")) opCode = 0;
        else if (op.equals("MFLO")) opCode = 1;
        else throw new RuntimeException(assembleError(op));
        return shiftCatCode(catCode) | shiftOpCode(opCode) | shiftFirstArg(tokens);
    }
}

class InstMFHI extends InstCat5 {
    int src1() {
        return RegisterFile.HI_INDEX;
    }

    public InstMFHI(int address, int word) {
        super(address, word);
    }
}

class InstMFLO extends InstCat5 {
    int src1() {
        return RegisterFile.LO_INDEX;
    }

    public InstMFLO(int address, int word) {
        super(address, word);
    }
}

// This class was created to aid in debugging.
class Assembler {
    public static List<String> run(String inputPath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(inputPath));
        List<String> output = new ArrayList<String>(lines.size());
        int k = 0;
        String line;
        for (; k < lines.size(); ++k) {
            line = lines.get(k);
            output.add(Instruction.assembleString(line));
            if (line.equals("BREAK")) {
                ++k;
                break;
            }
        }
        int word;
        for (; k < lines.size(); ++k) {
            word = Integer.parseInt(lines.get(k));
            output.add(Memory.word2string(word));
        }
        return output;
    }
}

class Memory {
    /* The actual contents of this memory object. */
    protected int[] _words;

    protected static final int _minAddr = 256;
    /** The minimum valid memory address. */
    public int minAddr() {
        return _minAddr;
    }

    protected int _maxAddr;
    /** One plus the largest valid memory address. */
    public int maxAddr() {
        return _maxAddr;
    }

    protected int _dataStartAddr;
    /** The address at which program data starts. */
    int dataStartAddr() {
        return _dataStartAddr;
    }

    protected static final int breakWord = 0x18000000;

    public Memory(List<String> lines) {
        _words = new int[lines.size()];
        _maxAddr = index2addr(_words.length);
        for (int k = 0; k < _words.length; ++k) {
            _words[k] = string2word(lines.get(k));
            if (_words[k] == breakWord) _dataStartAddr = index2addr(k + 1);
        }
    }

    public Memory(String pathString) throws IOException, InvalidPathException {
        this(Files.readAllLines(Paths.get(pathString)));
    }

    public static int index2addr(int index) {
        return 4 * index + _minAddr;
    }

    public static int addr2index(int addr) {
        return (addr - _minAddr) / 4;
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
        String newLine = MIPSsim.LINE_SEP;
        StringBuffer buff = new StringBuffer(33 * _words.length);
        for (int k = 0; k < _words.length; ++k) {
            buff.append(word2string(_words[k]) + newLine);
        }
        return buff.toString();        
    }

    protected String badIndexMsg() {
        return String.format("must have %d <= addr <= %d", _minAddr, _maxAddr);
    }

    public int fetch(int addr) throws IllegalArgumentException {
        try {
            return _words[addr2index(addr)];
        }
        catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(badIndexMsg());
        }
    }

    public void store(int addr, int word) throws IllegalArgumentException {
        if (addr < _dataStartAddr) {
            throw new IllegalArgumentException("Segmentation Fault: cannot store data in the code segment.");
        }
        try {
            _words[addr2index(addr)] = word;
        }
        catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(badIndexMsg());
        }
    }

    public String disassemble() {
        String newLine = MIPSsim.LINE_SEP;
        StringBuilder builder = new StringBuilder(64 * _words.length);
        int addr;
        for (addr = _minAddr; addr < _dataStartAddr; addr += 4) {
            builder.append(Instruction.decode(addr, _words[addr2index(addr)]).toString() + newLine);
        }
        int index;
        for (; addr < _maxAddr; addr += 4) {
            index = addr2index(addr);
            builder.append(String.format("%s\t%d\t%d%n",
                word2string(_words[index]), addr, _words[index]
            ));
        }
        return builder.toString().trim();
    }
}

class RegisterFile {
    static final int LO_INDEX = 32;
    static final int HI_INDEX = 33;
    // This must not be a valid index for the registers array.
    static final int LO_HI_INDEX = 255;

    final static int numGprs = 32;
    final static int numRegisters = numGprs + 2;
    private int[] registers = new int[numRegisters];
    // The await array indicates that a register is awaiting the output of
    // an issued instruction.
    private boolean[] registersAwait = new boolean[numRegisters];

    public int get(int index) {
        if (-1 == index) return 0;
        return registers[index];
    }

    public int lo() {
        return registers[LO_INDEX];
    }

    public int hi() {
        return registers[HI_INDEX];
    }

    public void set(int index, int val) {
        if (-1 == index) return;
        registers[index] = val;
        registersAwait[index] = false;
    }

    public boolean getAwaiting(int index) {
        if (-1 == index) {
            return false;
        }
        else if (LO_HI_INDEX == index) {
            return registersAwait[LO_INDEX] || registersAwait[HI_INDEX];
        }
        return registersAwait[index];
    }

    public boolean getAwaiting(Instruction inst) {
        return getAwaiting(inst.dest()) || getAwaiting(inst.src1())
            || getAwaiting(inst.src2());
    }

    public void setAwaiting(int index) {
        if (-1 == index) {
            return;
        }
        else if (LO_HI_INDEX == index) {
            registersAwait[LO_INDEX] = true;
            registersAwait[HI_INDEX] = true;
            return;
        }
        registersAwait[index] = true;
    }

    public static void copy(RegisterFile dest, RegisterFile source) {
        for (int k = 0; k < numRegisters; ++k) {
            dest.registers[k] = source.registers[k];
            dest.registersAwait[k] = source.registersAwait[k];
        }
    }
}

class ProcessorState {
    int pc = 256;
    RegisterFile regFile = new RegisterFile();

    Instruction[] Buf1 = new Instruction[8];
    InstLoadStore[] Buf2 = new InstLoadStore[2];
    InstCat4[] Buf3 = new InstCat4[2];
    InstCat4[] Buf4 = new InstCat4[2];
    Instruction[] Buf5 = new Instruction[2];

    InstLoadStore Buf6;
    InstCat4 Buf7;
    InstCat4 Buf8;
    Instruction Buf9;
    InstLW Buf10;
    InstCat4 Buf11;
    InstCat4 Buf12;

    InstCat1 waitingBranch;
    InstCat1 executedBranch;

    // consolidate all the non-null entries of array so they are contiguous at
    // the beginning of the array.
    public static void consolidate(Object[] array) {
        int index = Processor.firstNullIndex(array);
        if (index < 0) return;
        for (int k = index + 1; k < array.length; ++k) {
            if (array[k] != null) {
                array[index] = array[k];
                array[k] = null;
                index = Processor.firstNullIndex(array, index + 1);
            }
        }
    }

    public static void copyRefs(Object[] dest, Object[] source) {
        int end;
        if (source.length < dest.length) end = source.length;
        else end = dest.length;
        consolidate(source);
        for (int k = 0; k < end; ++k) {
            dest[k] = source[k];
        }
    }

    public static void copy(ProcessorState dest, ProcessorState source) {
        dest.pc = source.pc;
        RegisterFile.copy(dest.regFile, source.regFile);

        copyRefs(dest.Buf1, source.Buf1);
        copyRefs(dest.Buf2, source.Buf2);
        copyRefs(dest.Buf3, source.Buf3);
        copyRefs(dest.Buf4, source.Buf4);
        copyRefs(dest.Buf5, source.Buf5);

        dest.Buf6 = source.Buf6;
        dest.Buf7 = source.Buf7;
        dest.Buf8 = source.Buf8;
        dest.Buf9 = source.Buf9;
        dest.Buf10 = source.Buf10;
        dest.Buf11 = source.Buf11;
        dest.Buf12 = source.Buf12;

        dest.waitingBranch = source.waitingBranch;
        dest.executedBranch = source.executedBranch;
    }
}
class Processor {
    Memory memory;
    ProcessorState state = new ProcessorState();
    ProcessorState stateNext = new ProcessorState();

    public Processor(Memory memory) {
        this.memory = memory;
    }

    public Processor(String pathString) throws IOException {
        this(new Memory(pathString));
    }

    public static int firstNonNullIndex(Object[] array, int start) {
        for (int k = start; k < array.length; ++k) {
            if (array[k] != null) return k;
        }
        return -1;
    }

    public static int firstNonNullIndex(Object[] array) {
        return firstNonNullIndex(array, 0);
    }

    public static int firstNullIndex(Object[] array, int start) {
        for (int k = start; k < array.length; ++k) {
            if (array[k] == null) return k;
        }
        return -1;
    }

    public static int firstNullIndex(Object[] array) {
        return firstNullIndex(array, 0);
    }

    public static boolean rawPresent(Instruction[] buf, Instruction input, int end) {
        int dest, src1 = input.src1(), src2 = input.src2();
        for (int k = 0; k < end; ++k) {
            if (buf[k] == null) continue;
            dest = buf[k].dest();
            if (dest < 0) continue;
            if (RegisterFile.LO_HI_INDEX == dest) {
                if (RegisterFile.LO_INDEX == src1 || RegisterFile.HI_INDEX == src1
                 || RegisterFile.LO_INDEX == src2 || RegisterFile.HI_INDEX == src2) {
                    return true;
                }
            }
            if (dest == src1 || dest == src2) {
                return true;
            }
        }
        return false;
    }


    protected void tryExecWaitingBranch() {
        // We read from stateNext because a branch whose operands are available or
        // immediate should be executed in the same cycle it was fetched.
        if (null == stateNext.waitingBranch) return;
        InstCat1 inst = stateNext.waitingBranch;
        if (state.regFile.getAwaiting(inst.src1()) || state.regFile.getAwaiting(inst.src2())) {
            return;
        }
        if (rawPresent(stateNext.Buf1, inst, stateNext.Buf1.length)) {
            return;
        }
        inst.src1val = state.regFile.get(inst.src1());
        inst.src2val = state.regFile.get(inst.src2());
        inst.execute();
        stateNext.pc = inst.result();
        stateNext.executedBranch = inst;
        stateNext.waitingBranch = null;
    }

    protected boolean fetch() {
        stateNext.executedBranch = null;
        if (null != state.waitingBranch) {
            tryExecWaitingBranch();
            return true;
        }

        int index = firstNullIndex(state.Buf1);
        if (index < 0) return true;

        Instruction inst;
        loop:
            for (; stateNext.pc < state.pc + 16
                    && index < state.Buf1.length
                    && stateNext.pc < memory.maxAddr()
                 ; stateNext.pc += 4)
            {
                inst = Instruction.decode(stateNext.pc, memory.fetch(stateNext.pc));
                switch (inst.type()) {
                    case J:
                    case BEQ:
                    case BNE:
                    case BGTZ:
                        stateNext.waitingBranch = (InstCat1)inst;
                        tryExecWaitingBranch();
                        break loop;
                    case BREAK:
                        stateNext.executedBranch = (InstCat1)inst;
                        return false;
                    default:
                        // put the instruction in Buf1.
                        stateNext.Buf1[index++] = inst;
                }
            }
        return true;
    }

    public boolean warPresent(int address, int dest) {
        if (dest < 0) {
            return false;
        }
        else if (RegisterFile.LO_HI_INDEX == dest) {
            int src1, src2;
            for (Instruction inst : state.Buf1) {
                if (inst == null) continue;
                if (inst.address() >= address) return false;
                src1 = inst.src1();
                src2 = inst.src2();
                if (RegisterFile.LO_INDEX == src1 || RegisterFile.HI_INDEX == src1
                 || RegisterFile.LO_INDEX == src2 || RegisterFile.HI_INDEX == src2) {
                    return true;
                }
            }
        }
        else {
            for (Instruction inst : state.Buf1) {
                if (inst == null) continue;
                if (inst.address() >= address) return false;
                if (inst.src1() == dest || inst.src2() == dest) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean earlierSW(int address) {
        for (Instruction inst : state.Buf1) {
            if (inst == null) continue;
            if (inst.address() >= address) return false;
            if (inst.type() == InstType.SW) return true;
        }
        return false;
    }

    public void issueIfSpace(int k, Instruction[] buf) {
        Instruction inst = state.Buf1[k];
        // Check to see if there is space in the destination buffer.
        int index = firstNullIndex(buf);
        if (index < 0) return;
        // Read operands and update the scoreboard.
        inst.src1val = state.regFile.get(inst.src1());
        inst.src2val = state.regFile.get(inst.src2());
        stateNext.regFile.setAwaiting(inst.dest());
        // Copy the instruction to the destination and remove it from the source.
        buf[index] = inst;
        stateNext.Buf1[k] = null;
    }

    /* ALU2 instructions:
        ADD, SUB, AND, OR, SRL, SRA,
        ADDI, ANDI, ORI,
        MFHI, MFLO
    */
    public void issue() {
        Instruction inst;
        for (int k = 0; k < state.Buf1.length; ++k) {
            inst = state.Buf1[k];
            if (inst == null) continue;
            if (state.regFile.getAwaiting(inst)) continue;
            if (rawPresent(state.Buf1, inst, k) || warPresent(inst.address(), inst.dest())) continue;
            switch (inst.type()) {
                case LW:
                case SW:
                    if (earlierSW(inst.address())) continue;
                    issueIfSpace(k, stateNext.Buf2);
                    break;
                case DIV:
                    issueIfSpace(k, stateNext.Buf3);
                    break;
                case MULT:
                    issueIfSpace(k, stateNext.Buf4);
                    break;
                default:
                    issueIfSpace(k, stateNext.Buf5);
            }
        }
    }

    public void alu2() {
        stateNext.Buf6 = null;
        int k = firstNonNullIndex(state.Buf2);
        if (k < 0) return;
        stateNext.Buf6 = state.Buf2[k];
        stateNext.Buf2[k] = null;
        if (null == stateNext.Buf6) return;
        stateNext.Buf6.execute();
    }

    public void mem() {
        stateNext.Buf10 = null;
        if (null == state.Buf6) return;
        InstLoadStore inst = state.Buf6;
        switch (inst.type()) {
            case LW:
                inst.data = memory.fetch(inst.result());
                stateNext.Buf10 = (InstLW)inst;
                break;
            case SW:
                memory.store(inst.result(), state.regFile.get(inst.src2()));
                break;
            default:
                throw new UnknownError("The entry in Buf6 is not a LW nor a SW.");
        }
    }

    public void div() {
        stateNext.Buf7 = null;
        int k = firstNonNullIndex(state.Buf3);
        if (k < 0) return;
        stateNext.Buf7 = state.Buf3[k];
        stateNext.Buf3[k] = null;
        if (null == stateNext.Buf7) return;
        stateNext.Buf7.execute();
    }

    public void mul1() {
        stateNext.Buf8 = null;
        int k = firstNonNullIndex(state.Buf4);
        if (k < 0) return;
        stateNext.Buf8 = state.Buf4[k];
        stateNext.Buf4[k] = null;
        if (null == stateNext.Buf8) return;
        stateNext.Buf8.execute();
    }

    public void alu1() {
        stateNext.Buf9 = null;
        int k = firstNonNullIndex(state.Buf5);
        if (k < 0) return;
        Instruction inst = state.Buf5[k];
        stateNext.Buf9 = inst;
        stateNext.Buf5[k] = null;
        if (null == inst) return;
        inst.execute();
    }

    public void mul2() {
        stateNext.Buf11 = state.Buf8;
    }

    public void mul3() {
        stateNext.Buf12 = state.Buf11;
    }

    public void writeBack() {
        if (state.Buf10 != null) {
            stateNext.regFile.set(state.Buf10.dest(), state.Buf10.data);
        }
        if (state.Buf7 != null) {
            stateNext.regFile.set(RegisterFile.LO_INDEX, state.Buf7.lo());
            stateNext.regFile.set(RegisterFile.HI_INDEX, state.Buf7.hi());
        }
        if (state.Buf12 != null) {
            stateNext.regFile.set(RegisterFile.LO_INDEX, state.Buf12.lo());
            // The state of hi must always to 0 to be consistent with the sample simulation.
            stateNext.regFile.set(RegisterFile.HI_INDEX, 0);
        }
        if (state.Buf9 != null) {
            stateNext.regFile.set(state.Buf9.dest(), state.Buf9.result());
        }
    }

    public String simulate() {
        StringBuilder builder = new StringBuilder(8096);
        int cycle = 1;
        while (fetch()) {
            issue();
            alu2();
            mem();
            div();
            mul1();
            mul2();
            mul3();
            alu1();
            writeBack();
            ProcessorState.copy(state, stateNext);
            builder.append(cycleSnapshot(cycle++));
        }
        ProcessorState.copy(state, stateNext);
        builder.append(cycleSnapshot(cycle++));
        return builder.toString().trim();
    }

    public String getGprState(int index) {
        String[] values = new String[8];
        for (int k = 0; k < 8; ++k, ++index) {
            values[k] = Integer.toString(state.regFile.get(index));
        }
        return String.join("\t", values);
    }

    public String getDataState(int addr) {
        String[] values = new String[8];
        for (int k = 0; k < 8 && addr < memory.maxAddr(); ++k, addr += 4) {
            values[k] = Integer.toString(memory.fetch(addr));
        }
        return String.join("\t", values);
    }

    public static void bufferSnapshot(StringBuilder builder, Instruction[] buf, String name) {
        String newLine = MIPSsim.LINE_SEP;
        builder.append(name + ":" + newLine);
        for (int k = 0; k < buf.length; ++k) {
            builder.append(String.format("\tEntry %d:", k));
            if (null != buf[k])
                builder.append(String.format(" [%s]%n", buf[k].disassemble()));
            else
                builder.append(newLine);
        }
    }

    public String cycleSnapshot(int cycle) {
        if (0 == cycle) return "";
        String newLine = MIPSsim.LINE_SEP;
        StringBuilder builder = new StringBuilder(480);
        builder.append("--------------------" + newLine);
        builder.append(String.format("Cycle %d:%n%n", cycle));

        builder.append("IF:" + newLine);
        builder.append(String.format("\tWaiting:"));
        if (state.waitingBranch != null)
            builder.append(String.format(" [%s]%n", state.waitingBranch.disassemble()));
        else
            builder.append(newLine);
        builder.append("\tExecuted:");
        if (state.executedBranch != null)
            builder.append(String.format(" [%s]%n", state.executedBranch.disassemble()));
        else
            builder.append(newLine);

        bufferSnapshot(builder, state.Buf1, "Buf1");
        bufferSnapshot(builder, state.Buf2, "Buf2");
        bufferSnapshot(builder, state.Buf3, "Buf3");
        bufferSnapshot(builder, state.Buf4, "Buf4");
        bufferSnapshot(builder, state.Buf5, "Buf5");

        builder.append("Buf6:");
        if (null != state.Buf6)
            builder.append(String.format(" [%s]%n", state.Buf6.disassemble()));
        else
            builder.append(newLine);

        builder.append("Buf7:");
        if (null != state.Buf7)
            builder.append(String.format(" [%d, %d]%n", state.Buf7.hi(), state.Buf7.lo()));
        else
            builder.append(newLine);
        
        builder.append("Buf8:");
        if (null != state.Buf8)
            builder.append(String.format(" [%s]%n", state.Buf8.disassemble()));
        else
            builder.append(newLine);

        builder.append("Buf9:");
        if (null != state.Buf9)
            builder.append(String.format(" [%d, R%d]%n", state.Buf9.result(), state.Buf9.dest()));
        else
            builder.append(newLine);

        builder.append("Buf10:");
        if (null != state.Buf10)
            builder.append(String.format(" [%d, R%d]%n", state.Buf10.data, state.Buf10.dest()));
        else
            builder.append(newLine);

        builder.append("Buf11:");
        if (null != state.Buf11)
            builder.append(String.format(" [%s]%n", state.Buf11.disassemble()));
        else
            builder.append(newLine);

        builder.append("Buf12:");
        if (null != state.Buf12)
            builder.append(String.format(" [%d]%n", state.Buf12.lo()));
        else
            builder.append(newLine);

        builder.append(newLine);
        builder.append("Registers" + newLine);
        for (int k = 0; k < 32; k += 8) {
            builder.append(String.format("R%02d:\t%s%n", k, getGprState(k)));
        }
        builder.append(String.format("HI:\t%d%n", state.regFile.hi()));
        builder.append(String.format("LO:\t%d%n", state.regFile.lo()));
        builder.append(newLine);
        builder.append("Data" + newLine);
        for (int k = memory.dataStartAddr(); k < memory.maxAddr(); k += 32) {
            builder.append(String.format("%d:\t%s%n", k, getDataState(k)));
        }
        return builder.toString();
    }
}

public class MIPSsim {
    public static final String DEFAULT_INPUT = "sample.txt";
    public static final String DISASSEMBLY_NAME = "disassembly.txt";
    public static final String SIMULATION_NAME = "simulation.txt";
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final String LINE_SEP = System.getProperty("line.separator");

    public static void write2file(String contents, String fileName) throws IOException {
        PrintWriter writer = new PrintWriter(
            Files.newBufferedWriter(Paths.get(fileName), CHARSET,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            )
        );
        writer.write(contents);
        writer.close();
    }

    public static void writeDisassembly(String disassembly) throws IOException {
        write2file(disassembly, DISASSEMBLY_NAME);
    }

    public static void writeSimulation(String simulation) throws IOException {
        write2file(simulation, SIMULATION_NAME);
    }

    public static void main(String[] args) {
        String inputPath;
        if (args.length >= 1) {
            inputPath = args[0];
        }
        else {
            inputPath = DEFAULT_INPUT;
        }

        try {
            Memory memory = new Memory(inputPath);
            // No disassembly file was requested for project 2.
            // try {
            //     writeDisassembly(memory.disassemble());
            // }
            // catch (IOException e) {
            //     System.err.println("Failed to write dissassembly to file: " + DISASSEMBLY_NAME);
            // }
            Processor proc = new Processor(memory);
            try {
                writeSimulation(proc.simulate());
            }
            catch (IOException e) {
                System.err.println("Failed to write simulation file: " + SIMULATION_NAME);
            }
        }
        catch (IOException e) {
            System.err.println("Unable to read input file: " + inputPath);
        }
    }
}