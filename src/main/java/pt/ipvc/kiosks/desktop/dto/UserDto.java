package pt.ipvc.kiosks.desktop.dto;

public class UserDto {
    public Long    id;
    public String  username;
    public String  email;
    public Boolean active;
    public String  roleName;
    public Long    storeId;
    public String  storeName;

    @Override public String toString() { return username != null ? username : ""; }
}
