package facadeLocal;

import entity.Product;
import jakarta.ejb.Local;

import java.util.List;
import java.util.UUID;

@Local
public interface ProductFacadeLocal {

    List<Product> findAll(boolean includeDeleted);

    Product findById(UUID id);

    Product findByCode(String code);

    List<Product> findByCategory(UUID categoryId);

    List<Product> searchByNameOrCode(String q);

    List<Product> findLowStock();

    Product save(Product product);

    void softDelete(UUID id);
}
