package mir;

import frontend.lexer.Token;
import frontend.lexer.TokenType;
import frontend.semantic.InitValue;
import frontend.syntaxChecker.Ast;

import java.util.HashMap;

public class GlobalValue extends Constant {
    //constant fixed address (after linking).
    //拥有@的全局标识符
    public Ast.Ident ident;
    public InitValue initValue;
    public static final HashMap<Type, GlobalValue> undefTable = new HashMap<>();



    public static Constant getUndef(Type type) {
        if(type.isInt32Ty()) {
            return new Constant.ConstantInt(0);
        }
        if(type.isFloatTy()) {
            return new Constant.ConstantFloat(0);
        }
        if(undefTable.containsKey(type)) {
            return undefTable.get(type);
        }
        GlobalValue undef = new GlobalValue(type);
        undefTable.put(type, undef);
        return undef;
    }

    private GlobalValue(Type type) {
        //Type must be pointer，存的是全局变量地址
        super(new Type.PointerType(type));
        ident = new Ast.Ident(new Token(TokenType.IDENTIFIER, "undef_" + undefTable.size()));
        if(type.isInt32Ty()) {
            initValue = new InitValue.ValueInit(new ConstantInt(0), type);
        } else if(type.isFloatTy()) {
            initValue = new InitValue.ValueInit(new ConstantFloat(0), type);
        } else if(type.isPointerTy()) {
            initValue = new InitValue.ZeroArrayInit(((Type.PointerType) type).getInnerType());
        } else {
            throw new RuntimeException("Unsupported type for undef");
        }
    }

    public GlobalValue(Type innerType, Ast.Ident ident, InitValue initValue) {
        super(new Type.PointerType(innerType));
        this.ident = ident;
        this.initValue = initValue;
    }

    public Type getInnerType() {
        Type.PointerType pointerType = (Type.PointerType) getType();
        return pointerType.getInnerType();
    }

    @Override
    public Object getConstValue() {
        return initValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        return ident.equals(((GlobalValue) o).ident);
    }

    @Override
    public boolean isZero() {
        for (InitValue.Flatten.Slice slice:
             initValue.flatten()) {
            if (slice.isZero() == false) {
                return false;
            }
        }
        return true;
    }
    @Override
    public String getDescriptor() {
        return "@"+ident.identifier.content;
    }

}
