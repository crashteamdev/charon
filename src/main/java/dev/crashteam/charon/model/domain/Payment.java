package dev.crashteam.charon.model.domain;

import lombok.Data;
import lombok.ToString;
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
    @SequenceGenerator(name = "paymentHistoryIdSeqGen", sequenceName = "PAYMENT_HISTORY_ID_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "paymentHistoryIdSeqGen")
    private Long id;
    @Column(name = "payment_id")
    private String paymentId;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "status")
    private String status;
    @Column(name = "value")
    private Long value;
    @Column(name = "currency")
    private String currency;
    @Column(name = "external_id")
    private String externalId;
    @CreatedDate
    @Column(name = "created")
    private LocalDateTime created;
    @LastModifiedDate
    @Column(name = "updated")
    private LocalDateTime updated;
}
