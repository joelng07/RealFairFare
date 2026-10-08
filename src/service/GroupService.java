package service;

import model.Group;
import model.User;
import persistence.FairFareRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Creates groups and controls membership. */
public final class GroupService {
    private final List<Group> groups = new ArrayList<>();
    private final FairFareRepository repository;
    public GroupService(FairFareRepository repository, List<Group> existingGroups) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        groups.addAll(existingGroups);
    }
    public Group createGroup(String name, User owner) {
        Group group = new Group(name);
        group.addMember(Objects.requireNonNull(owner, "owner must not be null"));
        repository.saveGroup(group);
        repository.saveMembership(group, owner);
        groups.add(group);
        return group;
    }
    public void addMember(Group group, User user) {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(user, "user must not be null");
        if (!group.getMembers().contains(user)) {
            repository.saveMembership(group, user);
            group.addMember(user);
        }
    }
    public List<Group> groupsFor(User user) { return user.getGroups(); }
    public List<Group> allGroups() { return List.copyOf(groups); }
    public void removeGroup(Group group) {
        groups.remove(Objects.requireNonNull(group, "group must not be null"));
        group.getMembers().forEach(member -> member.leaveGroup(group));
    }
}
