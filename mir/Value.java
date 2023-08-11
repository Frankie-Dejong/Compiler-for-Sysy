package mir;

import utils.SyncLinkedList;

import java.util.LinkedList;

public class Value extends SyncLinkedList.SyncLinkNode {

    protected String name;
    protected final Type type;
    /**
     * 维护使用者的唯一性
     */
    private final LinkedList<Use> uses;

    public Value(String name, Type type) {
        this.name = name;
        this.type = type;
        uses = new LinkedList<>();
    }

    public Value(Type type) {
        this.type = type;
        uses = new LinkedList<>();
        name = "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor(){
        return name;
    }

    public LinkedList<Use> getUses() {
        return uses;
    }

    public boolean use_empty() {
        return uses.isEmpty();
    }

    public int use_size() {
        return uses.size();
    }

    public Use use_begin() {
        return uses.getFirst();
    }

    public Use use_back() {
        return uses.getLast();
    }

    public void use_add(Use use) {
        if(!uses.contains(use)){
            uses.add(use);
        }
    }

    public void use_remove(Use use) {
        uses.remove(use);
    }

    public Type getType() {
        return type;
    }

    public void replaceAllUsesWith(Value v) {
        while (!use_empty()) {
            Use use = use_begin();
            // 每次必然删该条边
            use.getUser().replaceUseOfWith(this, v);
        }
    }

}
