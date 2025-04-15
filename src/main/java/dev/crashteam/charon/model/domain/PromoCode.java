package dev.crashteam.charon.model.domain;

import lombok.Data;
import lombok.ToString;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@ToString(callSuper = true)
@Table(name = "promo_code")
public class PromoCode {
    @Id
    @SequenceGenerator(name = "promoCodeIdSeq", sequenceName = "promo_code_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "promoCodeIdSeq")
    private Long id;
    @Column(name = "code")
    private String code;
    @Column(name = "valid_until")
    private LocalDateTime validUntil;
    @Column(name = "usage_limit")
    private Long usageLimit;
    @Column(name = "description")
    private String description;
    @Column(name = "discount_percentage")
    private Integer discountPercentage;

    @ManyToMany
    @JoinTable(
            name = "promo_code_app_user",
            joinColumns = @JoinColumn(name = "promo_code_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @ToString.Exclude
    private Set<User> users;

}
