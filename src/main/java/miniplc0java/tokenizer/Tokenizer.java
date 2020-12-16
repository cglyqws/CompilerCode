package miniplc0java.tokenizer;

import miniplc0java.error.TokenizeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token>  的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 词法分析
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {

        it.readAll();
        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        skipcomment();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        Pos start = new Pos(it.currentPos().row, it.currentPos().col);

        if (Character.isDigit(peek)) {
            return lexUInt();
        } else if (Character.isAlphabetic(peek)||peek=='_') {
            return lexIdentOrKeyword();
        } else if (peek =='\"')
        {
            return lexString();
        }
        else if (peek == '\'')
        {
            return lexchar();
        }
        else {
            return lexOperatorOrUnknown();
        }
    }

    private Token lexchar() throws TokenizeError
    {
        Pos start = new Pos(it.currentPos().row,it.currentPos().col);
        it.nextChar();
        char c = it.nextChar();

        if (c == '\\')
        {
            c = it.peekChar();
            it.nextChar();
            if (c=='\\')
            {
                if (it.nextChar()!='\'')throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                return new Token(TokenType.CHAR_LITERAL,'\\',start,it.currentPos());
            }
            else if (c=='\"')
            {
                if (it.nextChar()!='\'')throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                return new Token(TokenType.CHAR_LITERAL,'\"',start,it.currentPos());
            }
            else if (c=='\'')
            {
                if (it.nextChar()!='\'')throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                return new Token(TokenType.CHAR_LITERAL,'\'',start,it.currentPos());
            }
            else if (c=='n')
            {
                if (it.nextChar()!='\'')throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                return new Token(TokenType.CHAR_LITERAL,'\n',start,it.currentPos());
            }
            else if (c=='t')
            {
                if (it.nextChar()!='\'')throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                return new Token(TokenType.CHAR_LITERAL,'\t',start,it.currentPos());
            }
            else if (c=='r')
            {
                if (it.nextChar()!='\'')throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
                return new Token(TokenType.CHAR_LITERAL,'\t',start,it.currentPos());
            }
            else throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
        else if (c!='\'')
        {
            if (it.nextChar()!='\'')throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            return new Token(TokenType.CHAR_LITERAL,c,start,it.currentPos());
        }
        else throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());

    }

    private Token lexString() throws TokenizeError
    {
        Pos start = new Pos(it.currentPos().row,it.currentPos().col);
        StringBuilder tempstring = new StringBuilder();

        it.nextChar();
        while (true)
        {
            char c = it.peekChar();
            if (c == '\\')
            {
                it.nextChar();
                c = it.peekChar();
                if (c=='\\')
                {
                    tempstring.append(it.peekChar());
                }
                else if (c=='\"')
                {
                    tempstring.append("\"");
                }
                else if (c=='\'')
                {
                    tempstring.append("\'");
                }
                else if (c=='n')
                {
                    tempstring.append("\n");
                }
                else if (c=='t')
                {
                    tempstring.append("\t");
                }
                else if (c=='r')
                {
                    tempstring.append("\r");
                }
                else throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
            else if (c!='\"')
            {
                tempstring.append(it.peekChar());
            }
            else if (c=='\"')
            {
                it.nextChar();
                break;
            }
            else
            {
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
            }
            it.nextChar();
        }
        return new Token(TokenType.STRING_LITERAL,tempstring.toString(),start,it.currentPos());

    }
    private Token lexUInt() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 解析存储的字符串为无符号整数
        // 解析成功则返回无符号整数类型的token，否则返回编译错误
        //
        // Token 的 Value 应填写数字的值

        Pos UintBegin = new Pos(it.currentPos().row, it.currentPos().col);
        StringBuilder tempstring = new StringBuilder();

        while(Character.isDigit(it.peekChar()))
        {
            tempstring.append(it.nextChar());
        }
        int uil = 0;
        int deci = 0;
        double result;
        int flag=1;
        uil = Integer.parseInt(tempstring.toString());

        if (it.peekChar()=='.')
        {
            it.nextChar();
            tempstring = new StringBuilder();
            while(Character.isDigit(it.peekChar()))
            {
                tempstring.append(it.nextChar());
            }
            deci = Integer.parseInt(tempstring.toString());
            int len = tempstring.length();
            if (it.peekChar()=='e'||it.peekChar()=='E')
            {
                it.nextChar();
                if (it.peekChar()=='-'){
                    flag = -1;
                    it.nextChar();
                }
                else if (it.peekChar()=='+')
                {
                    it.nextChar();
                }
                tempstring = new StringBuilder();
                while(Character.isDigit(it.peekChar()))
                {
                    tempstring.append(it.nextChar());
                }
                result = uil + deci * Math.pow(10,-len) ;
                result *= Math.pow(10,flag*Integer.parseInt(tempstring.toString()));
            }
            else {
                result = uil + deci * Math.pow(10,-len);
            }
            return new Token(TokenType.DOUBLE_LITERAL,result,UintBegin,it.currentPos());

        }
        else {
            return new Token(TokenType.Uint,uil,UintBegin,it.currentPos());
        }

    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字或字母为止:
        // -- 前进一个字符，并存储这个字符
        //
        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        //
        // Token 的 Value 应填写标识符或关键字的字符串

        int row = it.currentPos().row;
        int column =  it.currentPos().col;
        Pos StringBeginPos = new Pos(row, column);
        StringBuilder tempstring2 = new StringBuilder();

        while(Character.isDigit(it.peekChar())||Character.isLetter(it.peekChar())||it.peekChar()=='_')
        {
            tempstring2.append(it.nextChar());
        }
        String s1 = tempstring2.toString();
        String s = s1.toLowerCase();


        if (s.equals("fn"))
        {
            return new Token(TokenType.FN_KW,s1,StringBeginPos, it.currentPos());
        }
        else if (s.equals("let"))
        {
            return new Token(TokenType.LET_KW,s1, StringBeginPos, it.currentPos());
        }
        else if (s.equals("const"))
        {
            return new Token(TokenType.CONST_KW,s1,StringBeginPos,it.currentPos());
        }
        else if (s.equals("as"))
        {
            return new Token(TokenType.AS_KW,s1,StringBeginPos,it.currentPos());
        }
        else if (s.equals("while"))
        {
            return new Token(TokenType.WHILE_KW,s1,StringBeginPos,it.currentPos());
        }
        else if (s.equals("if"))
        {
            return new Token(TokenType.IF_KW,s1,StringBeginPos,it.currentPos());
        }
        else if (s.equals("else"))
        {
            return new Token(TokenType.ELSE_KW,s1,StringBeginPos,it.currentPos());
        }
        else if (s.equals("return"))
        {
            return new Token(TokenType.RETURN_KW,s1,StringBeginPos,it.currentPos());
        }
        else if (s.equals("break"))
        {
            return new Token(TokenType.BREAK_KW,s1,StringBeginPos,it.currentPos());
        }
        else if (s.equals("continue"))
        {
            return new Token(TokenType.CONTINUE_KW,s1,StringBeginPos,it.currentPos());
        }
        else return new Token(TokenType.Ident,s1,StringBeginPos,it.currentPos());

    }

    private Token lexOperatorOrUnknown() throws TokenizeError {

        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.Plus, '+', it.previousPos(), it.currentPos());
            case '-':
                if (it.peekChar() == '>')
                {
                    it.nextChar();
                    return new Token(TokenType.Arrow,"->",it.previousPos(),it.currentPos());
                }
                return new Token(TokenType.Minus,'-', it.previousPos(), it.currentPos());
            case '*':
                return new Token(TokenType.Mult, '*', it.previousPos(), it.currentPos());
            case '/':
                return new Token(TokenType.Div, '/', it.previousPos(), it.currentPos());
            case '=':
                if (it.peekChar()=='=')
                {
                    it.nextChar();
                    return new Token(TokenType.DoubleEqual, "==", it.previousPos(), it.currentPos());
                }
                else return new Token(TokenType.Equal, '=', it.previousPos(), it.currentPos());
            case ';':
                return new Token(TokenType.Semicolon, ';', it.previousPos(), it.currentPos());
            case '(':
                return new Token(TokenType.LParen, '(', it.previousPos(), it.currentPos());
            case ')':
                return new Token(TokenType.RParen, ')', it.previousPos(), it.currentPos());
            case '!':
                if (it.peekChar() == '=')
                {
                    it.nextChar();
                    return new Token(TokenType.Nequal, "!=", it.previousPos(), it.currentPos());
                }
                else throw new TokenizeError(ErrorCode.InvalidInput,it.previousPos());
            case '<':
                if (it.peekChar() == '=')
                {
                    it.nextChar();
                    return new Token(TokenType.Lequal, "<=", it.previousPos(), it.currentPos());
                }
                return new Token(TokenType.Less, '<', it.previousPos(), it.currentPos());
            case '>':
                if (it.peekChar() == '=')
                {
                    it.nextChar();
                    return new Token(TokenType.Mequal,">=",it.previousPos(),it.currentPos());
                }
                return new Token(TokenType.More, '>', it.previousPos(), it.currentPos());
            case '{':
                return new Token(TokenType.LBParen, '{', it.previousPos(), it.currentPos());
            case '}':
                return new Token(TokenType.RBParen, '}', it.previousPos(), it.currentPos());
            case ',':
                return new Token(TokenType.Comma, ',', it.previousPos(), it.currentPos());
            case ':':
                return new Token(TokenType.Collon, ':', it.previousPos(), it.currentPos());
            default:
                // 不认识这个输入，摸了
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }

    private void skipcomment()
    {
        while (it.peekChar()=='/')
        {
            int row = it.currentPos().row;
            int col = it.currentPos().col+1;
            if (it.linesBuffer.get(row).charAt(col)=='/')
            {
                while (it.nextChar()!='\n')
                {

                }
                skipSpaceCharacters();
            }
            else return;
        }
    }
}
