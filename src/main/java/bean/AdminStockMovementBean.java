package bean;

import entity.Product;
import entity.StockMovement;
import enums.Permission;
import enums.Scope;
import facadeLocal.ProductFacadeLocal;
import facadeLocal.StockMovementFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import procedure.RbacProcedureBean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Named
@ViewScoped
public class AdminStockMovementBean implements Serializable {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    @EJB
    private StockMovementFacadeLocal stockMovementFacade;

    @EJB
    private ProductFacadeLocal productFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    private Product selectedProduct;
    private List<StockMovement> movements = Collections.emptyList();
    private StockMovement newMovement;
    private LocalDate filterFrom;
    private LocalDate filterTo;

    @PostConstruct
    public void init() {
        if (!rbacProcedure.require(Scope.STOCK_MOVEMENT, Permission.ACCESS)) {
            return;
        }
        newMovement = new StockMovement();
        newMovement.setMovementType("IN");
        newMovement.setQuantity(BigDecimal.ZERO);
        newMovement.setUnitCost(BigDecimal.ZERO);
        readSelectedProductFromQuery();
        refreshMovements();
    }

    public void refreshMovements() {
        if (!rbacProcedure.require(Scope.STOCK_MOVEMENT, Permission.READ)) {
            movements = Collections.emptyList();
            return;
        }
        if (selectedProduct == null || selectedProduct.getId() == null) {
            movements = Collections.emptyList();
            return;
        }
        movements = stockMovementFacade.getMovements(selectedProduct.getId(), filterFrom, filterTo);
    }

    public void saveMovement() {
        if (!rbacProcedure.require(Scope.STOCK_MOVEMENT, Permission.CREATE)) {
            return;
        }
        if (selectedProduct == null || selectedProduct.getId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Ürün", "Önce bir ürün seçin."));
            return;
        }
        stockMovementFacade.addMovement(
                selectedProduct.getId(),
                newMovement.getMovementType(),
                newMovement.getQuantity(),
                newMovement.getUnitCost(),
                newMovement.getDescription(),
                newMovement.getReferenceType(),
                newMovement.getReferenceId(),
                rbacProcedure.currentUserId().orElse(null),
                "ADJUSTMENT".equalsIgnoreCase(newMovement.getMovementType())
        );
        newMovement = new StockMovement();
        newMovement.setMovementType("IN");
        newMovement.setQuantity(BigDecimal.ZERO);
        newMovement.setUnitCost(BigDecimal.ZERO);
        refreshMovements();
    }

    public List<Product> completeProduct(String query) {
        return productFacade.searchByNameOrCode(query);
    }

    public String movementBadgeClass(String type) {
        String t = type == null ? "" : type.toUpperCase(Locale.ROOT);
        return switch (t) {
            case "IN", "RETURN_IN" -> "badge text-bg-success";
            case "OUT", "RETURN_OUT" -> "badge text-bg-danger";
            default -> "badge text-bg-secondary";
        };
    }

    public String formatCurrency(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        return NumberFormat.getCurrencyInstance(TURKISH).format(safe);
    }

    public List<String> getMovementTypes() {
        return List.of("IN", "OUT", "ADJUSTMENT", "RETURN_IN", "RETURN_OUT");
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product selectedProduct) {
        this.selectedProduct = selectedProduct;
    }

    public List<StockMovement> getMovements() {
        return movements;
    }

    public StockMovement getNewMovement() {
        return newMovement;
    }

    public void setNewMovement(StockMovement newMovement) {
        this.newMovement = newMovement;
    }

    public LocalDate getFilterFrom() {
        return filterFrom;
    }

    public void setFilterFrom(LocalDate filterFrom) {
        this.filterFrom = filterFrom;
    }

    public LocalDate getFilterTo() {
        return filterTo;
    }

    public void setFilterTo(LocalDate filterTo) {
        this.filterTo = filterTo;
    }

    private void readSelectedProductFromQuery() {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        Map<String, String> params = ec.getRequestParameterMap();
        String productId = params.get("productId");
        if (productId == null || productId.isBlank()) {
            return;
        }
        try {
            selectedProduct = productFacade.findById(UUID.fromString(productId));
        } catch (IllegalArgumentException ignored) {
            selectedProduct = null;
        }
    }
}
