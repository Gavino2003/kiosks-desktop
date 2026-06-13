package pt.ipvc.kiosks.desktop.util;

import pt.ipvc.kiosks.desktop.dto.UserDto;

public class SessionManager {

    private static SessionManager instance;
    private UserDto currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public UserDto getCurrentUser() { return currentUser; }
    public void setCurrentUser(UserDto user) { this.currentUser = user; }
    public void logout() { currentUser = null; }

    public boolean isAdmin() {
        return currentUser != null && "ADMIN".equals(currentUser.roleName);
    }

    public boolean isManager() {
        return currentUser != null &&
                ("MANAGER".equals(currentUser.roleName) || isAdmin());
    }

    public boolean isOperator() {
        return currentUser != null &&
                ("OPERATOR".equals(currentUser.roleName) || isManager());
    }
}
