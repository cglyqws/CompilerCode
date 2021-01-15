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

            FuntionEntry f = funtionTable.get(funtionTable.size()-1);
            return f.getSymbolTable();

    }

    /**
     * 获取当前指令集
     * @return
     */
    public List<Instruction> getnowinstructions()
    {

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
            else analysedecl_stmt();
        }

        return ;

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

    private void analyseblockstmt() throws CompileError
    {
        expect(TokenType.LBParen);

        while (!check(TokenType.RBParen))
        {
            analysestmt();
        }
        expect(TokenType.RBParen);
    }

    private void analyseif_stmt() throws CompileError
    {
        expect(TokenType.IF_KW);
        analyseexpr();
        analyseblockstmt();
        if (check(TokenType.ELSE_KW))
        {
            expect(TokenType.ELSE_KW);
            if (check(TokenType.LBParen))
            {
                analyseblockstmt();
            }
            else if (check(TokenType.IF_KW))
            {
                analyseif_stmt();
            }
        }

    }

    private void analysewhile_stmt() throws CompileError
    {
        expect(TokenType.WHILE_KW);
//        var value = analyseexpr();
        analyseexpr();
        analyseblockstmt();
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
            if (tv.type!=ReturnType.VOID)
            {
                in.add(new Instruction(Operation.store64));
            }
            expect(TokenType.Semicolon);

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
        if (T.getTokenType()==TokenType.Ident || T.getTokenType() ==TokenType.Uint ||T.getTokenType()==TokenType.DOUBLE_LITERAL)
        {
            return true;
        }
        else return false;
    }

    public int oprity(Token in,Token out)
    {
        if (isitem(in) && out.getTokenType()==TokenType.Plus)
        {
            return 1;
        }
        if (in.getTokenType()==TokenType.Plus && isitem(out) )
        {
            return -1;
        }
        if (out.getTokenType() == TokenType.Semicolon)
        {
            return 1;
        }
        return -1;
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
            }
            else oprit = oprity(stackop.get(stackop.size()-1),peek());

            if (oprit<0)
            {
                if (isitem(peek()))
                {
                    stackitem.add(peek());
                }
                else {
                    stackop.add(peek());
                }
                expect(peek().getTokenType());
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
                        if (s.getSymbolType()==SymbolType.INT)
                        {
                            re.type = ReturnType.INT;
                        }
                        instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(l.getValueString())+ arg));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (l.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push, (Integer)l.getValue()));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    if (r.getTokenType()==TokenType.Ident)
                    {
                        SymbolEntry s = gt.findsymbolbyname(r.getValueString());
                        instructions1.add(new Instruction(Operation.arga,gt.findsymbolindexbyname(r.getValueString())+arg));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    else if (r.getTokenType()==TokenType.Uint){
                        instructions1.add(new Instruction(Operation.push,(Integer)r.getValue()));
                        instructions1.add(new Instruction(Operation.load64));
                    }
                    instructions1.add(new Instruction(Operation.addi));
                }

                if (check(TokenType.Semicolon))
                {
                    break;
                }
                else {
                    expect(peek().getTokenType());
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

                expect(TokenType.Equal);
                int index1 = gt.findsymbolindexbyname(tokent.getValueString());
                in.add(new Instruction(Operation.loca,index1));
                TypeValue TV = analyseexpr();
                SymbolEntry s = gt.findsymbolbyname(tokent.getValueString());
                if (typematch(s.getType(),TV.type)||s.returnType==TV.type){
                    in.add(new Instruction(Operation.store64,index1));
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
                in.add(new Instruction(Operation.loca, gt.findsymbolindexbyname(tokent.getValueString())));
                in.add(new Instruction(Operation.load64));
                return null;
            }
        }

        else if (check(TokenType.Minus))
        {
            expect(TokenType.Minus);
            analyseexpr();
        }
        else if (check(TokenType.Uint))
        {
            expect(TokenType.Uint);
        }
        else if (check(TokenType.DOUBLE_LITERAL))
        {
            expect(TokenType.DOUBLE_LITERAL);
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
            expect(TokenType.LParen);
            analyseexpr();
            expect(TokenType.RParen);
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
    private void analysestmt() throws CompileError
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
            analysewhile_stmt();
        }
        else if (check(TokenType.RETURN_KW))
        {
            analysereturn_stmt();
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
            analysebreak_stmt();
        }
        else if (check(TokenType.CONTINUE_KW))
        {
            analysecontinue_stmt();
        }
        else {
            analyseexpr_stmt();
        }
    }

    private void analysebreak_stmt() throws CompileError
    {
        expect(TokenType.BREAK_KW);
        expect(TokenType.Semicolon);
    }

    private void analysecontinue_stmt() throws  CompileError
    {
        expect(TokenType.CONTINUE_KW);
        expect(TokenType.Semicolon);
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
        if (check(TokenType.Const))
        {
            expect(TokenType.Const);
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
            sym.setSymbolType(SymbolType.INT);
        }
        else if (token2.getValue().equals("double"))
        {
            sym.setSymbolType(SymbolType.DOUBLE);
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
                in.add(new Instruction(Operation.loca,off));
                expect(TokenType.Equal);
                analyseexpr();
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
                fun.setReturnType(ReturnType.CHAR);
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
