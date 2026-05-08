package service;

import enums.Permission;
import enums.Scope;

import jakarta.ejb.Local;

@Local
public interface AuthorizationServiceLocal {

    boolean can(String userId, Permission permission);

    boolean can(String userId, Permission permission, Scope scope);
}
