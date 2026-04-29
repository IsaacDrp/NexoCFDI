package mx.synectura.nexo_cfdi.shared.domain.user.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.synectura.nexo_cfdi.shared.domain.user.PersonType;
import mx.synectura.nexo_cfdi.shared.domain.user.RegimenFiscal;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "microsoft_sub", unique = true, nullable = false)
    private String microsoftSub;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "paternal_surname", nullable = false, length = 100)
    private String paternalSurname;

    @Column(name = "maternal_surname", length = 100)
    private String maternalSurname;

    @Column(name = "rfc", unique = true, length = 13)
    private String rfc;

    @Column(name = "razon_social")
    private String razonSocial;

    @Column(name = "postal_code", length = 5)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", length = 10)
    private PersonType personType;

    @Enumerated(EnumType.STRING)
    @Column(name = "regimen_fiscal", length = 50)
    private RegimenFiscal regimenFiscal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
