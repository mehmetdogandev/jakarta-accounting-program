package facadeLocal;

import entity.StockMovement;
import jakarta.ejb.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Local
public interface StockMovementFacadeLocal {

    StockMovement addMovement(UUID productId,
                              String type,
                              BigDecimal qty,
                              BigDecimal cost,
                              String description,
                              String referenceType,
                              UUID referenceId,
                              String createdBy,
                              boolean adjustmentSetsAbsolute);

    List<StockMovement> getMovements(UUID productId, LocalDate from, LocalDate to);
}
