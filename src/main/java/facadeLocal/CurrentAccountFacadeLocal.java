package facadeLocal;

import entity.CurrentAccount;
import jakarta.ejb.Local;

import java.util.List;
import java.util.UUID;

@Local
public interface CurrentAccountFacadeLocal {

    List<CurrentAccount> findAll(boolean includeDeleted);

    CurrentAccount findById(UUID id);

    CurrentAccount findByCode(String code);

    List<CurrentAccount> findByType(String type);

    List<CurrentAccount> searchByName(String query);

    CurrentAccount save(CurrentAccount account);

    void softDelete(UUID id);
}
