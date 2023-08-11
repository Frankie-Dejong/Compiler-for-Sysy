package manager;

import frontend.semantic.SymTable;
import frontend.semantic.Symbol;
import mir.Function;
import mir.GlobalValue;
import mir.Module;
import mir.Type;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Manager {

    private final ArrayList<String> outputList = new ArrayList<>();
    private final Module module;

    public Manager(SymTable globalSymTable, ArrayList<String> globalStrings, ArrayList<GlobalValue> globalValues) {
        module = new Module(globalSymTable, globalStrings, globalValues);
    }

    public Module getModule() {
        return module;
    }

    public static class ExternFunc {
        public static final Function MEMSET = new Function(Type.VoidType.VOID_TYPE, "memset",
                new Type.PointerType(Type.BasicType.I32_TYPE), Type.BasicType.I32_TYPE, Type.BasicType.I32_TYPE);
        public static final Function GETINT = new Function(Type.BasicType.I32_TYPE, "getint");
        public static final Function PUTINT = new Function(Type.VoidType.VOID_TYPE, "putint", Type.BasicType.I32_TYPE);
        public static final Function GETCH = new Function(Type.BasicType.I32_TYPE, "getch");
        public static final Function GETFLOAT = new Function(Type.BasicType.F32_TYPE, "getfloat");
        public static final Function PUTCH = new Function(Type.VoidType.VOID_TYPE, "putch", Type.BasicType.I32_TYPE);
        public static final Function PUTFLOAT = new Function(Type.VoidType.VOID_TYPE, "putfloat", Type.BasicType.F32_TYPE);
        public static final Function STARTTIME = new Function(Type.VoidType.VOID_TYPE, "starttime");
        public static final Function STOPTIME = new Function(Type.VoidType.VOID_TYPE, "stoptime");
        public static final Function GETARRAY = new Function(Type.BasicType.I32_TYPE, "getarray", new Type.PointerType(Type.BasicType.I32_TYPE));
        public static final Function GETFARRAY = new Function(Type.BasicType.I32_TYPE, "getfarray", new Type.PointerType(Type.BasicType.F32_TYPE));
        public static final Function PUTARRAY = new Function(Type.VoidType.VOID_TYPE, "putarray", Type.BasicType.I32_TYPE, new Type.PointerType(Type.BasicType.I32_TYPE));
        public static final Function PUTFARRAY = new Function(Type.VoidType.VOID_TYPE, "putfarray", Type.BasicType.I32_TYPE, new Type.PointerType(Type.BasicType.F32_TYPE));
        public static final Function PUTF = new Function(Type.VoidType.VOID_TYPE, "putf");

        public static final HashMap<String, Function> externFunctions = new HashMap<>() {{
            put(MEMSET.getName(), MEMSET);
            put(GETINT.getName(), GETINT);
            put(PUTINT.getName(), PUTINT);
            put(GETCH.getName(), GETCH);
            put(GETFLOAT.getName(), GETFLOAT);
            put(PUTCH.getName(), PUTCH);
            put(PUTFLOAT.getName(), PUTFLOAT);
            put(STARTTIME.getName(), STARTTIME);
            put(STOPTIME.getName(), STOPTIME);
            put(GETARRAY.getName(), GETARRAY);
            put(GETFARRAY.getName(), GETFARRAY);
            put(PUTARRAY.getName(), PUTARRAY);
            put(PUTFARRAY.getName(), PUTFARRAY);
            put(PUTF.getName(), PUTF);
        }};
    }


    public void outputLLVM(String name) throws FileNotFoundException {
        OutputStream out = new FileOutputStream(name);
        outputList.clear();
        HashMap<String, Function> functions = module.getFunctions();
        SymTable globalSymTable = module.getGlobalSymTable();
        ArrayList<String> globalStrings = module.getGlobalStrings();

        for (int i = 0; i < globalStrings.size(); i++) {
            outputList.add("@.str_" + (i + 1) + " = constant [" + str2llvmIR(globalStrings.get(i)));
        }

        //全局变量

        // 未定义常量表
        for (Map.Entry<Type, GlobalValue> entry : GlobalValue.undefTable.entrySet()) {
            outputList.add(String.format("%s = global %s", entry.getValue().getDescriptor(), entry.getValue().initValue.toString()));
        }


        for (Map.Entry<String, Symbol> globalSymbolEntry :
                globalSymTable.getSymbolMap().entrySet()) {
            outputList.add(String.format("@%s = global %s", globalSymbolEntry.getKey(), globalSymbolEntry.getValue().getInitValue().toString()));
        }



        //函数声明
        for (Map.Entry<String, Function> functionEntry :
                functions.entrySet()) {
            if (functionEntry.getValue().isExternal()) {
                Function function = functionEntry.getValue();
                if (functionEntry.getKey().equals(ExternFunc.PUTF.getName())) {
                    outputList.add("declare void @" + ExternFunc.PUTF.getName() + "(ptr, ...)");
                } else {
                    outputList.add(String.format("declare %s @%s(%s)", function.getRetType().toString(), functionEntry.getKey(), function.FArgsToString()));
                }
            }
        }

        //函数定义
        for (Map.Entry<String, Function> functionEntry :
                functions.entrySet()) {
            Function function = functionEntry.getValue();
            if (function.isDeleted() || function.isExternal()) {
                continue;
            }
            outputList.addAll(function.output());
        }


        streamOutput(out, outputList);

    }

    public HashMap<String, Function> getFunctions() {
        return module.getFunctions();
    }

    public void addFunction(Function function) {
        module.addFunction(function);
    }


    //region outputLLVM IR
    private int countOfSubStr(String str, String sub) {
        int count = 0;
        int index = str.indexOf(sub);
        while (index != -1) {
            count++;
            index = str.indexOf(sub, index + sub.length());
        }
        return count;
    }

    private String str2llvmIR(String str) {
        str = str.substring(0, str.length() - 1).replace("\\n", "\\0A");
        str += "\\00\"";
        int length = str.length() - 2;
        length -= (countOfSubStr(str, "\\0A") + countOfSubStr(str, "\\00")) * 2;
        StringBuilder sb = new StringBuilder();
        sb.append(length).append(" x i8] c");
        sb.append(str);
        return sb.toString();
    }

    private static void streamOutput(OutputStream fop, ArrayList<String> outputStringList) {
        OutputStreamWriter writer;
        writer = new OutputStreamWriter(fop, StandardCharsets.UTF_8);
        for (String t : outputStringList) {
            try {
                writer.append(t).append("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //end region

}
