package enums;

public enum RoleEnum {
    ADMIN("admin"),
    STUDENT("student");

    private String name;

    private RoleEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
