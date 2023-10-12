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
    private String type;

    @Column(name = "plan")
    private String plan;

    @Column(name = "value")
    private Long value;

    @Column(name = "currency")
    private String currency;

}
