package dev.crashteam.charon.model.domain;

import lombok.Data;
import lombok.ToString;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@ToString(callSuper = true)
@Table(name = "currency_rate")
public class CurrencyRate {

    @Id
    @SequenceGenerator(name = "currencyRateIdSeq", sequenceName = "currency_rate_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "currencyRateIdSeq")
    private Long id;

    @Column(name = "currency")
    private String currency;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "init_currency")
    private String initCurrency;
}
