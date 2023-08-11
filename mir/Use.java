package mir;


import utils.SyncLinkedList;

import java.util.Objects;

public class Use extends SyncLinkedList.SyncLinkNode {
    private final User user;
    private Value value;

    public Use(User user, Value value) {
        this.user = user;
        this.value = value;
    }

    public User getUser() {
        return user;
    }

    public Value get() {
        return value;
    }
    public void set(Value value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Use use = (Use) o;
        return user.equals(use.user) && value.equals(use.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, value);
    }
}
