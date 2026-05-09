package bean;

import entity.Product;
import entity.ProductCategory;
import enums.Permission;
import enums.Scope;
import facadeLocal.ProductFacadeLocal;
import facadeLocal.StockMovementFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import procedure.RbacProcedureBean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Named
@ViewScoped
public class AdminProductBean implements Serializable {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    @EJB
    private ProductFacadeLocal productFacade;

    @EJB
    private StockMovementFacadeLocal stockMovementFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    private List<Product> products = Collections.emptyList();
    private List<ProductCategory> categories = Collections.emptyList();
    private Product selected;
    private Product selectedForMovement;

    @PostConstruct
    public void init() {
        if (!rbacProcedure.require(Scope.PRODUCT, Permission.ACCESS)) {
            return;
        }
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.PRODUCT, Permission.READ)) {
            products = Collections.emptyList();
            categories = Collections.emptyList();
            return;
        }
        products = productFacade.findAll(false);
        categories = Collections.emptyList();
    }

    public void openNew() {
        if (!rbacProcedure.require(Scope.PRODUCT, Permission.CREATE)) {
            return;
        }
        selected = new Product();
        selected.setUnit("ADET");
        selected.setPurchasePrice(BigDecimal.ZERO);
        selected.setSalesPrice(BigDecimal.ZERO);
        selected.setTaxRate(BigDecimal.valueOf(18));
        selected.setStockQuantity(BigDecimal.ZERO);
        selected.setMinStockQuantity(BigDecimal.ZERO);
        selected.setActive(Boolean.TRUE);
    }

    public void openEdit(Product row) {
        if (!rbacProcedure.require(Scope.PRODUCT, Permission.UPDATE) || row == null || row.getId() == null) {
            return;
        }
        Product loaded = productFacade.findById(row.getId());
        selected = loaded != null ? loaded : row;
    }

    public void save() {
        if (selected == null) {
            return;
        }
        boolean isNew = selected.getId() == null || productFacade.findById(selected.getId()) == null;
        if (isNew) {
            if (!rbacProcedure.require(Scope.PRODUCT, Permission.CREATE)) {
                return;
            }
        } else {
            if (!rbacProcedure.require(Scope.PRODUCT, Permission.UPDATE)) {
                return;
            }
        }
        Product existing = productFacade.findByCode(selected.getCode());
        if (existing != null && (selected.getId() == null || !existing.getId().equals(selected.getId()))) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ürün kodu", "Bu kod zaten kullanılıyor."));
            return;
        }
        productFacade.save(selected);
        refresh();
        PrimeFaces.current().executeScript("PF('productDlg').hide()");
    }

    public void softDelete(Product row) {
        if (!rbacProcedure.require(Scope.PRODUCT, Permission.DELETE) || row == null || row.getId() == null) {
            return;
        }
        productFacade.softDelete(row.getId());
        refresh();
    }

    public void openStockMovement(Product row) {
        if (!rbacProcedure.require(Scope.PRODUCT, Permission.READ) || row == null || row.getId() == null) {
            return;
        }
        selectedForMovement = row;
        PrimeFaces.current().executeScript("window.location.href='stock-movements.xhtml?productId=" + row.getId() + "'");
    }

    public int getLowStockCount() {
        return productFacade.findLowStock().size();
    }

    public int getActiveCount() {
        int count = 0;
        for (Product p : products) {
            if (Boolean.TRUE.equals(p.getActive())) {
                count++;
            }
        }
        return count;
    }

    public String formatCurrency(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        return NumberFormat.getCurrencyInstance(TURKISH).format(safe);
    }

    public List<String> getUnitOptions() {
        return List.of("ADET", "KG", "LT", "M", "M2", "M3", "PAKET");
    }

    public List<Product> getProducts() {
        return products;
    }

    public List<ProductCategory> getCategories() {
        return categories;
    }

    public Product getSelected() {
        return selected;
    }

    public void setSelected(Product selected) {
        this.selected = selected;
    }

    public Product getSelectedForMovement() {
        return selectedForMovement;
    }
}
