package facade;

import entity.Product;
import facadeLocal.ProductFacadeLocal;
import jakarta.ejb.Stateless;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Stateless
public class ProductFacade extends AbstractFacade implements ProductFacadeLocal {

    @Override
    public List<Product> findAll(boolean includeDeleted) {
        if (includeDeleted) {
            return entityManager.createQuery("SELECT p FROM Product p ORDER BY p.code", Product.class)
                    .getResultList();
        }
        return entityManager.createQuery("SELECT p FROM Product p WHERE p.deletedAt IS NULL ORDER BY p.code", Product.class)
                .getResultList();
    }

    @Override
    public Product findById(UUID id) {
        return entityManager.find(Product.class, id);
    }

    @Override
    public Product findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        List<Product> rows = entityManager.createQuery(
                        "SELECT p FROM Product p WHERE p.deletedAt IS NULL AND LOWER(p.code) = :code",
                        Product.class)
                .setParameter("code", code.trim().toLowerCase(Locale.ROOT))
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public List<Product> findByCategory(UUID categoryId) {
        return entityManager.createQuery(
                        "SELECT p FROM Product p WHERE p.deletedAt IS NULL AND p.category.id = :categoryId ORDER BY p.code",
                        Product.class)
                .setParameter("categoryId", categoryId)
                .getResultList();
    }

    @Override
    public List<Product> searchByNameOrCode(String q) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return findAll(false);
        }
        return entityManager.createQuery(
                        "SELECT p FROM Product p WHERE p.deletedAt IS NULL "
                                + "AND (LOWER(p.name) LIKE :q OR LOWER(p.code) LIKE :q) "
                                + "ORDER BY p.code",
                        Product.class)
                .setParameter("q", "%" + query + "%")
                .getResultList();
    }

    @Override
    public List<Product> findLowStock() {
        return entityManager.createQuery(
                        "SELECT p FROM Product p WHERE p.deletedAt IS NULL AND p.stockQuantity < p.minStockQuantity ORDER BY p.stockQuantity",
                        Product.class)
                .getResultList();
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null || findById(product.getId()) == null) {
            entityManager.persist(product);
            entityManager.flush();
            return product;
        }
        Product merged = entityManager.merge(product);
        entityManager.flush();
        return merged;
    }

    @Override
    public void softDelete(UUID id) {
        Product product = findById(id);
        if (product == null || product.getDeletedAt() != null) {
            return;
        }
        product.setDeletedAt(Instant.now());
        entityManager.merge(product);
        entityManager.flush();
    }
}
