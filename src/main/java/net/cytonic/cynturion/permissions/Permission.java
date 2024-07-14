package net.cytonic.cynturion.permissions;

import org.jetbrains.annotations.NotNull;

/**
 * Each permission has a string representation used as an identifier.
 * The class is immutable.
 */
public record Permission(String node) {

    /**
     * Creates a new permission object
     *
     * @param node the name of the permission
     */
    public Permission(@NotNull String node) {
        this.node = node;
    }

    /**
     * Gets the name of the permission.
     *
     * @return the permission name
     */
    @NotNull
    public String node() {
        return node;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return node.equals(that.node);
    }

}
