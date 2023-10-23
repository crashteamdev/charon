package dev.crashteam.charon.model.domain;


import lombok.Data;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@ToString(callSuper = true)
@Table(name = "paid_service")
public class PaidService {

    @Id
    @SequenceGenerator(name = "paidServiceIdSeq", sequenceName = "paid_service_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "paidServiceIdSeq")
    private Long id;

    @Column(name = "type")
    private Long type;

    @Column(name = "name")
    private String name;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "currency")
    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_type_id", referencedColumnName = "id")
    private SubscriptionType subscriptionType;
    @Column(name = "subscription_type_id", insertable = false, updatable = false)
    private Long subscriptionTypeId;

}
