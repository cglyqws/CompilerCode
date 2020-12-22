package miniplc0java.analyser;

import miniplc0java.tokenizer.TokenType;

public class SymbolEntry {
    boolean isConstant;
    boolean isInitialized;
    TokenType type;
    int stackOffset;
    int location;
    int level;
    String sysname;

    public SymbolEntry()
    {

    }

    public SymbolEntry(String name,boolean isConstant,SymbolType symbolType)
    {
        this.setSymbolType(symbolType);
        this.setSysname(name);
        this.setConstant(isConstant);
    }
    public SymbolType getSymbolType() {
        return symbolType;
    }

    public void setSymbolType(SymbolType symbolType) {
        this.symbolType = symbolType;
    }

    SymbolType symbolType;
    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public int getLocation() {
        return location;
    }

    public void setLocation(int location) {
        this.location = location;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getSysname() {
        return sysname;
    }

    public void setSysname(String sysname) {
        this.sysname = sysname;
    }

    /**
     * @param isConstant
     * @param isDeclared
     * @param stackOffset
     */
    public SymbolEntry(boolean isConstant, TokenType type,boolean isDeclared, int stackOffset,int location) {
        this.isConstant = isConstant;
        this.isInitialized = isDeclared;
        this.stackOffset = stackOffset;
        this.type = type;
        this.location = location;
    }

    /**
     * @return the stackOffset
     */
    public int getStackOffset() {
        return stackOffset;
    }

    /**
     * @return the isConstant
     */
    public boolean isConstant() {
        return isConstant;
    }

    /**
     * @return the isInitialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * @param isConstant the isConstant to set
     */
    public void setConstant(boolean isConstant) {
        this.isConstant = isConstant;
    }

    /**
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    /**
     * @param stackOffset the stackOffset to set
     */
    public void setStackOffset(int stackOffset) {
        this.stackOffset = stackOffset;
    }

    @Override
    public String toString() {
        return "SymbolEntry{" +
                "isConstant=" + isConstant +
                ", isInitialized=" + isInitialized +
                ", type=" + type +
                ", stackOffset=" + stackOffset +
                ", location=" + location +
                '}';
    }
}
