package entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_role")
public class UserRole implements Serializable {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "last_updated_by", length = 36)
    private String lastUpdatedBy;

    @Column(name = "deleted_by", length = 36)
    private String deletedBy;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
