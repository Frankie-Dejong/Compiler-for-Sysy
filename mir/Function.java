package mir;

import midend.CloneInfo;
import midend.ControlFlowGraph;
import midend.DominanceGraph;
import midend.Mem2Reg;
import utils.SyncLinkedList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class Function extends Value {

    //Note that Function is a GlobalValue and therefore also a Constant.
    // The value of the function
    // is its address (after linking) which is guaranteed to be constant.
    //basic inf

    public static class Argument extends Value {
        private Function parentFunction;

        public int idx;

        public Argument(Type type) {
            super(type);
            this.parentFunction = null;
        }

        public Argument(Type type, Function parentFunction) {
            super(type);
            this.parentFunction = parentFunction;
        }

        public void setParentFunction(Function function) {
            parentFunction = function;
        }

        @Override
        public String getDescriptor() {
            return "%arg_" + idx;
        }

    }

    private final Type retType; //返回值类型
    private final ArrayList<Argument> myArguments; // 参数表
    private ArrayList<Argument> funcRArguments = new ArrayList<>();
    private final SyncLinkedList<BasicBlock> blocks; // 内含基本块链表
    private BasicBlock entry; // 入口基本块
    //GVN
    private boolean gvn = false;

    public void setGvn(boolean gvn) {
        this.gvn = gvn;
    }

    public boolean isGvn() {
        return gvn;
    }

    private boolean deleted = false;

    public boolean isLeaf = true;

    private final ControlFlowGraph CG = new ControlFlowGraph(this);
    private final DominanceGraph DG = new DominanceGraph(this);

    public Function(Type type, String name, Type... argumentTypes) {
        super(Type.FunctionType.FUNC_TYPE);
        entry = null;
        setName(name);
        retType = type;
        blocks = new SyncLinkedList<>();

        ArrayList<Argument> arguments = new ArrayList<>();

        for (int i = 0; i < argumentTypes.length; i++) {
            Argument arg = new Argument(argumentTypes[i], this);
            arguments.add(arg);
            arg.idx = i;
        }

        myArguments = arguments;
    }

    public Function(Type type, String name, ArrayList<Type> argumentTypes) {
        super(Type.FunctionType.FUNC_TYPE);
        setName(name);
        entry = null;
        retType = type;
        blocks = new SyncLinkedList<>();

        ArrayList<Argument> arguments = new ArrayList<>();


        for (int i = 0; i < argumentTypes.size(); i++) {
            Argument arg = new Argument(argumentTypes.get(i), this);
            arguments.add(arg);
            arg.idx = i;
        }

        myArguments = arguments;
    }

    public boolean isExternal() {
        return blocks.isEmpty();
    }

    public void setDeleted() {
        deleted = true;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public BasicBlock getEntry() {
        return getBlocks().getFirst();
    }

    public BasicBlock getFirstBlock() {
        return blocks.getFirst();
    }

    public BasicBlock getLastBlock() {
        return blocks.getLast();
    }

    public SyncLinkedList<BasicBlock> getBlocks() {
        return blocks;
    }

    public void appendBlock(BasicBlock block) {
        if (blocks.isEmpty()) {
            entry = block;
        }
        blocks.addLast(block);
    }

    //region outputLLVMIR
    public String FArgsToString() {
        StringBuilder str = new StringBuilder();
        Iterator<Type> iter = getArgumentsTP().iterator();
        while (iter.hasNext()) {
            str.append(iter.next().toString());
            if (iter.hasNext()) {
                str.append(',');
            }
        }
        return str.toString();
    }

    public String RArgsToString() {
        StringBuilder str = new StringBuilder();
        Iterator<Type> iter = getArgumentsTP().iterator();
        int idx = 0;
        while (iter.hasNext()) {
            str.append(iter.next().toString());
            str.append(String.format(" %%arg_%d", idx++));
            if (iter.hasNext()) {
                str.append(',');
            }
        }
        return str.toString();
    }

    public ArrayList<String> output() {
        ArrayList<String> outputList = new ArrayList<>();
        outputList.add(String.format("define %s @%s(%s) {", getRetType().toString(), name, RArgsToString()));
        for (BasicBlock block :
                blocks) {
            outputList.addAll(block.output());
            outputList.add("\n");
        }
        outputList.add("}\n");
        return outputList;
    }


    //endregion


    public Type getRetType() {
        return retType;
    }


    public ArrayList<Type> getArgumentsTP() {
        ArrayList<Type> types = new ArrayList<>();
        for (Argument arg :
                myArguments) {
            types.add(arg.getType());
        }
        return types;
    }

    public void setFuncRArguments(ArrayList<Argument> arguments) {
        this.funcRArguments = arguments;
    }

    public ArrayList<Argument> getFuncRArguments() {
        return funcRArguments;
    }

    public ArrayList<Argument> getMyArguments() {
        return myArguments;
    }

    public void buildControlFlowGraph() {
        CG.build();
    }

    public void buildDominanceGraph() {
        DG.build();
    }

    public void checkCFG() {
        CG.checkGraph();
    }

    public void runMem2Reg() {
            Mem2Reg.run(this);
    }


    public Instruction.Load inlineToFunc(Function tagFunc, BasicBlock retBB, Instruction.Call call, Instruction.Alloc alloc, int idx) {
        //Instruction.Phi retPhi = null;


        for (BasicBlock block:
             getBlocks()) {
            block.cloneToFunc(tagFunc, idx);
        }

        ArrayList<Value> callParams = call.getParams();
        ArrayList<Argument> funcParams = getMyArguments();

        for (int i = 0; i < callParams.size(); i++) {
            CloneInfo.addValueReflect(funcRArguments.get(i), callParams.get(i));
//            for (Use use:
//                 funcRArguments.get(i).getUses()) {
//                use.getUser().replaceUseOfWith(use.get(), callParams.get(i));
//            }
        }

        Instruction.Load load = null;

        for(BasicBlock block : getBlocks()) {
            //((BasicBlock) CloneInfoMap.getReflectedValue(bb)).fix();
            BasicBlock needFixBB = (BasicBlock) CloneInfo.getReflectedValue(block);

            for(Instruction inst : needFixBB.getInstructions()) {
                inst.fix();
                if (inst instanceof Instruction.Return && ((Instruction.Return) inst).hasValue()) {
                    Instruction jumpToRetBB = new Instruction.Jump(needFixBB, retBB);
                    jumpToRetBB.remove();
                    needFixBB.getInstructions().insertBefore(jumpToRetBB, inst);
                    //instr.insertBefore(jumpToRetBB);
//                    retBB.getPreBlocks().add(needFixBB);
//                    needFixBB.setSucBlocks(retSucc);
                    assert alloc != null;
                    //retPhi.addOptionalValue(((Instr.Return) instr).getRetValue());
                    Instruction.Store store = new Instruction.Store(needFixBB, ((Instruction.Return) inst).getRetValue(), alloc);
                    store.remove();
                    needFixBB.getInstructions().insertBefore(store, jumpToRetBB);
                    //load = new Instruction.Load(retBB, alloc);
                    //load.remove();
                    //retBB.getInstructions().insertAfter(load, store);
                    inst.remove();
                    //维护前驱后继
                } else if (inst instanceof Instruction.Return) {
                    Instruction jumpToRetBB = new Instruction.Jump(needFixBB, retBB);
                    jumpToRetBB.remove();
                    needFixBB.getInstructions().insertBefore(jumpToRetBB, inst);
                    //instr.insertBefore(jumpToRetBB);
//                    retBB.getPreBlocks().add(needFixBB);
//                    needFixBB.setSucBlocks(retSucc);
                    inst.remove();
                }
            }
        }

        if (!(retType instanceof Type.VoidType)) {
            load = new Instruction.Load(retBB, alloc);
            //CloneInfo.addValueReflect(call, load);
        }
        return load;

    }



}
