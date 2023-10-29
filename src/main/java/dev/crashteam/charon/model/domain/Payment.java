package dev.crashteam.charon.model.domain;

import dev.crashteam.charon.model.RequestPaymentStatus;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@ToString(callSuper = true)
@Table(name = "payment")
public class Payment {

    @Id
    @Column(name = "payment_id")
    private String paymentId;
    @Column(name = "user_id", insertable = false, updatable = false)
    private String userId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @Type(type = "dev.crashteam.charon.model.domain.PaymentStatusPsqlType")
    private RequestPaymentStatus status;
    @Column(name = "amount")
    private Long amount;
    @Column(name = "currency")
    private String currency;
    @Column(name = "provider_amount")
    private Long providerAmount;
    @Column(name = "provider_currency")
    private String providerCurrency;
    @Column(name = "external_id")
    private String externalId;
    @Column(name = "payment_system")
    private String paymentSystem;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id", referencedColumnName = "id")
    private PromoCode promoCode;
    @Column(name = "promo_code_id", insertable = false, updatable = false)
    private Long promoCodeId;
    @Column(name = "operation_id")
    private String operationId;
    @Column(name = "meta_data")
    private String metadata;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_service_id", referencedColumnName = "id")
    private PaidService paidService;
    @Column(name = "paid_service_id", insertable = false, updatable = false)
    private Long paidServiceId;
    @Column(name = "month_paid")
    private Long monthPaid;
    @Column(name = "email")
    private String email;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_type_id", referencedColumnName = "id")
    private OperationType operationType;
    @Column(name = "operation_type_id", insertable = false, updatable = false)
    private Long operationTypeId;
    @CreatedDate
    @Column(name = "created")
    private LocalDateTime created;
    @LastModifiedDate
    @Column(name = "updated")
    private LocalDateTime updated;
}
