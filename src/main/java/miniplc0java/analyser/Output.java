package miniplc0java.analyser;

import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.TokenType;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Output {
    static int magic = 0x72303b3e;
    static int version = 0x00000001;
    List<Byte> all;

    public static String he(byte b)
    {
        String hex = Integer.toHexString(b & 0xFF);
        if (hex.length() == 1) {
        hex = '0' + hex;}
        return hex;
    }


    public static void print(PrintStream p,byte[] b) {
//        p.write(b);
        int size = b.length;
        for (int i = 0; i < size; i++) {
            p.print(he(b[i]));
        }
    }

    public static void printfile(PrintStream output)
    {
        List<Byte> outputbytes = new ArrayList<>();
        byte[] temp = Data2Byte.getBytes(magic);

        print(output,temp);
        temp = Data2Byte.getBytes(version);
        print(output,temp);


        GlobalTable gt = GlobalTable.getGlobalTable();
        List<SymbolEntry> gsys = gt.getSymbolTable();
//        SymbolEntry s = new SymbolEntry();
//        s.setSysname("_start");
//        s.setConstant(true);
//        s.setSymbolType(SymbolType.FUNTION);
//        gsys.add(s);
//        FuntionEntry f =new FuntionEntry();
//        f.setFuncname("_start");
        int glen = gsys.size();
        byte[] globalsyscount = Data2Byte.getBytes(glen);
        print(output,globalsyscount);

        for (int i=0;i<glen;i++) {
            SymbolEntry s = gsys.get(i);
            byte[] tem = Data2Byte.getBytes(s.isConstant());
            print(output, tem);


            if (s.getSymbolType() == SymbolType.FUNTION || s.getSymbolType() == SymbolType.STRING) {
                tem = Data2Byte.getBytes(s.getSysname());
            } else if (s.getSymbolType() == SymbolType.INT || s.getSymbolType() == SymbolType.DOUBLE) {
                tem = Data2Byte.getBytes2(0);
            }
            print(output, Data2Byte.getBytes(tem.length));
            print(output, tem);
        }
            List<FuntionEntry> funtiontable = gt.getFuntionTable();
            int funsize= funtiontable.size();
            print(output,Data2Byte.getBytes(funsize));

            for (int i = 0; i < funsize; i++) {
                FuntionEntry fun = funtiontable.get(i);
                int nameloca = gt.findfuntionindexbyname(fun.getFuncname());
                print(output,Data2Byte.getBytes(nameloca+1));
                int returncount = fun.getReturncount();
                print(output,Data2Byte.getBytes(returncount));

                int param = fun.getParam();
                print(output,Data2Byte.getBytes(param));
                int localvar = fun.getLocalvar();
                print(output,Data2Byte.getBytes(localvar));
                List<Instruction> instructions = fun.getInstructions();
                print(output,Data2Byte.getBytes(instructions.size()));


                for (Instruction instruction : instructions) {
                    print(output,Data2Byte.getBytes3(instruction.indexinstruct()));
                    if (instruction.getOpt()== Operation.push)
                    {
                        print(output,Data2Byte.getBytes2(instruction.getX()));
                    }
                    else if (instruction.getOpt()==Operation.popn
                    ||instruction.getOpt()==Operation.loca
                    ||instruction.getOpt()==Operation.arga
                    ||instruction.getOpt()==Operation.globa
                    ||instruction.getOpt()==Operation.stackalloc
                    ||instruction.getOpt()==Operation.br
                    ||instruction.getOpt()==Operation.brfalse
                    ||instruction.getOpt()==Operation.brtrue
                    ||instruction.getOpt()==Operation.call
                    ||instruction.getOpt()==Operation.callname)
                    {
                        print(output,Data2Byte.getBytes(instruction.getX()));
                    }
                }
            }


    }
}
