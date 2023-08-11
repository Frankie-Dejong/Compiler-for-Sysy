package midend;

import mir.Instruction;
import mir.Value;

import java.util.HashMap;
import java.util.HashSet;

public class CloneInfo {

    public static HashMap<Integer, Integer> loopCondCntMap = new HashMap<>();
    public static HashMap<Value, Value> valueMap = new HashMap<>();
    //public static HashSet<Loop> loopNeedFix = new HashSet<>();
    //public static HashSet<BasicBlock> bbNeedFix = new HashSet<>();


//
//    public static boolean isReflected(Loop loop) {
//        return loopMap.containsKey(loop);
//    }



    public static void addLoopCondCntReflect(Integer src, Integer tag) {
        loopCondCntMap.put(src, tag);
    }

    public static Integer getLoopCondCntReflect(Integer cnt) {
        if (loopCondCntMap.containsKey(cnt)) {
            return loopCondCntMap.get(cnt);
        } else {
            return cnt;
        }
    }

    public static void addValueReflect(Value src, Value tag) {
        valueMap.put(src, tag);
    }

    public static Value getReflectedValue(Value value) {
        if (valueMap.containsKey(value)) {
            return valueMap.get(value);
        }
        return value;
    }


//    public static void addLoopNeedFix(Loop loop) {
//        loopNeedFix.add(loop);
//    }
//
//    public static void rmBBNeedFix(BasicBlock bb) {
//        bbNeedFix.remove(bb);
//    }

    public static void clear() {
        loopCondCntMap.clear();
        valueMap.clear();
        //loopNeedFix.clear();
    }

}
