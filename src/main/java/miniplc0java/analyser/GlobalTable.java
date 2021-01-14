package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;

import java.util.ArrayList;
import java.util.function.Function;

public class GlobalTable {

    private static GlobalTable globalTable = new GlobalTable();

    // 私有化构造方法
    private GlobalTable() {

    }

    public static GlobalTable getGlobalTable() {
        return globalTable;
    }

    public ArrayList<Instruction> getInstructions() {
        return instructions;
    }

    public void setInstructions(ArrayList<Instruction> instructions) {
        this.instructions = instructions;
    }

    public ArrayList<SymbolEntry> getSymbolTable() {
        return symbolTable;
    }

    public void setSymbolTable(ArrayList<SymbolEntry> symbolTable) {
        this.symbolTable = symbolTable;
    }

    public ArrayList<FuntionEntry> getFuntionTable() {
        return funtionTable;
    }

    public void setFuntionTable(ArrayList<FuntionEntry> funtionTable) {
        this.funtionTable = funtionTable;
    }

    private ArrayList<Instruction> instructions = new ArrayList<>();
    private  ArrayList<SymbolEntry> symbolTable = new ArrayList<>();
    private ArrayList<FuntionEntry> funtionTable = new ArrayList<>();

    public int findsymbolindexbyname (String name)
    {
        int len = symbolTable.size();
        for (int i =0 ;i<len ;i++)
        {
            if (symbolTable.get(i).getSysname().equals(name))
            {
                return i;
            }
        }
        return -1;
    }

    public SymbolEntry findsymbolbyname (String name)
    {
        int len = symbolTable.size();
        for (int i =0 ;i<len ;i++)
        {
            if (symbolTable.get(i).getSysname().equals(name))
            {
                return symbolTable.get(i);
            }
        }
        return null;
    }

    public int findfuntionindexbyname (String name)
    {
        int len = funtionTable.size();
        for (int i=0;i<len ;i++)
        {
            if (funtionTable.get(i).getFuncname().equals(name)){
                return i;
            }
        }
        return -1;
    }
    public FuntionEntry findfuntionbyname (String name)
    {
        int len = funtionTable.size();
        for (int i=0;i<len ;i++)
        {
            if (funtionTable.get(i).getFuncname().equals(name)){
                return funtionTable.get(i);
            }
        }
        return null;
    }

    public FuntionEntry getnowfunction()
    {
        return funtionTable.get(funtionTable.size()-1);
    }
}
