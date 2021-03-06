package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;
import org.checkerframework.checker.units.qual.A;

import java.util.*;

public final class Analyser {

    private int level = 0;
    Tokenizer tokenizer;
    int localvar=0;
    /** 当前偷看的 token */
    Token peekedToken = null;
    int whilecount=0;
    int whilestart =0;
    int whileend=0;
    GlobalTable gt = GlobalTable.getGlobalTable();
    ArrayList<FuntionEntry> funtionTable = gt.getFuntionTable();
    ArrayList<Instruction> instructions = gt.getInstructions();
    ArrayList<SymbolEntry> symbolTable = gt.getSymbolTable();



    int now = -1;
    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer)  {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public void test() throws CompileError
    {
        analyseProgram();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        return instructions;
    }

    public List<Instruction> getglobalinstruction ()
    {
        FuntionEntry f = gt.findfuntionbyname("_start");
        return f.getInstructions();
    }
    public void symaddfun(String name)
    {
        SymbolEntry s = new SymbolEntry();
        s.setSysname(name);
        if (isstandard(name))
        {
            s.setConstant(true);
        }
        else s.setConstant(false);
        s.setSymbolType(SymbolType.FUNTION);

        symbolTable.add(s);
    }

    /**
     * 在当前符号表中搜索
     */
    public SymbolEntry nowgetSymbol(String name )
    {
        List<SymbolEntry> l = getnowsymboltable();
        for (int i=l.size()-1;i>=0;i--)
        {
            if (l.get(i).getSysname().equals(name))
            {
                return l.get(i);
            }
        }
        return null;
    }
    /**
     * 获取当前符号表
     * @return
     */
    public List<SymbolEntry> getnowsymboltable()
    {
        if (funtionTable.size()==0)
        {
            return symbolTable;
        }
            FuntionEntry f = funtionTable.get(funtionTable.size()-1);
            return f.getSymbolTable();

    }

    /**
     * 获取当前指令集
     * @return
     */
    public List<Instruction> getnowinstructions()
    {
        if (funtionTable.size()==0)
        {
            return getglobalinstruction();
        }
            FuntionEntry f = funtionTable.get(funtionTable.size()-1);
            return f.getInstructions();
    }
    /**
     * 查看下一个 Token
     *
     * @return
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     *
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     *
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     *
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     *
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    /**
     * 获取下一个变量的栈偏移
     *
     * @return
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }

    public void initsymboltable(ArrayList<SymbolEntry> f)
    {
        f.add(new SymbolEntry("_start",true,SymbolType.FUNTION));
        f.add(getstandardsym("getint"));
        f.add(getstandardsym("getdouble"));
        f.add(getstandardsym("getchar"));
        f.add(getstandardsym("putint"));
        f.add(getstandardsym("putdouble"));
        f.add(getstandardsym("putstr"));
        f.add(getstandardsym("putchar"));
        f.add(getstandardsym("putln"));

    }

    private void analyseProgram() throws CompileError {
        // 程序 -> 'begin' 主过程 'end'
        // 示例函数，示例如何调用子程序
        // 'begin'
        initsymboltable(gt.getSymbolTable());

        FuntionEntry f = new FuntionEntry();
        f.setFuncname("_start");
        funtionTable.add(f);

        while (!check(TokenType.EOF))
        {
            if (check(TokenType.FN_KW))
            {
                analysefn();
            }
            else {
                analyseglobaldecl_stmt();
                if (check(TokenType.Semicolon))
                {
                    expect(TokenType.Semicolon);
                }
            }
        }

        return ;

    }

    private void analyseglobaldecl_stmt() throws CompileError
    {

        if (check(TokenType.LET_KW))
        {
            localvar += 1;
            expect(TokenType.LET_KW);
            var token = expect(TokenType.Ident);
            expect(TokenType.Collon);
            var token2 = analysety();

            if (token2.getValueString().equals("void"))
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ token2.getStartPos());
            }

            if (check(TokenType.Equal))
            {
                List<SymbolEntry> nowsymboltable = symbolTable;
                var sys = gt.findglobalsymbolbyname(token.getValueString());

                int off = nowsymboltable.size();
                if (sys==null)
                {
                    SymbolEntry s = new SymbolEntry();
                    s.setConstant(false);
                    s.setLevel(now);
                    s.setSysname(token.getValueString());
                    s.setInitialized(true);
                    s.setType(token2.getTokenType());
                    if (token2.getValueString().equals("int"))
                    {
                        s.setReturnType(ReturnType.INT);
                        s.setSymbolType(SymbolType.INT);
                    }
                    else if (token2.getValueString().equals("double"))
                    {
                        s.setReturnType(ReturnType.DOUBLE);
                    }

                    nowsymboltable.add(s);
                }
                else {
                    throw new AnalyzeError(ErrorCode.DuplicateDeclaration, /* 当前位置 */ token.getStartPos());
                }
                List<Instruction> in = getglobalinstruction();
                in.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(token.getValueString())));
                expect(TokenType.Equal);
                analyseexpr();
                in.add(new Instruction(Operation.store64));
                expect(TokenType.Semicolon);
            }
            else {
                List<SymbolEntry> nowsymboltable = symbolTable;
                var sys = nowgetSymbol(token.getValueString());
                int off = nowsymboltable.size();
                if (sys==null)
                {
                    SymbolEntry s = new SymbolEntry();
                    s.setConstant(false);
                    s.setLevel(now);
                    s.setSysname(token.getValueString());
                    s.setInitialized(false);
                    s.setType(token2.getTokenType());
                    if (token2.getValueString().equals("int"))
                    {
                        s.setReturnType(ReturnType.INT);
                        s.setSymbolType(SymbolType.INT);
                    }
                    else if (token2.getValueString().equals("double"))
                    {
                        s.setReturnType(ReturnType.DOUBLE);
                    }
                    nowsymboltable.add(s);
                }
                else {
                    throw new AnalyzeError(ErrorCode.DuplicateDeclaration, /* 当前位置 */ token.getStartPos());
                }

            }
        }
        else if (check(TokenType.CONST_KW))
        {

            expect(TokenType.CONST_KW);
            localvar += 1;

            var token = expect(TokenType.Ident);
            expect(TokenType.Collon);
            var token2 = analysety();

            if (token2.getValueString().equals("void"))
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ token2.getStartPos());
            }

            if (check(TokenType.Equal))
            {
                List<SymbolEntry> nowsymboltable = symbolTable;
                var sys = gt.findglobalsymbolbyname(token.getValueString());

                int off = nowsymboltable.size();
                if (sys==null)
                {
                    SymbolEntry s = new SymbolEntry();
                    s.setConstant(true);
                    s.setLevel(now);
                    s.setSysname(token.getValueString());
                    s.setInitialized(true);
                    s.setType(token2.getTokenType());
                    if (token2.getValueString().equals("int"))
                    {
                        s.setReturnType(ReturnType.INT);
                        s.setSymbolType(SymbolType.INT);
                    }
                    else if (token2.getValueString().equals("double"))
                    {
                        s.setReturnType(ReturnType.DOUBLE);
                        s.setSymbolType(SymbolType.INT);
                    }

                    nowsymboltable.add(s);
                }
                else {
                    throw new AnalyzeError(ErrorCode.DuplicateDeclaration, /* 当前位置 */ token.getStartPos());
                }
                List<Instruction> in = getglobalinstruction();
                in.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(token.getValueString())));
                expect(TokenType.Equal);
                analyseexpr();
                in.add(new Instruction(Operation.store64));
                expect(TokenType.Semicolon);
            }

        }
        else throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ peek().getStartPos());
    }
    /**
     * 函数fn
     * @throws CompileError
     */
    private void analysefn() throws  CompileError
    {
        int param = 0;
        expect(TokenType.FN_KW);
        var token1 = expect(TokenType.Ident);

        if(gt.findfuntionbyname(token1.getValueString())!=null)
        {
            throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ token1.getStartPos());
        }

        FuntionEntry f = new FuntionEntry();
        f.setFuncname(token1.getValueString());

        symaddfun(token1.getValueString());
        funtionTable.add(f);
        if (token1.getValueString().equals("main"))
        {

            FuntionEntry func = funtionTable.get(0);
            func.getInstructions().add(new Instruction(Operation.stackalloc,0));
            func.getInstructions().add(new Instruction(Operation.call,funtionTable.size()-1));
        }
        expect(TokenType.LParen);
        if (check(TokenType.RParen))
        {
            expect(TokenType.RParen);
        }
        else {
            param = analyseparamlist();
        }

        expect(TokenType.Arrow);

        var token = analysety();
        f = funtionTable.get(funtionTable.size()-1);

        if (token.getValueString().equals("void"))
        {
            f.setReturnType(ReturnType.VOID);
        }
        else if (token.getValueString().equals("int"))
        {
            f.setReturnType(ReturnType.INT);

            if (token1.getValueString().equals("main"))
            {
                FuntionEntry fu = gt.findfuntionbyname("_start");
                fu.getInstructions().get(0).setX(1);
                fu.getInstructions().add(new Instruction(Operation.popn,1));
            }
        }
        else if (token.getValueString().equals("double"))
        {
            f.setReturnType(ReturnType.DOUBLE);
        }

        f.setParam(param);

        if (token.getValueString().equals("void"))
        {
            f.setReturncount(0);
        }
        else
        {
            f.setReturncount(1);
        }

        f.setFuncname(token1.getValueString());
        now++;
        funtionTable.set(funtionTable.size()-1,f);
        localvar = 0;
        analyseblockstmt();
        now--;
        f = funtionTable.get(funtionTable.size()-1);
        f.setLocalvar(localvar);
        f.getInstructions().add(new Instruction(Operation.ret));
        funtionTable.set(funtionTable.size()-1,f);
    }

    private int analyseblockstmt() throws CompileError
    {
        expect(TokenType.LBParen);

        while (!check(TokenType.RBParen))
        {
            int i = analysestmt();
        }
        expect(TokenType.RBParen);
        return 1;
    }

    private void analyseif_stmt() throws CompileError
    {
        expect(TokenType.IF_KW);
        analyseexpr();
        List<Instruction> in = getnowinstructions();
        in.add(new Instruction(Operation.brtrue,1));
        in.add(new Instruction(Operation.br,0));
        int start = in.size();
        analyseblockstmt();
        in = getnowinstructions();
        int end = in.size();
        in.get(start-1).setX(end-start+1);

        if (check(TokenType.ELSE_KW))
        {
            expect(TokenType.ELSE_KW);
            int start2 = in.size();
            in.add(new Instruction(Operation.br,0));
            if (check(TokenType.LBParen))
            {
                analyseblockstmt();
                in = getnowinstructions();
                int end1 = in.size();
                in.get(start2).setX(end1-start2);
            }
            else if (check(TokenType.IF_KW))
            {
                analyseif_stmt();
                in = getnowinstructions();
                int end1 = in.size();
                in.get(start2).setX(end1-start2);
            }
        }
        in.add(new Instruction(Operation.br,0));

    }

    private void analysewhile_stmt() throws CompileError
    {
        expect(TokenType.WHILE_KW);

        List<Instruction> in =getnowinstructions();
        int start1= in.size();

        in.add(new Instruction(Operation.br,0));
//        var value = analyseexpr();
        analyseexpr();

        in = getnowinstructions();

        in.add(new Instruction(Operation.brtrue,1));
        int start2 = in.size();
        in.add(new Instruction(Operation.br,1));
        whilestart = in.size();
        int i = analyseblockstmt();
        in = getnowinstructions();
        whileend = in.size();
        for (int j=start1;j<whileend;j++)
        {
            if (in.get(j).getX()==null)
            {
                continue;
            }
            else if (in.get(j).getX()==-10000)
            {
                in.get(j).setX(whileend-j);
            }
            else if (in.get(j).getX()==-10001)
            {
                in.get(j).setX(start1-j);
            }
        }
        if (i==0)
        {
            return;
        }
        in = getnowinstructions();
        int end = in.size();
        in.add(new Instruction(Operation.br,start1-end));


        in.get(start2).setX(end-start2);
    }

    private void analysereturn_stmt() throws CompileError
    {
        expect(TokenType.RETURN_KW);
//        var value = analyseexpr();
        if (check(TokenType.Semicolon))
        {
            FuntionEntry f = gt.getnowfunction();
            if (f.getReturnType()!=ReturnType.VOID)
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ peek().getStartPos());
            }
            expect(TokenType.Semicolon);
            List<Instruction> in = getnowinstructions();
            in.add(new Instruction(Operation.ret));
        }
        else {
            List<Instruction> in =getnowinstructions();
            in.add(new Instruction(Operation.arga,0));
            TypeValue tv = analyseexpr();
            FuntionEntry f = gt.getnowfunction();

            if ( f.getReturnType() != tv.type)
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ peek().getStartPos());
            }

            while (peek().getTokenType()==TokenType.Plus)
            {
                expect(TokenType.Plus);

                tv = analyseexpr();
                f = gt.getnowfunction();

                if ( f.getReturnType() != tv.type)
                {
                    throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ peek().getStartPos());
                }

                in.add(new Instruction(Operation.addi));

            }
            expect(TokenType.Semicolon);
            if (tv.type!=ReturnType.VOID)
            {
                in.add(new Instruction(Operation.store64));
            }
            in.add(new Instruction(Operation.ret));

        }


    }
    private int analysepcallparamlist() throws  CompileError
    {
        int count = 1;
        analyseexpr();
        while(check(TokenType.Comma))
        {
            expect(TokenType.Comma);
            analyseexpr();
            count ++;
        }
        return count;
    }
    public boolean isop() throws CompileError
    {
        var token = peek();
        switch (token.getTokenType())
        {
            case Plus:return true;
            case Minus:return true;
            case Mult:return true;
            case Div:return true;
            case DoubleEqual:return true;
            case Nequal:return true;
            case Less:return true;
            case More:return true;
            case Mequal:return true;
            case Lequal:return true;
            default:return false;
        }
    }

    public boolean typematch(TokenType tp,ReturnType rp)
    {
        if (rp==ReturnType.VOID)
        {
            return false;
        }
        else if (tp==TokenType.CHAR_LITERAL&&rp == ReturnType.CHAR)
        {
            return true;
        }
        else if (tp==TokenType.Uint && rp == ReturnType.INT)
        {
            return true;
        }
        else if (tp == TokenType.STRING_LITERAL && rp ==ReturnType.STRING)
        {
            return true;
        }
        else if (tp==TokenType.DOUBLE_LITERAL && rp == ReturnType.DOUBLE)
        {
            return true;
        }
        return false;
    }

    public boolean isitem(Token T)
    {
        if (T.getTokenType()==TokenType.Ident || T.getTokenType() ==TokenType.Uint ||T.getTokenType()==TokenType.DOUBLE_LITERAL||T.getTokenType()==TokenType.CHAR_LITERAL)
        {
            return true;
        }
        else return false;
    }

    public int oprity(Token in,Token out)
    {
        if (in.getTokenType()==TokenType.Mult&&out.getTokenType()==TokenType.Plus)
        {
            return 1;
        }
        if (isitem(in) && out.getTokenType()==TokenType.Plus)
        {
            return 1;
        }
        if (in.getTokenType()==TokenType.Plus && isitem(out) )
        {
            return -1;
        }
        if (out.getTokenType() == TokenType.Semicolon || out.getTokenType() == TokenType.LBParen)
        {
            return 1;
        }
        if (in.getTokenType()==TokenType.Minus && out.getTokenType()==TokenType.Minus)
        {
            return 1;
        }
        if (isitem(in)&&out.getTokenType()==TokenType.LBParen)
        {
            return 1;
        }
        if (in.getTokenType() == TokenType.DoubleEqual &&out.getTokenType()==TokenType.LBParen)
        {
            return 1;
        }
        if (in.getTokenType() == TokenType.Nequal &&out.getTokenType()==TokenType.LBParen)
        {
            return 1;
        }
        if (in.getTokenType() == TokenType.Mequal &&out.getTokenType()==TokenType.LBParen)
        {
            return 1;
        }
        if (in.getTokenType() == TokenType.Lequal &&out.getTokenType()==TokenType.LBParen)
        {
            return 1;
        }
        if (in.getTokenType() == TokenType.More &&out.getTokenType()==TokenType.LBParen)
        {
            return 1;
        }
        if (in.getTokenType() == TokenType.Less &&out.getTokenType()==TokenType.LBParen)
        {
            return 1;
        }
        if (in.getTokenType()==TokenType.LParen && out.getTokenType() == TokenType.RParen)
        {
            return 0;
        }
        if (out.getTokenType()==TokenType.RParen)
        {
            return 1;
        }
        if (in.getTokenType()==TokenType.Less &&out.getTokenType()==TokenType.Minus)
        {
            return -1;
        }
        if (in.getTokenType()==TokenType.Div && out.getTokenType()== TokenType.Mult)
        {
            return 1;
        }
        if (out.getTokenType()==TokenType.Comma)
        {
            return 1;
        }
        if (in.getTokenType()==TokenType.Plus && out.getTokenType()==TokenType.RParen)
        {
            return 1;
        }
        if (in.getTokenType()==TokenType.Plus && out.getTokenType()==TokenType.Plus)
        {
            return 1;
        }
        if (in.getTokenType()==TokenType.Plus && out.getTokenType()==TokenType.Minus)
        {
            return 1;
        }
        if (in.getTokenType()==TokenType.Minus && out.getTokenType()==TokenType.Plus)
        {
            return 1;
        }
        if (in.getTokenType()==TokenType.Minus&& out.getTokenType()==TokenType.Minus)
        {
            return 1;
        }
        return -1;
    }
    public boolean iscpm(TokenType a)
    {
        if (a==TokenType.Comma.More)
        {
            return true;
        }
        return false;
    }
    private TypeValue analyseopgexpr(Token first) throws CompileError
    {
        TypeValue re = new TypeValue();
        List<Token> stackop = new ArrayList<>();
        List<Token> stackitem = new ArrayList<>();

        int top;

        if (isitem(first))
        {
            stackitem.add(first);
        }
        else {
            stackop.add(first);
        }

        while (true)
        {
            int oprit;
            if (stackop.size()==0)
            {
                oprit = -1;
                if (check(TokenType.LBParen))
                {
                    return re;
                }
            }

            else
            {
                oprit = oprity(stackop.get(stackop.size()-1),peek());
                if (stackop.get(stackop.size()-1).getTokenType()==TokenType.Minus&&peek().getTokenType()==TokenType.Minus&&stackitem.size()==0)
                {
                    oprit=-1;
                }
            }

            if (oprit<0)
            {
                if (isitem(peek()))
                {
                    stackitem.add(peek());
                }
                else {
                    if (stackop.size()!=0&&iscpm(stackop.get(stackop.size()-1).getTokenType())&&peek().getTokenType()==TokenType.Minus&&stackitem.size()<=1)
                    {
                        stackitem.add(peek());
                    }
                    else stackop.add(peek());
                }
                expect(peek().getTokenType());
            }
            else if (oprit==0)
            {
                stackop.remove(stackop.size()-1);
                expect(TokenType.RParen);
            }
            else if (oprit>0)
            {
                List<Instruction> instructions1 = getnowinstructions();
                Token op = stackop.get(stackop.size()-1);
                int arg = 0;
                FuntionEntry f = funtionTable.get(funtionTable.size()-1);
                if (f.getReturncount()!=0)
                {
                    arg = 1;
                }
                if (op.getTokenType()==TokenType.Plus)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        boolean global = false;
                        if(s==null)
                        {
                            s = gt.findglobalsymbolbyname(l.getValueString());
                            global = true;
                        }
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (global)
                        {
                            instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                        }
                        else if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())-gt.findparamindex("")+arg));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }
                    else if (l.getTokenType()==TokenType.CHAR_LITERAL)
                    {
                        instructions1.add(new Instruction(Operation.push, r.getValue().hashCode()));
                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    instructions1.add(new Instruction(Operation.addi));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                    stackitem.add(new Token(TokenType.expr));
                }
                else if (op.getTokenType()==TokenType.Minus) {
                    if (stackitem.size() == 1) {
                        Token l = stackitem.get(stackitem.size() - 1);
                        if (l.getTokenType() == TokenType.Ident) {
                            SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                            boolean global = false;
                            if(s==null)
                            {
                                s = gt.findglobalsymbolbyname(l.getValueString());
                                global = true;
                            }
                            if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                            {
                                re.type = ReturnType.INT;
                            }
                            if (global)
                            {
                                instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                            }
                            else if (s.getSymbolType()==SymbolType.PARAM)
                            {
                                instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                            }
                            else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())-gt.findparamindex("")+arg));
                            instructions1.add(new Instruction(Operation.load64));
                        } else if (l.getTokenType() == TokenType.Uint) {
                            instructions1.add(new Instruction(Operation.push, (Integer) l.getValue()));
                            re.type = ReturnType.INT;
                        }
                        else if (l.getTokenType()==TokenType.DOUBLE_LITERAL)
                        {
                            instructions1.add(new Instruction(Operation.push, Data2Byte.doubletoint((double)l.getValue())));
                            re.type = ReturnType.DOUBLE;
                        }

                        while (stackop.get(stackop.size()-1).getTokenType()==TokenType.Minus)
                        {
                            instructions1.add(new Instruction(Operation.negi));
                            stackop.remove(stackop.size()-1);
                            Token temp = stackitem.get(stackitem.size()-1);
                            if (stackop.size()==0)break;
                        }



                    } else {
                        Token l = stackitem.get(stackitem.size() - 2);
                        Token r = stackitem.get(stackitem.size() - 1);

                        if (l.getTokenType() == TokenType.Ident) {
                            SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                            boolean global = false;
                            if(s==null)
                            {
                                s = gt.findglobalsymbolbyname(l.getValueString());
                                global = true;
                            }
                            if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                            {
                                re.type = ReturnType.INT;
                            }
                            if (global)
                            {
                                instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                            }
                            else if (s.getSymbolType()==SymbolType.PARAM)
                            {
                                instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                            }
                            else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())-gt.findparamindex("")+arg));
                            instructions1.add(new Instruction(Operation.load64));
                        } else if (l.getTokenType() == TokenType.Uint) {
                            instructions1.add(new Instruction(Operation.push, (Integer) l.getValue()));

                        }
                        else if (l.getTokenType()==TokenType.DOUBLE_LITERAL)
                        {
                            instructions1.add(new Instruction(Operation.push, Data2Byte.doubletoint((double)l.getValue())));
                            re.type = ReturnType.DOUBLE;
                        }

                        if (r.getTokenType() == TokenType.Ident) {
                            SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                            if (s.getSymbolType()==SymbolType.PARAM)
                            {
                                instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                            }
                            else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())-gt.findparamindex(" ")+arg));
                            instructions1.add(new Instruction(Operation.load64));
                        } else if (r.getTokenType() == TokenType.Uint) {
                            instructions1.add(new Instruction(Operation.push, (Integer) r.getValue()));

                            re.type = ReturnType.INT;
                        }
                        else if (r.getTokenType()==TokenType.CHAR_LITERAL)
                        {
                            instructions1.add(new Instruction(Operation.push, r.getValue().hashCode()));
                        }
                        instructions1.add(new Instruction(Operation.subi));
                        if (r.getTokenType()==TokenType.expr&&l.getTokenType()!=TokenType.expr)
                        {
                            instructions1.add(new Instruction(Operation.negi));
                        }
                        stackitem.remove(stackitem.size() - 1);
                        stackitem.remove(stackitem.size() - 1);
                        stackop.remove(stackop.size()-1);
                        stackitem.add(new Token(TokenType.expr));
                    }
                }
                else if (op.getTokenType()==TokenType.DoubleEqual)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())-gt.findparamindex(" ")+arg));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    else if (r.getTokenType()==TokenType.CHAR_LITERAL)
                    {
                        instructions1.add(new Instruction(Operation.push, r.getValue().hashCode()));
                        if ((char)r.getValue()=='\r')
                        {
                            instructions1.get(instructions1.size()-1).setX(13);
                        }
                    }
                    instructions1.add(new Instruction(Operation.cmpi));
                    instructions1.add(new Instruction(Operation.not));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                }
                else if (op.getTokenType()==TokenType.Nequal)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    instructions1.add(new Instruction(Operation.cmpi));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                }
                else if (op.getTokenType()==TokenType.Mequal)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    instructions1.add(new Instruction(Operation.cmpi));
                    instructions1.add(new Instruction(Operation.setlt));
                    instructions1.add(new Instruction(Operation.not));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                }
                else if (op.getTokenType()==TokenType.Lequal)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    instructions1.add(new Instruction(Operation.cmpi));
                    instructions1.add(new Instruction(Operation.setgt));
                    instructions1.add(new Instruction(Operation.not));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                }
                else if (op.getTokenType()==TokenType.More)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = new Token();
                    int flagr = 0;
                    if (l.getTokenType()==TokenType.Minus)
                    {
                        l = stackitem.get(stackitem.size()-3);
                        flagr = -1;
                    }
                    r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())-gt.findparamindex(" ")+arg));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));
                    }
                    else if (r.getTokenType()==TokenType.CHAR_LITERAL)
                    {
                        instructions1.add(new Instruction(Operation.push, r.getValue().hashCode()));
                    }
                    if (flagr == -1){
                        instructions1.add(new Instruction(Operation.negi));
                    }
                    instructions1.add(new Instruction(Operation.cmpi));
                    instructions1.add(new Instruction(Operation.setgt));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                }
                else if (op.getTokenType()==TokenType.Less)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    else if (r.getTokenType()==TokenType.CHAR_LITERAL)
                    {
                        instructions1.add(new Instruction(Operation.push,r.getValue().hashCode()));
                    }

                    if (r.getTokenType()==TokenType.expr){
                        instructions1.add(new Instruction(Operation.cmpi));
                        instructions1.add(new Instruction(Operation.setgt));
                        stackitem.remove(stackitem.size()-1);
                        stackitem.remove(stackitem.size()-1);
                        stackop.remove(stackop.size()-1);

                    }
                    else {
                        instructions1.add(new Instruction(Operation.cmpi));
                        instructions1.add(new Instruction(Operation.setlt));
                        stackitem.remove(stackitem.size() - 1);
                        stackitem.remove(stackitem.size() - 1);
                        stackop.remove(stackop.size() - 1);
                    }
                }
                else if (op.getTokenType()== TokenType.Mult)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        boolean global = false;
                        if(s==null)
                        {
                            s = gt.findglobalsymbolbyname(l.getValueString());
                            global = true;
                        }
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (global)
                        {
                            instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                        }
                        else if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())-gt.findparamindex("")+arg));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }
                    else if (l.getTokenType()==TokenType.DOUBLE_LITERAL)
                    {
                        instructions1.add(new Instruction(Operation.push, Data2Byte.doubletoint((double)l.getValue())));
                        re.type = ReturnType.DOUBLE;
                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        boolean global = false;
                        if(s==null)
                        {
                            s = gt.findglobalsymbolbyname(r.getValueString());
                            global = true;
                        }
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (global)
                        {
                            instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                        }
                        else if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())-gt.findparamindex("")+arg));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    else if (r.getTokenType()==TokenType.DOUBLE_LITERAL)
                    {
                        instructions1.add(new Instruction(Operation.push, Data2Byte.doubletoint((double)r.getValue())));
                        re.type = ReturnType.DOUBLE;
                    }
                    instructions1.add(new Instruction(Operation.muli));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                    stackitem.add(new Token(TokenType.expr));
                }
                else if (op.getTokenType()== TokenType.Div)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        boolean global = false;
                        if(s==null)
                        {
                            s = gt.findglobalsymbolbyname(l.getValueString());
                            global = true;
                        }
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (global)
                        {
                            instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                        }
                        else if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())-gt.findparamindex("")+arg));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    instructions1.add(new Instruction(Operation.divi));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                    stackitem.add(new Token(TokenType.expr));
                }
                if ((check(TokenType.Semicolon)||check(TokenType.LBParen)||check(TokenType.RParen)||check(TokenType.Comma))&&stackop.size()==0)
                {
                    break;
                }
                else {
                    if (peek().getTokenType()!=TokenType.LBParen)
                    {
//                        expect(peek().getTokenType());
                    }
                }
            }

        }
        return re;
    }

    private TypeValue analyseopgexpr2(Token first) throws CompileError
    {
        TypeValue re = new TypeValue();
        List<Token> stackop = new ArrayList<>();
        List<Token> stackitem = new ArrayList<>();

        int top;

        if (isitem(first))
        {
            stackitem.add(first);
        }
        else {
            stackop.add(first);
        }

        while (true)
        {
            int oprit;
            if (stackop.size()==0)
            {
                oprit = -1;
                if (check(TokenType.LBParen)){
                    return re;
                }
            }
            else oprit = oprity(stackop.get(stackop.size()-1),peek());

            if (oprit<0)
            {
                if (isitem(peek()))
                {
                    stackitem.add(peek());
                }
                else {
                    if (stackop.size()!=0&&iscpm(stackop.get(stackop.size()-1).getTokenType())&&peek().getTokenType()==TokenType.Minus&&stackitem.size()>1)
                    {
                        stackitem.add(peek());
                    }
                    else stackop.add(peek());
                }
                expect(peek().getTokenType());
            }
            else if (oprit==0)
            {
                stackop.remove(stackop.size()-1);
                expect(TokenType.RParen);
            }
            else if (oprit>0)
            {
                List<Instruction> instructions1 = getnowinstructions();
                Token op = stackop.get(stackop.size()-1);
                int arg = 0;
                FuntionEntry f = funtionTable.get(funtionTable.size()-1);
                if (f.getReturncount()!=0)
                {
                    arg = 1;
                }
                if (op.getTokenType()==TokenType.Plus)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        boolean global = false;
                        if(s==null)
                        {
                            s = gt.findglobalsymbolbyname(l.getValueString());
                            global = true;
                        }
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (global)
                        {
                            instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                        }
                        else if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }
                    else if (l.getTokenType()==TokenType.CHAR_LITERAL)
                    {
                        instructions1.add(new Instruction(Operation.push, l.getValue().hashCode()));
                        re.type = ReturnType.INT;
                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        boolean global = false;
                        if(s==null)
                        {
                            s = gt.findglobalsymbolbyname(r.getValueString());
                            global = true;
                        }
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (global)
                        {
                            instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                        }
                        else if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    instructions1.add(new Instruction(Operation.addi));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                    stackitem.add(new Token(TokenType.expr));
                }
                else if (op.getTokenType()==TokenType.Minus) {
                    if (stackitem.size() == 1) {
                        Token l = stackitem.get(stackitem.size() - 1);
                        if (l.getTokenType() == TokenType.Ident) {
                            SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                            if (s.getSymbolType() == SymbolType.INT) {
                                re.type = ReturnType.INT;
                            }
                            if (s.getSymbolType()==SymbolType.PARAM)
                            {
                                instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                            }
                            else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())+ arg));
                            instructions1.add(new Instruction(Operation.load64));
                        } else if (l.getTokenType() == TokenType.Uint) {
                            instructions1.add(new Instruction(Operation.push, (Integer) l.getValue()));
                            re.type = ReturnType.INT;
                        }

                        while (stackop.get(stackop.size()-1).getTokenType()==TokenType.Minus)
                        {
                            instructions1.add(new Instruction(Operation.negi));
                            stackop.remove(stackop.size()-1);
                            Token temp = stackitem.get(stackitem.size()-1);
                            temp.setValue(-(Integer)temp.getValue());
                            if (stackop.size()==0)break;
                        }



                    } else {
                        Token l = stackitem.get(stackitem.size() - 2);
                        Token r = stackitem.get(stackitem.size() - 1);

                        if (l.getTokenType() == TokenType.Ident) {
                            SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                            boolean global = false;
                            if(s==null)
                            {
                                s = gt.findglobalsymbolbyname(l.getValueString());
                                global = true;
                            }
                            if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                            {
                                re.type = ReturnType.INT;
                            }
                            if (global)
                            {
                                instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                            }
                            else if (s.getSymbolType()==SymbolType.PARAM)
                            {
                                instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                            }
                            else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                            instructions1.add(new Instruction(Operation.load64));
                        } else if (l.getTokenType() == TokenType.Uint) {
                            instructions1.add(new Instruction(Operation.push, (Integer) l.getValue()));

                        }

                        if (r.getTokenType() == TokenType.Ident) {
                            SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                            boolean global = false;
                            if(s==null)
                            {
                                s = gt.findglobalsymbolbyname(r.getValueString());
                                global = true;
                            }
                            if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                            {
                                re.type = ReturnType.INT;
                            }
                            if (global)
                            {
                                instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                            }
                            else if (s.getSymbolType()==SymbolType.PARAM)
                            {
                                instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                            }
                            else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                            instructions1.add(new Instruction(Operation.load64));
                        } else if (r.getTokenType() == TokenType.Uint) {
                            instructions1.add(new Instruction(Operation.push, (Integer) r.getValue()));

                            re.type = ReturnType.INT;
                        }
                        instructions1.add(new Instruction(Operation.subi));
                        if (r.getTokenType()==TokenType.expr&&l.getTokenType()!=TokenType.expr)
                        {
                            instructions1.add(new Instruction(Operation.negi));
                        }

                        stackitem.remove(stackitem.size() - 1);
                        stackitem.remove(stackitem.size() - 1);
                        stackop.remove(stackop.size()-1);
                        stackitem.add(new Token(TokenType.expr));
                    }
                }
                else if (op.getTokenType()== TokenType.Mult)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    instructions1.add(new Instruction(Operation.muli));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                    stackitem.add(new Token(TokenType.expr));
                }
                else if (op.getTokenType()== TokenType.Div)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    instructions1.add(new Instruction(Operation.divi));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                    stackitem.add(new Token(TokenType.expr));
                }
                else if (op.getTokenType()==TokenType.DoubleEqual)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())-gt.findparamindex(" ")+arg));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    instructions1.add(new Instruction(Operation.cmpi));
                    instructions1.add(new Instruction(Operation.not));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                }
                else if (op.getTokenType()==TokenType.Nequal)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    instructions1.add(new Instruction(Operation.cmpi));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                }
                else if (op.getTokenType()==TokenType.Mequal)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    instructions1.add(new Instruction(Operation.cmpi));
                    instructions1.add(new Instruction(Operation.setlt));
                    instructions1.add(new Instruction(Operation.not));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                }
                else if (op.getTokenType()==TokenType.Lequal)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }
                    instructions1.add(new Instruction(Operation.cmpi));
                    instructions1.add(new Instruction(Operation.setgt));
                    instructions1.add(new Instruction(Operation.not));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                }
                else if (op.getTokenType()==TokenType.More)
                {
                    Token l = stackitem.get(stackitem.size()-2);

                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        boolean global = false;
                        if(s==null)
                        {
                            s = gt.findglobalsymbolbyname(l.getValueString());
                            global = true;
                        }
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (global)
                        {
                            instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                        }
                        else if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        boolean global = false;
                        if(s==null)
                        {
                            s = gt.findglobalsymbolbyname(r.getValueString());
                            global = true;
                        }
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (global)
                        {
                            instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                        }
                        else if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));
                    }

                    instructions1.add(new Instruction(Operation.cmpi));
                    instructions1.add(new Instruction(Operation.setgt));
                    stackitem.remove(stackitem.size()-1);
                    stackitem.remove(stackitem.size()-1);
                    stackop.remove(stackop.size()-1);
                }
                else if (op.getTokenType()==TokenType.Less)
                {
                    Token l = stackitem.get(stackitem.size()-2);
                    Token r = stackitem.get(stackitem.size()-1);

                    if (l.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(l.getValueString());
                        boolean global = false;
                        if(s==null)
                        {
                            s = gt.findglobalsymbolbyname(l.getValueString());
                            global = true;
                        }
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (global)
                        {
                            instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                        }
                        else if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(l.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        re.type = ReturnType.INT;
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));

                    }

                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        boolean global = false;
                        if(s==null)
                        {
                            s = gt.findglobalsymbolbyname(r.getValueString());
                            global = true;
                        }
                        if (s.getSymbolType()==SymbolType.INT||s.getReturnType() == ReturnType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        if (global)
                        {
                            instructions1.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                        }
                        else if (s.getSymbolType()==SymbolType.PARAM)
                        {
                            instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+ arg));
                        }
                        else instructions1.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(r.getValueString())));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));

                    }

                    if (r.getTokenType()==TokenType.expr){
                        instructions1.add(new Instruction(Operation.cmpi));
                        instructions1.add(new Instruction(Operation.setgt));
                        stackitem.remove(stackitem.size()-1);
                        stackitem.remove(stackitem.size()-1);
                        stackop.remove(stackop.size()-1);

                    }
                    else {
                        instructions1.add(new Instruction(Operation.cmpi));
                        instructions1.add(new Instruction(Operation.setlt));
                        stackitem.remove(stackitem.size() - 1);
                        stackitem.remove(stackitem.size() - 1);
                        stackop.remove(stackop.size() - 1);
                    }
                }
                if ((check(TokenType.Semicolon)||check(TokenType.LBParen))&&stackop.size()==0)
                {
                    break;
                }
                else {

                }
            }

        }
        return re;
    }
    private TypeValue analyseexpr() throws CompileError
    {
        var token = peek();
        if (check(TokenType.Ident))
        {

            var tokent = expect(TokenType.Ident);

            //赋值语句
            if (check(TokenType.Equal))
            {
                List<Instruction> in = getnowinstructions();
                int arg = gt.getnowfunction().getReturncount()==0?0:1;
                expect(TokenType.Equal);
                SymbolEntry s1 = gt.findsymbolbyname(tokent.getValueString());
                boolean global = false;
                if (s1==null)
                {
                    s1 = gt.findglobalsymbolbyname(tokent.getValueString());
                    global = true;
                }
                if (s1.isConstant())
                {
                    throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ token.getStartPos());
                }
                if (global)
                {
                    in.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s1.getSysname())));
                }

                else if (s1.getSymbolType()==SymbolType.PARAM)
                {
                    in.add(new Instruction(Operation.arga,gt.findparamindex(s1.getSysname())));
                }
                else {
                    in.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(tokent.getValueString())-gt.findparamindex(" ")+arg));
                    int index1 = gt.findsymbolindexbyname(tokent.getValueString());
                }


                TypeValue TV = analyseexpr();
                SymbolEntry s = gt.findsymbolbyname(tokent.getValueString());
                if (s==null)
                {
                    s = gt.findglobalsymbolbyname(tokent.getValueString());
                }
                if (typematch(s.getType(),TV.type)||s.returnType==TV.type){
                    in.add(new Instruction(Operation.store64,null));
                }
                else {
                    throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ token.getStartPos());
                }
                return new TypeValue(ReturnType.VOID,null);
            }

            //函数调用
            else if (check(TokenType.LParen))
            {
                //判断函数是否存在、是否为标准库函数。

                if (isstandard(tokent.getValueString()))
                {
                    FuntionEntry f = getstandardfun(tokent.getValueString());
                    int returncount = f.getReturncount();
                    List<Instruction> instructions = getnowinstructions();
                    int ind = gt.findsymbolindexstandardname(tokent.getValueString());
                    instructions.add(new Instruction(Operation.stackalloc,returncount));
                    expect(TokenType.LParen);
                    if (!check(TokenType.RParen)) {
                        analysepcallparamlist();//参数压栈
                    }
                    expect(TokenType.RParen);
                    instructions.add(new Instruction(Operation.callname,ind));
                    return new TypeValue(f.getReturnType(),null);

                }
                else {
                    var fun = gt.findfuntionbyname(tokent.getValueString());
                    List<Instruction> in = getnowinstructions();

                    if (fun==null)
                    {
                        throw new AnalyzeError(ErrorCode.NotDeclared,tokent.getStartPos());
                    }
                    else {
                        int returncount = fun.getReturncount();
                        in.add(new Instruction(Operation.stackalloc,returncount));
                        expect(TokenType.LParen);
                        if (!check(TokenType.RParen)) {
                            analysepcallparamlist();//参数压栈
                        }
                        expect(TokenType.RParen);
                        int ind = gt.findfuntionindexbyname(tokent.getValueString());
                        in.add(new Instruction(Operation.call,ind));
                    }
                    return new TypeValue(fun.getReturnType(),null);

                }


            }

            else if (isop())
            {
                return analyseopgexpr(tokent);
            }
            else if (!check(TokenType.AS_KW)&&!isop())
            {
                List<Instruction> in = getnowinstructions();
                boolean global = false;
                 SymbolEntry s = gt.findsymbolbyname(tokent.getValueString());
                 if (s==null)
                 {
                     s = gt.findglobalsymbolbyname(tokent.getValueString());
                     global = true;
                 }
                 if (s==null)
                 {
                      throw new AnalyzeError(ErrorCode.NotDeclared,peek().getStartPos());
                 }
                 if (global)
                 {
                     in.add(new Instruction(Operation.globa,gt.findglobalsymbolindexbyname(s.getSysname())));
                 }
                 else if (s.getSymbolType()==SymbolType.PARAM){
                     int order = gt.findparamindex(tokent.getValueString());
                     in.add(new Instruction(Operation.arga, order));
                 }
                else {
                    int arg = 0;
                    FuntionEntry f= gt.getnowfunction();
                    if (f.getReturncount()!=0)
                    {
                        arg = 1;
                    }
                    int pcount = gt.findparamindex(" ");
                    in.add(new Instruction(Operation.loca, gt.findsymbolindexbyname(tokent.getValueString())-pcount+arg));
                }
                in.add(new Instruction(Operation.load64));

                return new TypeValue(ReturnType.INT,null);
            }
        }

        else if (check(TokenType.Minus))
        {
            Token token1 = expect(TokenType.Minus);
            return  analyseopgexpr(token1);

        }
        else if (check(TokenType.Uint))
        {
            Token to = expect(TokenType.Uint);
            if (isop())
            {
                analyseopgexpr(to);
            }
            else {
                List<Instruction> in = getnowinstructions();
                in.add(new Instruction(Operation.push,(Integer) to.getValue()));
            }
            return new TypeValue(ReturnType.INT,null);
        }
        else if (check(TokenType.DOUBLE_LITERAL))
        {

            Token d = expect(TokenType.DOUBLE_LITERAL);
            if (check(TokenType.Semicolon))
            {
                List<Instruction> in = getnowinstructions();
                in.add(new Instruction(Operation.push,Data2Byte.doubletoint((double)d.getValue())));
                return new TypeValue(ReturnType.DOUBLE,null);
            }
            else if (peek().getValueString().equals("f"))
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ token.getStartPos());
            }
            else return analyseopgexpr(d);
        }
        else if (check(TokenType.STRING_LITERAL))
        {
            FuntionEntry f = funtionTable.get(funtionTable.size()-1);
            List<SymbolEntry> symlist = symbolTable;
            List<Instruction> instructions = getnowinstructions();
            SymbolEntry s = new SymbolEntry();
            s.setSysname(peek().getValueString());
            s.setSymbolType(SymbolType.STRING);
            symlist.add(s);
            instructions.add(new Instruction(Operation.push,symlist.size()-1));
            expect(TokenType.STRING_LITERAL);
        }
        else if (check(TokenType.LParen))
        {
            Token l = expect(TokenType.LParen);
            TypeValue re = analyseopgexpr2(l);

            return re;
        }
        else if (check(TokenType.CHAR_LITERAL))
        {
            Token l = expect(TokenType.CHAR_LITERAL);
            if (check(TokenType.RParen))
            {
                List<Instruction> in = getnowinstructions();
                in.add(new Instruction(Operation.push,l.getValue().hashCode()));
            }
            else {
                TypeValue re = analyseopgexpr2(l);
                return re;
            }


        }

        else throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ peek().getStartPos());
        if (check(TokenType.AS_KW))
        {
            expect(TokenType.AS_KW);
        }
        else if (isop())
        {
            analyseoperator();
            analyseexpr();
        }

        return null;
    }

    private void analyseoperator() throws CompileError
    {
        if (check(TokenType.Plus))
        {
            expect(TokenType.Plus);
        }
        else if (check(TokenType.Minus))
        {
            expect(TokenType.Minus);
        }
        else if (check(TokenType.Mult))
        {
            expect(TokenType.Mult);
        }
        else if (check(TokenType.Div))
        {
            expect(TokenType.Div);
        }
        else if (check(TokenType.DoubleEqual))
        {
            expect(TokenType.DoubleEqual);
        }
        else if (check(TokenType.Nequal))
        {
            expect(TokenType.Nequal);
        }
        else if (check(TokenType.Less))
        {
            expect(TokenType.Less);
        }
        else if (check(TokenType.More))
        {
            expect(TokenType.More);
        }
        else if (check(TokenType.Lequal))
        {
            expect(TokenType.Lequal);
        }
        else if (check(TokenType.Mequal))
        {
            expect(TokenType.Mequal);
        }

    }

    private void analyseexpr_stmt() throws CompileError
    {
//        var value = analyseexpr();
        analyseexpr();
        expect(TokenType.Semicolon);
    }

    /**
     * 最复杂的部分
     * @throws CompileError
     */
    private int analysestmt() throws CompileError
    {
         var token = peek();
        if (check(TokenType.LET_KW) || check (TokenType.CONST_KW))
        {
            analysedecl_stmt();
        }
        else if (check(TokenType.IF_KW))
        {
            analyseif_stmt();
        }
        else if (check(TokenType.WHILE_KW))
        {
            whilecount++;
            analysewhile_stmt();
            whilecount--;
        }
        else if (check(TokenType.RETURN_KW))
        {
            analysereturn_stmt();
            return 0;
        }
        else if (check(TokenType.Semicolon))
        {
            expect(TokenType.Semicolon);
        }
        else if (check(TokenType.LBParen))
        {
            analyseblockstmt();
        }
        else if (check(TokenType.BREAK_KW))
        {
            if (whilecount==0)
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ peek().getStartPos());
            }
            analysebreak_stmt();
        }
        else if (check(TokenType.CONTINUE_KW))
        {
            if (whilecount==0)
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ peek().getStartPos());
            }
            analysecontinue_stmt();
        }
        else {
            analyseexpr_stmt();
        }
        return 1;
    }

    private void analysebreak_stmt() throws CompileError
    {
        expect(TokenType.BREAK_KW);
        expect(TokenType.Semicolon);
        List<Instruction> in = getnowinstructions();
        int now = in.size();
        in.add(new Instruction(Operation.br,-10000));
    }

    private void analysecontinue_stmt() throws  CompileError
    {
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.Semicolon);
        List<Instruction> in = getnowinstructions();
        int now = in.size();
        in.add(new Instruction(Operation.br,-10001));
    }
    /**
     *
     * @return
     */
    private int analyseparamlist() throws CompileError
    {
        int count = 1;
        analyseparam();
        while (check(TokenType.Comma))
        {
            expect(TokenType.Comma);
            analyseparam();
            count++;
        }
        expect(TokenType.RParen);

        return count;
    }
    private void analyseparam() throws  CompileError
    {
        if (check(TokenType.CONST_KW))
        {
            expect(TokenType.CONST_KW);
        }
        var token = expect(TokenType.Ident);
        expect(TokenType.Collon);
        var token2 = expect(TokenType.Ident);
        List<SymbolEntry> symlist = getnowsymboltable();
        SymbolEntry sym = new SymbolEntry();
        sym.setSymbolType(SymbolType.PARAM);
        sym.setSysname(token.getValueString());
        sym.setType(token2.getTokenType());
        sym.setInitialized(true);
        sym.setConstant(false);
        sym.setLevel(now);
        symlist.add(sym);
        if (token2.getValue().equals("int"))
        {
            sym.setReturnType(ReturnType.INT);
        }
        else if (token2.getValue().equals("double"))
        {
            sym.setReturnType(ReturnType.DOUBLE);
        }
        else if (token2.getValue().equals("void"))
        {

        }
        else throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ token2.getStartPos());

    }
    /**
     * 类型ty
     * @throws CompileError
     */
    private Token analysety() throws CompileError {
        var token = peek();
        if (token.getValue().equals("int")||token.getValue().equals("double")||token.getValue().equals("void"))
        {
            return expect(TokenType.Ident);
        }
        else {
            throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ token.getStartPos());
        }
    }


    private void analysedecl_stmt() throws CompileError
    {

        if (check(TokenType.LET_KW))
        {
            localvar += 1;
            expect(TokenType.LET_KW);
            var token = expect(TokenType.Ident);
            expect(TokenType.Collon);
            var token2 = analysety();

            if (token2.getValueString().equals("void"))
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ token2.getStartPos());
            }

            if (check(TokenType.Equal))
            {
                List<SymbolEntry> nowsymboltable = getnowsymboltable();
                var sys = nowgetSymbol(token.getValueString());
                int off = nowsymboltable.size();
                SymbolEntry s = new SymbolEntry();
                if (sys==null)
                {

                    s.setConstant(false);
                    s.setLevel(now);
                    s.setSysname(token.getValueString());
                    s.setInitialized(true);
                    s.setType(token2.getTokenType());
                    if (token2.getValueString().equals("int"))
                    {
                        s.setReturnType(ReturnType.INT);
                    }
                    else if (token2.getValueString().equals("double"))
                    {
                        s.setReturnType(ReturnType.DOUBLE);
                    }

                    nowsymboltable.add(s);
                }
                else {
                    throw new AnalyzeError(ErrorCode.DuplicateDeclaration, /* 当前位置 */ token.getStartPos());
                }
                List<Instruction> in = getnowinstructions();
                FuntionEntry f = gt.getnowfunction();
                int arg=0;
                if (f.getReturncount()!=0)
                {
                    arg=1;
                }
                in.add(new Instruction(Operation.loca,gt.findsymbolindexbyname(token.getValueString())-gt.findparamindex(" ")+arg));
                expect(TokenType.Equal);
                TypeValue tv = analyseexpr();
                if (tv.type!=s.getReturnType())
                {
                    throw new AnalyzeError(ErrorCode.DuplicateDeclaration, /* 当前位置 */ token.getStartPos());
                }
                in.add(new Instruction(Operation.store64));
                expect(TokenType.Semicolon);
            }
            else {
                List<SymbolEntry> nowsymboltable = getnowsymboltable();
                var sys = nowgetSymbol(token.getValueString());
                int off = nowsymboltable.size();
                if (sys==null)
                {
                    SymbolEntry s = new SymbolEntry();
                    s.setConstant(false);
                    s.setLevel(now);
                    s.setSysname(token.getValueString());
                    s.setInitialized(false);
                    s.setType(token2.getTokenType());
                    if (token2.getValueString().equals("int"))
                    {
                        s.setReturnType(ReturnType.INT);
                    }
                    else if (token2.getValueString().equals("double"))
                    {
                        s.setReturnType(ReturnType.DOUBLE);
                    }
                    nowsymboltable.add(s);
                }
                else {
                    throw new AnalyzeError(ErrorCode.DuplicateDeclaration, /* 当前位置 */ token.getStartPos());
                }

            }
        }
        else if (check(TokenType.CONST_KW))
        {

            expect(TokenType.CONST_KW);
            var token = expect(TokenType.Ident);
            expect(TokenType.Collon);
            var token2 = analysety();

            if (token2.getValueString().equals("void"))
            {
                throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ token2.getStartPos());
            }

            List<SymbolEntry> nowsymboltable = getnowsymboltable();
            var sys = nowgetSymbol(token.getValueString());
            int off = nowsymboltable.size();
            if (sys==null)
            {
                SymbolEntry s = new SymbolEntry();
                s.setConstant(true);
                s.setLevel(now);
                s.setSysname(token.getValueString());
                s.setInitialized(true);
                s.setType(token2.getTokenType());

                nowsymboltable.add(s);
            }
            else {
                throw new AnalyzeError(ErrorCode.DuplicateDeclaration, /* 当前位置 */ token.getStartPos());
            }
            instructions.add(new Instruction(Operation.loca,off));
            expect(TokenType.Equal);
            analyseexpr();
            expect(TokenType.Semicolon);

        }
        else throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ peek().getStartPos());
    }


    public boolean isstandard(String f)
    {
        switch (f)
        {
            case "getint":
                return true;
            case "getdouble":
                return true;
            case "getchar":
                return true;
            case "putint":
                return true;
            case "putdouble":
                return true;
            case "putchar":
                return true;
            case "putstr":
                return true;
            case "putln":
                return true;
            default:
                return false;
        }
    }
    public FuntionEntry getstandardfun(String f)
    {
        FuntionEntry fun = new FuntionEntry();
        switch (f)
        {
            case "getint":
                fun.setParam(1);
                fun.setLocalvar(0);
                fun.setFuncname("getint");
                fun.setReturncount(1);
                fun.setReturnType(ReturnType.INT);
                return fun;
            case "getdouble":
                fun.setParam(1);
                fun.setLocalvar(0);
                fun.setFuncname("getdouble");
                fun.setReturnType(ReturnType.DOUBLE);
                fun.setReturncount(1);
                return fun;
            case "getchar":
                fun.setParam(1);
                fun.setLocalvar(0);
                fun.setFuncname("getchar");
                fun.setReturnType(ReturnType.INT);
                fun.setReturncount(1);
                return fun;
            case "putint":
                fun.setParam(0);
                fun.setLocalvar(0);
                fun.setFuncname("putint");
                fun.setReturncount(0);
                fun.setReturnType(ReturnType.VOID);
                return fun;
            case "putdouble":
                fun.setParam(0);
                fun.setLocalvar(0);
                fun.setFuncname("putdouble");
                fun.setReturnType(ReturnType.VOID);
                fun.setReturncount(0);
                return fun;
            case "putchar":
                fun.setParam(0);
                fun.setLocalvar(0);
                fun.setFuncname("putchar");
                fun.setReturnType(ReturnType.VOID);
                fun.setReturncount(0);
                return fun;
            case "putstr":
                fun.setParam(0);
                fun.setLocalvar(0);
                fun.setFuncname("putstr");
                fun.setReturnType(ReturnType.VOID);
                fun.setReturncount(0);
                return fun;
            case "putln":
                fun.setParam(0);
                fun.setLocalvar(0);
                fun.setFuncname("putln");
                fun.setReturnType(ReturnType.VOID);
                fun.setReturncount(0);
                return fun;
            default:
                return null;
        }
    }
    public SymbolEntry getstandardsym(String f)
    {
        SymbolEntry fun = new SymbolEntry();
        switch (f)
        {
            case "getint":
                fun.setSysname(f);
                fun.setSymbolType(SymbolType.FUNTION);
                fun.setConstant(true);
                fun.setReturnType(ReturnType.INT);
                return fun;
            case "getdouble":
                fun.setSysname(f);
                fun.setSymbolType(SymbolType.FUNTION);
                fun.setReturnType(ReturnType.DOUBLE);
                fun.setConstant(true);
                return fun;
            case "getchar":
                fun.setSysname(f);
                fun.setSymbolType(SymbolType.FUNTION);
                fun.setReturnType(ReturnType.CHAR);
                fun.setConstant(true);
                return fun;
            case "putint":
                fun.setSysname(f);
                fun.setSymbolType(SymbolType.FUNTION);
                fun.setReturnType(ReturnType.VOID);
                fun.setConstant(true);
                return fun;
            case "putdouble":
                fun.setSysname(f);
                fun.setSymbolType(SymbolType.FUNTION);
                fun.setReturnType(ReturnType.VOID);
                fun.setConstant(true);
                return fun;
            case "putchar":
                fun.setSysname(f);
                fun.setSymbolType(SymbolType.FUNTION);
                fun.setReturnType(ReturnType.VOID);
                fun.setConstant(true);
                return fun;
            case "putstr":
                fun.setSysname(f);
                fun.setSymbolType(SymbolType.FUNTION);
                fun.setReturnType(ReturnType.VOID);
                fun.setConstant(true);
                return fun;
            case "putln":
                fun.setSysname(f);
                fun.setSymbolType(SymbolType.FUNTION);
                fun.setConstant(true);
                fun.setReturnType(ReturnType.VOID);
                return fun;
            default:
                return null;
        }
    }
    public void callhandle(Token token)
    {
        String funname = token.getValueString();
        if (funname.equals("putstr"))
        {

        }
    }
}
