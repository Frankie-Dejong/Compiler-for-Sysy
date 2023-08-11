package midend;

import mir.*;
import mir.Module;
import utils.SyncLinkedList;

import java.util.*;

public class FunctionInline {

    //TODO: 不能递归
    //TODO: 寻找函数调用链
    //TODO: 递归的内联


    //fixme:考虑库函数如何维持正确性
    //      因为建立反向图的时候,只考虑了自定义的函数

    //TODO:如果存在调用:
    // A -> A
    // B -> A
    // C -> B
    // 在当前判断中 A B C 入度均为1,无法内联
    // 但是,其实可以把B内联到C里,
    // 所以可以内联的条件可以加强为:对于一个函数,如果入度为0/入度不为0,但是所有的入边对应的函数,均只存在自调用

    private static Collection<Function> functions;

    private static Module module;
    private static ArrayList<Function> funcCanInline = new ArrayList<>();

    //A调用B则存在B->A
    private static HashMap<Function, HashSet<Function>> reserveMap = new HashMap<>();
    //记录反图的入度
    private static HashMap<Function, Integer> inNum = new HashMap<>();
    //A调用B则存在A->B
    private static HashMap<Function, HashSet<Function>> Map = new HashMap<>();
    private static Queue<Function> queue = new LinkedList<>();


    public static void run(Module module) {
        FunctionInline.module = module;
        functions = module.getFuncSet();
        GetFuncCanInline();
        for (Function function : funcCanInline) {
            inlineFunc(function);
            module.removeFunction(function);
//            functions.remove(function);
            //function.setDeleted();
//            for (BasicBlock bb = function.getFirstBlock(); bb.getNext() != null; bb = (BasicBlock) bb.getNext()) {
//                for (Instruction instr = bb.getFirstInst(); instr.getNext() != null; instr = (Instruction) instr.getNext()) {
//                    instr.remove();
//                }
//            }
//            function.setDeleted();
        }
        //System.err.println("fun_inline_end");
    }

    private static void GetFuncCanInline() {
        makeReserveMap();
        topologySort();
    }

    //f1调用f2 添加一条f2到f1的边
    private static void makeReserveMap() {
        for (Function function : functions) {
            Map.put(function, new HashSet<>());
        }
        for (Function function : functions) {
            reserveMap.put(function, new HashSet<>());
            if (!inNum.containsKey(function)) {
                inNum.put(function, 0);
            }

            for (Use use :
                    function.getUses()) {
                assert use.getUser() instanceof Instruction.Call;
                Function userFunc = ((Instruction.Call) use.getUser()).getParentBlock().getParentFunction();
                Map.get(userFunc).add(function);
                if (!inNum.containsKey(userFunc)) {
                    inNum.put(userFunc, 0);
                }
                if (reserveMap.get(function).add(userFunc)) {
                    inNum.put(userFunc, inNum.get(userFunc) + 1);
                }
            }
        }
    }


    private static void topologySort() {
        for (Function function : inNum.keySet()) {
            if (inNum.get(function) == 0 && !function.getName().equals("main") && !function.isExternal()) {
                queue.add(function);
            }
        }
        while (!queue.isEmpty()) {
            Function pos = queue.peek();
            funcCanInline.add(pos);
            for (Function next : reserveMap.get(pos)) {
                inNum.put(next, inNum.get(next) - 1);
                if (inNum.get(next) == 0 && !next.getName().equals("main") && !next.isExternal()) {
                    queue.add(next);
                }
            }
            queue.poll();
        }
    }


    private static void inlineFunc(Function function) {
        ArrayList<Instruction.Call> callers = new ArrayList<>();
        ArrayList<BasicBlock> targets = new ArrayList<>();
        for (Use use : function.getUses()) {
            assert use.getUser() instanceof Instruction.Call;
            callers.add((Instruction.Call) use.getUser());
            targets.add(((Instruction.Call) use.getUser()).getParentBlock());
        }
        int idx = 0;
        for (Instruction.Call call :
                callers) {
            transCallToFunc(function, call, idx, callers);
            idx++;
        }
    }

    private static void transCallToFunc(Function function, Instruction.Call call, int idx, ArrayList<Instruction.Call> callers) {
        CloneInfo.clear();
        Function inFunction = ((Instruction.Call) CloneInfo.getReflectedValue(call)).getParentBlock().getParentFunction();
        BasicBlock beforeCallBB = call.getParentBlock();

        BasicBlock nxtBlock = null;


        Instruction inst = null;
        for (Instruction tmp :
                beforeCallBB.getInstructions()) {
            if (tmp == call) {
                inst = tmp;
                break;
            }
        }
        assert inst != null;


        BasicBlock retBB = new BasicBlock(function.getName() + "_ret_" + idx, inFunction);

        Instruction.Alloc alloc = null;
        if (!(function.getRetType() instanceof Type.VoidType)) {
            alloc = new Instruction.Alloc(inFunction.getFirstBlock(), function.getRetType());
            alloc.remove();
            inFunction.getFirstBlock().getInstructions().addFirst(alloc);
        }

        Instruction.Load load = function.inlineToFunc(inFunction, retBB, call, alloc, idx);

        BasicBlock afterCallBB = new BasicBlock(inFunction.getName() + "_after_call_" + function.getName() + "_" + idx, inFunction);
        LinkedList<Instruction> instrs = new LinkedList<>();
        SyncLinkedList.SyncLinkNode instr = inst.getNext();
        while (instr instanceof Instruction) {
            instrs.add((Instruction) instr);
            instr = instr.getNext();
        }

        for (Instruction instr1 : instrs) {
            Instruction newInst = instr1.cloneToBBAndAddInfo(afterCallBB);
            newInst.fix();
            if (instr1 instanceof Instruction.Call && callers.contains(instr1)) {
                callers.set(callers.indexOf(instr1), (Instruction.Call) newInst);
            } else if (instr1 instanceof Instruction.Call) {
                instr1.remove();
                instr1.setParentBlock(afterCallBB);
                afterCallBB.getInstructions().insertBefore(instr1, newInst);
                newInst.remove();
                CloneInfo.addValueReflect(instr1, instr1);

            }
            ArrayList<Use> toFix = new ArrayList<>(instr1.getUses());
            for (Use use :
                    toFix) {
                ((Instruction) use.getUser()).fix();
            }
        }


        Instruction jumpToCallBB = new Instruction.Jump(beforeCallBB, (BasicBlock) CloneInfo.getReflectedValue(function.getFirstBlock()));
        jumpToCallBB.remove();
        beforeCallBB.getInstructions().insertBefore(jumpToCallBB, inst);
        Instruction jumpToAfterCallBB = new Instruction.Jump(retBB, afterCallBB);
        if (load != null) {
            load.remove();
            retBB.getInstructions().insertBefore(load, jumpToAfterCallBB);
        }


        if (load != null) {
            ArrayList<Use> toFix = new ArrayList<>(inst.getUses());
            for (Use use :
                    toFix) {
                use.getUser().replaceUseOfWith(use.get(), load);
            }
        }

        assert inst.getParentBlock().equals(beforeCallBB);


        while (inst.hasNext()) {
            inst.getNext().remove();
        }
        //beforeCallBB.getInstructions().setEnd(inst);
        inst.remove();

    }


}
