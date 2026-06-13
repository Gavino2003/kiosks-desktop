package pt.ipvc.kiosks.desktop.util;

import pt.ipvc.kiosks.dal.entities.User;

public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }

    public void logout() { currentUser = null; }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() != null
                && "ADMIN".equals(currentUser.getRole().getRoleName());
    }

    public boolean isManager() {
        return currentUser != null && currentUser.getRole() != null
                && ("MANAGER".equals(currentUser.getRole().getRoleName()) || isAdmin());
    }

    public boolean isOperator() {
        return currentUser != null && currentUser.getRole() != null
                && ("OPERATOR".equals(currentUser.getRole().getRoleName()) || isManager());
    }
}
