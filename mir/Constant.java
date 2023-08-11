package mir;

import java.util.ArrayList;
import java.util.Objects;

public abstract class Constant extends User {

    public Constant(Type type) {
        super(type);
    }

    public abstract Object getConstValue();

    public abstract boolean isZero();

    @Override
    public String getDescriptor() {
        return toString();
    }

    public static class ConstantInt extends Constant {
        private final int intValue;//当前int具体的值

        public ConstantInt(int Value) {
            super(Type.BasicType.I32_TYPE);
            intValue = Value;
        }

        @Override
        public Object getConstValue() {
            return intValue;
        }

        @Override
        public String toString() {
            return String.valueOf(intValue);
        }

        @Override
        public boolean isZero() {
            return intValue == 0;
        }

    }

    public static class ConstantFloat extends Constant {
        private final float floatValue;

        public ConstantFloat(float val) {
            super(Type.BasicType.F32_TYPE);
            floatValue = val;
        }

        @Override
        public Object getConstValue() {
            return floatValue;
        }

        @Override
        public String toString() {
            return String.format("0x%x", Double.doubleToRawLongBits((floatValue)));
        }

        @Override
        public boolean isZero() {
            return floatValue == 0;
        }

    }

    public static class ConstantArray extends Constant {
        private final ArrayList<Constant> constArray;
        private final Type eleType;

        public ConstantArray(Type type, Type eleType, ArrayList<Constant> constArray) {
            //todo: why pass 'type' as a param
            super(type);
            assert type instanceof Type.ArrayType;
            this.constArray = constArray;
            this.eleType = eleType;
        }

        @Override
        public Object getConstValue() {
            return constArray;
        }

        public Type getEleType() {
            return eleType;
        }

        @Override
        public boolean isZero() {
            for (Constant ele :
                    constArray) {
                if (!ele.isZero())
                    return false;
            }
            return true;
        }
    }

    public static class ConstantBool extends Constant {
        int boolValue;//0 or 1

        public ConstantBool(int val) {
            super(Type.BasicType.I1_TYPE);
            boolValue = val;
        }

        @Override
        public Object getConstValue() {
            return boolValue;
        }

        @Override
        public boolean isZero() {
            return boolValue == 0;
        }

        @Override
        public String toString() {
            return String.valueOf(boolValue);
        }

    }

}
