package frontend.semantic;

import frontend.syntaxChecker.Ast;
import mir.Type;
import mir.Value;
public class Symbol {
    private final String name;
    private final Type type;
    private InitValue initValue;
    private final boolean isConstant;
    private final Value allocInst;

    private InitValue curValue;

    public boolean isChanged = false;

    public Symbol(String name, Type type, InitValue initValue, Boolean isConstant, Value allocInst) {
        this.name = name;
        this.type = type;
        this.initValue = initValue;
        this.isConstant = isConstant;
        this.allocInst = allocInst;
        this.curValue = initValue;
    }

    public Symbol(Ast.Ident ident, Type type, InitValue initValue, Boolean isConstant, Value allocInst) {
        this.name = ident.identifier.content;
        this.type = type;
        this.initValue = initValue;
        this.isConstant = isConstant;
        this.allocInst = allocInst;
        this.curValue = initValue;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public InitValue getInitValue() {
        return initValue;
    }

    public boolean isConstant() {
        return isConstant;
    }

    public Value getAllocInst() {
        return allocInst;
    }

    public void setCurValue(InitValue curValue) {
        this.curValue = curValue;
    }

    public InitValue getCurValue() {
        return curValue;
    }
}
