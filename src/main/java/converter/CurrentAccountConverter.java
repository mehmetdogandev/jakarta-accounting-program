package converter;

import entity.CurrentAccount;
import facadeLocal.CurrentAccountFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

import java.util.UUID;

@FacesConverter(value = "currentAccountConverter", managed = true)
public class CurrentAccountConverter implements Converter<CurrentAccount> {

    @EJB
    private CurrentAccountFacadeLocal currentAccountFacade;

    @Override
    public CurrentAccount getAsObject(FacesContext facesContext, UIComponent uiComponent, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return currentAccountFacade.findById(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent uiComponent, CurrentAccount value) {
        if (value == null || value.getId() == null) {
            return "";
        }
        return value.getId().toString();
    }
}
