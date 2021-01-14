package miniplc0java.analyser;

import miniplc0java.tokenizer.TokenType;

public class TypeValue {

    ReturnType type;
    Object vlaue;
    
    public TypeValue(ReturnType rt,Object var)
    {
        this.type = rt;
        this.vlaue = var;
    }
}
