package facade;

import entity.Users;
import facadeLocal.UserFacadeLocal;
import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;

@Stateless
public class UserFacade extends AbstractFacade implements UserFacadeLocal {
    public void createUser(Users u) {
        this.entityManager.persist(u);
        this.entityManager.flush();
    }

    public Users editUser(Users entity) {
        this.entityManager.merge(entity);
        this.entityManager.flush();
        return entity;
    }

    public void remove(Users entity) {
        Users merged = this.entityManager.merge(entity);
        this.entityManager.remove(merged);
    }

    public List<Users> usersList() {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Users> cq = cb.createQuery(Users.class);
        Root<Users> root = cq.from(Users.class);
        CriteriaQuery<Users> all = cq.select(root);
        TypedQuery<Users> q = this.entityManager.createQuery(all);
        return q.getResultList();
    }

    public Users login(String email, String password) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Users> cq = cb.createQuery(Users.class);
        Root<Users> root = cq.from(Users.class);
        cq.where(cb.equal(root.get("email"), email), cb.equal(root.get("password"), password));
        CriteriaQuery<Users> all = cq.select(root);
        TypedQuery<Users> q = this.entityManager.createQuery(all);
        List<Users> found = q.getResultList();
        if (found.isEmpty()) {
            return null;
        } else {
            return found.getFirst();
        }
    }
}
