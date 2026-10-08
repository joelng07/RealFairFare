package service;

import model.Expense;
import model.Group;
import model.User;
import model.UserRole;
import persistence.FairFareRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Administrator-only operations for users, groups, and expense records. */
public final class AdminService {
    private final AuthService authService;
    private final GroupService groupService;
    private final BalanceManager balanceManager;
    private final FairFareRepository repository;

    public AdminService(AuthService authService, GroupService groupService, BalanceManager balanceManager, FairFareRepository repository) {
        this.authService = Objects.requireNonNull(authService);
        this.groupService = Objects.requireNonNull(groupService);
        this.balanceManager = Objects.requireNonNull(balanceManager);
        this.repository = Objects.requireNonNull(repository);
    }

    public List<User> users(User actor) { requireAdmin(actor); return authService.getUsers().stream().sorted(Comparator.comparing(User::getUsername)).toList(); }
    public List<Group> groups(User actor) { requireAdmin(actor); return groupService.allGroups(); }
    public List<Expense> expenses(User actor) { requireAdmin(actor); return groupService.allGroups().stream().flatMap(group -> group.getExpenses().stream()).sorted(Comparator.comparing(Expense::getCreatedAt).reversed()).toList(); }

    public void setRole(User actor, User user, UserRole role) {
        requireAdmin(actor);
        if (actor.equals(user) && role != UserRole.ADMIN) throw new IllegalArgumentException("You cannot remove your own administrator access.");
        user.setRole(role); repository.updateUserRole(user);
    }

    public void renameGroup(User actor, Group group, String name) {
        requireAdmin(actor); group.rename(name); repository.renameGroup(group);
    }

    public void deleteGroup(User actor, Group group) {
        requireAdmin(actor); repository.deleteGroup(group);
        groupService.removeGroup(group);
        balanceManager.rebuild(groupService.allGroups());
    }

    public void renameExpense(User actor, Group group, Expense expense, String title) {
        requireAdmin(actor); Expense replacement = expense.withTitle(title); repository.updateExpenseTitle(replacement); group.replaceExpense(expense, replacement); balanceManager.rebuild(groupService.allGroups());
    }

    public void deleteExpense(User actor, Group group, Expense expense) {
        requireAdmin(actor); repository.deleteExpense(expense); group.removeExpense(expense); balanceManager.rebuild(groupService.allGroups());
    }

    private static void requireAdmin(User actor) {
        if (actor == null || !actor.isAdmin()) throw new SecurityException("Administrator access is required.");
    }
}
