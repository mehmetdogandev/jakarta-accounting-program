package facade;

import entity.AppUser;
import facadeLocal.UserFacadeLocal;
import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.time.Instant;
import java.util.List;

@Stateless
public class UserFacade extends AbstractFacade implements UserFacadeLocal {

    @Override
    public void createUser(AppUser u) {
        this.entityManager.persist(u);
        this.entityManager.flush();
    }

    @Override
    public AppUser editUser(AppUser entity) {
        this.entityManager.merge(entity);
        this.entityManager.flush();
        return entity;
    }

    @Override
    public void remove(AppUser entity) {
        AppUser merged = this.entityManager.merge(entity);
        this.entityManager.remove(merged);
    }

    @Override
    public List<AppUser> usersList() {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<AppUser> cq = cb.createQuery(AppUser.class);
        Root<AppUser> root = cq.from(AppUser.class);
        cq.select(root).where(cb.isNull(root.get("deletedAt")));
        TypedQuery<AppUser> q = this.entityManager.createQuery(cq);
        return q.getResultList();
    }

    @Override
    public AppUser login(String email, String password) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<AppUser> cq = cb.createQuery(AppUser.class);
        Root<AppUser> root = cq.from(AppUser.class);
        cq.where(
                cb.equal(root.get("email"), email),
                cb.equal(root.get("password"), password),
                cb.isNull(root.get("deletedAt")));
        cq.select(root);
        TypedQuery<AppUser> q = this.entityManager.createQuery(cq);
        List<AppUser> found = q.getResultList();
        if (found.isEmpty()) {
            return null;
        }
        return found.get(0);
    }

    @Override
    public AppUser findById(String id) {
        return this.entityManager.find(AppUser.class, id);
    }

    @Override
    public boolean emailExists(String email, String excludeUserId) {
        String jpql = "SELECT COUNT(u) FROM AppUser u WHERE u.email = :e AND u.deletedAt IS NULL";
        if (excludeUserId != null && !excludeUserId.isBlank()) {
            jpql += " AND u.id <> :ex";
        }
        var q = this.entityManager.createQuery(jpql, Long.class).setParameter("e", email);
        if (excludeUserId != null && !excludeUserId.isBlank()) {
            q.setParameter("ex", excludeUserId);
        }
        Long count = q.getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public void softDeleteUser(String targetUserId, String actorUserId) {
        AppUser u = findById(targetUserId);
        if (u == null || u.getDeletedAt() != null) {
            return;
        }
        Instant now = Instant.now();
        u.setDeletedAt(now);
        u.setDeletedBy(actorUserId);
        u.setLastUpdatedBy(actorUserId);
        this.entityManager.merge(u);
        this.entityManager.flush();
    }
}
