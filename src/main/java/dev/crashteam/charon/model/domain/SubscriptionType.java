package dev.crashteam.charon.model.domain;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@ToString(callSuper = true)
@Table(name = "subscription_type")
public class SubscriptionType {

    @Id
    @SequenceGenerator(name = "subscriptionTypeIdSeq", sequenceName = "subscription_type_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subscriptionTypeIdSeq")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    private String type;
}
