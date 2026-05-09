package converter;

import entity.BankAccount;
import facadeLocal.BankAccountFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

import java.util.UUID;

@FacesConverter(value = "bankAccountConverter", managed = true)
public class BankAccountConverter implements Converter<BankAccount> {

    @EJB
    private BankAccountFacadeLocal bankAccountFacade;

    @Override
    public BankAccount getAsObject(FacesContext facesContext, UIComponent uiComponent, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return bankAccountFacade.findById(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent uiComponent, BankAccount value) {
        if (value == null || value.getId() == null) {
            return "";
        }
        return value.getId().toString();
    }
}
