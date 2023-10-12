package dev.crashteam.charon.model.domain;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

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
    @Column(name = "discount_percentage")
    private Integer discountPercentage;

}
