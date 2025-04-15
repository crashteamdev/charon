package dev.crashteam.charon.model.domain;

import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_saved_payment")
public class UserSavedPayment {

    @Id
    @SequenceGenerator(name = "userSavedPaymentIdSeq", sequenceName = "user_saved_payment_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "userSavedPaymentIdSeq")
    private Long id;

    @Column(name = "retry")
    private Integer retry;

    @Column(name = "last_paid_date")
    private LocalDateTime lastPaidDate;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "payment_system")
    private String paymentSystem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_service_id", referencedColumnName = "id")
    private PaidService paidService;
    @Column(name = "paid_service_id", insertable = false, updatable = false)
    private Long paidServiceId;

    @Column(name = "month_paid")
    private Long monthPaid;
}
