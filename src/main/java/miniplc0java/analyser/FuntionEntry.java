package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;

import java.util.ArrayList;
import java.util.List;

public class FuntionEntry {

    private int returncount;
    private String funcname;
    private int param;
    private int localvar;
    private List<SymbolEntry> symbolTable = new ArrayList<>();
    private List<Instruction> instructions = new ArrayList<>();

    public int getParam() {
        return param;
    }
    public void setParam(int param) {
        this.param = param;
    }
    public int getReturncount() {
        return returncount;
    }
    public void setReturncount(int returncount) {
        this.returncount = returncount;
    }
    public int getLocalvar() {
        return localvar;
    }
    public void setLocalvar(int localvar) {
        this.localvar = localvar;
    }
    public String getFuncname() {
        return funcname;
    }
    public void setFuncname(String funcname) {
        this.funcname = funcname;
    }
    public List<Instruction> getInstructions() {
        return instructions;
    }
    public void setInstructions(List<Instruction> instructions) {
        this.instructions = instructions;
    }
    public List<SymbolEntry> getSymbolTable() {
        return symbolTable;
    }
    public void setSymbolTable(List<SymbolEntry> symbolTable) {
        this.symbolTable = symbolTable;
    }


    @Override
    public String toString() {
        return "FuntionEntry{" +
                "param=" + param +
                ", localvar=" + localvar +
                '}';
    }
}
