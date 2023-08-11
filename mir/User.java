package mir;

import java.util.LinkedList;

public class User extends Value {
    /**
     * 维护操作数的唯一性
     */
    private final LinkedList<Value> operands;

    protected User(String name, Type type) {
        super(name, type);
        operands = new LinkedList<>();
    }

    protected User(Type type) {
        super(type);
        operands = new LinkedList<>();
    }

    public int getNumOperands() {
        return operands.size();
    }

    public LinkedList<Value> getOperands() {
        return operands;
    }

    /**
     * 维护了双向边关系
     */
    public void addOperand(Value operand) {
        // 如果已包含直接返回
        if (operands.contains(operand)) {
            return;
        }
        operands.add(operand);
        Use use = new Use(this, operand);
        operand.use_add(use);
    }

    public Value getOperand(int idx) {
        return operands.get(idx);
    }

    /**
     * 同时删除双向边关系
     *
     * @param value
     * @param v
     */
    public void replaceUseOfWith(Value value, Value v) {
        // replace all operands that equals value with v
        value.use_remove(new Use(this, value));
        operands.remove(value);
        addOperand(v);
        //operands.set(operands.indexOf(value), v);
    }
}
