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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

public final class Analyser {
    private int level = 0;
    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    public HashMap<String, SymbolEntry> symbolTable = new HashMap<>();
    ArrayList<String> symbol = new ArrayList<>();
    int top = 0;
    int [] index = new int[99999];
    int processpoint = 0;
    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
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

    /**
     * 添加一个符号
     *
     * @param name          名字
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addSymbol(String name, boolean isInitialized,TokenType type, boolean isConstant, Pos curPos,int location) throws AnalyzeError {
        if (this.symbolTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.symbolTable.put(name, new SymbolEntry(isConstant, type,isInitialized, getNextVariableOffset(),location));
        }
    }

    /**
     * 设置符号为已赋值
     *
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }

    /**
     * 获取变量在栈上的偏移
     *
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 栈偏移
     * @throws AnalyzeError
     */
    private int getOffset(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getStackOffset();
        }
    }

    /**
     * 获取变量是否是常量
     *
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 是否为常量
     * @throws AnalyzeError
     */
    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.isConstant();
        }
    }

    private void analyseProgram() throws CompileError {
        // 程序 -> 'begin' 主过程 'end'
        // 示例函数，示例如何调用子程序
        // 'begin'
        while (!check(TokenType.EOF))
        {
            if (check(TokenType.FN_KW))
            {
                analysefn();
            }
            else analysedecl_stmt();//这个还没写
        }

        String outputFileName = "C:\\Users\\MSI NBOOK\\Desktop\\out.txt";
        PrintStream output;
        if (outputFileName.equals("-")) {
            output = System.out;
        } else {
            try {
                output = new PrintStream(new FileOutputStream(outputFileName));
            } catch (FileNotFoundException e) {
                System.err.println("Cannot open output file.");
                e.printStackTrace();
                System.exit(3);
                return;
            }
        }
        for (Instruction in :instructions)
        {
            switch(in.getOpt())
            {
                case LIT:
                    output.println("LocA("+String.valueOf(in.getX()-1)+")");

            }

        }
        for(String key: symbolTable.keySet())
        {
            output.println("Key: "+key+" Value: "+ symbolTable.get(key));
        }
        return ;

    }

    /**
     * 函数fn
     * @throws CompileError
     */
    private void analysefn() throws  CompileError
    {
        expect(TokenType.FN_KW);
        expect(TokenType.Ident);
        expect(TokenType.LParen);
        if (check(TokenType.RParen))
        {
            expect(TokenType.RParen);
        }
        else {
            analyseparamlist();
        }

        expect(TokenType.Arrow);
        analysety();
        analyseblockstmt();

    }
    private void analyseblockstmt() throws CompileError
    {
        expect(TokenType.LBParen);
        index[processpoint++] = top;
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
        analyseexpr();
        expect(TokenType.Semicolon);

    }
    private void analysepcallparamlist() throws  CompileError
    {
        analyseexpr();
        while(check(TokenType.Comma))
        {
            expect(TokenType.Comma);
            analyseexpr();
        }
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
    private void analyseexpr() throws CompileError
    {
        var token = peek();
        if (check(TokenType.Ident))
        {
            expect(TokenType.Ident);
            if (check(TokenType.Equal))
            {
                expect(TokenType.Equal);
                analyseexpr();
            }
            else if (check(TokenType.LParen))
            {
                expect(TokenType.LParen);
                if (!check(TokenType.RParen))
                {
                    analysepcallparamlist();
                }
                expect(TokenType.RParen);
            }
            else if (!check(TokenType.AS_KW)&&!isop())
            {
                return ;
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
            expect(TokenType.STRING_LITERAL);
        }
        else if (check(TokenType.LParen))
        {
            expect(TokenType.LParen);
            analyseexpr();
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
     */
    private void analyseparamlist() throws CompileError
    {
        analyseparam();
        while (check(TokenType.Comma))
        {
            expect(TokenType.Comma);
            analyseparam();
        }
        expect(TokenType.RParen);
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
        if (token2.getValue().equals("int"))
        {

        }
        else if (token2.getValue().equals("double"))
        {

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
            if (check(TokenType.Equal))
            {
                var sys = symbolTable.get(token.getValueString());
                if (sys==null)
                {
                    addSymbol(token.getValueString(), true,token2.getTokenType(), false, token.getStartPos(),top-index[processpoint-1]);
                    symbol.add(token.getValueString());
                    top++;
                }
                else {
                    int flag = 0;
                    for (int i=top-1;i>=0;i--)
                    {
                        if (symbol.get(i).equals(token.getValueString())){
                            flag = i;
                            break;
                        }
                    }
                    if (flag < processpoint)
                    {
                        addSymbol(token.getValueString(), true,token2.getTokenType(), false, token.getStartPos(),top-index[processpoint-1]);
                        symbol.add(token.getValueString());
                        top++;
                    }
                    else throw new AnalyzeError(ErrorCode.DuplicateDeclaration, /* 当前位置 */ token.getStartPos());
                }
                instructions.add(new Instruction(Operation.LIT,top-index[processpoint]));
                expect(TokenType.Equal);
                analyseexpr();
                expect(TokenType.Semicolon);

            }
            else {
                var sys = symbolTable.get(token.getValueString());
                if (sys==null)
                {
                    symbol.add(token.getValueString());
                    top++;
                    addSymbol(token.getValueString(), false,token2.getTokenType(), false, token.getStartPos(),top-index[processpoint-1]);
                }
                else {
                    int flag = 0;
                    for (int i=top-1;i>=0;i--)
                    {
                        if (symbol.get(i).equals(token.getValueString())){
                            flag = i;
                            break;
                        }
                    }
                    if (flag < processpoint)
                    {
                        addSymbol(token.getValueString(), false,token2.getTokenType(), false, token.getStartPos(),top-index[processpoint]);
                        symbol.add(token.getValueString());
                        top++;
                        instructions.add(new Instruction(Operation.LIT,top-index[processpoint]));
                    }
                    throw new AnalyzeError(ErrorCode.DuplicateDeclaration, /* 当前位置 */ token.getStartPos());
                }
                expect(TokenType.Semicolon);
                instructions.add(new Instruction(Operation.LIT,top-index[processpoint]));
            }
        }
        else if (check(TokenType.CONST_KW))
        {

            expect(TokenType.CONST_KW);
            var token = expect(TokenType.Ident);
            expect(TokenType.Collon);
            var token2 = analysety();
            expect(TokenType.Equal);
            symbol.add(token.getValueString());
            top++;
            addSymbol(token.getValueString(),true,token2.getTokenType(),true,token.getStartPos(),top-processpoint);
            instructions.add(new Instruction(Operation.LIT,top-index[processpoint]));
            analyseexpr();
            expect(TokenType.Semicolon);

        }
        else throw new AnalyzeError(ErrorCode.InvalidInput, /* 当前位置 */ peek().getStartPos());
    }

//    private void analyseConstantDeclaration() throws CompileError {
//        // 示例函数，示例如何解析常量声明
//        // 常量声明 -> 常量声明语句*
//
//        // 如果下一个 token 是 const 就继续
//        while (nextIf(TokenType.Const) != null) {
//            // 常量声明语句 -> 'const' 变量名 '=' 常表达式 ';'
//
//            // 变量名
//            var nameToken = expect(TokenType.Ident);
//
//            // 加入符号表
//            String name = (String) nameToken.getValue();
//            addSymbol(name, true, true, nameToken.getStartPos());
//
//            // 等于号
//            expect(TokenType.Equal);
//
//            // 常表达式
//            var value = analyseConstantExpression();
//
//            // 分号
//            expect(TokenType.Semicolon);
//
//            // 这里把常量值直接放进栈里，位置和符号表记录的一样。
//            // 更高级的程序还可以把常量的值记录下来，遇到相应的变量直接替换成这个常数值，
//            // 我们这里就先不这么干了。
//            instructions.add(new Instruction(Operation.LIT, value));
//        }
//    }
//
//    private void analyseVariableDeclaration() throws CompileError {
//        // 变量声明 -> 变量声明语句*
//
//        // 如果下一个 token 是 var 就继续
//        while (nextIf(TokenType.Var) != null) {
//            // 变量声明语句 -> 'var' 变量名 ('=' 表达式)? ';'
//
//            // 变量名
//            var nameToken = expect(TokenType.Ident);
//            // 变量初始化了吗
//            boolean initialized = false;
//
//            // 下个 token 是等于号吗？如果是的话分析初始化
//            if (check(TokenType.Equal))
//            {
//                initialized =true;
//                next();
//                analyseExpression();
//            }
//            // 分析初始化的表达式
//
//            // 分号
//            expect(TokenType.Semicolon);
//
//            // 加入符号表，请填写名字和当前位置（报错用）
//            String name = /* 名字 */ nameToken.getValueString();
//            addSymbol(name, initialized, false, /* 当前位置 */ nameToken.getStartPos());
//
//            // 如果没有初始化的话在栈里推入一个初始值
//            if (!initialized) {
//                instructions.add(new Instruction(Operation.LIT, 0));
//            }
//        }
//    }
//
//    private void analyseStatementSequence() throws CompileError {
//        // 语句序列 -> 语句*
//        // 语句 -> 赋值语句 | 输出语句 | 空语句
//
//        while (true) {
//            // 如果下一个 token 是……
//            var peeked = peek();
//            if (peeked.getTokenType() == TokenType.Ident) {
//                analyseAssignmentStatement();
//                // 调用相应的分析函数
//                // 如果遇到其他非终结符的 FIRST 集呢？
//            }
//            else if (peeked.getTokenType() == TokenType.Print)
//            {
//                analyseOutputStatement();
//            }
//            else if (peeked.getTokenType() == TokenType.Semicolon)
//            {
//                expect(TokenType.Semicolon);
//            }
//            else {
//                // 都不是，摸了
//                break;
//            }
//        }
//
//    }
//
//    private int analyseConstantExpression() throws CompileError {
//        // 常表达式 -> 符号? 无符号整数
//        boolean negative = false;
//        if (nextIf(TokenType.Plus) != null) {
//            negative = false;
//        } else if (nextIf(TokenType.Minus) != null) {
//            negative = true;
//        }
//
//        var token = expect(TokenType.Uint);
//
//        int value = (int) token.getValue();
//        if (negative) {
//            value = -value;
//        }
//
//        return value;
//    }
//
//    private void analyseExpression() throws CompileError {
//        // 表达式 -> 项 (加法运算符 项)*
//        // 项
//        analyseItem();
//
//        while (true) {
//            // 预读可能是运算符的 token
//            var op = peek();
//            if (op.getTokenType() != TokenType.Plus && op.getTokenType() != TokenType.Minus) {
//                break;
//            }
//
//            // 运算符
//            next();
//
//            // 项
//            analyseItem();
//
//            // 生成代码
//            if (op.getTokenType() == TokenType.Plus) {
//                instructions.add(new Instruction(Operation.ADD));
//            } else if (op.getTokenType() == TokenType.Minus) {
//                instructions.add(new Instruction(Operation.SUB));
//            }
//        }
//    }
//
//    private void analyseAssignmentStatement() throws CompileError {
//        // 赋值语句 -> 标识符 '=' 表达式 ';'
//
//        // 分析这个语句
//        var token = expect(TokenType.Ident);
//        expect(TokenType.Equal);
//        analyseExpression();
//        expect(TokenType.Semicolon);
//        // 标识符是什么？
//        String name = token.getValueString();
//        var symbol = symbolTable.get(name);
//        if (symbol == null) {
//            // 没有这个标识符
//            throw new AnalyzeError(ErrorCode.NotDeclared, /* 当前位置 */ token.getStartPos());
//        } else if (symbol.isConstant) {
//            // 标识符是常量
//            throw new AnalyzeError(ErrorCode.AssignToConstant, /* 当前位置 */ token.getStartPos());
//        }
//        // 设置符号已初始化
//        initializeSymbol(name, token.getStartPos());
//
//        // 把结果保存
//        var offset = getOffset(name, token.getStartPos());
//        instructions.add(new Instruction(Operation.STO, offset));
//    }
//
//    private void analyseOutputStatement() throws CompileError {
//        // 输出语句 -> 'print' '(' 表达式 ')' ';'
//
//        expect(TokenType.Print);
//        expect(TokenType.LParen);
//
//        analyseExpression();
//
//        expect(TokenType.RParen);
//        expect(TokenType.Semicolon);
//
//        instructions.add(new Instruction(Operation.WRT));
//    }
//
//    private void analyseItem() throws CompileError {
//        // 项 -> 因子 (乘法运算符 因子)*
//
//        // 因子
//        analyseFactor();
//        while (true) {
//            // 预读可能是运算符的 token
//            Token op = peek();
//
//            // 运算符
//            if (op.getTokenType() == TokenType.Mult || op.getTokenType() == TokenType.Div)
//            {
//                next();
//                analyseFactor();
//                // 生成代码
//                if (op.getTokenType() == TokenType.Mult) {
//                    instructions.add(new Instruction(Operation.MUL));
//                } else if (op.getTokenType() == TokenType.Div) {
//                    instructions.add(new Instruction(Operation.DIV));
//                }
//            }
//            else
//            {
//                break;
//            }
//            // 因子
//
//
//        }
//    }
//
//    private void analyseFactor() throws CompileError {
//        // 因子 -> 符号? (标识符 | 无符号整数 | '(' 表达式 ')')
//
//        boolean negate;
//        if (nextIf(TokenType.Minus) != null) {
//            negate = true;
//            // 计算结果需要被 0 减
//            instructions.add(new Instruction(Operation.LIT, 0));
//        } else {
//            nextIf(TokenType.Plus);
//            negate = false;
//        }
//
//        if (check(TokenType.Ident)) {
//            // 是标识符
//
//            // 加载标识符的值
//            var token = expect(TokenType.Ident);
//            String name =token.getValueString();
//            var symbol = symbolTable.get(name);
//            if (symbol == null) {
//                // 没有这个标识符
//                throw new AnalyzeError(ErrorCode.NotDeclared, /* 当前位置 */ token.getStartPos());
//            } else if (!symbol.isInitialized) {
//                // 标识符没初始化
//                throw new AnalyzeError(ErrorCode.NotInitialized, /* 当前位置 */ token.getStartPos());
//            }
//            var offset = getOffset(name, token.getStartPos());
//            instructions.add(new Instruction(Operation.LOD, offset));
//        } else if (check(TokenType.Uint)) {
//            // 是整数
//            // 加载整数值
//            var token = expect(TokenType.Uint);
//            int value = 0;
//            if (token.getValueString()!=null)
//            {
//                value = Integer.valueOf(token.getValueString());
//            }
//            instructions.add(new Instruction(Operation.LIT, value));
//        } else if (check(TokenType.LParen)) {
//            // 是表达式
//            // 调用相应的处理函数
//            expect(TokenType.LParen);
//            analyseExpression();
//            expect(TokenType.RParen);
//        } else {
//            // 都不是，摸了
//            throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Uint, TokenType.LParen), next());
//        }
//
//        if (negate) {
//            instructions.add(new Instruction(Operation.SUB));
//        }
//
//    }
}
