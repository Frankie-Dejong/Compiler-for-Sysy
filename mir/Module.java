package mir;

import frontend.semantic.SymTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class Module {
    private final HashMap<String, Function> functions = new HashMap<>();
    private final SymTable globalSymTable;
    private final ArrayList<String> globalStrings;
    private final ArrayList<GlobalValue> globalValues;

    public Module(SymTable globalSymTable, ArrayList<String> globalStrings, ArrayList<GlobalValue> globalValues) {
        this.globalSymTable = globalSymTable;
        this.globalStrings = globalStrings;
        this.globalValues = globalValues;
    }


    public HashMap<String, Function> getFunctions() {
        return functions;
    }

    public SymTable getGlobalSymTable() {
        return globalSymTable;
    }

    public ArrayList<GlobalValue> getGlobalValues() {
        return globalValues;
    }

    public ArrayList<String> getGlobalStrings() {
        return globalStrings;
    }

    public void addFunction(Function function) {
        functions.putIfAbsent(function.getName(), function);
    }

    public Collection<Function> getFuncSet() {
        return functions.values();
    }

    public void removeFunction(Function function) {
        functions.remove(function.getName());
    }
}
