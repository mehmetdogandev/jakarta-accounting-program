package converter;

import entity.Product;
import facadeLocal.ProductFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

import java.util.UUID;

@FacesConverter(value = "productConverter", managed = true)
public class ProductConverter implements Converter<Product> {

    @EJB
    private ProductFacadeLocal productFacade;

    @Override
    public Product getAsObject(FacesContext facesContext, UIComponent uiComponent, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return productFacade.findById(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent uiComponent, Product value) {
        if (value == null || value.getId() == null) {
            return "";
        }
        return value.getId().toString();
    }
}
