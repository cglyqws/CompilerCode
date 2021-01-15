package miniplc0java.instruction;

import java.util.Objects;

public class Instruction {
    private Operation opt;
    Integer x;

    public Instruction(Operation opt) {
        this.opt = opt;
        this.x = 0;
    }

    public Instruction(Operation opt, Integer x) {
        this.opt = opt;
        this.x = x;
    }

    public Instruction() {
        this.opt = Operation.nop;
        this.x = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Instruction that = (Instruction) o;
        return opt == that.opt && Objects.equals(x, that.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opt, x);
    }

    public Operation getOpt() {
        return opt;
    }

    public void setOpt(Operation opt) {
        this.opt = opt;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public int indexinstruct()
    {
        switch (this.opt)
        {
            case push:
                return 0x01;
            case pop:
                return 0x02;
            case popn:
                return 0x03;
            case dup:
                return 0x04;
            case loca:
                return 0x0a;
            case arga:
                return 0x0b;
            case globa:
                return 0x0c;
            case load8:
                return 0x10;
            case load16:
                return 0x11;
            case load32:
                return 0x12;
            case load64:
                return 0x13;
            case store8:
                return 0x14;
            case store16:
                return 0x15;
            case store32:
                return 0x16;
            case store64:
                return 0x17;
            case alloc:
                return 0x18;
            case free:
                return 0x19;
            case stackalloc:
                return 0x1a;
            case addi:
                return 0x20;
            case subi:
                return 0x21;
            case muli:
                return 0x22;
            case divi:
                return 0x23;
            case addf:
                return 0x24;
            case subf:
                return 0x25;
            case mulf:
                return 0x26;
            case divf:
                return 0x27;
            case divu:
                return 0x28;
            case shl:
                return 0x29;
            case shr:
                return 0x2a;
            case and:
                return 0x2b;
            case or:
                return 0x2c;
            case xor:
                return 0x2d;
            case not:
                return 0x2e;
            case cmpi:
                return 0x30;
            case cmpu:
                return 0x31;
            case cmpf:
                return 0x32;
            case negi:
                return 0x34;
            case negf:
                return 0x35;
            case itof:
                return 0x36;
            case ftoi:
                return 0x37;
            case shrl:
                return 0x38;
            case setlt:
                return 0x39;
            case setgt:
                return 0x3a;
            case br:
                return 0x41;
            case brfalse:
                return 0x42;
            case brtrue:
                return 0x43;
            case call:
                return 0x48;
            case ret:
                return 0x49;
            case callname:
                return 0x4a;
            case scani:
                return 0x50;
            case scanc:
                return 0x51;
            case scanf:
                return 0x52;
            case printi:
                return 0x54;
            case printc:
                return 0x55;
            case printf:
                return 0x56;
            case prints:
                return 0x57;
            case println:
                return 0x58;
            case panic:
                return 0xfe;



        }
        return -1;
    }
    @Override
    public String toString() {
        return "";
    }
}
