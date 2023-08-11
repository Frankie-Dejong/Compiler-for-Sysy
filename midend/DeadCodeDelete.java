package midend;

import mir.Function;
import mir.Module;
import mir.*;


import java.util.ArrayList;
import java.util.HashSet;

public class DeadCodeDelete {

    public static void run(Module module) {
        globalVar = module.getGlobalValues();
        uselessFuncDelete(module);
        for (Function function :
                module.getFuncSet()) {
            function.buildControlFlowGraph();
        }
//        printAllInsts(module);
        clean(module);
        br2Jump(module);
        cleanEmptyBlocks(module);
    }

    private static void br2Jump(Module module) {
        for (Function function :
                module.getFuncSet()) {
            if (function.isExternal()) {
                continue;
            }
            for (BasicBlock block :
                    function.getBlocks()) {
//                System.out.println("Block: " + block.getLabel());
                for (Instruction inst :
                        block.getInstructions()) {
                    if (inst instanceof Instruction.Branch) {
                        brToJump((Instruction.Branch) inst);
                    }
                }
            }
        }
    }

    private final static HashSet<Value> usefulVar = new HashSet<>();
    private final static HashSet<Function> usefulFunc = new HashSet<>();
    private final static HashSet<BasicBlock> usefulBB = new HashSet<>();
    private final static HashSet<Instruction> usefulInst = new HashSet<>();
    private static ArrayList<GlobalValue> globalVar;


    private static void cleanEmptyBlocks(Module module) {
        for (Function function :
                module.getFuncSet()) {
            if (function.isExternal()) {
                continue;
            }
            if (function.getBlocks().isEmpty()) {
                module.removeFunction(function);
                continue;
            }
            for (BasicBlock block :
                    function.getBlocks()) {
                if (block.getInstructions().isEmpty()) {
                    block.remove();
                }
            }
        }
    }

    private static void uselessFuncDelete(Module module) {
        Function main = module.getFunctions().get("main");

        usefulFunc.add(main);
        while (parseUseFulFunction(module)) ;

        for (Function function :
                module.getFuncSet()) {
            if (function.isExternal()) {
                continue;
            }
            uselessBBDelete(function);
        }

    }


    private static void uselessBBDelete(Function function) {
        for (BasicBlock block :
                function.getBlocks()) {
            uselessInstDelete(block);
            if (block.getInstructions().isEmpty()) {
                block.remove();
            }
        }
    }

    private static void uselessInstDelete(BasicBlock block) {
        for (Instruction inst :
                block.getInstructions()) {
            if (!usefulInst.contains(inst))
                inst.remove();
        }
    }

    private static boolean parseUseFulFunction(Module module) {
        int size = usefulVar.size();
        for (Function function :
                module.getFuncSet()) {
            if (function.isExternal()) {
                continue;
            }
            if (hasUseFulBB(function)) {
                usefulFunc.add(function);
            }
        }
        return size < usefulVar.size();
    }


    private static boolean hasUseFulBB(Function function) {
        int size = usefulVar.size();
        for (BasicBlock block :
                function.getBlocks()) {
            if (hasUseFulInst(block)) {
                usefulBB.add(block);
            }
        }
        return size < usefulVar.size();
    }

    private static boolean hasUseFulInst(BasicBlock block) {
        int size = usefulVar.size();

        for (Instruction inst :
                block.getInstructions()) {

            switch (inst.getInstType()) {
                case JUMP, BRANCH -> {
                    if (usefulBB.contains(inst.getParentBlock())) {
                        usefulInst.add(inst);
                        updateUse(inst);
                    }
                }
                case RETURN -> {
                    usefulBB.add(inst.getParentBlock());
                    usefulInst.add(inst);
                    updateUse(inst);
                }
                case STORE -> {
//                    Value addr = inst.getOperands().get(1);
                    Value addr = ((Instruction.Store) inst).getAddr();
                    if (usefulVar.contains(addr) || addr instanceof Instruction.GetElementPtr) {
                        usefulInst.add(inst);
                        updateUse(inst);
                    }
                }
                case CALL -> {
                    if (((Instruction.Call) inst).getDestFunction().isExternal()) {
                        usefulFunc.add(inst.getParentBlock().getParentFunction());
                        usefulFunc.add(((Instruction.Call) inst).getDestFunction());
                        usefulInst.add(inst);
                        updateUse(inst);
                    } else {
                        if (usefulFunc.contains(((Instruction.Call) inst).getDestFunction())) {
                            usefulInst.add(inst);
                            updateUse(inst);
                        } else if (usefulVar.contains(inst)) {
                            usefulFunc.add(((Instruction.Call) inst).getDestFunction());
                            usefulInst.add(inst);
                            updateUse(inst);
                        }
                        for (Value val :
                                inst.getOperands()) {
                            if ((val.getType() instanceof Type.PointerType)) {
                                usefulFunc.add(((Instruction.Call) inst).getDestFunction());
                                usefulInst.add(inst);
                                updateUse(inst);
                            }
                        }
                    }
                }
                default -> {
                    if (usefulVar.contains(inst)) {
                        usefulInst.add(inst);
                        updateUse(inst);
                    }
                }

            }
        }
        return size < usefulVar.size();
    }


    private static void updateUse(Instruction inst) {
        if (inst.getType() != Type.VoidType.VOID_TYPE) {
            usefulVar.add(inst);
        }
        usefulBB.add(inst.getParentBlock());
        for (BasicBlock block :
                inst.getParentBlock().getPreBlocks()) {
            usefulBB.add(block);
        }
        if (inst instanceof Instruction.Call) {
            for (Value operand :
                    ((Instruction.Call) inst).getParams()) {
                if (operand instanceof Instruction) {
                    usefulInst.add((Instruction) operand);
                    usefulVar.add(operand);
                }
                if (globalVar.contains(operand)) {
                    usefulVar.add(operand);
                }
            }
        } else {
            for (Value operand :
                    inst.getOperands()) {
                if (operand instanceof Instruction) {
                    usefulInst.add((Instruction) operand);
                    usefulVar.add(operand);
                }
                if (operand instanceof BasicBlock) {
                    usefulBB.add((BasicBlock) operand);
                }
                if (globalVar.contains(operand)) {
                    usefulVar.add(operand);
                }
            }
        }
    }

    private static void clean(Module module) {
        for (Function function :
                module.getFuncSet()) {


            for (BasicBlock block :
                    function.getBlocks()) {
                if (block.getInstructions().isEmpty()) {
                    continue;
                }
                Instruction inst = block.getLastInst();
                // print block label with inst
                //System.out.println(block.getLabel() + " " + inst.getDescriptor());
                if (inst instanceof Instruction.Return) {
                    continue;
                }
                assert inst instanceof Instruction.Terminator;

                if (inst instanceof Instruction.Branch) {
                    brToJump((Instruction.Branch) inst);
                }
            }


            for (BasicBlock block :
                    function.getBlocks()) {
                if (block.getInstructions().isEmpty()) {
                    continue;
                }
                Instruction inst = block.getLastInst();
                if (inst instanceof Instruction.Return) {
                    continue;
                }
                assert inst instanceof Instruction.Terminator;
                changeTarget(inst);
            }

            function.buildControlFlowGraph();

            for (BasicBlock block :
                    function.getBlocks()) {
                if (block.getInstructions().getSize() == 0 || block.isDeleted) {
                    continue;
                }
                mergeBlock(block);
            }

            for (BasicBlock block :
                    function.getBlocks()) {
                if (block.getInstructions().isEmpty()) {
                    continue;
                }
                Instruction inst = block.getLastInst();
                if (inst instanceof Instruction.Jump) {
                    replaceJump((Instruction.Jump) inst);
                }
            }


        }
    }

    private static void brToJump(Instruction.Branch br) {
//        System.out.println("else " + br.getElseBlock().getLabel() + " then " + br.getThenBlock().getLabel());
        if (br.getElseBlock().equals(br.getThenBlock())) {
            // 输出 else then block label

            BasicBlock block = br.getParentBlock();
            new Instruction.Jump(block, br.getElseBlock());
            br.remove();
            return;
        }

        if (br.getCond() instanceof Constant.ConstantBool) {
            if (((Constant.ConstantBool) br.getCond()).isZero()) {
                BasicBlock block = br.getParentBlock();
                new Instruction.Jump(block, br.getElseBlock());
                br.remove();
            } else {
                BasicBlock block = br.getParentBlock();
                new Instruction.Jump(block, br.getThenBlock());
                br.remove();
            }
        }

    }

    private static void changeTarget(Instruction inst) {
        assert inst instanceof Instruction.Jump || inst instanceof Instruction.Branch;
        if (inst instanceof Instruction.Jump) {
            Instruction nxtFirst = ((Instruction.Jump) inst).getTargetBlock().getFirstInst();
            if (nxtFirst instanceof Instruction.Jump) {
                do {
                    Instruction next = ((Instruction.Jump) nxtFirst).getTargetBlock().getFirstInst();
                    if (next instanceof Instruction.Jump) {
                        nxtFirst = next;
                    } else {
                        break;
                    }
                } while (true);
                BasicBlock newTarget = ((Instruction.Jump) nxtFirst).getTargetBlock();
                inst.replaceUseOfWith(((Instruction.Jump) inst).getTargetBlock(), newTarget);
            } else if (((Instruction.Jump) inst).getTargetBlock().getFirstInst() instanceof Instruction.Return) {
                Instruction.Return that = (Instruction.Return) ((Instruction.Jump) inst).getTargetBlock().getFirstInst();
                that.cloneToBB(inst.getParentBlock());
                inst.remove();
            }
        } else {
            Instruction thenFirst = ((Instruction.Branch) inst).getThenBlock().getFirstInst();
            Instruction elseFirst = ((Instruction.Branch) inst).getElseBlock().getFirstInst();
            if (thenFirst instanceof Instruction.Jump) {
                do {
                    Instruction next = ((Instruction.Jump) thenFirst).getTargetBlock().getFirstInst();
                    if (next instanceof Instruction.Jump) {
                        thenFirst = next;
                    } else {
                        break;
                    }
                } while (true);
                inst.replaceUseOfWith(((Instruction.Branch) inst).getThenBlock(), ((Instruction.Jump) thenFirst).getTargetBlock());
            }
            if (elseFirst instanceof Instruction.Jump) {
                do {
                    Instruction next = ((Instruction.Jump) elseFirst).getTargetBlock().getFirstInst();
                    if (next instanceof Instruction.Jump) {
                        elseFirst = next;
                    } else {
                        break;
                    }
                } while (true);
                inst.replaceUseOfWith(((Instruction.Branch) inst).getElseBlock(), ((Instruction.Jump) elseFirst).getTargetBlock());
            }
        }
    }


    private static void mergeBlock(BasicBlock block) {
        BasicBlock curBlock = block;
        do {
            Instruction inst = curBlock.getLastInst();
            if (inst instanceof Instruction.Jump) {
                BasicBlock that = ((Instruction.Jump) inst).getTargetBlock();
                if (that.getPreBlocks().size() == 1) {
                    inst.remove();
                    for (Instruction instruction : that.getInstructions()) {
                        instruction.setParentBlock(curBlock);
                    }
                    curBlock.getInstructions().concat(that.getInstructions());
                    that.remove();
                    for (BasicBlock suc :
                            that.getSucBlocks()) {
                        suc.getPreBlocks().remove(that);
                        suc.getPreBlocks().add(curBlock);
                    }
                    curBlock.getSucBlocks().remove(that);
                } else {
                    break;
                }
            } else {
                break;
            }
        } while (true);
    }


    private static void replaceJump(Instruction.Jump jump) {
        BasicBlock that = jump.getTargetBlock();
        if (that.getInstructions().isEmpty()) {
            return;
        }
        if (that.getFirstInst() instanceof Instruction.Branch) {
            ((Instruction.Branch) that.getFirstInst()).cloneToBB(jump.getParentBlock());
            jump.remove();
        }
    }


}
