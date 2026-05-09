package facade;

import entity.Product;
import entity.StockMovement;
import facadeLocal.StockMovementFacadeLocal;
import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Stateless
public class StockMovementFacade extends AbstractFacade implements StockMovementFacadeLocal {

    @Override
    public StockMovement addMovement(UUID productId,
                                     String type,
                                     BigDecimal qty,
                                     BigDecimal cost,
                                     String description,
                                     String referenceType,
                                     UUID referenceId,
                                     String createdBy,
                                     boolean adjustmentSetsAbsolute) {
        Product product = entityManager.find(Product.class, productId);
        if (product == null || product.getDeletedAt() != null) {
            throw new IllegalStateException("Ürün bulunamadı.");
        }
        if (qty == null) {
            throw new IllegalStateException("Miktar zorunludur.");
        }

        String movementType = normalizeType(type);
        BigDecimal normalizedQty = qty.abs();
        if ("ADJUSTMENT".equals(movementType) && adjustmentSetsAbsolute) {
            product.setStockQuantity(normalizedQty);
        } else if ("IN".equals(movementType) || "RETURN_IN".equals(movementType)) {
            product.setStockQuantity(product.getStockQuantity().add(normalizedQty));
        } else if ("OUT".equals(movementType) || "RETURN_OUT".equals(movementType)) {
            product.setStockQuantity(product.getStockQuantity().subtract(normalizedQty));
        } else {
            product.setStockQuantity(product.getStockQuantity().add(normalizedQty));
        }
        entityManager.merge(product);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementDate(Instant.now());
        movement.setMovementType(movementType);
        movement.setQuantity(normalizedQty);
        movement.setUnitCost(cost == null ? BigDecimal.ZERO : cost);
        movement.setDescription(description);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setCreatedBy(createdBy);
        entityManager.persist(movement);
        entityManager.flush();
        return movement;
    }

    @Override
    public List<StockMovement> getMovements(UUID productId, LocalDate from, LocalDate to) {
        Instant fromTs = from == null ? Instant.EPOCH : from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toTs = to == null ? Instant.now().plusSeconds(86400) : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return entityManager.createQuery(
                        "SELECT sm FROM StockMovement sm "
                                + "WHERE sm.product.id = :productId "
                                + "AND sm.movementDate >= :fromTs AND sm.movementDate < :toTs "
                                + "ORDER BY sm.movementDate DESC",
                        StockMovement.class)
                .setParameter("productId", productId)
                .setParameter("fromTs", fromTs)
                .setParameter("toTs", toTs)
                .getResultList();
    }

    private static String normalizeType(String type) {
        String t = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        return switch (t) {
            case "IN", "OUT", "ADJUSTMENT", "RETURN_IN", "RETURN_OUT" -> t;
            default -> throw new IllegalStateException("Geçersiz hareket tipi.");
        };
    }
}
