package miniplc0java.tokenizer;

public enum TokenType {
    /** 空 */
    None,
    FN_KW,
    LET_KW,
    CONST_KW,
    AS_KW,
    WHILE_KW,
    IF_KW,
    ELSE_KW,
    RETURN_KW,
    BREAK_KW,
    CONTINUE_KW,
    escape_sequence,// /+ / " ' n r t
    string_regular_char,// 除去 " \
    STRING_LITERAL,
    DOUBLE_LITERAL,
    char_regular_char,
    CHAR_LITERAL,
    COMMENT,
    /** 无符号整数 */
    Uint,
    /** 标识符 */
    Ident,
    /** Begin */
    Begin,
    /** End */
    End,
    /** Var */
    Var,
    /** Const */
    Const,
    /** Print */
    Print,

    /** 加号 */
    Plus,
    /** 减号 */
    Minus,
    /** 乘号 */
    Mult,
    /** 除号 */
    Div,
    /** 等号 */
    Equal,
    /**双等号*/
    DoubleEqual,
    Void,
    Nequal,
    Less,
    More,
    Lequal,
    Mequal,
    Arrow,
    Comma,
    Collon,
    /** 分号 */
    Semicolon,
    /** 左括号 */
    LParen,
    /** 右括号 */
    RParen,
    LBParen,
    RBParen,
    /** 文件尾 */
    EOF,
    expr;

    @Override
    public String toString() {
        switch (this) {
            case CONST_KW:
                return "const";
            case WHILE_KW:
                return "while";
            case RETURN_KW:
                return "return";
            case CONTINUE_KW:
                return "continue";
            case DoubleEqual:
                return "doubleequal";
            case CHAR_LITERAL:
                return "char";
            case DOUBLE_LITERAL:
                return "doubleliteral";
            case STRING_LITERAL:
                return "String";
            case escape_sequence:
                return "escape";
            case char_regular_char:
                return "char";
            case string_regular_char:
                return "regularchar";
            case Less:
                return "Less";
            case More:
                return "More";
            case Void:
                return "Void";
            case Arrow:
                return "Arrow";
            case AS_KW:
                return "As";
            case Comma:
                return "comma";
            case FN_KW:
                return "kw";
            case IF_KW:
                return "if";
            case Collon:
                return "collon";
            case Lequal:
                return "lequal";
            case LET_KW:
                return "let";
            case Mequal:
                return "mequal";
            case Nequal:
                return "nequal";
            case COMMENT:
                return "comment";
            case ELSE_KW:
                return "else";
            case LBParen:
                return "lbparen";
            case RBParen:
                return "rbparen";
            case BREAK_KW:
                return "break";
            case None:
                return "NullToken";
            case Begin:
                return "Begin";
            case Const:
                return "Const";
            case Div:
                return "DivisionSign";
            case EOF:
                return "EOF";
            case End:
                return "End";
            case Equal:
                return "EqualSign";
            case Ident:
                return "Identifier";
            case LParen:
                return "LeftBracket";
            case Minus:
                return "MinusSign";
            case Mult:
                return "MultiplicationSign";
            case Plus:
                return "PlusSign";
            case Print:
                return "Print";
            case RParen:
                return "RightBracket";
            case Semicolon:
                return "Semicolon";
            case Uint:
                return "UnsignedInteger";
            case Var:
                return "Var";
            default:
                return "InvalidToken";
        }
    }
}
