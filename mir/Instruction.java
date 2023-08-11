package mir;

import manager.Manager;
import midend.CloneInfo;

import java.util.*;


public class Instruction extends User {

    public enum InstType {
        VOID, // null
        RETURN,
        CALL,
        PHI,
        ALLOC,
        STORE,
        LOAD,
        BRANCH,
        JUMP,
        SItofp,
        FPtosi,
        Zext,
        Icmp,
        Fcmp,
        GEP,
        BitCast,
        ADD,
        SUB,
        FAdd,
        FSUB,
        MUL,
        DIV,
        FMUL,
        FDIV,
        REM,
        FREM;


    }

    public static class ResNameManager {
        private static final HashMap<InstType, Integer> indexes = new HashMap<>();

        public static String getName(InstType instType) {
            indexes.putIfAbsent(instType, 0);
            int index = indexes.get(instType);
            String name = "%" + instType.toString().toLowerCase() + "_" + index;
            index++;
            indexes.put(instType, index);
            return name;
        }
    }

    protected BasicBlock parentBlock;
    protected final InstType instType;
    public final String resName;

    public InstType getInstType() {
        return instType;
    }

    @Override
    public String getDescriptor() {
        return resName;
    }

    protected Instruction(BasicBlock parentBlock, Type type, InstType instType) {
        super(type);
        setName("");
        this.parentBlock = parentBlock;
        this.instType = instType;
        // 同步在parentBlock 中插入
        parentBlock.addInstLast(this);
        //分配一个结果名
        resName = ResNameManager.getName(instType);
    }

    public void setParentBlock(BasicBlock parentBlock) {
        this.parentBlock = parentBlock;
    }

    public BasicBlock getParentBlock() {
        return parentBlock;
    }

    public Instruction cloneToBB(BasicBlock newBlock) {
        return new Instruction(newBlock, type, instType);
    }

    public Instruction cloneToBBAndAddInfo(BasicBlock newBlock) {
        CloneInfo.addValueReflect(this, cloneToBB(newBlock));
        return (Instruction) CloneInfo.getReflectedValue(this);
    }



    //public Instruction
    public void fix() {
        ArrayList<Value> toReplace = new ArrayList<>();
        for (Value operand : getOperands()) {
            //getOperands().set(getOperands().indexOf(operand), CloneInfo.getReflectedValue(operand));
            if (operand != CloneInfo.getReflectedValue(operand)) {
                toReplace.add(operand);
            }
            //replaceUseOfWith(operand, CloneInfo.getReflectedValue(operand));
        }
        //setParentBlock((BasicBlock) CloneInfo.getReflectedValue(parentBlock));
        for (Value operand:
             toReplace) {
            //assert operand.getType().isPointerTy() == CloneInfo.getReflectedValue(operand).getType().isPointerTy();
            replaceUseOfWith(operand, CloneInfo.getReflectedValue(operand));
        }
    }

    /**
     * 返回值决定指令Type
     */
    public static class Return extends Instruction implements Terminator {

        private Value retValue;

        public Return(BasicBlock parentBlock) {
            super(parentBlock, Type.VoidType.VOID_TYPE, InstType.RETURN);
        }

        public Return(BasicBlock parentBlock, Value retValue) {
            super(parentBlock, retValue.getType(), InstType.RETURN);
            addOperand(retValue);
            this.retValue = retValue;
        }

        public boolean hasValue() {
            return !getOperands().isEmpty();
        }

        public Value getRetValue() {
            return retValue;
        }

        @Override
        public String toString() {
            Value retValue = getRetValue();
            if (retValue != null) {
                return String.format("ret %s %s", retValue.getType().toString(), retValue.getDescriptor());
            } else {
                return "ret void";
            }
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            retValue = v;
        }


        @Override
        public Return cloneToBB(BasicBlock block) {
            if (retValue != null)
                return new Return(block, retValue);
            return new Return(block);
        }



    }

    public static class Call extends Instruction {
        private final ArrayList<Value> params;
        private Function destFunction;
        private int strIdx = -1;

        public Call(BasicBlock parentBlock, Function destFunction, ArrayList<Value> params) {
            super(parentBlock, destFunction.getRetType(), InstType.CALL);
            this.destFunction = destFunction;
            this.params = params;
            parentBlock.getParentFunction().isLeaf = false;

            addOperand(destFunction);
            for (Value param : params) {
                addOperand(param);
            }
        }

        public Call(BasicBlock parentBlock, Function destFunction, ArrayList<Value> params, int strIdx) {
            super(parentBlock, destFunction.getRetType(), InstType.CALL);
            this.destFunction = destFunction;
            this.params = params;
            this.strIdx = strIdx;
            parentBlock.getParentFunction().isLeaf = false;

            addOperand(destFunction);
            for (Value param : params) {
                addOperand(param);
            }
        }

        public ArrayList<Value> getParams() {
            return params;
        }

        public Function getDestFunction() {
            return destFunction;
        }

        private String paramsToString() {
            StringBuilder str = new StringBuilder();
            Iterator<Value> iter = params.iterator();
            while (iter.hasNext()) {
                Value val = iter.next();
                str.append(val.type.toString() + " " + val.getDescriptor());
                if (iter.hasNext()) {
                    str.append(", ");
                }
            }
            return str.toString();
        }

        @Override
        public String toString() {
            if (strIdx != -1) {
                //fixme:to putf
                if (!params.isEmpty())
                    return "call void @" + Manager.ExternFunc.PUTF.getName() + "(ptr @.str_" + strIdx + ", " + paramsToString() + ")";
                else
                    return "call void @" + Manager.ExternFunc.PUTF.getName() + "(ptr @.str_" + strIdx + ")";
            }
            if (destFunction.getRetType() instanceof Type.VoidType) {
                return String.format("call void @%s(%s)", destFunction.name, paramsToString());
            } else {
                return String.format("%s = call %s @%s(%s)", getDescriptor(), destFunction.getRetType().toString(), destFunction.name, paramsToString());
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o.getClass() != getClass()) {
                return false;
            }
            if (params != ((Call) o).params) {
                return false;
            }
            return destFunction == ((Call) o).destFunction;
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            if (destFunction.equals(value)) {
                destFunction = (Function) v;
            }
            LinkedList<Value> toReplace = new LinkedList<>();
            for (Value param : params) {
                if (param.equals(value)) {
                    toReplace.add(param);
                }
            }
            for (Value param : toReplace) {
                params.set(params.indexOf(param), v);
            }
        }


        @Override
        public Call cloneToBB(BasicBlock newBlock) {
            return new Call(newBlock, destFunction, params);
        }

    }

    /**
     * 终结符, ValueType: Void
     */
    public interface Terminator {
    }

    public static class Branch extends Instruction implements Terminator {
        private Value cond;
        private BasicBlock thenBlock;
        private BasicBlock elseBlock;

        public Branch(BasicBlock parentBlock,
                      Value cond, BasicBlock thenBlock, BasicBlock elseBlock) {
            super(parentBlock, Type.VoidType.VOID_TYPE, InstType.BRANCH);
            this.cond = cond;
            this.thenBlock = thenBlock;
            this.elseBlock = elseBlock;

            assert cond.getType().isInt1Ty();

            addOperand(cond);
            addOperand(thenBlock);
            addOperand(elseBlock);

        }

        public Value getCond() {
            return cond;
        }

        public BasicBlock getThenBlock() {
            return thenBlock;
        }

        public BasicBlock getElseBlock() {
            return elseBlock;
        }

        @Override
        public String toString() {
            return String.format("br i1 %s, label %%%s, label %%%s", cond.getDescriptor(), thenBlock.getLabel(), elseBlock.getLabel());
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            if (cond.equals(value)) {
                cond = v;
            }
            if (thenBlock.equals(value)) {
                thenBlock = (BasicBlock) v;
            }
            if (elseBlock.equals(value)) {
                elseBlock = (BasicBlock) v;
            }
        }

        @Override
        public Branch cloneToBB(BasicBlock block) {
            return new Branch(block, cond, thenBlock, elseBlock);
        }
    }

    public static class Jump extends Instruction implements Terminator {
        private BasicBlock targetBlock;
        private Recorder.Mark mark;

        public Jump(BasicBlock parentBlock, BasicBlock targetBlock) {
            super(parentBlock, Type.VoidType.VOID_TYPE, InstType.JUMP);
            this.targetBlock = targetBlock;
            addOperand(targetBlock);
        }

        public Jump(BasicBlock parentBlock, Recorder.Mark mark) {
            super(parentBlock, Type.VoidType.VOID_TYPE, InstType.JUMP);
            this.targetBlock = null;
            this.mark = mark;
        }

        public void backFill(BasicBlock targetBlock) {
            assert this.targetBlock == null;
            this.targetBlock = targetBlock;
            addOperand(targetBlock);
        }

        public BasicBlock getTargetBlock() {
            return targetBlock;
        }

        @Override
        public String toString() {
            return "br label %" + targetBlock.getLabel();
        }

        public Recorder.Mark getMark() {
            return mark;
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            if (targetBlock.equals(value)) {
                targetBlock = (BasicBlock) v;
            }
        }

        @Override
        public Jump cloneToBB(BasicBlock newBlock) {
            return new Jump(newBlock, targetBlock);
        }
    }

    public static class Alloc extends Instruction {
        private final Type contentType;

        public Alloc(BasicBlock parentBlock, Type contentType) {
            super(parentBlock, new Type.PointerType(contentType), InstType.ALLOC);
            this.contentType = contentType;
        }

        public Type getContentType() {
            return contentType;
        }

        public boolean isArrayAlloc() {
            return contentType instanceof Type.ArrayType;
        }

        @Override
        public String toString() {
            return String.format("%s = alloca %s", resName, contentType);
        }

        @Override
        public Alloc cloneToBB(BasicBlock block) {
            return new Alloc(block, contentType);
        }
    }

    public static class Load extends Instruction {
        private Value addr;

        public Load(BasicBlock parentBlock, Value addr) {
            super(parentBlock, ((Type.PointerType) addr.getType()).getInnerType(), InstType.LOAD);
            this.addr = addr;
            assert addr.getType().isPointerTy();

            addOperand(addr);
        }

        public Value getAddr() {
            return addr;
        }

        public Type getInnerType(){
            return ((Type.PointerType) addr.getType()).getInnerType();
        }


        @Override
        public String toString() {
            Type.PointerType ptrTp = (Type.PointerType) addr.getType();
            return String.format("%s = load %s, %s %s", resName, ptrTp.getInnerType().toString(), ptrTp, addr.getDescriptor());
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            if (addr.equals(value)) {
                addr = v;
            }
        }

        @Override
        public Load cloneToBB(BasicBlock block) {
            return new Load(block, addr);
        }
    }

    public static class Store extends Instruction {
        private Value value;
        private Value addr;

        public Store(BasicBlock parentBlock, Value value, Value addr) {
            super(parentBlock, Type.VoidType.VOID_TYPE, InstType.STORE);
            this.value = value;
            this.addr = addr;

            assert addr.getType() instanceof Type.PointerType;

            //assert addr.getType().equals(new Type.PointerType(value.getType()));

            addOperand(value);
            addOperand(addr);
        }

        public Value getValue() {
            return value;
        }

        public Value getAddr() {
            return addr;
        }

        @Override
        public String toString() {
            return String.format("store %s %s, %s %s", value.getType().toString(), value.getDescriptor(), addr.getType().toString(), addr.getDescriptor());
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            if (this.value.equals(value)) {
                this.value = v;
            }
            if (addr.equals(value)) {
                addr = v;
            }
        }

        @Override
        public Store cloneToBB(BasicBlock block) {
            return new Store(block, value, addr);
        }
    }

    public static class SItofp extends Instruction {
        private Value src;

        public Value getSrc() {
            return src;
        }

        public SItofp(BasicBlock parentBlock, Value src) {
            super(parentBlock, Type.BasicType.F32_TYPE, InstType.SItofp);
            assert src.getType().isInt32Ty();
            this.src = src;

            addOperand(src);
        }

        @Override
        public String toString() {
            return String.format("%s = sitofp i32 %s to float", getDescriptor(), src.getDescriptor());
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            if (src.equals(value)) {
                src = v;
            }
        }

        @Override
        public SItofp cloneToBB(BasicBlock block) {
            return new SItofp(block, src);
        }
    }

    public static class FPtosi extends Instruction {
        private Value src;

        public Value getSrc() {
            return src;
        }

        public FPtosi(BasicBlock parentBlock, Value src) {
            super(parentBlock, Type.BasicType.I32_TYPE, InstType.FPtosi);
            assert src.getType().isFloatTy();
            this.src = src;
            addOperand(src);
        }

        @Override
        public String toString() {
            return String.format("%s = fptosi float %s to i32", getDescriptor(), src.getDescriptor());
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            if (src.equals(value)) {
                src = v;
            }
        }

        @Override
        public FPtosi cloneToBB(BasicBlock block) {
            return new FPtosi(block, src);
        }
    }

    //zero extend I1 to I32
    public static class Zext extends Instruction {
        private Value src;

        public Zext(BasicBlock parentBlock, Value src) {
            super(parentBlock, Type.BasicType.I32_TYPE, InstType.Zext);
            assert src.getType().isInt1Ty();
            this.src = src;
            addOperand(src);
        }

        @Override
        public String toString() {
            return String.format("%s = zext i1 %s to i32", getDescriptor(), src.getDescriptor());
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            if (src.equals(value)) {
                src = v;
            }
        }

        @Override
        public Zext cloneToBB(BasicBlock block) {
            return new Zext(block, src);
        }
    }

    public static class Icmp extends Instruction {
        public enum CondCode {
            EQ("eq"),
            NE("ne"),
            SGT("sgt"),
            SGE("sge"),
            SLT("slt"),
            SLE("sle");
            private final String str;

            CondCode(final String str) {
                this.str = str;
            }

            public String toString() {
                return str;
            }
        }

        private final CondCode condCode;

        public CondCode getCondCode() {
            return condCode;
        }

        private Value src1;
        private Value src2;

        public Icmp(BasicBlock parentBlock, CondCode condCode, Value src1, Value src2) {
            super(parentBlock, Type.BasicType.I1_TYPE, InstType.Icmp);
            assert !src1.getType().isFloatTy();
            assert src1.getType() == src2.getType();
            this.condCode = condCode;
            this.src1 = src1;
            this.src2 = src2;

            addOperand(src1);
            addOperand(src2);
        }


        @Override
        public String toString() {
            return String.format("%s = icmp %s %s %s, %s", getDescriptor(), condCode.toString(), src1.getType().toString(), src1.getDescriptor(), src2.getDescriptor());
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            if (src1.equals(value)) {
                src1 = v;
            }
            if (src2.equals(value)) {
                src2 = v;
            }
        }

        @Override
        public Icmp cloneToBB(BasicBlock block) {
            return new Icmp(block, condCode, src1, src2);
        }

    }

    public static class Fcmp extends Instruction {
        public enum CondCode {
            EQ("oeq"),
            NE("one"),
            OGT("ogt"),
            OGE("oge"),
            OLT("olt"),
            OLE("ole");
            private final String str;

            CondCode(final String str) {
                this.str = str;
            }

            public String toString() {
                return str;
            }
        }

        private final CondCode condCode;

        public CondCode getCondCode() {
            return condCode;
        }

        private Value src1;
        private Value src2;

        public Fcmp(BasicBlock parentBlock, CondCode condCode, Value src1, Value src2) {
            super(parentBlock, Type.BasicType.I1_TYPE, InstType.Fcmp);
            assert src1.getType().isFloatTy();
            assert src2.getType().isFloatTy();
            this.condCode = condCode;
            this.src1 = src1;
            this.src2 = src2;

            addOperand(src1);
            addOperand(src2);
        }

        @Override
        public String toString() {
            return String.format("%s = fcmp %s float %s, %s", getDescriptor(), condCode.toString(), src1.getDescriptor(), src2.getDescriptor());
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            if (src1.equals(value)) {
                src1 = v;
            }
            if (src2.equals(value)) {
                src2 = v;
            }
        }

        @Override
        public Fcmp cloneToBB(BasicBlock block) {
            return new Fcmp(block, condCode, src1, src2);
        }
    }

    public static class Phi extends Instruction {
        // 等同于返回值类型
        private final Type type;
        // 对齐前驱块的顺序
        private final LinkedList<Value> optionalValues = new LinkedList<>();

        public Phi(BasicBlock parentBlock, Type type, LinkedList<Value> optionalValues) {
            super(parentBlock, type, InstType.PHI);
            for (Value optionalValue : optionalValues) {
                appendOptionalValue(optionalValue);
            }
            this.type = type;
        }

        public void appendOptionalValue(Value value) {
            optionalValues.add(value);
            addOperand(value);
        }

        public Value getOptionalValue(int idx) {
            return optionalValues.get(idx);
        }

        public void replaceOptionalValueAtWith(int idx, Value value) {
            Value old = optionalValues.get(idx);
            optionalValues.set(idx, value);
            replaceUseOfWith(old, value);
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            for (int i = 0; i < optionalValues.size(); i++) {
                if (optionalValues.get(i).equals(value)) {
                    optionalValues.set(i, v);
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder(resName + " = phi " + type.toString() + " ");
            int len = optionalValues.size();
            for (int i = 0; i < len; i++) {
                Value value = optionalValues.get(i);
                assert i + 1 <= parentBlock.getPreBlocks().size();
                str.append("[ ");
                str.append(value.getDescriptor());
                str.append(", %");
                str.append(parentBlock.getPreBlocks().get(i).getLabel());
                str.append(" ]");
                if (i + 1 != len) {
                    str.append(", ");
                }
            }
            return str.toString();
        }

        @Override
        public Phi cloneToBB(BasicBlock block) {
            return new Phi(block, type, optionalValues);
        }
    }

    /**
     * 寻址指令，我们规定每次仅能寻址一维，即只支持base[offset]，对于高维数组的寻址可通过多个该指令完成
     */
    public static class GetElementPtr extends Instruction {
        private Value base;
        private final Type eleType;
        private final ArrayList<Value> offsets;

        public GetElementPtr(BasicBlock parentBlock, Value base, Type eleType, ArrayList<Value> offsets) {
            super(parentBlock, new Type.PointerType(eleType), InstType.GEP);
            this.base = base;
            this.eleType = eleType;
            this.offsets = offsets;

            addOperand(base);
            for (Value offset :
                    offsets) {
                addOperand(offset);
            }
        }

        public Value getBase() {
            return base;
        }

        public ArrayList<Value> getOffsets() {
            return offsets;
        }

        @Override
        public String toString() {
            StringBuilder str;
            str = new StringBuilder(String.format("%s = getelementptr %s, %s %s, ",
                    getDescriptor(),
                    ((Type.PointerType) base.getType()).getInnerType().toString(),
                    base.getType().toString(),
                    base.getDescriptor())
            );
            Iterator<Value> iter = offsets.iterator();
            while (iter.hasNext()) {
                str.append("i32 " + iter.next().getDescriptor());
                if (iter.hasNext()) {
                    str.append(", ");
                }
            }
            return str.toString();
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            if (base.equals(value)) {
                base = v;
            }
            for (int i = 0; i < offsets.size(); i++) {
                if (offsets.get(i).equals(value)) {
                    offsets.set(i, v);
                }
            }
        }

        @Override
        public GetElementPtr cloneToBB(BasicBlock newBlock) {
            ArrayList<Value> offsets = new ArrayList<>();
            for (Value offset :
                 this.offsets) {
                offsets.add(offset);
            }
            return new GetElementPtr(newBlock, base, eleType, offsets);
        }
    }

    public static class BitCast extends Instruction {
        private Value src;

        public BitCast(BasicBlock parentBlock, Value src, Type targetType) {
            super(parentBlock, targetType, InstType.BitCast);
            this.src = src;

            addOperand(src);
        }

        @Override
        public String toString() {
            return String.format("%s = bitcast %s %s to %s", getDescriptor(), src.getType().toString(), src.getDescriptor(), getType().toString());
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            System.out.println("BitCast replaceUseOfWith");
            super.replaceUseOfWith(value, v);
            if (src.equals(value)) {
                src = v;
            }
        }

        @Override
        public BitCast cloneToBB(BasicBlock block) {
            return new BitCast(block, src, getType());
        }
    }

    public static abstract class BinaryOperation extends Instruction{
        protected Value operand_1;
        protected Value operand_2;
        protected final Type resType;
        public BinaryOperation(BasicBlock parentBlock, Type resType, InstType instType, Value operand_1, Value operand_2) {
            super(parentBlock, resType, instType);
            assert operand_1.getType() == operand_2.getType();
            this.operand_1 = operand_1;
            this.operand_2 = operand_2;
            this.resType = resType;

            addOperand(operand_1);
            addOperand(operand_2);
        }

        @Override
        public void replaceUseOfWith(Value value, Value v) {
            super.replaceUseOfWith(value, v);
            if (operand_1.equals(value)) {
                operand_1 = v;
            }
            if (operand_2.equals(value)) {
                operand_2 = v;
            }
        }

    }

    public static class Add extends BinaryOperation{
        public Add(BasicBlock parentBlock, Type resType, Value operand_1, Value operand_2) {
            super(parentBlock, resType, InstType.ADD, operand_1, operand_2);
        }

        @Override
        public String toString() {
            return String.format("%s = add %s %s, %s", getDescriptor(), resType.toString(), operand_1.getDescriptor(), operand_2.getDescriptor());
        }

        @Override
        public Add cloneToBB(BasicBlock block) {
            return new Add(block, resType, operand_1, operand_2);
        }

    }

    public static class Sub extends BinaryOperation {

        public Sub(BasicBlock parentBlock, Type resType, Value operand_1, Value operand_2) {
            super(parentBlock, resType, InstType.SUB, operand_1, operand_2);
        }

        @Override
        public String toString() {
            return String.format("%s = sub %s %s, %s", getDescriptor(), resType.toString(), operand_1.getDescriptor(), operand_2.getDescriptor());
        }

        @Override
        public Sub cloneToBB(BasicBlock block) {
            return new Sub(block, resType, operand_1, operand_2);
        }
    }

    public static class FAdd extends  BinaryOperation {

        public FAdd(BasicBlock parentBlock, Type resType, Value operand_1, Value operand_2) {
            super(parentBlock, resType, InstType.FAdd, operand_1, operand_2);
        }

        @Override
        public String toString() {
            return String.format("%s = fadd %s %s, %s", getDescriptor(), resType.toString(), operand_1.getDescriptor(), operand_2.getDescriptor());
        }

        @Override
        public FAdd cloneToBB(BasicBlock block) {
            return new FAdd(block, resType, operand_1, operand_2);
        }

    }

    public static class FSub extends BinaryOperation {

        public FSub(BasicBlock parentBlock, Type resType, Value operand_1, Value operand_2) {
            super(parentBlock, resType, InstType.FSUB, operand_1, operand_2);
        }

        @Override
        public String toString() {
            return String.format("%s = fsub %s %s, %s", getDescriptor(), resType.toString(), operand_1.getDescriptor(), operand_2.getDescriptor());
        }

        @Override
        public FSub cloneToBB(BasicBlock block) {
            return new FSub(block, resType, operand_1, operand_2);
        }

    }

    public static class Mul extends  BinaryOperation {
        public Mul(BasicBlock parentBlock, Type resType, Value operand_1, Value operand_2) {
            super(parentBlock, resType, InstType.MUL, operand_1, operand_2);
        }

        @Override
        public String toString() {
            return String.format("%s = mul %s %s, %s", getDescriptor(), resType.toString(), operand_1.getDescriptor(), operand_2.getDescriptor());
        }

        @Override
        public Mul cloneToBB(BasicBlock block) {
            return new Mul(block, resType, operand_1, operand_2);
        }

    }

    public static class Div extends  BinaryOperation {

        public Div(BasicBlock parentBlock, Type resType, Value operand_1, Value operand_2) {
            super(parentBlock, resType, InstType.DIV, operand_1, operand_2);
        }

        @Override
        public String toString() {
            return String.format("%s = sdiv %s %s, %s", getDescriptor(), resType.toString(), operand_1.getDescriptor(), operand_2.getDescriptor());
        }

        @Override
        public Div cloneToBB(BasicBlock block) {
            return new Div(block, resType, operand_1, operand_2);
        }

    }

    public static class FMul extends  BinaryOperation  {

        public FMul(BasicBlock parentBlock, Type resType, Value operand_1, Value operand_2) {
            super(parentBlock, resType, InstType.FMUL, operand_1, operand_2);
        }

        @Override
        public String toString() {
            return String.format("%s = fmul %s %s, %s", getDescriptor(), resType.toString(), operand_1.getDescriptor(), operand_2.getDescriptor());
        }

        @Override
        public FMul cloneToBB(BasicBlock block) {
            return new FMul(block, resType, operand_1, operand_2);
        }

    }

    public static class FDiv extends  BinaryOperation {
        public FDiv(BasicBlock parentBlock, Type resType, Value operand_1, Value operand_2) {
            super(parentBlock, resType, InstType.FDIV, operand_1, operand_2);
        }

        @Override
        public String toString() {
            return String.format("%s = fdiv %s %s, %s", getDescriptor(), resType.toString(), operand_1.getDescriptor(), operand_2.getDescriptor());
        }

        @Override
        public FDiv cloneToBB(BasicBlock block) {
            return new FDiv(block, resType, operand_1, operand_2);
        }

    }

    public static class Rem extends  BinaryOperation {

        public Rem(BasicBlock parentBlock, Type resType, Value operand_1, Value operand_2) {
            super(parentBlock, resType, InstType.REM, operand_1, operand_2);
        }

        @Override
        public String toString() {
            return String.format("%s = srem %s %s, %s", getDescriptor(), resType.toString(), operand_1.getDescriptor(), operand_2.getDescriptor());
        }

        @Override
        public Rem cloneToBB(BasicBlock block) {
            return new Rem(block, resType, operand_1, operand_2);
        }

    }

    public static class FRem extends  BinaryOperation  {

        public FRem(BasicBlock parentBlock, Type resType, Value operand_1, Value operand_2) {
            super(parentBlock, resType, InstType.FREM, operand_1, operand_2);
        }

        @Override
        public String toString() {
            return String.format("%s = frem %s %s, %s", getDescriptor(), resType.toString(), operand_1.getDescriptor(), operand_2.getDescriptor());
        }

        @Override
        public FRem cloneToBB(BasicBlock block) {
            return new FRem(block, resType, operand_1, operand_2);
        }

    }



}
