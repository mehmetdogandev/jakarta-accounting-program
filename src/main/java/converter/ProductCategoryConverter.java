package converter;

import entity.ProductCategory;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.UUID;

@FacesConverter(value = "productCategoryConverter", managed = true)
public class ProductCategoryConverter implements Converter<ProductCategory> {

    @PersistenceContext(unitName = "testPU")
    private EntityManager entityManager;

    @Override
    public ProductCategory getAsObject(FacesContext facesContext, UIComponent uiComponent, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return entityManager.find(ProductCategory.class, UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent uiComponent, ProductCategory value) {
        if (value == null || value.getId() == null) {
            return "";
        }
        return value.getId().toString();
    }
}
