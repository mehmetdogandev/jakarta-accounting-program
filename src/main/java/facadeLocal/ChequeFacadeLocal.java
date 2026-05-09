package facadeLocal;

import entity.Cheque;
import jakarta.ejb.Local;

import java.util.List;
import java.util.UUID;

@Local
public interface ChequeFacadeLocal {

    List<Cheque> findAll();

    Cheque findById(UUID id);

    List<Cheque> findByType(String type);

    List<Cheque> findByStatus(String status);

    List<Cheque> findDueWithin(int days);

    Cheque save(Cheque cheque);

    void deposit(UUID id, UUID bankAccountId);

    void collect(UUID id);

    void returnCheque(UUID id);

    void protest(UUID id);

    void pay(UUID id);
}
