package mir;

import java.util.ArrayList;
import java.util.Objects;

public class Type {

    public static class BasicType extends Type{

        public static final BasicType I32_TYPE = new BasicType();
        public static final BasicType F64_TYPE = new BasicType();
        public static final BasicType I64_TYPE = new BasicType();
        public static final BasicType I1_TYPE = new BasicType();
        public static final BasicType F32_TYPE = new BasicType();


        @Override
        public String toString() {
            if(this.equals(I1_TYPE)) {
                return "i1";
            }
            if(this.equals(I32_TYPE)) {
                return "i32";
            }
            if(this.equals(F32_TYPE)) {
                return "float";
            }
            if(this.equals(I64_TYPE)) {
                return "i64";
            }
            if(this.equals(F64_TYPE)) {
                return "double";
            }
            return "WRONG_TYPE";
        }

    }

    public boolean isInt1Ty() {
        return this == BasicType.I1_TYPE;
    }
    public boolean isInt64Ty() {
        return this == BasicType.I64_TYPE;
    }

    public boolean isInt32Ty() {
        return this == BasicType.I32_TYPE;
    }

    public boolean isFloatTy() {
        return this == BasicType.F32_TYPE || this == BasicType.F64_TYPE;
    }

    public static class VoidType extends Type {
        public static final VoidType VOID_TYPE = new VoidType();
        @Override
        public String toString() {
            return "void";
        }
    }

    public boolean isSizedTy() {
        return this != VoidType.VOID_TYPE;
    }

    public static class LabelType extends Type {
        public static final LabelType LABEL_TYPE = new LabelType();
        @Override
        public String toString() {
            return "LABEL_TYPE";
        }
    }

    public boolean isLabelTy() {return this == LabelType.LABEL_TYPE;}

    public static class ArrayType extends Type {
        private final int size;
        private final Type eleType;
        private final ArrayList<Integer> dims = new ArrayList<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ArrayType arrayType = (ArrayType) o;
            return size == arrayType.size && Objects.equals(eleType, arrayType.eleType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(size, eleType);
        }

        @Override
        public String toString() {
            return String.format("[%d x %s]", size, eleType);
        }

        public ArrayType(final int size, final Type eleType) {
            this.size = size;
            this.eleType = eleType;
            dims.add(size);
            if (eleType.isArrayTy()) {
                dims.addAll(((ArrayType) eleType).dims);
            }
        }

        public int getSize() {
            return this.size;
        }

        public int getFlattenSize() {
            if (eleType instanceof BasicType) {
                return size;
            }
            assert eleType.isArrayTy();
            return ((ArrayType) eleType).getFlattenSize() * size;
        }

        public Type getEleType() {
            return this.eleType;
        }

        public BasicType getBasicEleType() {
            if (getEleType() instanceof BasicType) {
                return (BasicType) getEleType();
            } else {
                assert getEleType().isArrayTy();
                ArrayType arrayType = (ArrayType) getEleType();
                return arrayType.getBasicEleType();
            }
        }

        public ArrayList<Integer> getDims() {
            // TODO: 没有用高速缓存, 每次都新建
            dims.clear();
            dims.add(size);
            if(eleType.isArrayTy()){
                dims.addAll(((ArrayType) eleType).dims);
            }
            return dims;
        }

        public int getBasicDim() {
            return dims.get(dims.size() - 1);
        }

        public int getDimSize(){
            if(eleType.isArrayTy()){
                return ((ArrayType) eleType).getDimSize() + 1;
            } else {
                return 1;
            }
        }
    }


    public boolean isArrayTy() {
        return this instanceof ArrayType;
    }
    public boolean isVoidTy(){ return this instanceof VoidType;}
    public static class PointerType extends Type {
        private final Type innerType;
        public PointerType(Type type) {
            innerType = type;
        }

        public Type getInnerType() {
            return this.innerType;
        }

        @Override
        public String toString() {
            return innerType.toString()+"*";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (getClass() != o.getClass() || o == null) {
                return false;
            }
            return ((PointerType) o).innerType.equals(innerType);
        }

    }

    public boolean isPointerTy() {return this instanceof PointerType;}


    public static class FunctionType extends Type {
        public static final FunctionType FUNC_TYPE = new FunctionType();
        @Override
        public String toString() {
            return "FUNC_TYPE";
        }
    }

}
