package bootstrap;

import facadeLocal.UserAssignmentFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * When enabled via {@code SEED_ADMIN_ENSURE_SUPER_ADMIN}, ensures the seeded admin user has an
 * active {@code super_admin} role group assignment after startup (e.g. Docker {@code pnpm dev}).
 * Soft-deleted rows are restored; missing rows are inserted by {@link facade.UserAssignmentFacade}.
 */
@Singleton
@Startup
public class SeedAdminSuperAdminBootstrap {

    private static final Logger LOG = Logger.getLogger(SeedAdminSuperAdminBootstrap.class.getName());

    /** Matches {@code V1}/{@code V3} seed admin id */
    private static final String SEED_ADMIN_ID = "00000000-0000-4000-8000-000000000001";

    /** Matches {@code V3__super_admin_rbac_seed.sql} {@code role_group} id */
    private static final UUID SUPER_ADMIN_GROUP_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");

    @EJB
    private UserAssignmentFacadeLocal assignmentFacade;

    @PostConstruct
    public void ensureSeedAdminSuperAdminGroup() {
        if (!isEnsureEnabled()) {
            return;
        }
        LOG.info("SEED_ADMIN_ENSURE_SUPER_ADMIN: ensuring super_admin role group for seed admin user");
        assignmentFacade.assignRoleGroup(SEED_ADMIN_ID, SUPER_ADMIN_GROUP_ID, SEED_ADMIN_ID);
    }

    private static boolean isEnsureEnabled() {
        String raw = System.getenv("SEED_ADMIN_ENSURE_SUPER_ADMIN");
        if (raw == null) {
            return false;
        }
        String v = raw.trim().toLowerCase();
        return "1".equals(v) || "true".equals(v) || "yes".equals(v);
    }
}
