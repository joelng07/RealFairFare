package service;

import model.Group;
import model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Creates groups and controls membership. */
public final class GroupService {
    private final List<Group> groups = new ArrayList<>();
    public Group createGroup(String name, User owner) {
        Group group = new Group(name);
        group.addMember(Objects.requireNonNull(owner, "owner must not be null"));
        groups.add(group);
        return group;
    }
    public void addMember(Group group, User user) { group.addMember(user); }
    public List<Group> groupsFor(User user) { return user.getGroups(); }
}
