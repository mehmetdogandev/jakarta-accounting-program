package facade;

import entity.Student;
import jakarta.ejb.Stateless;

@Stateless
public class StudentFacade extends AbstractFacade {

    public Student createStudent(Student entity) {
        this.entityManager.persist(entity);
        this.entityManager.flush();
        return entity;
    }

}
