/* On my honor, I have neither given nor received any
 * unauthorized aid on this assignment */

 /** 
  * A simulator for a pseudo-MIPS ISA.
  * @author Matthew Carr
  */

package cda5155.proj1;

import java.util.List;
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

    public static int getSecondArg(int word) {
        return (word & 0x001F0000) >> 16;
    }

    public static int getThirdArg(int word) {
        return (word & 0x0000F800) >> 11;
    }

    public static short getSignedLower16(int word) {
        return (short)(word & 0x0000FFFF);
    }

    public static int getUnsignedLower16(int word) {
        return word & 0x0000FFFF;
    }

    public abstract String disassemble();

    public String toString() {
        return String.join("\t", Memory.word2string(_word), Integer.toString(_address), disassemble());
    }
}

abstract class InstCat1 extends Instruction {
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
                return new InstLoadStore(address, word);
            case 5:
                return new InstLoadStore(address, word);
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
}

class InstJ extends InstCat1 {
    protected int _target;
    int target() {
        return _target;
    }

    public InstJ(int address, int word) {
        super(address, word);
        _type = InstType.J;
        _target = (address + 4) & 0xC0000000 | (word & 0x03FFFFFF) << 2;
    }

    public String disassemble() {
        return String.format("J #%d", _target);
    }
}

class InstBranchCmpr extends InstCat1 {
    protected int _rs;
    int rs() {
        return _rs;
    }

    protected int _rt;
    int rt() {
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
}

class InstBGTZ extends InstCat1 {
    protected int _rs;
    int rs() {
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

    public String disassemble() {
        return String.format("BGTZ R%d, #%d", _rs, _offset);
    }
}

class InstLoadStore extends InstCat1 {
    protected int _base;
    int base() {
        return _base;
    }

    protected int _rt;
    int rt() {
        return _rt;
    }

    protected short _offset;
    short offset() {
        return _offset;
    }

    public InstLoadStore(int address, int word) {
        super(address, word);
        _type = getInstType(word);
        _base = getFirstArg(word);
        _rt = getSecondArg(word);
        _offset = getSignedLower16(word);
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
}

class InstBREAK extends InstCat1 {
    public InstBREAK(int address, int word) {
        super(address, word);
        _type = InstType.BREAK;
    }

    public String disassemble() {
        return "BREAK";
    }
}

abstract class InstCat2 extends Instruction {
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
}

class InstArithType extends InstCat2 {
    public InstArithType(int address, int word) {
        super(address, word);
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
                throw new UnknownError("Invalid instruction type InstArithType: " + _type);
        }
        return String.format("%s R%d, R%d, R%d", typeString, _dest, _src1, _src2);
    }
}

class InstBitShift extends InstCat2 {
    public InstBitShift(int address, int word) {
        super(address, word);
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
                throw new UnknownError("Invalid instruction type for InstBitShift: " + _type);
        }
        return String.format("%s R%d, R%d, #%d", typeString, _dest, _src1, _src2);
    }
}

abstract class InstCat3 extends Instruction {
    protected int _dest;
    int dest() {
        return _dest;
    }

    protected int _src;
    int src() {
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

    public String disassemble() {
        return  String.format("ADDI R%d, R%d, #%d", _dest, _src, _imm);
    }
}

class InstLogicalImm extends InstCat3 {
    protected int _imm;
    int imm() {
        return _imm;
    }

    public InstLogicalImm(int address, int word) {
        super(address, word);
        _type = getInstType(word);
        _imm = getUnsignedLower16(word);
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
                throw new UnknownError("Invalid instruction type for InstLogicalImm: " + _type);
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

    public static InstCat5 decode(int address, int word) {
        return new InstCat5(address, word);
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

class Processor {
    Memory memory;
    int pc;
    int[] gprs;
    int hi;
    int lo;

    public Processor(Memory memory) {
        this.memory = memory;
        pc = 256;
        gprs = new int[32];
    }

    public Processor(String pathString) throws IOException {
        this(new Memory(pathString));
    }

    protected Instruction fetchAndDecode(int addr) {
        return Instruction.decode(addr, memory.fetch(addr));
    }

    protected void executeJ(InstJ inst) {
        pc = inst.target() - 4;
    }

    protected void executeBEQ(InstBranchCmpr inst) {
        if (gprs[inst.rs()] == gprs[inst.rt()]) pc = inst.target() - 4;
    }

    protected void executeBNE(InstBranchCmpr inst) {
        if (gprs[inst.rs()] != gprs[inst.rt()]) pc = inst.target() - 4;
    }

    protected void executeBGTZ(InstBGTZ inst) {
        if (gprs[inst.rs()] > 0) pc = inst.target() - 4;
    }

    protected void executeSW(InstLoadStore inst) {
        memory.store(gprs[inst.base()] + inst.offset(), gprs[inst.rt()]);
    }

    protected void executeLW(InstLoadStore inst) {
        gprs[inst.rt()] = memory.fetch(gprs[inst.base()] + inst.offset());
    }

    protected void executeADD(InstArithType inst) {
        gprs[inst.dest()] = gprs[inst.src1()] + gprs[inst.src2()];
    }

    protected void executeSUB(InstArithType inst) {
        gprs[inst.dest()] = gprs[inst.src1()] - gprs[inst.src2()];
    }

    protected void executeAND(InstArithType inst) {
        gprs[inst.dest()] = gprs[inst.src1()] & gprs[inst.src2()];
    }

    protected void executeOR(InstArithType inst) {
        gprs[inst.dest()] = gprs[inst.src1()] | gprs[inst.src2()];
    }

    protected void executeSRL(InstBitShift inst) {
        gprs[inst.dest()] = gprs[inst.src1()] >>> inst.src2();
    }

    protected void executeSRA(InstBitShift inst) {
        gprs[inst.dest()] = gprs[inst.src1()] >> inst.src2();
    }

    protected void executeADDI(InstADDI inst) {
        gprs[inst.dest()] = gprs[inst.src()] + inst.imm();
    }

    protected void executeANDI(InstLogicalImm inst) {
        gprs[inst.dest()] = gprs[inst.src()] & inst.imm();
    }

    protected void executeORI(InstLogicalImm inst) {
        gprs[inst.dest()] = gprs[inst.src()] | inst.imm();
    }

    protected void executeMULT(InstCat4 inst) {
        long result = gprs[inst.src1()] * gprs[inst.src2()];
        lo = (int) (result & 0x00000000FFFFFFFFL);
        hi = (int)((result & 0xFFFFFFFF00000000L) >>> 32);
    }

    protected void executeDIV(InstCat4 inst) {
        lo = gprs[inst.src1()] / gprs[inst.src2()];
        hi = gprs[inst.src1()] % gprs[inst.src2()];
    }

    protected void executeMFHI(InstCat5 inst) {
        gprs[inst.dest()] = hi;
    }

    protected void executeMFLO(InstCat5 inst) {
        gprs[inst.dest()] = lo;
    }

    protected void execute(Instruction inst) {
        switch (inst.type()) {
            case J:
                executeJ((InstJ)inst);
                return;
            case BEQ:
                executeBEQ((InstBranchCmpr)inst);
                return;
            case BNE:
                executeBNE((InstBranchCmpr)inst);
                return;
            case BGTZ:
                executeBGTZ((InstBGTZ)inst);
                return;
            case SW:
                executeSW((InstLoadStore)inst);
                return;
            case LW:
                executeLW((InstLoadStore)inst);
                return;
            case BREAK:
                return;
            case ADD:
                executeADD((InstArithType)inst);
                return;
            case SUB:
                executeSUB((InstArithType)inst);
                return;
            case AND:
                executeAND((InstArithType)inst);
                return;
            case OR:
                executeOR((InstArithType)inst);
                return;
            case SRL:
                executeSRL((InstBitShift)inst);
                return;
            case SRA:
                executeSRA((InstBitShift)inst);
                return;
            case ADDI:
                executeADDI((InstADDI)inst);
                return;
            case ANDI:
                executeANDI((InstLogicalImm)inst);
                return;
            case ORI:
                executeORI((InstLogicalImm)inst);
                return;
            case MULT:
                executeMULT((InstCat4)inst);
                return;
            case DIV:
                executeDIV((InstCat4)inst);
                return;
            case MFHI:
                executeMFHI((InstCat5)inst);
                return;
            case MFLO:
                executeMFLO((InstCat5)inst);
                return;
            default:
                throw new UnknownError();
        }
    }

    public String simulate() {
        StringBuilder builder = new StringBuilder(8096);
        Instruction inst;
        int maxAddr = memory.maxAddr();
        for (int cycle = 1; pc < maxAddr; pc += 4, cycle += 1) {
            inst = fetchAndDecode(pc);
            execute(inst);
            builder.append(cycleSnapshot(cycle, inst));
            if (inst.type() == InstType.BREAK) break;
        }
        return builder.toString().trim();
    }

    public String getGprState(int index) {
        String[] values = new String[8];
        for (int k = 0; k < 8; ++k, ++index) {
            values[k] = Integer.toString(gprs[index]);
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

    public String cycleSnapshot(int cycle, Instruction inst) {
        String newLine = MIPSsim.LINE_SEP;
        StringBuilder builder = new StringBuilder(220);
        builder.append("--------------------" + newLine);
        builder.append(String.format("Cycle %d:\t%d\t%s%n", cycle, inst.address(), inst.disassemble()));
        builder.append(newLine + "Registers" + newLine);
        for (int k = 0; k < 32; k += 8) {
            builder.append(String.format("R%02d:\t%s%n", k, getGprState(k)));
        }
        builder.append(newLine + "Data" + newLine);
        for (int k = memory.dataStartAddr(); k < memory.maxAddr(); k += 32) {
            builder.append(String.format("%d:\t%s%n", k, getDataState(k)));
        }
        builder.append(newLine);
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
        if (args.length >= 2) {
            inputPath = args[1];
        }
        else {
            inputPath = DEFAULT_INPUT;
        }

        try {
            Memory memory = new Memory(inputPath);
            try {
                writeDisassembly(memory.disassemble());
            }
            catch (IOException e) {
                System.err.println("Failed to write dissassembly to file: " + DISASSEMBLY_NAME);
                System.err.println(e.getMessage());
            }
            Processor proc = new Processor(memory);
            try {
                writeSimulation(proc.simulate());
            }
            catch (IOException e) {
                System.err.println("Failed to write simulation file: " + SIMULATION_NAME);
                System.err.println(e.getMessage());
            }
        }
        catch (IOException e) {
            System.err.println("Unable to read input file: " + inputPath);
            System.err.println(e.getMessage());
        }
    }
}